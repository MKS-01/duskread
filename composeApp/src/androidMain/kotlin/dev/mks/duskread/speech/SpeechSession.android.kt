package dev.mks.duskread.speech

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

/**
 * Forwards [SpeechSession.request] to [SpeechPlaybackService] purely through intents.
 */
@Composable
actual fun DriveSpeechSession() {
    val context = LocalContext.current
    val request by SpeechSession.request.collectAsState()

    // The app is in front when these are tapped, so an intent may still start the service.
    LaunchedEffect(Unit) {
        SpeechSession.skips.collect {
            context.startService(Intent(context, SpeechPlaybackService::class.java).setAction(SpeechPlaybackService.ActionNext))
        }
    }

    LaunchedEffect(request) {
        val current = request
        if (current == null) {
            // Also reached right after the service's own natural-completion or
            // explicit-stop path clears the request.
            context.startService(Intent(context, SpeechPlaybackService::class.java).setAction(SpeechPlaybackService.ActionStop))
            return@LaunchedEffect
        }
        // Already playing: the service moved to it itself, possibly with the screen off,
        // when starting a foreground service from here would be refused.
        if (current.advanced) return@LaunchedEffect

        context.startForegroundService(
            Intent(context, SpeechPlaybackService::class.java)
                .setAction(SpeechPlaybackService.ActionPlay)
                .putExtra(SpeechPlaybackService.ExtraKey, current.key)
                .putExtra(SpeechPlaybackService.ExtraTitle, current.title)
                .putExtra(SpeechPlaybackService.ExtraText, current.text)
                .putExtra(SpeechPlaybackService.ExtraPosition, current.position)
                .putExtra(SpeechPlaybackService.ExtraTotal, current.total),
        )
    }
}
