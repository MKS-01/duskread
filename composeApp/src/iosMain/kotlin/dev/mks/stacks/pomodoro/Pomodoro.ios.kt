package dev.mks.stacks.pomodoro

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPomodoroController(): PomodoroController = SimplePomodoroController
