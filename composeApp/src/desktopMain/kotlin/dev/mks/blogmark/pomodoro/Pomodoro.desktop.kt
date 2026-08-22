package dev.mks.blogmark.pomodoro

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPomodoroController(): PomodoroController = SimplePomodoroController
