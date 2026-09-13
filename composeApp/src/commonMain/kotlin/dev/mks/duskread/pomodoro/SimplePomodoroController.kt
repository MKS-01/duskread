package dev.mks.duskread.pomodoro

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A plain coroutine tick, lost if the process dies.
 */
internal object SimplePomodoroController : PomodoroController {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    override val state: StateFlow<PomodoroState> = PomodoroClock.state
    private var job: Job? = null

    override fun start(minutes: Int) {
        job?.cancel()
        val total = minutes * 60
        PomodoroClock.set(PomodoroState(total, total, running = true))
        tick()
    }

    override fun pause() {
        job?.cancel()
        val current = state.value
        if (current.idle) return
        PomodoroClock.set(current.copy(running = false))
    }

    override fun resume() {
        val current = state.value
        if (current.idle || current.running) return
        PomodoroClock.set(current.copy(running = true))
        tick()
    }

    override fun reset() {
        job?.cancel()
        PomodoroClock.set(PomodoroState())
    }

    private fun tick() {
        job?.cancel()
        job = scope.launch {
            while (true) {
                delay(1000)
                val current = state.value
                val remaining = (current.remainingSeconds - 1).coerceAtLeast(0)
                if (remaining == 0) {
                    PomodoroClock.set(current.copy(remainingSeconds = 0, running = false))
                    break
                }
                PomodoroClock.set(current.copy(remainingSeconds = remaining))
            }
        }
    }
}
