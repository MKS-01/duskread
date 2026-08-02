package dev.mks.algoatlas.pomodoro

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
