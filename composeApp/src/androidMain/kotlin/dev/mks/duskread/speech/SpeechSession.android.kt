package dev.mks.duskread.speech

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

/**
 * Forwards [SpeechSession.request] to [SpeechPlaybackService] purely through
 * intents, the same fire-and-forget shape `AndroidAudioPlayer` already uses
 * to talk to `ReaderPlaybackService`. Nothing here holds playback state of
 * its own — [SpeechSession.state] is what the service publishes back into,
 * and every reader of it (the floating bar, whichever panel started the
 * read) already watches that directly.
 */
@Composable
actual fun DriveSpeechSession() {
    val context = LocalContext.current
    val request by SpeechSession.request.collectAsState()

    LaunchedEffect(request) {
        val current = request
        if (current == null) {
            // Also reached right after the service's own natural-completion
            // or explicit-stop path clears the request — a stop sent to an
            // already-stopped service is a harmless no-op, and the
            // alternative (trying to tell the difference) is not worth the
            // service having to report back which one it was.
            context.startService(Intent(context, SpeechPlaybackService::class.java).setAction(SpeechPlaybackService.ActionStop))
            return@LaunchedEffect
        }

        context.startForegroundService(
            Intent(context, SpeechPlaybackService::class.java)
                .setAction(SpeechPlaybackService.ActionPlay)
                .putExtra(SpeechPlaybackService.ExtraKey, current.key)
                .putExtra(SpeechPlaybackService.ExtraTitle, current.title)
                .putExtra(SpeechPlaybackService.ExtraText, current.text),
        )
    }
}
