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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * The platform's own text-to-speech, which is already on the phone.
 */
internal class SystemSpeaker(context: Context) : Speaker {
    private var engine: TextToSpeech? = null

    override var state: SpeakerState by mutableStateOf(SpeakerState.Unavailable("Starting up…"))
        private set

    /**
     * Settles once `onInit` has answered, so [speak] can wait for the engine rather than
     * refuse the read.
     */
    private val ready = CompletableDeferred<SpeakerState>()

    init {
        // The constructor's callback is the only way to learn whether the engine came up;
        // there is no synchronous form of this question.
        engine = TextToSpeech(context.applicationContext) { status ->
            val settled = if (status == TextToSpeech.SUCCESS) evaluate() else SpeakerState.Unavailable(NoEngine)
            state = settled
            ready.complete(settled)
        }
    }

    override suspend fun refresh() {
        state = if (engine == null) {
            SpeakerState.Unavailable(NoEngine)
        } else {
            // Same wait as [speak]: asked before `onInit` has landed, `setLanguage`
            // answers for an engine that isn't up yet.
            withTimeoutOrNull(InitTimeoutMs) { ready.await() }
            evaluate()
        }
    }

    /**
     * Whether a voice for the device's own language is actually installed.
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
     * Speaks the article in chunks, reporting the end of each one. Chunking is not an
     * optimisation.
     */
    override fun speak(title: String, text: String): Flow<SpeechProgress> = callbackFlow {
        val tts = engine
        if (tts == null) {
            close(IllegalStateException(NoEngine))
            return@callbackFlow
        }

        // Waited for, not tested — see [ready].
        val settled = withTimeoutOrNull(InitTimeoutMs) { ready.await() }
            ?: SpeakerState.Unavailable("The voice engine didn't start.")

        // Closed *with* the reason rather than silently: the caller shows it, and
        // "nothing happened" is the one outcome a reader cannot act on.
        when (settled) {
            is SpeakerState.Ready -> Unit
            is SpeakerState.NeedsVoice -> {
                close(IllegalStateException(settled.detail))
                return@callbackFlow
            }
            is SpeakerState.Unavailable -> {
                close(IllegalStateException(settled.reason))
                return@callbackFlow
            }
        }

        // The title is read first and counted as part of the whole, so the progress bar
        // starts where the audio starts.
        val chunks = chunk("$title. \n\n$text")
        val total = chunks.sumOf { it.length }

        // Where each chunk begins in the whole, so a word offset reported within a chunk
        // can be turned into an offset through the article.
        val offsets = chunks.runningFold(0) { acc, part -> acc + part.length }

        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                /**
                 * Per-word progress, which is the only thing that makes this look alive.
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

            // Checked, because a refused `speak` is silent.
            if (tts.speak(part, mode, null, index.toString()) == TextToSpeech.ERROR) {
                close(IllegalStateException("The voice engine refused to speak this."))
                return@callbackFlow
            }
        }

        // Leaving the screen stops the audio. Without this the queue outlives the
        // collector and keeps reading an article nobody is looking at.
        awaitClose { tts.stop() }
    }

    override fun pause() {
        // The platform has no pause, only stop — the queue is discarded, not held.
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

            // A single sentence longer than the limit is rare but real — a wall-of-text
            // paragraph with no punctuation — so it is cut hard rather than dropped.
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
         * Well under the platform's own cap.
         */
        const val SafeChunk = 3_500

        /**
         * How long to wait for `onInit` before calling the engine wedged. A cold Google
         * TTS takes a second or two; nothing that has not answered in five is going to.
         */
        const val InitTimeoutMs = 5_000L

        /** Keeps the terminator with the sentence it ends, so the pause lands after the full stop. */
        val SentenceEnd = Regex("(?<=[.!?])\\s+")
    }
}

/**
 * The speaker for [voice], torn down when it leaves the composition.
 */
@Composable
actual fun rememberSpeaker(voice: VoiceChoice): Speaker {
    val context = LocalContext.current

    // Every voice this app offers speaks through the platform engine.
    val speaker = remember(context) { SystemSpeaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.release() } }
    return speaker
}

actual fun speechSupported(): Boolean = true
