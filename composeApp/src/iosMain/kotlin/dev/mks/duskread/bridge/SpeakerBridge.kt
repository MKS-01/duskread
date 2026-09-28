package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.links.latestPosts
import dev.mks.duskread.links.loadArticle
import dev.mks.duskread.speech.SpeakerState
import dev.mks.duskread.speech.SpeechNowPlaying
import dev.mks.duskread.speech.SpeechSession
import dev.mks.duskread.speech.iosSpeaker
import dev.mks.duskread.speech.weekListenQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Reading an article aloud. The whole read lives behind one call: Swift hands over a URL
 * and a title, and this fetches the article, extracts its text and speaks it.
 */
class SpeakerBridge internal constructor(private val graph: AppGraph) {
    private val speaker = iosSpeaker()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var reading: Job? = null
    private var skipped = false

    /** `Ready`, or the reason it is not — the text is shown as written. */
    fun status(): String? = when (val state = speaker.state) {
        is SpeakerState.Ready -> null
        is SpeakerState.NeedsVoice -> state.detail
        is SpeakerState.Unavailable -> state.reason
    }

    fun isReady(): Boolean = speaker.state is SpeakerState.Ready

    /**
     * Fetches [url] and speaks it.
     */
    fun speak(
        url: String,
        title: String,
        onProgress: (Float) -> Unit,
        onFinished: (String?) -> Unit,
    ) {
        stop()
        reading = scope.launch {
            val article = runCatching { loadArticle(graph.http, url, null, null) }.getOrNull()
            if (article == null || article.text.isBlank()) {
                onFinished("Could not read that page.")
                return@launch
            }
            runCatching {
                speaker.speak(title, article.text).collect { onProgress(it.fraction) }
            }
            // `runCatching` swallows the cancellation a stop sends, so without this a
            // stopped read would report itself finished.
            if (!isActive) return@launch
            onFinished(null)
        }
    }

    /**
     * Latest's unread posts, one after another. False when the week is all read. Driven
     * here rather than through `SpeechSession`, which only Android's service needs.
     */
    fun listenToTheWeek(
        now: Long,
        onProgress: (SpeechNowPlaying) -> Unit,
        onFinished: () -> Unit,
    ): Boolean {
        val items = latestPosts(graph.feeds.feeds, graph.feedPosts, graph.links, now)
        val queue = weekListenQueue(items, graph.feeds, graph.feedPosts, graph.links, graph.signals, graph.http) ?: return false
        stop()
        reading = scope.launch {
            var request = queue.next(this)
            while (request != null) {
                val current: SpeechSession.Request = request
                skipped = false
                onProgress(nowPlaying(current, 0f))
                val outcome = runCatching {
                    speaker.speak(current.title, current.text).collect { onProgress(nowPlaying(current, it.fraction)) }
                }
                if (!isActive) return@launch
                // A skip ends the utterance the same way finishing does; only the flag
                // tells them apart.
                if (outcome.isSuccess && !skipped) queue.heard(current.key)
                request = queue.next(this)
            }
            onFinished()
        }
        return true
    }

    /** On to the next post; the post skipped stays unread. */
    fun skip() {
        if (reading?.isActive != true) return
        skipped = true
        speaker.stop()
    }

    private fun nowPlaying(request: SpeechSession.Request, fraction: Float) = SpeechNowPlaying(request.key, request.title, fraction, playing = true, position = request.position, total = request.total)

    fun pause() = speaker.pause()

    fun resume() = speaker.resume()

    fun stop() {
        reading?.cancel()
        reading = null
        speaker.stop()
    }
}
