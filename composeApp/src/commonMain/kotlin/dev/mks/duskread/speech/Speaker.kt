package dev.mks.duskread.speech

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Reading an article aloud, on this device.
 */
interface Speaker {
    val state: SpeakerState

    /** Re-asks the system what it can do. The answer changes without warning — a voice pack can be uninstalled. */
    suspend fun refresh()

    /**
     * Speaks [text], emitting progress as it goes.
     */
    fun speak(title: String, text: String): Flow<SpeechProgress>

    fun pause()

    fun resume()

    fun stop()
}

/** How far through, in characters of the text handed to [Speaker.speak]. */
data class SpeechProgress(val spokenChars: Int, val totalChars: Int) {
    val fraction: Float
        get() = if (totalChars <= 0) 0f else (spokenChars.toFloat() / totalChars).coerceIn(0f, 1f)
}

/**
 * Whether this engine can speak right now, and if not, what would fix it.
 */
sealed class SpeakerState {
    data object Ready : SpeakerState()

    /** The engine exists but has no usable voice installed. [detail] is shown as-is. */
    data class NeedsVoice(val detail: String) : SpeakerState()

    data class Unavailable(val reason: String) : SpeakerState()
}

/**
 * Which voice reads, as the reader chose it in Settings.
 */
enum class VoiceChoice(val label: String, val detail: String) {
    System("System voice", "Instant · nothing to download"),
    ReadbackLibrary("Readback library", "Audio synced from readback"),
    ;

    /**
     * The engine that speaks text this voice has no recording for.
     */
    val engine: VoiceChoice
        get() = if (this == ReadbackLibrary) System else this
}

/** Every platform without a speech engine: the control disappears rather than failing. */
object UnavailableSpeaker : Speaker {
    override val state: SpeakerState =
        SpeakerState.Unavailable("Reading aloud needs an Android phone.")

    override suspend fun refresh() = Unit

    override fun speak(title: String, text: String): Flow<SpeechProgress> = emptyFlow()

    override fun pause() = Unit

    override fun resume() = Unit

    override fun stop() = Unit
}

@Composable
expect fun rememberSpeaker(voice: VoiceChoice): Speaker

/**
 * Whether this platform can speak at all — not whether a voice is installed, which only
 * [Speaker.state] can answer.
 */
expect fun speechSupported(): Boolean
