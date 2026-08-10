package dev.mks.stacks.reader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared between [ReaderPlaybackService] and [AndroidAudioPlayer] so every UI
 * entry point reads the same live session. Does not carry a [ReadItem], only the transport state;
 * [AndroidAudioPlayer] attaches whichever item it last asked the service to
 * play, since the service itself only ever sees a resolved URI.
 */
internal object ReaderPlaybackClock {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun set(state: PlaybackState) {
        _state.value = state
    }
}

/**
 * Which [ReadItem] `AndroidAudioPlayer` last asked the service to play — held
 * here rather than as a field on `AndroidAudioPlayer` itself, because that
 * class is recreated (via `remember`) every time the Readback tab leaves and
 * re-enters composition (switching tabs unmounts it). A plain instance field
 * would reset to null on that recreation even though the service keeps
 * playing underneath; this survives it the same way [ReaderPlaybackClock]
 * does.
 */
internal object CurrentReaderItem {
    private val _item = MutableStateFlow<ReadItem?>(null)
    val item: StateFlow<ReadItem?> = _item.asStateFlow()

    fun set(item: ReadItem?) {
        _item.value = item
    }
}
