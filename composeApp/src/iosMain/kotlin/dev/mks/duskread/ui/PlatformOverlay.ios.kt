package dev.mks.duskread.ui

import androidx.compose.runtime.Composable

/** [rememberUrlOpener] already presents its own `SFSafariViewController` sheet — nothing to overlay. */
@Composable
actual fun PlatformOverlay(mono: Boolean) = Unit
