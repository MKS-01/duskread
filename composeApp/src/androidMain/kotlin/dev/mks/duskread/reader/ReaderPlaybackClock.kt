package dev.mks.duskread.reader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared between [ReaderPlaybackService] and [AndroidAudioPlayer] so every UI entry point
 * reads the same live session.
 */
internal object ReaderPlaybackClock {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun set(state: PlaybackState) {
        _state.value = state
    }
}

/**
 * Which [ReadItem] `AndroidAudioPlayer` last asked the service to play — held here rather
 * than as a field on `AndroidAudioPlayer` itself.
 */
internal object CurrentReaderItem {
    private val _item = MutableStateFlow<ReadItem?>(null)
    val item: StateFlow<ReadItem?> = _item.asStateFlow()

    fun set(item: ReadItem?) {
        _item.value = item
    }
}
