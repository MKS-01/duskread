package dev.mks.duskread.ui.pomodoro

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Lets something outside Compose ask for the full-screen Focus view.
 *
 * The same handoff shape as `HomeTabRequest` and `SharedLinkRequest`, and for
 * the same reason. Focus mode is a plain `remember`ed flag in `App.kt` with no
 * way in from outside, which was fine while the only way to reach it was
 * tapping Home. The home-screen widget adds a second door: tapping a session
 * already counting down should land on the timer, not on whichever tab the app
 * last showed.
 *
 * Deliberately not a tab. Focus is an overlay over Home rather than a
 * destination beside it, so it cannot be expressed as a `HomeTabRequest` and
 * needs its own one-bit channel.
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
