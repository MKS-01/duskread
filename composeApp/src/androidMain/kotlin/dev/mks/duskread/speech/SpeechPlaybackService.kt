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
import kotlinx.coroutines.isActive
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
    private var inQueue = false

    override fun onCreate() {
        super.onCreate()
        speaker = SystemSpeaker(applicationContext)
        session = MediaSessionCompat(this, "SpeechPlaybackService").apply {
            setCallback(
                object : MediaSessionCompat.Callback() {
                    override fun onStop() = stopAndRelease()

                    override fun onSkipToNext() = skip()
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
            ActionNext -> skip()
            ActionPlay -> {
                val key = intent.getStringExtra(ExtraKey)
                val requestedTitle = intent.getStringExtra(ExtraTitle)
                val text = intent.getStringExtra(ExtraText)
                if (key != null && requestedTitle != null && text != null) {
                    start(
                        SpeechSession.Request(
                            key = key,
                            title = requestedTitle,
                            text = text,
                            position = intent.getIntExtra(ExtraPosition, 0),
                            total = intent.getIntExtra(ExtraTotal, 0),
                        ),
                    )
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun start(request: SpeechSession.Request) {
        job?.cancel()
        speaker?.stop()
        title = request.title
        inQueue = request.total > 1

        // Same claim-first-swap-later shape `ReaderPlaybackService` uses; called again for
        // each post in a queue, which only swaps the notification.
        startForeground(NotificationId, buildNotification())
        publish(nowPlaying(request, 0f))

        val engine = speaker ?: return
        job = scope.launch {
            val outcome = runCatching {
                engine.speak(request.title, request.text).collect { progress ->
                    publish(nowPlaying(request, progress.fraction))
                }
            }
            // Superseded by a newer start, a skip or a stop: none of those is an ending.
            if (!isActive) return@launch

            // A read that cannot happen has to say so.
            outcome.exceptionOrNull()?.let { failure ->
                if (failure !is CancellationException) {
                    ToastRequest.show(failure.message ?: "Couldn't read this aloud.")
                }
            }
            // Moved on here, inside the service, because the screen may well be off.
            val next = SpeechSession.advance(request.key, heard = outcome.isSuccess)
            when {
                next != null -> start(next)
                SpeechSession.state.value?.key == request.key -> stopAndRelease()
            }
        }
    }

    private fun skip() {
        val key = SpeechSession.state.value?.key ?: return
        job?.cancel()
        speaker?.stop()
        job = scope.launch {
            val next = SpeechSession.advance(key, heard = false)
            if (next != null) start(next) else stopAndRelease()
        }
    }

    private fun nowPlaying(request: SpeechSession.Request, fraction: Float) = SpeechNowPlaying(request.key, request.title, fraction, playing = true, position = request.position, total = request.total)

    private fun publish(state: SpeechNowPlaying) {
        SpeechSession.publish(state)
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_STOP or if (inQueue) PlaybackStateCompat.ACTION_SKIP_TO_NEXT else 0L)
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

        val builder = NotificationCompat.Builder(this, ChannelId)
            .setContentTitle(title)
            .setContentText("Reading aloud")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOnlyAlertOnce(true)
            .setOngoing(false)
            .setContentIntent(contentIntent())
            .setDeleteIntent(stopIntent)
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_pause, "Stop", stopIntent))
        if (!inQueue) {
            return builder.setStyle(MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0)).build()
        }
        val nextIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, SpeechPlaybackService::class.java).setAction(ActionNext),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return builder
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", nextIntent))
            .setStyle(MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1))
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
        const val ActionNext = "dev.mks.duskread.speech.NEXT"
        const val ExtraKey = "key"
        const val ExtraTitle = "title"
        const val ExtraText = "text"
        const val ExtraPosition = "position"
        const val ExtraTotal = "total"
        private const val ChannelId = "speech_playback"
        private const val NotificationId = 1003
    }
}
