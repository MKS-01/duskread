package dev.mks.duskread.ui

import androidx.compose.runtime.Composable

/**
 * Intercepts the platform back gesture.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
