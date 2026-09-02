package dev.mks.duskread.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

/**
 * The platform's own text-to-speech, which is already on the phone.
 *
 * Chosen over a neural voice for the first pass because it costs nothing —
 * no dependency, no download, no APK growth — and because it makes the whole
 * path real end to end: the article is extracted, chunked, spoken, and
 * interrupted correctly. A better-sounding voice is a swap behind [Speaker]
 * once that path is known to work, not a prerequisite for building it.
 *
 * Quality is honestly uneven. On a Pixel or a recent Samsung the Google engine
 * is decent; on a budget OEM phone with the stock engine it is flat. That is
 * the argument for the downloadable neural voice that comes next, and it is an
 * argument this class does not have to answer.
 */
internal class SystemSpeaker(context: Context) : Speaker {
    private var engine: TextToSpeech? = null

    override var state: SpeakerState by mutableStateOf(SpeakerState.Unavailable("Starting up…"))
        private set

    init {
        // The constructor's callback is the only way to learn whether the
        // engine came up; there is no synchronous form of this question.
        engine = TextToSpeech(context.applicationContext) { status ->
            state = if (status == TextToSpeech.SUCCESS) evaluate() else SpeakerState.Unavailable(NoEngine)
        }
    }

    override suspend fun refresh() {
        state = if (engine == null) SpeakerState.Unavailable(NoEngine) else evaluate()
    }

    /**
     * Whether a voice for the device's own language is actually installed.
     *
     * `setLanguage` is the only reliable way to ask — `availableLanguages` can
     * return a locale whose data has not been downloaded, and speaking then
     * fails silently with no audio and no error, which is the single worst
     * outcome available here.
     */
    private fun evaluate(): SpeakerState {
        val tts = engine ?: return SpeakerState.Unavailable(NoEngine)

        return when (tts.setLanguage(Locale.getDefault())) {
            TextToSpeech.LANG_MISSING_DATA ->
                SpeakerState.NeedsVoice("Install a voice in Android's text-to-speech settings.")

            TextToSpeech.LANG_NOT_SUPPORTED ->
                SpeakerState.NeedsVoice("This phone has no voice for ${Locale.getDefault().displayLanguage}.")

            else -> SpeakerState.Ready
        }
    }

    /**
     * Speaks the article in chunks, reporting the end of each one.
     *
     * Chunking is not an optimisation. `speak` silently drops anything past
     * [TextToSpeech.getMaxSpeechInputLength], which is around four thousand
     * characters — comfortably shorter than most articles worth listening to,
     * so a single call would read the opening and stop without saying why.
     *
     * Splitting on sentence ends rather than at a fixed offset, because the
     * engine restarts its prosody at every chunk boundary: broken mid-clause it
     * is audible as a stumble, broken after a full stop it is just a pause.
     *
     * `QUEUE_ADD` after the first chunk, so the queue plays as one continuous
     * read; the first uses `QUEUE_FLUSH` to cut off whatever was playing.
     */
    override fun speak(title: String, text: String): Flow<SpeechProgress> = callbackFlow {
        val tts = engine
        if (tts == null || state !is SpeakerState.Ready) {
            close()
            return@callbackFlow
        }

        // The title is read first and counted as part of the whole, so the
        // progress bar starts where the audio starts.
        val chunks = chunk("$title. \n\n$text")
        val total = chunks.sumOf { it.length }

        // Where each chunk begins in the whole, so a word offset reported
        // within a chunk can be turned into an offset through the article.
        val offsets = chunks.runningFold(0) { acc, part -> acc + part.length }

        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                /**
                 * Per-word progress, which is the only thing that makes this
                 * look alive.
                 *
                 * [onDone] alone fires once per chunk — up to 3,500 characters
                 * apart, which on a long article is a meter that sits still
                 * for a minute at a time and reads as a hung player. This
                 * fires for every word the engine is about to speak.
                 */
                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    val index = utteranceId?.toIntOrNull() ?: return
                    trySend(SpeechProgress(offsets[index] + end, total))
                }

                override fun onDone(utteranceId: String?) {
                    val index = utteranceId?.toIntOrNull() ?: return
                    trySend(SpeechProgress(offsets[index] + chunks[index].length, total))
                    if (index == chunks.lastIndex) close()
                }

                @Deprecated("Required by the abstract class; the two-arg form below is what actually fires.")
                override fun onError(utteranceId: String?) {
                    close()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    close()
                }
            },
        )

        trySend(SpeechProgress(0, total))

        chunks.forEachIndexed { index, part ->
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD

            // Checked, because a refused `speak` is silent. It returns ERROR
            // and simply never calls the listener, so without this the panel
            // waits at nought per cent for an utterance that was never queued
            // — indistinguishable, to the reader, from a very long article.
            if (tts.speak(part, mode, null, index.toString()) == TextToSpeech.ERROR) {
                close(IllegalStateException("The voice engine refused to speak this."))
                return@callbackFlow
            }
        }

        // Leaving the screen stops the audio. Without this the queue outlives
        // the collector and keeps reading an article nobody is looking at.
        awaitClose { tts.stop() }
    }

    override fun pause() {
        // The platform has no pause, only stop — the queue is discarded, not
        // held. Callers that need resume-in-place have to re-speak from an
        // offset, which is why `speak` reports characters rather than a
        // percentage.
        engine?.stop()
    }

    override fun resume() = Unit

    override fun stop() {
        engine?.stop()
    }

    fun release() {
        engine?.stop()
        engine?.shutdown()
        engine = null
    }

    private fun chunk(text: String): List<String> {
        val limit = TextToSpeech.getMaxSpeechInputLength().coerceAtMost(SafeChunk)
        val parts = mutableListOf<String>()
        val current = StringBuilder()

        text.split(SentenceEnd).forEach { sentence ->
            if (sentence.isBlank()) return@forEach

            // A single sentence longer than the limit is rare but real — a
            // wall-of-text paragraph with no punctuation — so it is cut hard
            // rather than dropped.
            if (sentence.length > limit) {
                if (current.isNotEmpty()) {
                    parts += current.toString()
                    current.clear()
                }
                sentence.chunked(limit).forEach { parts += it }
                return@forEach
            }

            if (current.length + sentence.length > limit) {
                parts += current.toString()
                current.clear()
            }
            current.append(sentence).append(' ')
        }

        if (current.isNotEmpty()) parts += current.toString()
        return parts.ifEmpty { listOf(text.take(limit)) }
    }

    private companion object {
        const val NoEngine = "This phone has no text-to-speech engine."

        /**
         * Well under the platform's own cap. `getMaxSpeechInputLength` is the
         * hard limit, and sitting on it means a chunk that grew by one
         * character during assembly is silently truncated.
         */
        const val SafeChunk = 3_500

        /** Keeps the terminator with the sentence it ends, so the pause lands after the full stop. */
        val SentenceEnd = Regex("(?<=[.!?])\\s+")
    }
}

/**
 * The speaker for [voice], torn down when it leaves the composition.
 *
 * A `TextToSpeech` holds a binding to a system service, and one left unbound
 * leaks it for the life of the process — the same class of hazard the
 * playback service documents. `DisposableEffect` rather than `remember` alone
 * is what closes it.
 *
 * [VoiceChoice.ReadbackLibrary] is not a speech engine — it routes playback to
 * the synced WAV library through `AudioPlayer` instead — so there is nothing
 * for this to build.
 */
@Composable
actual fun rememberSpeaker(voice: VoiceChoice): Speaker {
    val context = LocalContext.current

    // Every voice this app offers speaks through the platform engine. The
    // readback library is not a speech engine at all — playback for it routes
    // to the synced WAVs through `AudioPlayer` — and `VoiceChoice.engine` is
    // what sends spoken text here instead.
    val speaker = remember(context) { SystemSpeaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.release() } }
    return speaker
}

actual fun speechSupported(): Boolean = true
