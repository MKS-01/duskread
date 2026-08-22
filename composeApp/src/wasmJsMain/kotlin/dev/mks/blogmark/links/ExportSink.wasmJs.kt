package dev.mks.blogmark.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The clipboard only.
 *
 * A browser can be made to download a blob, and `navigator.share` exists on
 * some of them, but the web build is a way to look at Blogmark on a machine that
 * is not your phone — not where a reading list is kept. Copying out is the one
 * route that works in every browser without feature-detection, and the screen
 * simply does not offer the rest here.
 */
@Composable
actual fun rememberExportSink(): ExportSink = remember { ClipboardOnlySink }
