package dev.mks.duskread.pomodoro

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPomodoroController(): PomodoroController = SimplePomodoroController

/**
 * The same controller, reachable without a composition.
 */
internal fun iosPomodoroController(): PomodoroController = SimplePomodoroController
