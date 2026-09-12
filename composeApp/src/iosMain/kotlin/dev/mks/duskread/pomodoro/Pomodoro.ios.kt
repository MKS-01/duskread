package dev.mks.duskread.pomodoro

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPomodoroController(): PomodoroController = SimplePomodoroController

/**
 * The same controller, reachable without a composition.
 *
 * `SimplePomodoroController` is `internal` — correctly, since nothing outside
 * the module should pick an implementation — but the SwiftUI shell needs one
 * and cannot call a `@Composable` to get it. Internal here too, so this widens
 * nothing: only the bridge in the same module can see it.
 */
internal fun iosPomodoroController(): PomodoroController = SimplePomodoroController
