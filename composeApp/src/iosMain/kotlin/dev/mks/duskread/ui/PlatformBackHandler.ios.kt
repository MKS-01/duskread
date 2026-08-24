package dev.mks.duskread.ui

import androidx.compose.runtime.Composable

/** iOS back is the in-app arrow; the swipe gesture is not wired up yet. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
