package dev.mks.duskread.ui

import androidx.compose.runtime.Composable

/**
 * A hook for whatever a platform needs layered above the whole app, outside any single
 * screen's own state — right now.
 */
@Composable
expect fun PlatformOverlay(mono: Boolean)
