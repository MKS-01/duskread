package dev.mks.algoatlas.ui

import androidx.compose.runtime.Composable

/** Desktop has no system back gesture — the two-pane layout is used instead. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
