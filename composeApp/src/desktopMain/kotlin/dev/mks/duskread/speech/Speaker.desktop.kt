package dev.mks.duskread.speech

import androidx.compose.runtime.Composable

/**
 * No speech engine here, the same answer `Summariser` gives on this target.
 *
 * Not a stub waiting to be filled in. A desktop already has a system reader a keystroke away, and this app
 * is used on a phone — a second implementation to keep in step would be
 * serving a case nobody has.
 */
@Composable
actual fun rememberSpeaker(voice: VoiceChoice): Speaker = UnavailableSpeaker

actual fun speechSupported(): Boolean = false
