package dev.mks.duskread.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mks.duskread.data.rememberUserPrefs
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.FeedPost
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.savedAgo
import dev.mks.duskread.speech.speechSupported
import dev.mks.duskread.summary.SummaryRequest
import dev.mks.duskread.summary.SummaryTarget
import dev.mks.duskread.summary.summariesSupported
import dev.mks.duskread.ui.OpenRecord
import dev.mks.duskread.ui.ReadingQueue
import dev.mks.duskread.ui.ReadingQueueEntry
import dev.mks.duskread.ui.common.ListRow
import dev.mks.duskread.ui.common.RowMeta
import dev.mks.duskread.ui.common.ToastRequest
import dev.mks.duskread.ui.rememberArticleOpener
import dev.mks.duskread.ui.summary.SummariseBackground
import dev.mks.duskread.ui.theme.DuskReadIcons

/**
 * One post from a followed blog, as a [ListRow]: sourcechip, title, when it went out, and
 * the bookmark that is the only way a feed post ever reaches the Saved tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopicRow(
    post: FeedPost,
    host: String,
    // Whether a hairline follows.
    last: Boolean,
    linkLibrary: LinkLibrary,
    /**
     * The blog's own posts, positioned at this one, so the reader can turn through them.
     */
    queue: ReadingQueue,
    /**
     * The subject of the blog this came from, so bookmarking a post keeps it.
     */
    topic: String? = null,
    modifier: Modifier = Modifier,
) {
    val open = rememberArticleOpener()
    val saved = linkLibrary.isSaved(post.url)
    val swipeDefault = rememberUserPrefs().swipeDefault

    // Swiped one way only, and never to remove: a feed post is not the reader's own
    // record, so there is nothing here to destroy.
    val dismiss = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                SummaryRequest.open(SummaryTarget(post.url, post.title, feedContent = post.content))
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismiss,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = summariesSupported() || speechSupported(),
        backgroundContent = { SummariseBackground(dismiss.progress, swipeDefault) },
    ) {
        TopicRowBody(post = post, host = host, last = last, saved = saved, linkLibrary = linkLibrary, topic = topic, onOpen = { open(queue) })
    }
}

/**
 * The row itself, split out so the swipe box above wraps one thing.
 */
@Composable
private fun TopicRowBody(
    post: FeedPost,
    host: String,
    last: Boolean,
    saved: Boolean,
    linkLibrary: LinkLibrary,
    topic: String?,
    onOpen: () -> Unit,
) {
    ListRow(
        host = host,
        title = post.title,
        last = last,
        onClick = onOpen,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        titleMaxLines = 3,
        trailing = {
            // Two glyphs, not one glyph in two colours: filled reads as "saved" at 14dp
            // in a way a tint change does not.
            Icon(
                imageVector = if (saved) DuskReadIcons.BookmarkFilled else DuskReadIcons.Bookmark,
                contentDescription = if (saved) "Saved — tap to unsave" else "Save",
                modifier = Modifier
                    .size(26.dp)
                    .clickable {
                        linkLibrary.toggleSaved(post.url, post.title, topic)
                        ToastRequest.show(if (saved) "Removed" else "Saved")
                    }
                    .padding(6.dp),
                tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        RowMeta(post.publishedAt?.let(::savedAgo) ?: host)

        // Marking what works rather than what does not: most posts carry this.
        if (post.offline) RowMeta("offline")
    }
}

/**
 * A blog's posts as something to turn through. Feed rows record nothing when opened —
 * tapping one never has — so the queue carries no bookkeeping either.
 */
internal fun List<FeedPost>.readingQueue(feed: Feed): ReadingQueue = ReadingQueue(
    entries = map { ReadingQueueEntry(it.url, it.title, feed.host, feed.topic) },
    source = feed.label,
    record = OpenRecord.None,
)
