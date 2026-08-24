package dev.mks.duskread.pomodoro

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.StateFlow

/**
 * Talks to [PomodoroService] purely through intents — start/pause/resume/reset
 * are one-way fire-and-forget calls, and the result is read back from the
 * shared [PomodoroClock] rather than a callback.
 */
private class AndroidPomodoroController(private val context: Context) : PomodoroController {
    override val state: StateFlow<PomodoroState> = PomodoroClock.state

    override fun start(minutes: Int) {
        val intent = Intent(context, PomodoroService::class.java)
            .putExtra(PomodoroService.ExtraMinutes, minutes)
        context.startForegroundService(intent)
    }

    override fun pause() = sendAction(PomodoroService.ActionPause)

    override fun resume() = sendAction(PomodoroService.ActionResume)

    override fun reset() = sendAction(PomodoroService.ActionReset)

    private fun sendAction(action: String) {
        if (state.value.idle) return
        context.startService(Intent(context, PomodoroService::class.java).setAction(action))
    }
}

@Composable
actual fun rememberPomodoroController(): PomodoroController {
    val context = LocalContext.current
    return remember(context) { AndroidPomodoroController(context) }
}
