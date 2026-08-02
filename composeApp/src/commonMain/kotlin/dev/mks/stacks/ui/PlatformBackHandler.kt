package dev.mks.stacks.ui

import androidx.compose.runtime.Composable

/**
 * Intercepts the platform back gesture.
 *
 * Android is the only target where this means anything today — the others have
 * no system back — but declaring it here keeps the navigation logic in common
 * code instead of forking the shell per platform.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
