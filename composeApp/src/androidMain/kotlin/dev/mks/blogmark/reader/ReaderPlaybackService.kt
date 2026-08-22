package dev.mks.blogmark.reader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import dev.mks.blogmark.ui.home.OpenReadbackTabExtra
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs a read's audio in a foreground service with a real `MediaSession`, so
 * playback survives the app being backgrounded and exposes proper media
 * controls — a notification with play/pause, lock-screen transport, and
 * Bluetooth/headset buttons — rather than a plain `MediaPlayer` tied to an
 * Activity that dies with it.
 */
class ReaderPlaybackService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var player: MediaPlayer? = null
    private var progressJob: Job? = null
    private lateinit var session: MediaSessionCompat
    private var title: String = ""

    override fun onCreate() {
        super.onCreate()
        session = MediaSessionCompat(this, "ReaderPlaybackService").apply {
            setCallback(
                object : MediaSessionCompat.Callback() {
                    override fun onPlay() = resume()

                    override fun onPause() = pause()

                    override fun onSeekTo(pos: Long) = seek(pos)

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
            ActionToggle -> if (player?.isPlaying == true) pause() else resume()
            ActionSeek -> seek(intent.getLongExtra(ExtraPositionMs, 0))
            ActionStop -> stopAndRelease()
            ActionPlay -> {
                val uri = intent.getStringExtra(ExtraUri)?.let(Uri::parse)
                val requestedTitle = intent.getStringExtra(ExtraTitle)
                if (uri != null && requestedTitle != null) start(uri, requestedTitle)
            }
        }
        return START_NOT_STICKY
    }

    private fun start(uri: Uri, requestedTitle: String) {
        release()
        title = requestedTitle

        player = MediaPlayer().apply {
            setDataSource(this@ReaderPlaybackService, uri)
            setOnPreparedListener { prepared ->
                prepared.start()
                session.setMetadata(
                    MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, prepared.duration.toLong())
                        .build(),
                )
                publish(playing = true, positionMs = 0, durationMs = prepared.duration)
                startForeground(NotificationId, buildNotification(playing = true))
                tick()
            }
            setOnCompletionListener { completed ->
                progressJob?.cancel()
                publish(playing = false, positionMs = completed.duration, durationMs = completed.duration)
                stopForeground(STOP_FOREGROUND_DETACH)
            }
            prepareAsync()
        }
    }

    private fun resume() {
        val current = player ?: return
        current.start()
        publish(playing = true, positionMs = current.currentPosition, durationMs = current.duration)
        startForeground(NotificationId, buildNotification(playing = true))
        tick()
    }

    private fun pause() {
        val current = player ?: return
        current.pause()
        progressJob?.cancel()
        publish(playing = false, positionMs = current.currentPosition, durationMs = current.duration)
        startForeground(NotificationId, buildNotification(playing = false))
    }

    private fun seek(positionMs: Long) {
        val current = player ?: return
        current.seekTo(positionMs.toInt())
        publish(playing = current.isPlaying, positionMs = positionMs.toInt(), durationMs = current.duration)
    }

    /**
     * The only path that should clear [CurrentReaderItem] — [release] alone
     * is also called from [start] to tear down the *previous* player before
     * a new one begins, and clearing the current item there wiped out the
     * item [AndroidAudioPlayer] had just set a moment earlier, so the card
     * (and the pinned bar) flashed open and immediately closed again on
     * every play(). Genuinely stopping — the stop action, swiping the
     * notification away, or the app's task being killed — comes through
     * here instead.
     */
    private fun stopAndRelease() {
        release()
        CurrentReaderItem.set(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun release() {
        progressJob?.cancel()
        player?.release()
        player = null
        ReaderPlaybackClock.set(PlaybackState())
    }

    /** Swiping the app away from Recents should stop playback, not leave it running silently. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        stopAndRelease()
        super.onTaskRemoved(rootIntent)
    }

    private fun tick() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                delay(500)
                val current = player ?: break
                if (current.isPlaying) {
                    publish(playing = true, positionMs = current.currentPosition, durationMs = current.duration)
                }
            }
        }
    }

    private fun publish(playing: Boolean, positionMs: Int, durationMs: Int) {
        ReaderPlaybackClock.set(
            PlaybackState(playing = playing, positionSec = positionMs / 1000f, durationSec = durationMs / 1000f),
        )
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_STOP,
                )
                .setState(
                    if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    positionMs.toLong(),
                    1f,
                )
                .build(),
        )
    }

    private fun buildNotification(playing: Boolean): Notification {
        ensureChannel()

        val toggleIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, ReaderPlaybackService::class.java).setAction(ActionToggle),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, ReaderPlaybackService::class.java).setAction(ActionStop),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val icon = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        return NotificationCompat.Builder(this, ChannelId)
            .setContentTitle(title)
            .setContentText(if (playing) "Playing" else "Paused")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOnlyAlertOnce(true)
            // Swipeable in every state, not just paused — swiping it away is
            // how playback actually stops, via the delete intent below.
            .setOngoing(false)
            .setContentIntent(contentIntent())
            .setDeleteIntent(stopIntent)
            .addAction(NotificationCompat.Action(icon, if (playing) "Pause" else "Play", toggleIntent))
            .setStyle(MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0))
            .build()
    }

    /**
     * Tapping the notification body should bring the app back to the Readback
     * tab specifically, whether or not the process is still alive — this
     * module never references `MainActivity` directly (it lives in the host
     * `androidApp` module, and a library can't depend back on its host), so
     * the launcher intent is resolved by package instead of by class, with
     * [OpenReadbackTabExtra] carrying the "which tab" signal `MainActivity`
     * reads back out.
     */
    private fun contentIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?.putExtra(OpenReadbackTabExtra, true)
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
                NotificationChannel(ChannelId, "Readback playback", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    override fun onDestroy() {
        release()
        session.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ActionPlay = "dev.mks.blogmark.reader.PLAY"
        const val ActionToggle = "dev.mks.blogmark.reader.TOGGLE"
        const val ActionSeek = "dev.mks.blogmark.reader.SEEK"
        const val ActionStop = "dev.mks.blogmark.reader.STOP"
        const val ExtraUri = "uri"
        const val ExtraTitle = "title"
        const val ExtraPositionMs = "positionMs"
        private const val ChannelId = "reader_playback"
        private const val NotificationId = 1002
    }
}
