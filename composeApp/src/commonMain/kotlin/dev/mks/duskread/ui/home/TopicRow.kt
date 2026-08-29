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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mks.duskread.links.FeedPost
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.savedAgo
import dev.mks.duskread.summary.SummaryRequest
import dev.mks.duskread.summary.SummaryTarget
import dev.mks.duskread.summary.summariesSupported
import dev.mks.duskread.ui.common.ListRow
import dev.mks.duskread.ui.common.RowMeta
import dev.mks.duskread.ui.common.ToastRequest
import dev.mks.duskread.ui.rememberUrlOpener
import dev.mks.duskread.ui.summary.SummariseBackground
import dev.mks.duskread.ui.theme.DuskReadIcons

/**
 * One post from a followed blog, as a [ListRow]: sourcechip, title, when it
 * went out, and the bookmark that is the only way a feed post ever reaches
 * the Saved tab.
 *
 * In its own file because it belongs to neither of the two places that draw
 * it — the digest's expansion on Home shows the newest few, [TopicsScreen]
 * shows all of them, and they are the same list at two lengths.
 *
 * Saving is handled here rather than by each caller. It is not presentation:
 * the toggle and the toast that confirms it are what this row *does*, and
 * passing that in as a lambda meant writing the same four lines at both call
 * sites and inviting them to drift.
 *
 * The meta line carries the date alone rather than the host-and-time pair
 * Saved uses. Both callers sit directly under something that already names
 * the blog — a digest line, or the screen's eyebrow — so repeating the host
 * on every row would spend the one line a row gets on the one fact the reader
 * already has. A feed that dates nothing falls back to the host, which is at
 * least true.
 *
 * The bookmark shows on every row, unlike Saved's trailing tick, which shows
 * only on a read row. That is deliberate: saving a post straight from its
 * feed is a real feature, and a control that appears only once the thing is
 * already saved cannot offer it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopicRow(
    post: FeedPost,
    host: String,
    // Whether a hairline follows. False for a row with anything under it —
    // including the digest's all-posts row, which is not a post but is still
    // something the list continues into.
    last: Boolean,
    linkLibrary: LinkLibrary,
    /**
     * The subject of the blog this came from, so bookmarking a post keeps it.
     * Without this a post saved from Following arrives in the reading list
     * with no topic — and the ranking's inference from the host only works
     * while that blog is still followed.
     */
    topic: String? = null,
    modifier: Modifier = Modifier,
) {
    val open = rememberUrlOpener()
    val saved = linkLibrary.isSaved(post.url)

    // Swiped one way only, and never to remove: a feed post is not the
    // reader's own record, so there is nothing here to destroy. What the feed
    // already carried goes with the request — a post whose body arrived in
    // the feed is summarised without a single request.
    val dismiss = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                SummaryRequest.open(SummaryTarget(post.url, post.title, feedContent = post.content))
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismiss,
        modifier = modifier,
        enableDismissFromStartToEnd = summariesSupported(),
        enableDismissFromEndToStart = false,
        backgroundContent = { SummariseBackground(dismiss.progress) },
    ) {
        TopicRowBody(post = post, host = host, last = last, saved = saved, linkLibrary = linkLibrary, topic = topic, onOpen = { open(post.url) })
    }
}

/**
 * The row itself, split out so the swipe box above wraps one thing. It also
 * needs its own background: the box slides this over the summarise panel
 * behind it, and without one the label shows through the gaps between words.
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
            // Two glyphs, not one glyph in two colours: filled reads as
            // "saved" at 14dp in a way a tint change does not, and it is the
            // only signal that survives the monochrome scheme, where the
            // accent this would otherwise rely on is a grey.
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

        // Marking what works rather than what does not: most posts carry this,
        // so it reads as quiet reassurance instead of a warning on a row that
        // is perfectly fine whenever there is signal. Its absence is the
        // signal — and the three followed blogs that publish only a teaser are
        // the ones that will never have it.
        if (post.offline) RowMeta("offline")
    }
}
