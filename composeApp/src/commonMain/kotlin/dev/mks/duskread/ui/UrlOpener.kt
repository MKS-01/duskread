package dev.mks.duskread.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Returns a function that opens an article for *reading* — the embedded browser on
 * Android, `SFSafariViewController` on iOS. It takes the whole list the article came
 * from, so a platform that can page between them has what it needs; one that cannot
 * simply reads [ReadingQueue.current].
 */
@Composable
expect fun rememberArticleOpener(): (ReadingQueue) -> Unit

/**
 * The same, for a URL that arrived without a list behind it.
 */
@Composable
fun rememberUrlOpener(): (String) -> Unit {
    val open = rememberArticleOpener()
    return remember(open) { { url -> open(singleArticle(url)) } }
}

/**
 * For a URL that is a *task* rather than an article: it leaves the app entirely, for
 * whichever browser the reader already lives in.
 */
@Composable
expect fun rememberExternalUrlOpener(): (String) -> Unit
