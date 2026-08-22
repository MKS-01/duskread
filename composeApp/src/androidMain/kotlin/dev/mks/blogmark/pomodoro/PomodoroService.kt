package dev.mks.blogmark.pomodoro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keeps a focus session counting down while the app is backgrounded.
 *
 * Holds [PomodoroClock] directly rather than exposing a bound interface —
 * the UI only ever needs to read the shared state, so no call ever needs to
 * cross the Binder. Start/pause/resume/reset all arrive as plain intents,
 * including from the notification's own action buttons.
 */
class PomodoroService : Service() {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ActionPause -> pause()
            ActionResume -> resume()
            ActionReset -> stopAndReset()
            else -> start(intent?.getIntExtra(ExtraMinutes, DefaultMinutes) ?: DefaultMinutes)
        }
        return START_NOT_STICKY
    }

    private fun start(minutes: Int) {
        val total = minutes * 60
        PomodoroClock.set(PomodoroState(total, total, running = true))
        startForeground(NotificationId, buildNotification(PomodoroClock.state.value))
        tick()
    }

    private fun pause() {
        tickJob?.cancel()
        val current = PomodoroClock.state.value
        if (current.idle) return
        PomodoroClock.set(current.copy(running = false))
        updateNotification()
    }

    private fun resume() {
        val current = PomodoroClock.state.value
        if (current.idle || current.running) return
        PomodoroClock.set(current.copy(running = true))
        tick()
    }

    private fun stopAndReset() {
        tickJob?.cancel()
        PomodoroClock.set(PomodoroState())
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun tick() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(1000)
                val current = PomodoroClock.state.value
                val remaining = (current.remainingSeconds - 1).coerceAtLeast(0)
                if (remaining == 0) {
                    PomodoroClock.set(current.copy(remainingSeconds = 0, running = false))
                    updateNotification(done = true)
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                    break
                }
                PomodoroClock.set(current.copy(remainingSeconds = remaining))
                updateNotification()
            }
        }
    }

    private fun updateNotification(done: Boolean = false) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NotificationId, buildNotification(PomodoroClock.state.value, done))
    }

    private fun buildNotification(state: PomodoroState, done: Boolean = false): Notification {
        ensureChannel()

        val minutes = state.remainingSeconds / 60
        val seconds = state.remainingSeconds % 60
        val text = if (done) "Session complete" else "%d:%02d remaining".format(minutes, seconds)

        val builder = NotificationCompat.Builder(this, ChannelId)
            .setContentTitle("Focus session")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOnlyAlertOnce(true)
            .setOngoing(!done)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent())

        if (!done) {
            val toggle = if (state.running) {
                NotificationCompat.Action(0, "Pause", actionIntent(ActionPause))
            } else {
                NotificationCompat.Action(0, "Resume", actionIntent(ActionResume))
            }
            builder.addAction(toggle)
            builder.addAction(NotificationCompat.Action(0, "Reset", actionIntent(ActionReset)))
        }

        return builder.build()
    }

    private fun actionIntent(action: String): PendingIntent {
        val intent = Intent(this, PomodoroService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Tapping the notification body should bring the app back — see the identical note in `ReaderPlaybackService`. */
    private fun contentIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?: return null
        return PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(ChannelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(ChannelId, "Focus timer", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ActionPause = "dev.mks.blogmark.pomodoro.PAUSE"
        const val ActionResume = "dev.mks.blogmark.pomodoro.RESUME"
        const val ActionReset = "dev.mks.blogmark.pomodoro.RESET"
        const val ExtraMinutes = "minutes"
        private const val DefaultMinutes = 25
        private const val ChannelId = "pomodoro"
        private const val NotificationId = 1001
    }
}
