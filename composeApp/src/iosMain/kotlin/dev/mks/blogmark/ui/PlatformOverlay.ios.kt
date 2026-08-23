package dev.mks.blogmark.ui

import androidx.compose.runtime.Composable

/** [rememberUrlOpener] already presents its own `SFSafariViewController` sheet — nothing to overlay. */
@Composable
actual fun PlatformOverlay() = Unit
