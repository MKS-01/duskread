package dev.mks.duskread.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Opens links in the embedded reader browser rather than handing off to Chrome — see
 * [InAppBrowserScreen] for why.
 */
@Composable
actual fun rememberUrlOpener(): (String) -> Unit = remember { { url -> InAppBrowserRequest.open(url) } }

/**
 * Hands off to the phone's own browser, via the same `ACTION_VIEW` the embedded browser's
 * "open in browser" button uses — the escape hatch.
 */
@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { url -> context.openExternally(url) } }
}
