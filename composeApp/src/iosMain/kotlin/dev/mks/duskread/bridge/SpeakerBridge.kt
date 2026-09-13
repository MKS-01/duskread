package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.links.loadArticle
import dev.mks.duskread.speech.SpeakerState
import dev.mks.duskread.speech.iosSpeaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Reading an article aloud. The whole read lives behind one call: Swift hands over a URL
 * and a title, and this fetches the article, extracts its text and speaks it.
 */
class SpeakerBridge internal constructor(private val graph: AppGraph) {
    private val speaker = iosSpeaker()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var reading: Job? = null

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
            onFinished(null)
        }
    }

    fun pause() = speaker.pause()

    fun resume() = speaker.resume()

    fun stop() {
        reading?.cancel()
        reading = null
        speaker.stop()
    }
}
