package dev.mks.duskread.speech

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Foundation.NSRange
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.darwin.NSObject

/**
 * The phone reading an article aloud, through `AVSpeechSynthesizer`.
 */
internal class SystemSpeaker : Speaker {
    private val synthesizer = AVSpeechSynthesizer()

    /**
     * A plain field rather than snapshot state: the Compose UI does not run on this
     * platform, and the SwiftUI shell reads it through the bridge.
     */
    override var state: SpeakerState = evaluate()
        private set

    override suspend fun refresh() {
        state = evaluate()
    }

    /**
     * Whether a voice for the device's own language exists. iOS answers this
     * synchronously, which is the one place this is simpler than the Android actual.
     */
    private fun evaluate(): SpeakerState {
        val language = NSLocale.currentLocale.languageCode
        val voice = AVSpeechSynthesisVoice.voiceWithLanguage(language)
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
        return if (voice == null) {
            SpeakerState.NeedsVoice("This phone has no voice for $language.")
        } else {
            SpeakerState.Ready
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun speak(title: String, text: String): Flow<SpeechProgress> = callbackFlow {
        if (text.isBlank()) {
            close()
            return@callbackFlow
        }

        // Playback, not ambient: an article read aloud is the thing the reader is doing.
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            // `setActive` is bound as an extension, not a member, so it needs its own
            // import.
            session.setActive(true, null)
        }

        val total = text.length

        val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
            override fun speechSynthesizer(
                synthesizer: AVSpeechSynthesizer,
                willSpeakRangeOfSpeechString: kotlinx.cinterop.CValue<NSRange>,
                utterance: AVSpeechUtterance,
            ) {
                // Cumulative offset, not a percentage, so a caller can highlight the
                // sentence being spoken as well as draw a bar.
                val spoken = willSpeakRangeOfSpeechString.useContents {
                    (location.toInt() + length.toInt()).coerceIn(0, total)
                }
                trySend(SpeechProgress(spoken, total))
            }

            @ObjCSignatureOverride
            override fun speechSynthesizer(
                synthesizer: AVSpeechSynthesizer,
                didFinishSpeechUtterance: AVSpeechUtterance,
            ) {
                trySend(SpeechProgress(total, total))
                close()
            }

            @ObjCSignatureOverride
            override fun speechSynthesizer(
                synthesizer: AVSpeechSynthesizer,
                didCancelSpeechUtterance: AVSpeechUtterance,
            ) {
                close()
            }
        }

        synthesizer.delegate = delegate

        val utterance = AVSpeechUtterance.speechUtteranceWithString(text).apply {
            val language = NSLocale.currentLocale.languageCode
            voice = AVSpeechSynthesisVoice.voiceWithLanguage(language)
                ?: AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
            rate = AVSpeechUtteranceDefaultSpeechRate
        }

        trySend(SpeechProgress(0, total))
        synthesizer.speakUtterance(utterance)

        // Collecting stops the utterance — that is what makes leaving a screen
        // mid-article silence it without anyone remembering to call stop().
        awaitClose {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
            synthesizer.delegate = null
        }
    }

    override fun pause() {
        synthesizer.pauseSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    override fun resume() {
        synthesizer.continueSpeaking()
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }
}

@Composable
actual fun rememberSpeaker(voice: VoiceChoice): Speaker = remember(voice.engine) { SystemSpeaker() }

/**
 * The same speaker, reachable without a composition.
 */
internal fun iosSpeaker(): Speaker = SystemSpeaker()

actual fun speechSupported(): Boolean = true
