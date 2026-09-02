package dev.mks.duskread.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Opens links in the embedded reader browser rather than handing off to
 * Chrome — see [InAppBrowserScreen] for why. `MainActivity` is the one place
 * that can show it over everything else, so this only files the request;
 * [InAppBrowserRequest] is what actually gets it there.
 */
@Composable
actual fun rememberUrlOpener(): (String) -> Unit = remember { { url -> InAppBrowserRequest.open(url) } }

/**
 * Hands off to the phone's own browser, via the same `ACTION_VIEW` the
 * embedded browser's "open in browser" button uses — the escape hatch, reached
 * directly rather than after a detour through a WebView that cannot help.
 */
@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { url -> context.openExternally(url) } }
}
