package dev.mks.blogmark.ui

import androidx.compose.runtime.Composable

/** Desktop opens links straight in the system browser — nothing to overlay. */
@Composable
actual fun PlatformOverlay(mono: Boolean) = Unit
