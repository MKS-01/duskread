package dev.mks.duskread.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Opens links in the embedded browser rather than handing off to Safari —
 * see [InAppBrowserScreen] for why. `PlatformOverlay` is the one place that
 * can show it over everything else, so this only files the request;
 * [InAppBrowserRequest] is what gets it there. [openExternally] is still
 * reachable from the browser's own toolbar, as the escape hatch.
 */
@Composable
actual fun rememberUrlOpener(): (String) -> Unit = remember { { url -> InAppBrowserRequest.open(url) } }

/** That same escape hatch, reached directly: the system browser, with the session the reader already has. */
@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember { { url -> openExternally(url) } }
