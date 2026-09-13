package dev.mks.duskread.bridge

import dev.mks.duskread.pomodoro.PickableMinutes
import dev.mks.duskread.pomodoro.PomodoroController
import dev.mks.duskread.pomodoro.PomodoroState
import dev.mks.duskread.pomodoro.clockLabel
import dev.mks.duskread.pomodoro.iosPomodoroController

/**
 * The focus timer. Independent of the graph — the timer persists nothing, and a session
 * that outlives the process is not something this app claims to offer.
 */
class PomodoroBridge internal constructor() {
    private val controller: PomodoroController = iosPomodoroController()

    val pickableMinutes: List<Int> = PickableMinutes

    fun observe(onEach: (PomodoroState) -> Unit): Cancellable = controller.state.watch(onEach)

    fun start(minutes: Int) = controller.start(minutes)

    fun pause() = controller.pause()

    fun resume() = controller.resume()

    fun reset() = controller.reset()

    fun clockLabelFor(state: PomodoroState): String = state.clockLabel
}
