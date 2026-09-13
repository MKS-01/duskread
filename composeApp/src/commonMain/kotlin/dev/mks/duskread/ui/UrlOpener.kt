package dev.mks.duskread.ui

import androidx.compose.runtime.Composable

/**
 * Returns a function that opens a URL for *reading* — the embedded browser on Android and
 * desktop, `SFSafariViewController` on iOS, a new tab on web.
 */
@Composable
expect fun rememberUrlOpener(): (String) -> Unit

/**
 * The same, for a URL that is a *task* rather than an article: it leaves the app
 * entirely, for whichever browser the reader already lives in.
 */
@Composable
expect fun rememberExternalUrlOpener(): (String) -> Unit
