package dev.mks.duskread.ui

import androidx.compose.runtime.Composable

/**
 * Returns a function that opens a URL in the platform's in-app browser —
 * Custom Tabs on Android, `SFSafariViewController` on iOS, the system browser
 * on desktop and a new tab on web.
 *
 * Read as a composable because Android needs the local `Context` to launch the
 * intent, and because the mobile surfaces tint their chrome with the current
 * Material colours; both are only reachable from composition.
 */
@Composable
expect fun rememberUrlOpener(): (String) -> Unit
