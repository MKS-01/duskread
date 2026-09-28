package dev.mks.duskread.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
import dev.mks.duskread.links.SearchAlwaysShownAt
import dev.mks.duskread.links.discoverFeedUrl
import dev.mks.duskread.links.looksLikeUrl
import dev.mks.duskread.links.normaliseUrl
import dev.mks.duskread.links.pruneSummaries
import dev.mks.duskread.links.shortAgo
import dev.mks.duskread.summary.rememberSummaryCache
import dev.mks.duskread.ui.common.AppTextField
import dev.mks.duskread.ui.common.CompactEmptyState
import dev.mks.duskread.ui.common.EmptyState
import dev.mks.duskread.ui.common.EyebrowHeader
import dev.mks.duskread.ui.common.HairlineDivider
import dev.mks.duskread.ui.common.HeaderAction
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
 * The followed blogs as lazy items — tiles under NEW, CAUGHT UP and NO POSTS YET, or a
 * list of matching posts while searching — so a long list builds only what is
 * on screen.
 */
fun LazyListScope.followingRows(
    state: FollowingState,
    groups: FollowingGroups,
    linkLibrary: LinkLibrary,
    now: Long,
    onOpenTopics: (Feed) -> Unit,
) {
    if (state.managing) return
    group("new", "NEW", groups.fresh, state, linkLibrary, now, onOpenTopics)
    group("caught-up", "CAUGHT UP", groups.caughtUp, state, linkLibrary, now, onOpenTopics)
    group("unsynced", "NO POSTS YET", groups.unsynced, state, linkLibrary, now, onOpenTopics)
}

private fun LazyListScope.group(
    key: String,
    label: String,
    rows: List<DigestRow>,
    state: FollowingState,
    linkLibrary: LinkLibrary,
    now: Long,
    onOpenTopics: (Feed) -> Unit,
) {
    if (rows.isEmpty()) return
    item("following-$key") {
        // Muted: these sit under FOLLOWING, which keeps the section's own tint.
        EyebrowHeader(
            text = "$label · ${rows.size}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.animateItem().padding(top = 18.dp, bottom = 4.dp),
        )
    }
    // Browsing is tiles; a search is a list of posts, which a tile cannot show.
    if (state.query.isBlank()) {
        if (key == "new") bento(rows, now, onOpenTopics) else compactGrid(rows, now, onOpenTopics)
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
 * NEW, laid out unevenly on purpose: the most recent blog across the full width with its
 * two newest titles, then threes — one tall tile beside two small — flipping sides each
 * time, so a long run of tiles does not settle into a spreadsheet.
 */
private fun LazyListScope.bento(rows: List<DigestRow>, now: Long, onOpen: (Feed) -> Unit) {
    val lead = rows.first()
    item("new-lead-${lead.feed.id}") {
        FeaturedTile(lead, now, Modifier.animateItem().padding(bottom = TileGap)) { onOpen(lead.feed) }
    }
    val rest = rows.drop(1).chunked(3)
    rest.forEachIndexed { index, chunk ->
        item("new-${chunk.joinToString("+") { it.feed.id }}") {
            TileRow(Modifier.animateItem()) {
                when (chunk.size) {
                    3 -> {
                        val tall: @Composable () -> Unit = {
                            TallTile(chunk[0], now, Modifier.weight(1f).fillMaxHeight()) { onOpen(chunk[0].feed) }
                        }
                        val pair: @Composable () -> Unit = {
                            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(TileGap)) {
                                SmallTile(chunk[1], now, Modifier.weight(1f).fillMaxWidth()) { onOpen(chunk[1].feed) }
                                SmallTile(chunk[2], now, Modifier.weight(1f).fillMaxWidth()) { onOpen(chunk[2].feed) }
                            }
                        }
                        if (index % 2 == 0) {
                            tall()
                            pair()
                        } else {
                            pair()
                            tall()
                        }
                    }
                    2 -> chunk.forEach { row ->
                        TallTile(row, now, Modifier.weight(1f).fillMaxHeight()) { onOpen(row.feed) }
                    }
                    else -> SmallTile(chunk[0], now, Modifier.weight(1f)) { onOpen(chunk[0].feed) }
                }
            }
        }
    }
}

/** CAUGHT UP and NO POSTS YET: small tiles two across, since there is nothing to preview. */
private fun LazyListScope.compactGrid(rows: List<DigestRow>, now: Long, onOpen: (Feed) -> Unit) {
    items(rows.chunked(2), key = { pair -> pair.joinToString("+") { it.feed.id } }) { pair ->
        TileRow(Modifier.animateItem()) {
            pair.forEach { row -> SmallTile(row, now, Modifier.weight(1f).fillMaxHeight()) { onOpen(row.feed) } }
            // An odd one out keeps half the width, not the whole row.
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun TileRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = TileGap),
        horizontalArrangement = Arrangement.spacedBy(TileGap),
        content = content,
    )
}

/** The bordered shape every tile shares — [ArticleCard]'s radius and hairline. */
@Composable
private fun Tile(modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(Radius.Card)
    Column(
        modifier
            .clip(shape)
            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        content = content,
    )
}

@Composable
private fun FeaturedTile(row: DigestRow, now: Long, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Tile(modifier.fillMaxWidth(), onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TileName(row, Modifier.weight(1f), lines = 1)
            Spacer(Modifier.width(10.dp))
            TileMeta(row, now)
        }
        row.unsaved.take(2).forEach { post ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TallTile(row: DigestRow, now: Long, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Tile(modifier, onClick) {
        TileName(row, lines = 2)
        // Two, so a short first title does not leave the tall side half empty beside its
        // stacked pair.
        row.unsaved.take(2).forEach {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it.title,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
        TileMeta(row, now)
    }
}

@Composable
private fun SmallTile(row: DigestRow, now: Long, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Tile(modifier, onClick) {
        TileName(row, lines = 2)
        Spacer(Modifier.weight(1f).heightIn(min = 10.dp))
        TileMeta(row, now)
    }
}

@Composable
private fun TileName(row: DigestRow, modifier: Modifier = Modifier, lines: Int) {
    val scheme = MaterialTheme.colorScheme
    Text(
        // The feed's own name once a sync has read it, the host until then — see
        // Feed.label.
        text = row.feed.label,
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
        color = if (row.newCount > 0) scheme.onSurface else scheme.onSurface.copy(alpha = 0.78f),
        modifier = modifier,
    )
}

/** "3 new · 4h" or the last post's age, in grey — the accent stays for what is playing. */
@Composable
private fun TileMeta(row: DigestRow, now: Long) {
    val scheme = MaterialTheme.colorScheme
    val fresh = row.newCount > 0
    val age = row.lastPostAt?.let { shortAgo(it, now) }
    Text(
        text = if (fresh) "${row.newCount} new · ${age ?: "—"}" else age ?: "no posts yet",
        fontFamily = Mono,
        fontSize = 11.sp,
        fontWeight = if (fresh) FontWeight.SemiBold else FontWeight.Normal,
        color = if (fresh) scheme.onSurface else scheme.onSurfaceVariant,
    )
}

/** Between tiles, both ways — [Space.CardGap], which cards on Home already use. */
private val TileGap = Space.CardGap

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
