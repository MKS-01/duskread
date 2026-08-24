package dev.mks.duskread.pomodoro

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A plain coroutine tick, lost if the process dies. Used on every platform
 * except Android, where a foreground service takes over so a session
 * survives backgrounding — this app is read mostly on a phone, so a fuller
 * background-execution story elsewhere is not worth the extra platform code
 * yet.
 *
 * A singleton, not one instance per composable: the chip and the full-screen
 * focus mode both call `rememberPomodoroController()`, and if each held its
 * own ticking job, pausing from one would leave the other's job running
 * unseen — the clock would keep silently draining underneath a "paused" UI.
 * One shared engine, independent of any composable's lifecycle, is what
 * makes multiple entry points to the same session safe.
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
