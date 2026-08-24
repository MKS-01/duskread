package dev.mks.duskread.ui

import androidx.compose.runtime.Composable

/**
 * A hook for whatever a platform needs layered above the whole app, outside
 * any single screen's own state — right now, only Android's embedded reader
 * browser that reference links open into instead of Chrome (see
 * `dev.mks.duskread.ui.InAppBrowserScreen` in `androidMain`). Every other
 * platform already hands a link straight to the system browser or a native
 * modal sheet from the call site itself, so there is nothing to hook there.
 *
 * [mono] is passed through rather than read off `MaterialTheme.colorScheme`
 * inside the reader itself, so the reader's embedded article — a WebView
 * document, not Compose — can carry the same colourless choice into the CSS
 * it renders with.
 */
@Composable
expect fun PlatformOverlay(mono: Boolean)
