package dev.mks.duskread.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.data.LocalAppGraph
import dev.mks.duskread.links.DigestRow
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPost
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.FollowingGroups
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.PostPick
import dev.mks.duskread.links.SearchAlwaysShownAt
import dev.mks.duskread.links.cloudTier
import dev.mks.duskread.links.discoverFeedUrl
import dev.mks.duskread.links.looksLikeUrl
import dev.mks.duskread.links.normaliseUrl
import dev.mks.duskread.links.pruneSummaries
import dev.mks.duskread.links.savedAgo
import dev.mks.duskread.links.shortAgo
import dev.mks.duskread.summary.rememberSummaryCache
import dev.mks.duskread.ui.OpenRecord
import dev.mks.duskread.ui.ReadingQueue
import dev.mks.duskread.ui.ReadingQueueEntry
import dev.mks.duskread.ui.common.AppTextField
import dev.mks.duskread.ui.common.ArticleCard
import dev.mks.duskread.ui.common.CompactEmptyState
import dev.mks.duskread.ui.common.EmptyState
import dev.mks.duskread.ui.common.EyebrowHeader
import dev.mks.duskread.ui.common.HairlineDivider
import dev.mks.duskread.ui.common.HeaderAction
import dev.mks.duskread.ui.common.ListRow
import dev.mks.duskread.ui.common.RowMeta
import dev.mks.duskread.ui.rememberArticleOpener
import dev.mks.duskread.ui.theme.DesignTokens
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.SectionLabel
import dev.mks.duskread.ui.theme.Space
import dev.mks.duskread.ui.theme.Stroke
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch

/** What the Following list is doing, hoisted so its rows can be lazy items of the tab's list. */
@Stable
class FollowingState(managing: Boolean) {
    // Open by default with nothing followed yet, the same reason Saved's own paste field
    // is never hidden behind a toggle.
    var managing by mutableStateOf(managing)
    var feedUrl by mutableStateOf("")
    var discovering by mutableStateOf(false)
    var searching by mutableStateOf(false)
    var query by mutableStateOf("")
}

@Composable
fun rememberFollowingState(feeds: FeedLibrary): FollowingState = remember { FollowingState(managing = feeds.feeds.isEmpty()) }

/**
 * The header, search, manage panel and empty states above the list — one lazy item,
 * since none of it grows with the number of blogs followed.
 */
@Composable
fun FollowingHead(
    state: FollowingState,
    groups: FollowingGroups,
    feedLibrary: FeedLibrary,
    postCache: FeedPostCache,
    linkLibrary: LinkLibrary,
    client: HttpClient,
    modifier: Modifier = Modifier,
    /**
     * Sizes the true empty state — no feeds followed at all — against the viewport below
     * it, the same way Saved's own paste field does.
     */
    emptyStateModifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val summaries = rememberSummaryCache()
    val feedSync = LocalAppGraph.current.feedSync
    val sync = feedSync.state
    val followed = feedLibrary.feeds.size
    // A long list is exactly when search is wanted, so it stops hiding behind an icon —
    // but not over Manage, where the one field that matters is the address.
    val searchShown = !state.managing && (state.searching || followed >= SearchAlwaysShownAt)

    // Following a blog by its homepage rather than its exact feed address is the common
    // case.
    fun follow() {
        val typed = state.feedUrl
        if (state.discovering || !looksLikeUrl(typed)) return
        state.discovering = true
        scope.launch {
            val resolved = discoverFeedUrl(client, normaliseUrl(typed))
            feedLibrary.add(resolved)
            state.feedUrl = ""
            state.discovering = false
            // A blog just followed should show its posts, not wait for the next sync.
            feedSync.sync(manual = false)
        }
    }

    Column(modifier.fillMaxWidth()) {
        EyebrowHeader(
            text = if (followed > 0) "FOLLOWING · $followed" else "FOLLOWING",
            progress = sync.progress,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (followed < SearchAlwaysShownAt && !state.managing) {
                        HeaderAction(icon = DuskReadIcons.Search, label = "Search") {
                            state.searching = !state.searching
                            if (!state.searching) state.query = ""
                        }
                    }
                    if (followed > 0) {
                        HeaderAction(if (sync.running) sync.label else "Sync now") {
                            scope.launch { feedSync.sync() }
                        }
                    }
                    HeaderAction(if (state.managing) "Done" else "Manage") { state.managing = !state.managing }
                }
            },
        )
        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(searchShown) {
            AppTextField(
                value = state.query,
                onValueChange = { state.query = it },
                placeholder = if (followed >= SearchAlwaysShownAt) "Search $followed blogs, or their posts" else "Search by blog, host or topic",
                fontSize = 14.5.sp,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        AnimatedVisibility(state.managing) {
            FeedManagePanel(
                feeds = feedLibrary.feeds,
                url = state.feedUrl,
                onUrlChange = { state.feedUrl = it },
                discovering = state.discovering,
                onAdd = ::follow,
                onRemove = { id ->
                    feedLibrary.remove(id)
                    postCache.removeFeed(id)
                    // The unfollowed blog's posts are gone; its summaries describe nothing.
                    pruneSummaries(summaries, linkLibrary, postCache)
                },
                emptyStateModifier = emptyStateModifier,
            )
        }

        if (groups.isEmpty && !state.managing) {
            CompactEmptyState(
                title = if (state.query.isNotBlank()) "Nothing matches “${state.query}”" else "Follow a blog",
                message = if (state.query.isNotBlank()) {
                    "Try a different blog, host or topic."
                } else {
                    "Follow a blog's RSS or Atom feed to see its posts here."
                },
            )
        }
    }
}

/**
 * The followed blogs as one cloud of names, busiest first and largest, or a list of
 * matching posts while searching. No group headers: size and brightness carry the split.
 */
fun LazyListScope.followingRows(
    state: FollowingState,
    groups: FollowingGroups,
    linkLibrary: LinkLibrary,
    now: Long,
    /** Pixels left for the slider under everything above it, or null when there are none. */
    pickRoom: Int?,
    onOpenTopics: (Feed) -> Unit,
) {
    if (state.managing) return
    val rows = groups.fresh + groups.caughtUp + groups.unsynced
    if (rows.isEmpty()) return
    if (state.query.isBlank()) {
        // One item: the cloud reflows as a whole, and a few dozen short names are cheap.
        item("following-cloud") {
            BlogCloud(rows, now, onOpenTopics, Modifier.animateItem().padding(top = 6.dp))
        }
        item(PicksKey) { PickSlider(groups.picks, now, pickRoom, Modifier.animateItem()) }
        return
    }
    itemsIndexed(rows, key = { _, row -> row.feed.id }) { index, row ->
        // Every match shows its posts: the reader is looking for a post, not a blog.
        Column(Modifier.animateItem().fillMaxWidth()) {
            DigestLine(row = row, now = now, onClick = { onOpenTopics(row.feed) })
            TopicPreview(
                feed = row.feed,
                posts = row.posts,
                linkLibrary = linkLibrary,
                onOpenAll = { onOpenTopics(row.feed) },
            )
            // A hairline, not a gap — the same divider every other list in the app puts
            // between its rows.
            if (index != rows.lastIndex) HairlineDivider()
        }
    }
}

/**
 * A few recent posts from different blogs, side by side, so the screen under a short
 * cloud has something to read without turning the tab into a second feed.
 */
@Composable
private fun PickSlider(picks: List<PostPick>, now: Long, room: Int?, modifier: Modifier = Modifier) {
    if (picks.isEmpty()) return
    val density = LocalDensity.current
    var headPx by remember { mutableStateOf(0) }
    var naturalPx by remember { mutableStateOf(0) }
    // Down to the bar when there is room for more than the card's own height; never less.
    val cardHeight = room?.let { it - headPx }?.takeIf { headPx > 0 && naturalPx > 0 && it > naturalPx }
        ?.let { with(density) { it.toDp() } }
    val open = rememberArticleOpener()
    val listState = rememberLazyListState()
    // Turns through these cards, not one blog: they are what the reader is looking at.
    val queue = remember(picks) {
        ReadingQueue(
            entries = picks.map { ReadingQueueEntry(it.post.url, it.post.title, it.feed.host, it.feed.topic) },
            source = "From your blogs",
            record = OpenRecord.None,
        )
    }
    Column(modifier.fillMaxWidth()) {
        Column(Modifier.onSizeChanged { headPx = it.height }) {
            Spacer(Modifier.height(22.dp))
            EyebrowHeader(text = "FROM YOUR BLOGS", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
        LazyRow(
            state = listState,
            // One card at a time, with the next peeking in so the row reads as swipeable.
            flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
            horizontalArrangement = Arrangement.spacedBy(Space.CardGap),
        ) {
            itemsIndexed(picks, key = { _, it -> it.post.url }) { index, pick ->
                ArticleCard(
                    host = pick.feed.shortLabel,
                    title = pick.post.title,
                    body = pick.excerpt ?: "",
                    timeAgo = pick.post.publishedAt?.let { savedAgo(it, now) },
                    onClick = { open(queue.at(index)) },
                    height = cardHeight,
                    modifier = Modifier
                        .fillParentMaxWidth(PickWidth)
                        // Its own height is the floor, so it is read only while unset.
                        .onSizeChanged { if (cardHeight == null) naturalPx = maxOf(naturalPx, it.height) },
                ) {
                    // Always something here, so the meta line holds its height before
                    // "more" knows whether it is needed.
                    RowMeta("${pick.minutes} min")
                    pick.feed.topic?.let { RowMeta(it.lowercase()) }
                }
            }
        }
    }
}

// Short of the full width, so the next card shows at the edge.
private const val PickWidth = 0.86f

/** The slider's item key, which the tab measures the room above. */
internal const val PicksKey = "following-picks"

/** Blog names wrapping like a magazine index, so the list has no empty boxes to fill. */
@Composable
private fun BlogCloud(rows: List<DigestRow>, now: Long, onOpenTopics: (Feed) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth()) {
        // On one baseline, so mixed sizes read as a line of type rather than a ragged row.
        rows.forEach { row -> CloudName(row, now, Modifier.alignByBaseline()) { onOpenTopics(row.feed) } }
    }
}

/**
 * One blog in the cloud: its short name sized by [cloudTier], with "14" or "3w" set small
 * beside it. Grey, not the accent: the accent stays for what is playing.
 */
@Composable
private fun CloudName(row: DigestRow, now: Long, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fresh = row.newCount > 0
    val tag = if (fresh) "${row.newCount}" else row.lastPostAt?.let { shortAgo(it, now) } ?: "—"
    val text = buildAnnotatedString {
        append(row.feed.shortLabel)
        withStyle(SpanStyle(fontFamily = Mono, fontSize = 10.5.sp, fontWeight = FontWeight.Normal, color = scheme.onSurfaceVariant, letterSpacing = 0.sp, baselineShift = BaselineShift.Superscript)) {
            append("\u200A$tag")
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontSize = DesignTokens.CloudNameSizes[cloudTier(row)].sp,
        fontWeight = if (fresh) FontWeight.Medium else FontWeight.Normal,
        color = if (fresh) scheme.onSurface else scheme.onSurface.copy(alpha = 0.55f),
        maxLines = 1,
        softWrap = false,
        // Padding inside the click target: a name is a small thing to hit one-handed.
        modifier = modifier
            .clip(RoundedCornerShape(Radius.Chip))
            .clickable(onClick = onClick)
            .padding(start = 2.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
    )
}

/**
 * One blog as a search result: its name, then "3 new" or how long since it last posted.
 * All grey — the accent stays for what is playing.
 */
@Composable
private fun DigestLine(row: DigestRow, now: Long, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fresh = row.newCount > 0

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            // The publisher's name when the Notion sync supplied one, the host when it
            // did not — see Feed.label.
            text = row.feed.label,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (fresh) "${row.newCount} new" else row.lastPostAt?.let { shortAgo(it, now) } ?: "—",
            fontFamily = Mono,
            fontSize = 11.sp,
            fontWeight = if (fresh) FontWeight.SemiBold else FontWeight.Normal,
            color = if (fresh) scheme.onSurface else scheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = DuskReadIcons.Chevron,
            contentDescription = "All posts",
            modifier = Modifier.size(11.dp),
            tint = scheme.onSurfaceVariant,
        )
    }
}

/**
 * The posts behind one digest line, revealed on tap: the newest few as flat rows, then
 * the way through to all of them.
 */
@Composable
private fun TopicPreview(feed: Feed, posts: List<FeedPost>, linkLibrary: LinkLibrary, onOpenAll: () -> Unit) {
    // Positioned within the *whole* blog, not the three shown: turning past the third
    // preview row should carry on into the rest of the blog, not stop.
    val queue = posts.readingQueue(feed)

    Column(Modifier.padding(top = 4.dp, bottom = 12.dp)) {
        if (posts.isEmpty()) {
            Text(
                text = "Nothing synced from this blog yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        posts.take(PreviewPosts).forEachIndexed { index, post ->
            TopicRow(
                post = post,
                host = feed.host,
                // Never the last thing in the column — the all-posts row always follows,
                // so every preview row keeps its hairline.
                last = false,
                linkLibrary = linkLibrary,
                queue = queue.at(index),
                topic = feed.topic,
            )
        }

        AllPostsRow(count = posts.size, onClick = onOpenAll)
    }
}

/**
 * The door to [TopicsScreen], as a row rather than a card at the end of a strip.
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
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = DuskReadIcons.Chevron,
            contentDescription = null,
            modifier = Modifier.size(11.dp),
            tint = MaterialTheme.colorScheme.onSurface,
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
            // Sits low in the remaining viewport, the same treatment Saved gives its own
            // first-run state.
            Box(Modifier.fillMaxWidth().then(emptyStateModifier), contentAlignment = Alignment.BottomStart) {
                EmptyState(
                    title = "Nothing followed yet",
                    message = "Paste a blog's address above, or its RSS or Atom feed directly — " +
                        "DuskRead finds the feed behind a homepage on its own.",
                )
            }
        } else {
            // Flush on the background with a hairline between rows, like every other list
            // in the app.
            Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                feeds.forEachIndexed { index, feed ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            // The full path, not just the host.
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
