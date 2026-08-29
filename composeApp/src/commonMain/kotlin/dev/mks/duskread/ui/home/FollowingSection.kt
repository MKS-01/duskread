package dev.mks.duskread.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPost
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.discoverFeedUrl
import dev.mks.duskread.links.looksLikeUrl
import dev.mks.duskread.links.normaliseUrl
import dev.mks.duskread.links.savedAgo
import dev.mks.duskread.links.syncFeeds
import dev.mks.duskread.ui.common.AppTextField
import dev.mks.duskread.ui.common.CompactEmptyState
import dev.mks.duskread.ui.common.EyebrowHeader
import dev.mks.duskread.ui.common.HairlineDivider
import dev.mks.duskread.ui.common.MonogramBadge
import dev.mks.duskread.ui.common.ToastRequest
import dev.mks.duskread.ui.rememberUrlOpener
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Motion
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.SectionLabel
import dev.mks.duskread.ui.theme.Stroke
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The blogs followed for [syncFeeds] to pull from: a digest, not a carousel —
 * one line per feed, "host — N new", rather than a nested strip of clipped
 * cards competing with the rest of Home for weight. Tapping a line is what
 * expands it into the actual posts, so the browsing feature underneath
 * (save a post straight from its feed without ever visiting Saved) survives
 * without costing the digest its quiet, three-lines-and-done shape.
 *
 * Nothing here is a saved link on its own. What [FeedPostCache] holds is a
 * cache of the last successful sync, replaced only when Sync runs again —
 * tapping a post's bookmark is the one thing that copies it into
 * [LinkLibrary], where the Saved tab and the rest of the app can see it.
 */
@Composable
fun FollowingDigest(
    feedLibrary: FeedLibrary,
    postCache: FeedPostCache,
    linkLibrary: LinkLibrary,
    client: HttpClient,
    onOpenTopics: (Feed) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var managing by remember { mutableStateOf(false) }
    var feedUrl by remember { mutableStateOf("") }
    var discovering by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }

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

    Column(modifier.fillMaxWidth()) {
        EyebrowHeader(
            text = "FOLLOWING",
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (feedLibrary.feeds.isNotEmpty()) {
                        FollowingAction(if (syncing) "Syncing…" else "Sync now", onClick = ::sync)
                    }
                    FollowingAction(if (managing) "Done" else "Manage") { managing = !managing }
                }
            },
        )
        Spacer(Modifier.height(12.dp))

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
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (topics.isEmpty() && !managing) {
            CompactEmptyState(
                title = if (feedLibrary.feeds.isEmpty()) "Follow a blog" else "Nothing synced yet",
                message = if (feedLibrary.feeds.isEmpty()) {
                    "Follow a blog's RSS or Atom feed to see its posts here."
                } else {
                    "Tap Sync now to pull in its latest posts."
                },
            )
        }

        topics.forEachIndexed { index, feed ->
            val posts = postCache.postsByFeed[feed.id].orEmpty()
            DigestLine(
                feed = feed,
                newCount = posts.count { !linkLibrary.isSaved(it.url) },
                open = expanded == feed.id,
                onToggle = { expanded = if (expanded == feed.id) null else feed.id },
            )
            AnimatedVisibility(expanded == feed.id) {
                TopicPreview(
                    feed = feed,
                    posts = posts,
                    linkLibrary = linkLibrary,
                    onOpenAll = { onOpenTopics(feed) },
                )
            }
            if (index != topics.lastIndex) Spacer(Modifier.height(7.dp))
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
 * One line of the digest: "host — N new", the count in the accent when
 * there's something unsaved and a plain dash otherwise. Tapping it expands
 * the carousel below in place, rather than navigating anywhere — a digest
 * line is a summary of a thread, not a link to a different screen.
 */
@Composable
private fun DigestLine(feed: Feed, newCount: Int, open: Boolean, onToggle: () -> Unit) {
    // A right chevron is "expand" everywhere else in the app (Chevron, on a
    // row that opens something); rotating it to point down is what says
    // "this one is already open" without a second glyph to learn.
    val rotation by animateFloatAsState(if (open) 90f else 0f, tween(Motion.Chip), label = "chevron")

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            // The publisher's name when the Notion sync supplied one, the
            // host when it did not — see Feed.label.
            text = feed.label,
            fontFamily = Mono,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (newCount > 0) "$newCount new" else "—",
            fontFamily = Mono,
            fontSize = 11.sp,
            fontWeight = if (newCount > 0) FontWeight.SemiBold else FontWeight.Normal,
            color = if (newCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = DuskReadIcons.Chevron,
            contentDescription = if (open) "Collapse" else "Expand",
            modifier = Modifier.size(11.dp).rotate(rotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The posts behind one digest line, revealed on tap: the newest few as flat
 * rows, then the way through to all of them.
 *
 * This used to be a horizontal strip of boxed cards, which was the last
 * boxed surface left on Home and the reason the strip existed at all — a row
 * of cards has to agree on a width, so it could only ever show two at a time
 * and clip the third. Rows have no such constraint. They are also the same
 * rows [TopicsScreen] is built from, so opening a feed in full is a change
 * of length rather than a change of language.
 *
 * Three, not all of them: this is still a section inside a dashboard, and a
 * digest line that expanded into fifteen rows would push everything under it
 * off the screen. The rest are one tap further on.
 */
@Composable
private fun TopicPreview(feed: Feed, posts: List<FeedPost>, linkLibrary: LinkLibrary, onOpenAll: () -> Unit) {
    Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        posts.take(PreviewPosts).forEach { post ->
            TopicRow(
                post = post,
                host = feed.host,
                // Never the last thing in the column — the all-posts row
                // always follows, so every preview row keeps its hairline.
                last = false,
                linkLibrary = linkLibrary,
                topic = feed.topic,
            )
        }

        AllPostsRow(count = posts.size, onClick = onOpenAll)
    }
}

/**
 * The door to [TopicsScreen], as a row rather than a card at the end of a
 * strip: a reader who wants more than the preview holds should not have to
 * scroll a carousel to its end to find out that more exists.
 */
@Composable
private fun AllPostsRow(count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "ALL $count POSTS",
            style = SectionLabel,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = DuskReadIcons.Chevron,
            contentDescription = null,
            modifier = Modifier.size(11.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** How much of a feed the digest shows before handing over to [TopicsScreen]. */
private const val PreviewPosts = 3

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
    Column(Modifier.padding(bottom = 8.dp)) {
        AppTextField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = "Blog's address or its RSS/Atom feed",
            enabled = !discovering,
            fontSize = 14.5.sp,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
            trailing = {
                AnimatedVisibility(url.isNotBlank() || discovering) {
                    Text(
                        text = if (discovering) "Finding…" else "Follow",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = !discovering, onClick = onAdd)
                            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    )
                }
            },
        )

        if (feeds.isEmpty()) {
            Text(
                text = "Nothing followed yet.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        } else {
            // Flush on the background with a hairline between rows, like
            // every other list in the app. This was the last filled container
            // in the Following section, and a panel of rows behind a "Manage"
            // toggle is no more a card than the rows it was holding.
            Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                feeds.forEachIndexed { index, feed ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
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
                            style = SectionLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { onRemove(feed.id) }
                                .padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
                        )
                    }
                    if (index != feeds.lastIndex) {
                        HairlineDivider()
                    }
                }
            }
        }
    }
}
