package dev.mks.blogmark.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lets Android-only entry points outside Compose — right now just a tapped
 * notification — ask [HomeScreen] to switch tabs, without either side
 * depending on the other. `MainActivity` lives in the host `androidApp`
 * module and reads the intent extra; `ReaderPlaybackService` sets that extra
 * when it builds the notification's content intent. Neither can call into
 * `HomeScreen` directly, so this is the one shared handoff point.
 */
object HomeTabRequest {
    private val _target = MutableStateFlow<HomeTab?>(null)
    val target: StateFlow<HomeTab?> = _target

    fun request(tab: HomeTab) {
        _target.value = tab
    }

    fun consume() {
        _target.value = null
    }
}

/** Read by `MainActivity` and set on the Readback notification's content intent. */
const val OpenReadbackTabExtra = "dev.mks.blogmark.OPEN_READER_TAB"
