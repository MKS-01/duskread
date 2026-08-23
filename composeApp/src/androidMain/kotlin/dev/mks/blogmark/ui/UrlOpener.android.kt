package dev.mks.blogmark.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Opens links in the embedded reader browser rather than handing off to
 * Chrome — see [InAppBrowserScreen] for why. `MainActivity` is the one place
 * that can show it over everything else, so this only files the request;
 * [InAppBrowserRequest] is what actually gets it there.
 */
@Composable
actual fun rememberUrlOpener(): (String) -> Unit = remember { { url -> InAppBrowserRequest.open(url) } }
