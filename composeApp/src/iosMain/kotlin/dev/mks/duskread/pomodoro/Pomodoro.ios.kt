package dev.mks.duskread.pomodoro

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPomodoroController(): PomodoroController = SimplePomodoroController
