package dev.mks.stacks.ui

import androidx.compose.runtime.Composable

/** Browser history is not wired to the pane state yet. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
