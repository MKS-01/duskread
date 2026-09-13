package dev.mks.duskread.ui.pomodoro

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lets something outside Compose ask for the full-screen Focus view. The same handoff
 * shape as `HomeTabRequest` and `SharedLinkRequest`, and for the same reason.
 */
object FocusRequest {
    private val _open = MutableStateFlow(false)
    val open: StateFlow<Boolean> = _open

    fun request() {
        _open.value = true
    }

    fun consume() {
        _open.value = false
    }
}

/** Read by `MainActivity` and set on the widget's focusing-cell intent. */
const val OpenFocusExtra = "dev.mks.duskread.OPEN_FOCUS"
