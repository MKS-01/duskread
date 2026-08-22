package dev.mks.blogmark.reader

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

data class PlaybackState(
    val item: ReadItem? = null,
    val playing: Boolean = false,
    val positionSec: Float = 0f,
    val durationSec: Float = 0f,
)

/**
 * Plays one [ReadItem] at a time, resolving [ReadItem.audioFilename] against
 * whatever local folder [ReadRepository] was configured with — never the
 * `audio_path` column, for the same portability reason [ReadRepository]
 * documents.
 */
interface AudioPlayer {
    val state: StateFlow<PlaybackState>

    fun play(item: ReadItem)

    fun togglePlayPause()

    fun seekTo(seconds: Float)

    fun stop()
}

@Composable
expect fun rememberAudioPlayer(repository: ReadRepository): AudioPlayer
