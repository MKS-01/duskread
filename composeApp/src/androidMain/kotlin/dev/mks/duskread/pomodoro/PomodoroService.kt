package dev.mks.duskread.pomodoro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.mks.duskread.widget.WidgetState
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
        publishToWidget()
        tick()
    }

    private fun pause() {
        tickJob?.cancel()
        val current = PomodoroClock.state.value
        if (current.idle) return
        PomodoroClock.set(current.copy(running = false))
        updateNotification()
        publishToWidget()
    }

    private fun resume() {
        val current = PomodoroClock.state.value
        if (current.idle || current.running) return
        PomodoroClock.set(current.copy(running = true))
        publishToWidget()
        tick()
    }

    private fun stopAndReset() {
        tickJob?.cancel()
        PomodoroClock.set(PomodoroState())
        publishToWidget()
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
                    publishToWidget()
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                    break
                }
                PomodoroClock.set(current.copy(remainingSeconds = remaining))
                updateNotification()
            }
        }
    }

    /**
     * Hands the home-screen widget the session's deadline so it can draw a
     * countdown the system ticks for itself.
     *
     * Called only from the five transitions — start, pause, resume, reset,
     * finish — and deliberately *not* from [tick]. The notification has to be
     * rewritten every second because it shows a number we compute; the widget
     * does not, because it shows a number the launcher computes from a
     * deadline. Publishing here per-second would throw that away and make the
     * widget the most expensive thing in the app.
     */
    private fun publishToWidget() {
        val state = PomodoroClock.state.value
        WidgetState.setFocus(this, state.remainingSeconds, state.running)
    }

    private fun updateNotification(done: Boolean = false) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NotificationId, buildNotification(PomodoroClock.state.value, done))
    }

    private fun buildNotification(state: PomodoroState, done: Boolean = false): Notification {
        ensureChannels()

        val minutes = state.remainingSeconds / 60
        val seconds = state.remainingSeconds % 60
        val text = if (done) "Session complete" else "%d:%02d remaining".format(minutes, seconds)

        val builder = NotificationCompat.Builder(this, if (done) DoneChannelId else ChannelId)
            .setContentTitle("Focus session")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOnlyAlertOnce(true)
            .setOngoing(!done)
            .setAutoCancel(done)
            .setPriority(if (done) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
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

    /**
     * Two channels, not one: the ticking countdown updates once a second and
     * has to stay silent (IMPORTANCE_LOW, no vibration) or it would buzz the
     * reader's pocket all session long. "Session complete" is the one moment
     * that should actually interrupt them, and a channel's importance can't
     * be changed after creation — so it gets its own HIGH-importance channel
     * with vibration built in rather than a one-off manual vibrate() call.
     */
    private fun ensureChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(ChannelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(ChannelId, "Focus timer", NotificationManager.IMPORTANCE_LOW),
            )
        }
        if (manager.getNotificationChannel(DoneChannelId) == null) {
            val channel = NotificationChannel(DoneChannelId, "Focus session complete", NotificationManager.IMPORTANCE_HIGH)
            channel.enableVibration(true)
            channel.vibrationPattern = DoneVibrationPattern
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ActionPause = "dev.mks.duskread.pomodoro.PAUSE"
        const val ActionResume = "dev.mks.duskread.pomodoro.RESUME"
        const val ActionReset = "dev.mks.duskread.pomodoro.RESET"
        const val ExtraMinutes = "minutes"
        private const val DefaultMinutes = 25
        private const val ChannelId = "pomodoro"
        private const val DoneChannelId = "pomodoro_done"
        private const val NotificationId = 1001
        private val DoneVibrationPattern = longArrayOf(0, 400, 200, 400)
    }
}
