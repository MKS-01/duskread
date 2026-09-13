package dev.mks.duskread.speech

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import dev.mks.duskread.ui.common.ToastRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Runs a read's synthesis in a foreground service with a real notification, for the same
 * reason `ReaderPlaybackService` does.
 */
class SpeechPlaybackService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Built once, in [onCreate] rather than lazily on the first ActionPlay.
    private var speaker: SystemSpeaker? = null
    private var job: Job? = null

    private lateinit var session: MediaSessionCompat
    private var title: String = ""

    override fun onCreate() {
        super.onCreate()
        speaker = SystemSpeaker(applicationContext)
        session = MediaSessionCompat(this, "SpeechPlaybackService").apply {
            setCallback(
                object : MediaSessionCompat.Callback() {
                    override fun onStop() = stopAndRelease()
                },
            )
            isActive = true
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(session, intent)
        when (intent?.action) {
            ActionStop -> stopAndRelease()
            ActionPlay -> {
                val key = intent.getStringExtra(ExtraKey)
                val requestedTitle = intent.getStringExtra(ExtraTitle)
                val text = intent.getStringExtra(ExtraText)
                if (key != null && requestedTitle != null && text != null) start(key, requestedTitle, text)
            }
        }
        return START_NOT_STICKY
    }

    private fun start(key: String, requestedTitle: String, text: String) {
        job?.cancel()
        speaker?.stop()
        title = requestedTitle

        // Same claim-first-swap-later shape `ReaderPlaybackService` uses.
        startForeground(NotificationId, buildNotification())
        publish(SpeechNowPlaying(key, requestedTitle, fraction = 0f, playing = true))

        val engine = speaker ?: return
        job = scope.launch {
            val outcome = runCatching {
                engine.speak(requestedTitle, text).collect { progress ->
                    publish(SpeechNowPlaying(key, requestedTitle, progress.fraction, playing = true))
                }
            }

            // A read that cannot happen has to say so.
            outcome.exceptionOrNull()?.let { failure ->
                if (failure !is CancellationException) {
                    ToastRequest.show(failure.message ?: "Couldn't read this aloud.")
                }
            }
            // Reached on natural completion, on failure, and when [job] is cancelled by a
            // newer `start()` superseding this one.
            if (SpeechSession.state.value?.key == key) stopAndRelease()
        }
    }

    private fun publish(state: SpeechNowPlaying) {
        SpeechSession.publish(state)
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_STOP)
                .setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build(),
        )
    }

    private fun stopAndRelease() {
        job?.cancel()
        job = null
        speaker?.stop()
        SpeechSession.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Swiping the app away from Recents should stop the read, not leave it talking silently. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        stopAndRelease()
        super.onTaskRemoved(rootIntent)
    }

    private fun buildNotification(): Notification {
        ensureChannel()

        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, SpeechPlaybackService::class.java).setAction(ActionStop),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, ChannelId)
            .setContentTitle(title)
            .setContentText("Reading aloud")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOnlyAlertOnce(true)
            .setOngoing(false)
            .setContentIntent(contentIntent())
            .setDeleteIntent(stopIntent)
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_pause, "Stop", stopIntent))
            .setStyle(MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0))
            .build()
    }

    /**
     * Tapping the notification body reopens the app wherever it already was.
     */
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
                NotificationChannel(ChannelId, "Reading aloud", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    override fun onDestroy() {
        job?.cancel()
        speaker?.release()
        speaker = null
        session.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ActionPlay = "dev.mks.duskread.speech.PLAY"
        const val ActionStop = "dev.mks.duskread.speech.STOP"
        const val ExtraKey = "key"
        const val ExtraTitle = "title"
        const val ExtraText = "text"
        private const val ChannelId = "speech_playback"
        private const val NotificationId = 1003
    }
}
