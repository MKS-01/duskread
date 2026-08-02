package dev.mks.stacks.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private class NoOpAudioPlayer : AudioPlayer {
    override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState())

    override fun play(item: ReadItem) = Unit

    override fun togglePlayPause() = Unit

    override fun seekTo(seconds: Float) = Unit

    override fun stop() = Unit
}

@Composable
actual fun rememberAudioPlayer(repository: ReadRepository): AudioPlayer = remember { NoOpAudioPlayer() }
