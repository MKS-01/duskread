package dev.mks.duskread.speech

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Reading an article aloud, on this device.
 *
 * Until now "hear posts read back as audio" was true only by delegation: the
 * separate readback project generated a WAV on a laptop, a sync script copied
 * it onto the phone, and `reader/` played the file. That works beautifully for
 * the one person who runs the script and not at all for anyone else, which is
 * why the Readback tab is hidden by default now. This is the half that makes
 * the pillar true for everybody — the phone speaks the article itself.
 *
 * Shaped after `Summariser` rather than invented fresh: same expect/actual
 * split, same "is this platform capable at all" constant separate from "is
 * this engine ready", same do-nothing object for the platforms that have no
 * engine. That pattern is already proven here against a far more awkward
 * dependency than a TTS service, and a second shape for the same problem would
 * only be a second thing to learn.
 *
 * The text is [dev.mks.duskread.links.Article.text], which already exists and
 * whose own KDoc calls it "what a readback pass would speak".
 */
interface Speaker {
    val state: SpeakerState

    /** Re-asks the system what it can do. The answer changes without warning — a voice pack can be uninstalled. */
    suspend fun refresh()

    /**
     * Speaks [text], emitting progress as it goes.
     *
     * Cumulative character offset rather than a percentage, so a caller can
     * highlight the sentence being spoken as well as draw a bar. Collecting
     * stops the utterance, which is what makes leaving a screen mid-article
     * silence it without anyone having to remember to call [stop].
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
 *
 * Three states rather than a boolean because the fixes are different and only
 * one of them is the reader's to make: a missing voice is a download, a
 * platform with no engine is nothing anyone can do from here, and "ready" is
 * the only one that should show a play button.
 */
sealed class SpeakerState {
    data object Ready : SpeakerState()

    /** The engine exists but has no usable voice installed. [detail] is shown as-is. */
    data class NeedsVoice(val detail: String) : SpeakerState()

    data class Unavailable(val reason: String) : SpeakerState()
}

/**
 * Which voice reads, as the reader chose it in Settings.
 *
 * [ReadbackLibrary] is the odd one out and deliberately so: it is not a speech
 * engine at all, it is the synced WAV library `reader/` plays. It sits in the
 * same list because from the reader's side it answers the same question —
 * "what do I hear when I press play" — and splitting it into a second setting
 * elsewhere would be an accurate model of the code and a confusing one of the
 * app. It is only offered when the Readback tab is switched on.
 */
enum class VoiceChoice(val label: String, val detail: String) {
    System("System voice", "Instant · nothing to download"),
    ReadbackLibrary("Readback library", "Audio synced from readback"),
    ;

    /**
     * The engine that speaks text this voice has no recording for.
     *
     * [ReadbackLibrary] only has audio for articles readback was actually run
     * over, which is never true of a link saved a minute ago. Rather than
     * refuse those — the reader chose a *preference*, not a restriction — it
     * falls through to the system voice. Without this, picking the readback
     * library would silently disable reading aloud everywhere except the one
     * tab it applies to.
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
 * Whether this platform can speak at all — not whether a voice is installed,
 * which only [Speaker.state] can answer.
 *
 * A constant per platform, so a screen can decide whether to offer the control
 * without binding to a system service it may never use. The same split, for
 * the same reason, as `summariesSupported`.
 */
expect fun speechSupported(): Boolean
