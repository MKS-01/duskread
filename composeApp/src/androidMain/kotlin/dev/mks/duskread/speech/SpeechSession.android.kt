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

    LaunchedEffect(request) {
        val current = request
        if (current == null) {
            // Also reached right after the service's own natural-completion or
            // explicit-stop path clears the request.
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
