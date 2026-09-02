package dev.mks.duskread.ui

import androidx.compose.runtime.Composable

/**
 * Returns a function that opens a URL for *reading* — the embedded browser on
 * Android and desktop, `SFSafariViewController` on iOS, a new tab on web.
 *
 * Read as a composable because Android needs the local `Context` to launch the
 * intent, and because the mobile surfaces tint their chrome with the current
 * Material colours; both are only reachable from composition.
 */
@Composable
expect fun rememberUrlOpener(): (String) -> Unit

/**
 * The same, for a URL that is a *task* rather than an article: it leaves the
 * app entirely, for whichever browser the reader already lives in.
 *
 * Notion's integrations portal is what forced the split. Getting a token there
 * means signing in, working through a settings screen and copying a secret,
 * and [rememberUrlOpener]'s embedded reader is bad at every part of that: it
 * carries no session, so it asks someone to type a password into a WebView
 * this app owns, with no password manager to fill it and no address bar to
 * check who is asking. Their own browser is signed in already.
 */
@Composable
expect fun rememberExternalUrlOpener(): (String) -> Unit
