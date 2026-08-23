package dev.mks.blogmark.ui

import androidx.compose.runtime.Composable

/**
 * A hook for whatever a platform needs layered above the whole app, outside
 * any single screen's own state — right now, only Android's embedded reader
 * browser that reference links open into instead of Chrome (see
 * `dev.mks.blogmark.ui.InAppBrowserScreen` in `androidMain`). Every other
 * platform already hands a link straight to the system browser or a native
 * modal sheet from the call site itself, so there is nothing to hook there.
 */
@Composable
expect fun PlatformOverlay()
