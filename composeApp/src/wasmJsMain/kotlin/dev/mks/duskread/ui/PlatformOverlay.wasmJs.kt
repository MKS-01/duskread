package dev.mks.duskread.ui

import androidx.compose.runtime.Composable

/** The browser tab a link opens into is already outside this app — nothing to overlay. */
@Composable
actual fun PlatformOverlay(mono: Boolean) = Unit
