package dev.mks.blogmark.pomodoro

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A focus session's current shape — idle, running, or paused partway through. */
data class PomodoroState(
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val running: Boolean = false,
) {
    val idle: Boolean get() = totalSeconds == 0
}

/**
 * Holds the current session so the UI reads the same state regardless of what
 * is actually driving the countdown — a plain coroutine on most platforms, or
 * an Android foreground service, which is the only one of the two that
 * survives the app being backgrounded.
 */
object PomodoroClock {
    private val _state = MutableStateFlow(PomodoroState())
    val state: StateFlow<PomodoroState> = _state.asStateFlow()

    fun set(state: PomodoroState) {
        _state.value = state
    }
}

/** Durations offered wherever a session can be quick-started. */
val PickableMinutes = listOf(15, 25, 30)

/** `mm:ss` remaining — shared by the Focus card and the full-screen timer. */
val PomodoroState.clockLabel: String
    get() {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

interface PomodoroController {
    val state: StateFlow<PomodoroState>

    fun start(minutes: Int)

    fun pause()

    fun resume()

    fun reset()
}

/**
 * Composable for the same reason as `rememberUrlOpener` — Android needs a
 * `Context` to reach its foreground service, and that is only available from
 * composition.
 */
@Composable
expect fun rememberPomodoroController(): PomodoroController
