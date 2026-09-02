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
import androidx.compose.ui.graphics.vector.ImageVector
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
import dev.mks.duskread.ui.common.EmptyState
import dev.mks.duskread.ui.common.EyebrowHeader
import dev.mks.duskread.ui.common.HairlineDivider
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
    /**
     * Sizes the true empty state — no feeds followed at all — against the
     * viewport below it, the same way Saved's own paste field does. Built by
     * the caller because `fillParentMaxHeight` only resolves inside the
     * `LazyItemScope` this composable is invoked from.
     */
    emptyStateModifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    // Open by default with nothing followed yet, the same reason Saved's own
    // paste field is never hidden behind a toggle: a reader's first visit is
    // exactly when the one thing to do here is add something, and gating that
    // behind a small "Manage" label in the corner meant the empty state had
    // no way to act on itself — a title and a sentence, nothing to tap.
    var managing by remember { mutableStateOf(feedLibrary.feeds.isEmpty()) }
    var feedUrl by remember { mutableStateOf("") }
    var discovering by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    // Most-new-first by default — the same bias NEXT UP ranks by, so the feed
    // most worth a look leads the list rather than whichever was followed
    // first.
    var sortNewest by remember { mutableStateOf(true) }

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

    // Blank leaves every feed and every post exactly as they were; a query
    // narrows both — a feed whose name matches keeps its usual posts, one
    // that doesn't is kept only for the posts inside it that do, so a topic
    // typed here can surface a single article from a blog followed for
    // something else entirely.
    val topics = feedLibrary.feeds.filter { feed ->
        val posts = postCache.postsByFeed[feed.id].orEmpty()
        if (posts.isEmpty()) return@filter false
        if (query.isBlank()) return@filter true
        feed.matches(query) || posts.any { it.matches(query) }
    }.let { filtered ->
        if (sortNewest) {
            filtered.sortedByDescending { feed ->
                postCache.postsByFeed[feed.id].orEmpty().count { !linkLibrary.isSaved(it.url) }
            }
        } else {
            filtered.sortedBy { it.label.lowercase() }
        }
    }

    Column(modifier.fillMaxWidth()) {
        EyebrowHeader(
            text = "FOLLOWING",
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FollowingAction(icon = DuskReadIcons.Search, label = "Search") {
                        searching = !searching
                        if (!searching) query = ""
                    }
                    if (feedLibrary.feeds.isNotEmpty()) {
                        FollowingAction(if (syncing) "Syncing…" else "Sync now", onClick = ::sync)
                    }
                    FollowingAction(if (managing) "Done" else "Manage") { managing = !managing }
                }
            },
        )
        Spacer(Modifier.height(12.dp))

        if (feedLibrary.feeds.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                FollowingSortChip("Newest", sortNewest) { sortNewest = true }
                FollowingSortChip("A–Z", !sortNewest) { sortNewest = false }
            }
        }

        AnimatedVisibility(searching) {
            AppTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search by blog, host or topic",
                fontSize = 14.5.sp,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.padding(bottom = 12.dp),
            )
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
                emptyStateModifier = emptyStateModifier,
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
                title = when {
                    query.isNotBlank() -> "Nothing matches “$query”"
                    feedLibrary.feeds.isEmpty() -> "Follow a blog"
                    else -> "Nothing synced yet"
                },
                message = when {
                    query.isNotBlank() -> "Try a different blog, host or topic."
                    feedLibrary.feeds.isEmpty() -> "Follow a blog's RSS or Atom feed to see its posts here."
                    else -> "Tap Sync now to pull in its latest posts."
                },
            )
        }

        topics.forEachIndexed { index, feed ->
            val all = postCache.postsByFeed[feed.id].orEmpty()
            // A feed matched by its own name keeps its usual posts; one that
            // only surfaced because a post inside it matched shows just that
            // post, so a topic search doesn't dump an unrelated blog's whole
            // archive onto the screen.
            val posts = if (query.isBlank() || feed.matches(query)) all else all.filter { it.matches(query) }
            val isOpen = expanded == feed.id || query.isNotBlank()
            DigestLine(
                feed = feed,
                newCount = posts.count { !linkLibrary.isSaved(it.url) },
                // The newest post's own title, not shown once the row is open
                // and that same post is sitting right underneath it — the
                // hint's whole job is answering "is this worth opening" before
                // you do.
                hint = if (isOpen) null else posts.firstOrNull()?.title,
                open = isOpen,
                onToggle = { expanded = if (expanded == feed.id) null else feed.id },
            )
            AnimatedVisibility(expanded == feed.id || query.isNotBlank()) {
                TopicPreview(
                    feed = feed,
                    posts = posts,
                    linkLibrary = linkLibrary,
                    onOpenAll = { onOpenTopics(feed) },
                )
            }
            // A hairline, not a gap — the same divider every other list in
            // the app puts between its rows, so a page of feeds reads as a
            // list rather than a stack of paragraphs with nothing between
            // them.
            if (index != topics.lastIndex) HairlineDivider()
        }
    }
}

/**
 * The same bordered pill Readback's Newest/Oldest chips use (`ReaderTab.kt`)
 * — this app's one sort pattern, not a second one invented for this list.
 */
@Composable
private fun FollowingSortChip(label: String, active: Boolean, onClick: () -> Unit) {
    val tone = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = label.uppercase(),
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        color = tone,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.Chip))
            .border(Stroke.Hairline, tone, RoundedCornerShape(Radius.Chip))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

/** A blog's own fields, searched by [FollowingDigest]'s query field. */
private fun Feed.matches(query: String): Boolean = label.contains(query, ignoreCase = true) ||
    host.contains(query, ignoreCase = true) ||
    topic?.contains(query, ignoreCase = true) == true

/** One post's title, searched the same way as its feed. */
private fun FeedPost.matches(query: String): Boolean = title.contains(query, ignoreCase = true)

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

/** The icon-only sibling — Search sits with the other actions but has no word for one. */
@Composable
private fun FollowingAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Icon(
        imageVector = icon,
        contentDescription = label,
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(4.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * One line of the digest — "host — N new", the count in the accent when
 * there's something unsaved and a plain dash otherwise — plus a second,
 * quieter one: the newest post's own title, the fact that answers "is this
 * worth opening" before the tap that finds out. Tapping the row expands the
 * carousel below in place, rather than navigating anywhere — a digest line is
 * a summary of a thread, not a link to a different screen.
 */
@Composable
private fun DigestLine(feed: Feed, newCount: Int, hint: String?, open: Boolean, onToggle: () -> Unit) {
    // A right chevron is "expand" everywhere else in the app (Chevron, on a
    // row that opens something); rotating it to point down is what says
    // "this one is already open" without a second glyph to learn.
    val rotation by animateFloatAsState(if (open) 90f else 0f, tween(Motion.Chip), label = "chevron")

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                // The publisher's name when the Notion sync supplied one, the
                // host when it did not — see Feed.label.
                //
                // Jost, not Inconsolata — the tokens doc is explicit that mono
                // is for a value, not a name: "if a label names a section
                // rather than reporting a value, it is not mono." A feed's
                // name is a name, the same as every post title underneath it
                // once this opens, so it needs the same family [TopicRow]
                // draws those in (`bodyLarge`) — but SemiBold (`titleSmall`)
                // next to Regular read as shouting rather than a header,
                // since the row above it is smaller than what it introduces.
                // Medium is the one step this set actually has between the
                // two.
                text = feed.label,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
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

        hint?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                // Same family as the title above it, one notch down in size
                // and colour — a hint, not a second heading. Mono would read
                // it as a fact rather than a name, and a headline is a name.
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    emptyStateModifier: Modifier = Modifier,
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
            // Sits low in the remaining viewport, the same treatment Saved
            // gives its own first-run state — a single grey line under the
            // field was the whole rest of the screen left plain.
            Box(Modifier.fillMaxWidth().then(emptyStateModifier), contentAlignment = Alignment.BottomStart) {
                EmptyState(
                    title = "Nothing followed yet",
                    message = "Paste a blog's address above, or its RSS or Atom feed directly — " +
                        "DuskRead finds the feed behind a homepage on its own.",
                )
            }
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
