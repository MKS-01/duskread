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
 * Runs a read's synthesis in a foreground service with a real notification,
 * for the same reason `ReaderPlaybackService` does: TextToSpeech driven from
 * a Composable's `LaunchedEffect` stops the moment Android decides the app is
 * in the background, silently and without appeal — background execution
 * limits do not make an exception for "but a person is listening to this".
 * A foreground service is the one thing Android trusts to keep going, and it
 * comes with the obligation this pays: a notification saying so, with a way
 * to stop it, for exactly as long as it runs.
 *
 * No play/pause toggle, unlike Readback's own notification — see
 * [SystemSpeaker.pause]'s own note. The platform engine this runs on has no
 * true pause, only stop-and-restart-from-the-beginning, and a button that
 * promised to resume and instead started over would be worse than not
 * offering the button.
 */
class SpeechPlaybackService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Built once, in [onCreate] rather than lazily on the first ActionPlay,
    // so `TextToSpeech`'s async `onInit` has a head start on the network of
    // intents (app → this service) that a tap on "Read this aloud" already
    // takes a moment to arrive through. Kept across reads within the same
    // service instance rather than rebuilt per read, which is what lets a
    // second read start speaking immediately instead of re-racing `onInit`.
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

        // Same claim-first-swap-later shape `ReaderPlaybackService` uses: a
        // placeholder notification lands immediately so Android 12+'s
        // few-second window on `startForegroundService()` never lapses,
        // regardless of how long synthesis takes to actually begin.
        startForeground(NotificationId, buildNotification())
        publish(SpeechNowPlaying(key, requestedTitle, fraction = 0f, playing = true))

        val engine = speaker ?: return
        job = scope.launch {
            val outcome = runCatching {
                engine.speak(requestedTitle, text).collect { progress ->
                    publish(SpeechNowPlaying(key, requestedTitle, progress.fraction, playing = true))
                }
            }

            // A read that cannot happen has to say so. Everything the speaker
            // refuses — no engine, no voice installed, an engine that never
            // came up — used to end here as a swallowed exception, and all
            // the reader saw was the transport appearing and vanishing again.
            // A cancellation is not a failure: it is this read being
            // superseded by a newer one, which needs no announcement.
            outcome.exceptionOrNull()?.let { failure ->
                if (failure !is CancellationException) {
                    ToastRequest.show(failure.message ?: "Couldn't read this aloud.")
                }
            }
            // Reached on natural completion, on failure, and when [job] is
            // cancelled by a newer `start()` superseding this one — in the
            // last case `SpeechSession` has already moved on to the new
            // read by the time this runs, and `stopAndRelease` clearing it
            // out from under that would be exactly the bug the same-request
            // guard in `SummaryPanel`'s own effect exists to avoid. Stopping
            // is only correct when this is still the read `SpeechSession`
            // thinks is playing.
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
     * Unlike Readback's own notification there is no one right tab to
     * return to — a read can start from Saved, Following or the reader
     * itself — so this is a plain relaunch rather than one carrying a
     * "which tab" extra.
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
