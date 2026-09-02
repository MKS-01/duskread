package dev.mks.duskread.speech

import androidx.compose.runtime.Composable

/**
 * No speech engine here, the same answer `Summariser` gives on this target.
 *
 * Not a stub waiting to be filled in. iOS has `AVSpeechSynthesizer` and could speak, but nothing else in this
 * app is exercised on iOS: the summariser is a stub here too, and shipping a
 * voice nobody has listened to is worse than offering none.
 */
@Composable
actual fun rememberSpeaker(voice: VoiceChoice): Speaker = UnavailableSpeaker

actual fun speechSupported(): Boolean = false
