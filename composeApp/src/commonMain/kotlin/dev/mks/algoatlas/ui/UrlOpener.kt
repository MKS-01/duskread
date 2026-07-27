package dev.mks.algoatlas.ui

import androidx.compose.runtime.Composable

/**
 * Returns a function that opens a URL in the platform browser.
 *
 * Read as a composable because Android needs the local `Context` to launch the
 * intent, and that is only reachable from composition.
 */
@Composable
expect fun rememberUrlOpener(): (String) -> Unit
