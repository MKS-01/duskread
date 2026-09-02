package dev.mks.duskread.speech

import androidx.compose.runtime.Composable

/**
 * No speech engine here, the same answer `Summariser` gives on this target.
 *
 * Not a stub waiting to be filled in. A browser tab has the Web Speech API, but no foreground service to keep
 * speaking once the tab is backgrounded — which is the whole point of
 * reading an article aloud.
 */
@Composable
actual fun rememberSpeaker(voice: VoiceChoice): Speaker = UnavailableSpeaker

actual fun speechSupported(): Boolean = false
