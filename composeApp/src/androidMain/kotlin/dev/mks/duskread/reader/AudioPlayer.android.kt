package dev.mks.duskread.reader

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Talks to [ReaderPlaybackService] purely through intents — a fire-and-forget shape.
 */
internal class AndroidAudioPlayer(
    private val context: Context,
    private val repository: AndroidReadRepository,
    scope: CoroutineScope,
) : AudioPlayer {
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state

    init {
        scope.launch {
            combine(ReaderPlaybackClock.state, CurrentReaderItem.item) { clockState, item ->
                clockState.copy(item = item)
            }.collect { _state.value = it }
        }
    }

    override fun play(item: ReadItem) {
        val uri = repository.audioUri(item) ?: return
        CurrentReaderItem.set(item)
        val intent = Intent(context, ReaderPlaybackService::class.java)
            .setAction(ReaderPlaybackService.ActionPlay)
            .putExtra(ReaderPlaybackService.ExtraUri, uri.toString())
            .putExtra(ReaderPlaybackService.ExtraTitle, item.title)
        context.startForegroundService(intent)
    }

    override fun togglePlayPause() = sendAction(ReaderPlaybackService.ActionToggle)

    override fun seekTo(seconds: Float) {
        context.startService(
            Intent(context, ReaderPlaybackService::class.java)
                .setAction(ReaderPlaybackService.ActionSeek)
                .putExtra(ReaderPlaybackService.ExtraPositionMs, (seconds * 1000).toLong()),
        )
    }

    override fun stop() {
        CurrentReaderItem.set(null)
        sendAction(ReaderPlaybackService.ActionStop)
    }

    private fun sendAction(action: String) {
        context.startService(Intent(context, ReaderPlaybackService::class.java).setAction(action))
    }
}

@Composable
actual fun rememberAudioPlayer(repository: ReadRepository): AudioPlayer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val androidRepository = repository as AndroidReadRepository
    return remember(context, repository) { AndroidAudioPlayer(context, androidRepository, scope) }
}
