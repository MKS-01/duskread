package dev.mks.blogmark.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.blogmark.links.FeedPost
import dev.mks.blogmark.links.LinkLibrary
import dev.mks.blogmark.links.savedAgo
import dev.mks.blogmark.ui.common.MonogramBadge
import dev.mks.blogmark.ui.common.ToastRequest
import dev.mks.blogmark.ui.rememberUrlOpener
import dev.mks.blogmark.ui.theme.BlogmarkIcons
import dev.mks.blogmark.ui.theme.Mono

/**
 * One post from a followed blog: sourcechip, headline, meta line, and the
 * bookmark that is the only way a feed post ever reaches the Saved tab.
 *
 * The one row shape for feed posts, in its own file because it belongs to
 * neither of the two places that draw it — the digest's expansion on Home
 * shows the newest few, [TopicsScreen] shows all of them, and they are the
 * same list at two lengths. Two shapes for one thing was exactly what the
 * boxed carousel card had been.
 *
 * Saving is handled here rather than by each caller. It is not presentation:
 * the toggle and the toast that confirms it are what this row *does*, and
 * passing that in as a lambda meant writing the same four lines at both call
 * sites and inviting them to drift.
 *
 * The meta line carries the date alone rather than the usual host-and-time
 * pair. Both callers sit directly under something that already names the
 * blog — a digest line, or this screen's eyebrow — so repeating the host on
 * every row would spend the one line a row gets on the one fact the reader
 * already has. A feed that dates nothing falls back to the host, which is at
 * least true.
 *
 * The bookmark shows on every row, unlike Saved's trailing tick, which shows
 * only on a read row. That is deliberate: saving a post straight from its
 * feed is a real feature, and a control that appears only once the thing is
 * already saved cannot offer it.
 */
@Composable
internal fun TopicRow(
    post: FeedPost,
    host: String,
    // Whether a hairline and its spacing follow. False for a row with
    // anything under it — including the digest's all-posts row, which is not
    // a post but is still something the list continues into.
    last: Boolean,
    linkLibrary: LinkLibrary,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val open = rememberUrlOpener()
    val saved = linkLibrary.isSaved(post.url)

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { open(post.url) },
            verticalAlignment = Alignment.Top,
        ) {
            MonogramBadge(host = host, size = 22.dp)
            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = post.publishedAt?.let(::savedAgo) ?: host,
                    fontFamily = Mono,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(8.dp))
            // Two glyphs, not one glyph in two colours: filled reads as
            // "saved" at 14dp in a way a tint change does not, and it is the
            // only signal that survives the monochrome scheme, where the
            // accent this would otherwise rely on is a grey.
            Icon(
                imageVector = if (saved) BlogmarkIcons.BookmarkFilled else BlogmarkIcons.Bookmark,
                contentDescription = if (saved) "Saved — tap to unsave" else "Save",
                modifier = Modifier
                    .size(26.dp)
                    .clickable {
                        linkLibrary.toggleSaved(post.url, post.title)
                        ToastRequest.show(if (saved) "Removed" else "Saved")
                    }
                    .padding(6.dp),
                tint = if (saved) scheme.primary else scheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(15.dp))
        if (!last) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant))
            Spacer(Modifier.height(15.dp))
        }
    }
}
