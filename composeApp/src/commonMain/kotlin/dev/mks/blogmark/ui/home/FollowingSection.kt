package dev.mks.blogmark.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.blogmark.links.Feed
import dev.mks.blogmark.links.FeedLibrary
import dev.mks.blogmark.links.FeedPost
import dev.mks.blogmark.links.FeedPostCache
import dev.mks.blogmark.links.LinkLibrary
import dev.mks.blogmark.links.discoverFeedUrl
import dev.mks.blogmark.links.looksLikeUrl
import dev.mks.blogmark.links.normaliseUrl
import dev.mks.blogmark.links.syncFeeds
import dev.mks.blogmark.links.topicIcon
import dev.mks.blogmark.ui.common.CompactEmptyState
import dev.mks.blogmark.ui.common.ToastRequest
import dev.mks.blogmark.ui.rememberUrlOpener
import dev.mks.blogmark.ui.theme.BlogmarkIcons
import dev.mks.blogmark.ui.theme.Mono
import dev.mks.blogmark.ui.theme.Radius
import dev.mks.blogmark.ui.theme.SectionLabel
import dev.mks.blogmark.ui.theme.Stroke
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The blogs followed for [syncFeeds] to pull from, shown as one horizontally
 * scrollable row per feed — a thread of what that blog last published, the
 * way an Instagram or Threads feed groups posts by who made them.
 *
 * Nothing here is a saved link on its own. What [FeedPostCache] holds is a
 * cache of the last successful sync, replaced only when Sync runs again —
 * tapping a card's bookmark is the one thing that copies a post into
 * [LinkLibrary], where the Saved tab and the rest of the app can see it. That
 * split is deliberate: browsing what a followed blog posted should cost
 * nothing, and only the posts worth keeping should go in the reading list.
 */
@Composable
fun FollowingSection(
    feedLibrary: FeedLibrary,
    postCache: FeedPostCache,
    linkLibrary: LinkLibrary,
    client: HttpClient,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var managing by remember { mutableStateOf(false) }
    var feedUrl by remember { mutableStateOf("") }
    var discovering by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    // Following a blog by its homepage rather than its exact feed address is
    // the common case — this is what turns "swmansion.com/blog/" into the
    // `/rss.xml` underneath it before it ever reaches [FeedLibrary].
    fun follow() {
        val typed = feedUrl
        if (discovering || !looksLikeUrl(typed)) return
        discovering = true
        scope.launch {
            val resolved = discoverFeedUrl(client, normaliseUrl(typed))
            feedLibrary.add(resolved)
            feedUrl = ""
            discovering = false
        }
    }

    LaunchedEffect(note) {
        if (note != null) {
            delay(5_000)
            note = null
        }
    }

    fun sync() {
        if (syncing || feedLibrary.feeds.isEmpty()) return
        syncing = true
        scope.launch {
            val synced = syncFeeds(client, feedLibrary.feeds, postCache)
            note = when {
                synced == 0 -> "Couldn't reach any feed."
                synced == feedLibrary.feeds.size -> "Synced $synced feed${if (synced == 1) "" else "s"}."
                else -> "Synced $synced of ${feedLibrary.feeds.size} feeds."
            }
            syncing = false
        }
    }

    val topics = feedLibrary.feeds.filter { postCache.postsByFeed[it.id].orEmpty().isNotEmpty() }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Card))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "FOLLOWING",
                style = SectionLabel,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (feedLibrary.feeds.isNotEmpty()) {
                FollowingAction(if (syncing) "Syncing…" else "Sync now", onClick = ::sync)
            }
            FollowingAction(if (managing) "Done" else "Manage") { managing = !managing }
        }

        AnimatedVisibility(managing) {
            FeedManagePanel(
                feeds = feedLibrary.feeds,
                url = feedUrl,
                onUrlChange = { feedUrl = it },
                discovering = discovering,
                onAdd = ::follow,
                onRemove = { id ->
                    feedLibrary.remove(id)
                    postCache.removeFeed(id)
                },
            )
        }

        note?.let {
            Text(
                text = it,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 2.dp, top = 6.dp),
            )
        }

        if (topics.isEmpty() && !managing) {
            CompactEmptyState(
                icon = BlogmarkIcons.Feed,
                title = if (feedLibrary.feeds.isEmpty()) "Follow a blog" else "Nothing synced yet",
                message = if (feedLibrary.feeds.isEmpty()) {
                    "Follow a blog's RSS or Atom feed to see its posts here."
                } else {
                    "Tap Sync now to pull in its latest posts."
                },
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        topics.forEach { feed ->
            TopicRow(
                feed = feed,
                posts = postCache.postsByFeed[feed.id].orEmpty(),
                linkLibrary = linkLibrary,
            )
        }
    }
}

@Composable
private fun FollowingAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = SectionLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/**
 * One followed blog's thread: an icon and label for the topic, its posts as a
 * horizontally scrollable row of cards — several at once, the way an
 * Instagram or Threads profile shows a strip of what someone posted rather
 * than one at a time — and a scrubber standing in for where the row is.
 */
@Composable
private fun TopicRow(feed: Feed, posts: List<FeedPost>, linkLibrary: LinkLibrary) {
    val listState = rememberLazyListState()

    Column(Modifier.padding(top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = topicIcon(feed.host),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = feed.host,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(posts, key = { it.url }) { post ->
                PostCard(
                    post = post,
                    host = feed.host,
                    icon = topicIcon(feed.host),
                    saved = linkLibrary.isSaved(post.url),
                    onToggleSave = {
                        val wasSaved = linkLibrary.isSaved(post.url)
                        linkLibrary.toggleSaved(post.url, post.title)
                        ToastRequest.show(if (wasSaved) "Removed" else "Saved")
                    },
                )
            }
        }

        if (posts.size > 1) {
            Spacer(Modifier.height(8.dp))
            ScrollDots(listState, itemCount = posts.size, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

/**
 * One post: a corner of colour from the topic's own icon so the card isn't
 * bare type on a flat surface, the headline, the host it came from, and the
 * bookmark that's the only way it ever reaches the Saved tab. No thumbnail —
 * that would mean an image fetch per post on top of the feed fetch itself,
 * for a badge [topicIcon] already gives for free.
 */
@Composable
private fun PostCard(post: FeedPost, host: String, icon: ImageVector, saved: Boolean, onToggleSave: () -> Unit) {
    val open = rememberUrlOpener()

    Column(
        Modifier
            .width(200.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(Radius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Card))
            .clickable { open(post.url) }
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.weight(1f))
            // Tinted rather than swapped for a filled glyph, the same way a
            // saved link's "mark read" button changes colour instead of
            // shape — one hand-drawn bookmark, two states.
            Icon(
                imageVector = BlogmarkIcons.Bookmark,
                contentDescription = if (saved) "Saved — tap to unsave" else "Save",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleSave)
                    .padding(4.dp),
                tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = post.title,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.weight(1f))
        Text(
            text = host,
            fontFamily = Mono,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A page dot per post, centred under the row — not the continuous
 * thumb-in-track bar this used to be. That read as a progress bar for
 * something loading, which nothing here is; a handful of posts is closer to
 * a paged carousel than a scrollbar, and a dot per item says "which one am I
 * on" without borrowing a loading indicator's shape to say it.
 *
 * Pinned to one small total width rather than one fixed dot size: a feed can
 * carry up to 15 posts (see `EntriesPerFeed`), and 15 full-size dots would
 * span wider than the row of cards underneath them.
 */
@Composable
private fun ScrollDots(state: LazyListState, itemCount: Int, modifier: Modifier = Modifier) {
    val current = state.firstVisibleItemIndex.coerceIn(0, itemCount - 1)
    val gap = 3.dp
    val dot = ((DotsWidth - gap * (itemCount - 1)) / itemCount).coerceIn(2.dp, 5.dp)

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(itemCount) { index ->
            val active = index == current
            Box(
                Modifier
                    .size(dot)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            )
        }
    }
}

private val DotsWidth = 44.dp

/** The feed address field, and the list of what's already followed. */
@Composable
private fun FeedManagePanel(
    feeds: List<Feed>,
    url: String,
    onUrlChange: (String) -> Unit,
    discovering: Boolean,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(Modifier.padding(top = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (url.isEmpty()) {
                    Text(
                        text = "Blog's address or its RSS/Atom feed",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.5.sp,
                    )
                }
                BasicTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    enabled = !discovering,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.5.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAdd() }),
                )
            }

            AnimatedVisibility(url.isNotBlank() || discovering) {
                Text(
                    text = if (discovering) "Finding…" else "Follow",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = !discovering, onClick = onAdd)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        if (feeds.isEmpty()) {
            Text(
                text = "Nothing followed yet.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(Radius.Card))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                feeds.forEach { feed ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            // The full path, not just the host — a feed
                            // followed by its blog's homepage instead of its
                            // actual RSS/Atom endpoint fetches real HTML with
                            // nothing to parse, and looks identical to a
                            // working feed if only the host is shown here.
                            text = feed.url.removePrefix("https://").removePrefix("http://"),
                            fontFamily = Mono,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Unfollow",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onRemove(feed.id) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
