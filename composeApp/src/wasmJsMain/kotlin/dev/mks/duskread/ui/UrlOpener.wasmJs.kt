package dev.mks.duskread.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberUrlOpener(): (String) -> Unit = remember {
    { url -> window.open(url, "_blank") }
}

/** Already a browser tab either way — the distinction the other targets draw does not exist here. */
@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember {
    { url -> window.open(url, "_blank") }
}
