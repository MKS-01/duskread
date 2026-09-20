package dev.mks.duskread.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.ReadingSignals
import dev.mks.duskread.links.Scored
import dev.mks.duskread.links.latestPosts
import dev.mks.duskread.links.pool
import dev.mks.duskread.links.rank
import dev.mks.duskread.links.syncFeeds
import dev.mks.duskread.links.topPicks
import dev.mks.duskread.pomodoro.PickableMinutes
import dev.mks.duskread.pomodoro.clockLabel
import dev.mks.duskread.pomodoro.rememberPomodoroController
import dev.mks.duskread.summary.rememberSummaryCache
import dev.mks.duskread.ui.OpenRecord
import dev.mks.duskread.ui.ReadingQueue
import dev.mks.duskread.ui.ReadingQueueEntry
import dev.mks.duskread.ui.common.CompactEmptyState
import dev.mks.duskread.ui.common.EyebrowHeader
import dev.mks.duskread.ui.common.ListRow
import dev.mks.duskread.ui.common.RowMeta
import dev.mks.duskread.ui.common.ToastRequest
import dev.mks.duskread.ui.common.WaveformMeter
import dev.mks.duskread.ui.rememberArticleOpener
import dev.mks.duskread.ui.theme.CodeStyle
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.Stroke
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Home: a dashboard rather than a list.
 */
@Composable
fun DashboardTab(
    onOpenFocus: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenFollowing: () -> Unit,
    links: LinkLibrary,
    signals: ReadingSignals,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    feedClient: HttpClient,
    greeting: String?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val summaries = rememberSummaryCache()
    val open = rememberArticleOpener()
    var refreshing by remember { mutableStateOf(false) }

    // The day, not the instant: recomputing the week every recomposition would re-cut it
    // on a clock tick nothing else on this screen can see.
    val day = remember { Clock.System.now().toEpochMilliseconds() / DayMs }
    val latest = remember(feedPosts.postsByFeed, feeds.feeds, links.links, day) {
        latestPosts(feeds = feeds.feeds, cache = feedPosts, links = links, now = Clock.System.now().toEpochMilliseconds())
    }
    val bodies = rememberCardBodies(latest, feedPosts)

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            // The one thing on this screen that can go stale without the reader doing
            // anything.
            if (feeds.feeds.isEmpty()) return@PullToRefreshBox
            scope.launch {
                refreshing = true
                val synced = syncFeeds(feedClient, feeds.feeds, feedPosts, links, summaries)
                ToastRequest.show(
                    when {
                        synced == 0 -> "Couldn't reach any feed."
                        synced == feeds.feeds.size -> "Synced $synced feed${if (synced == 1) "" else "s"}."
                        else -> "Synced $synced of ${feeds.feeds.size} feeds."
                    },
                )
                refreshing = false
            }
        },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
        ) {
            // Settings used to live only here, behind a gear next to the greeting — the
            // row stayed even with no name to show, purely to give it somewhere to be.
            greeting?.let {
                item("head") {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 22.dp),
                    )
                }
            }

            // Only on a genuine first run — nothing saved, nothing followed — and gone
            // the moment either exists.
            if (links.links.isEmpty() && feeds.feeds.isEmpty()) {
                item("welcome") { WelcomeSection() }
            }

            // First, and small: what the reader came to do, before what there is to read.
            item("focus") { FocusSection(onOpen = onOpenFocus) }

            // The body of the screen now — the week itself rather than a count of it.
            latestSection(
                items = latest,
                bodies = bodies,
                hasFeeds = feeds.feeds.isNotEmpty(),
                onOpen = open,
                onFollow = onOpenFollowing,
            )

            // Last, and still a choice rather than a list: what to read when the week has
            // already been looked at.
            item("recommended") {
                NextUpSection(
                    links = links,
                    signals = signals,
                    feeds = feeds,
                    feedPosts = feedPosts,
                    // Nothing on this screen twice: the cards above already offered these.
                    exclude = remember(latest) { latest.mapTo(mutableSetOf()) { it.url } },
                    onOpenSaved = onOpenSaved,
                )
            }
        }
    }
}

/** Long enough that the week is not re-cut on every recomposition; see its one use. */
private const val DayMs = 86_400_000L

/** Vertical gap between one flat section and the next. */
internal val SectionGap = 28.dp

@Composable
private fun FocusSection(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val controller = rememberPomodoroController()
    val state by controller.state.collectAsState()

    Column(modifier.fillMaxWidth().padding(bottom = SectionGap)) {
        EyebrowHeader(
            text = "FOCUS",
            icon = if (!state.idle && state.running) DuskReadIcons.Pause else DuskReadIcons.Play,
        )
        Spacer(Modifier.height(12.dp))

        if (state.idle) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickableMinutes.forEach { minutes ->
                    PillButton(text = "$minutes min") { controller.start(minutes) }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
                Text(
                    text = state.clockLabel,
                    style = CodeStyle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.running) "Running — tap to open" else "Paused — tap to open",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A small bordered pill, the same `.pill` shape as the sort chips on Readback — never filled. */
@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    Text(
        text = text.uppercase(),
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.Inline))
            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Inline))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
    )
}

/**
 * A first look at the app, not a first look at emptiness.
 */
@Composable
private fun WelcomeSection(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(bottom = SectionGap)) {
        WaveformMeter(
            progress = 0f,
            modifier = Modifier.height(16.dp),
            barCount = 20,
            flat = true,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Welcome to DuskRead",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Save a link, follow a blog, hear it read back. Whatever you add " +
                "shows up below, ready whenever you are.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The one section on Home that makes a *choice* rather than reporting local state — and
 * now it chooses over both halves of the app.
 */

@Composable
private fun NextUpSection(
    links: LinkLibrary,
    signals: ReadingSignals,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    /** Already on the screen above, as a card. */
    exclude: Set<String>,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val open = rememberArticleOpener()

    // Only the *length*, not the countdown: mapped and de-duplicated so a running timer
    // does not recompose this section once a second for a number it does not draw.
    val controller = rememberPomodoroController()
    val focusMinutes by remember(controller) {
        controller.state.map { it.totalSeconds.takeIf { seconds -> seconds > 0 }?.div(60) }.distinctUntilChanged()
    }.collectAsState(initial = null)

    // The seed *is* the shuffle. Re-seeding re-ranks without abandoning the ranking, so a
    // shuffle offers something else good rather than anything at all.
    var shuffles by remember { mutableStateOf(0) }
    val day = remember { (Clock.System.now().toEpochMilliseconds() / 86_400_000L).toInt() }

    val ranked = remember(
        links.links,
        feedPosts.postsByFeed,
        feeds.feeds,
        signals.byHost,
        signals.topicReads,
        signals.skippedPosts,
        shuffles,
        focusMinutes,
        exclude,
    ) {
        rank(
            // Filtered before ranking rather than after: dropping a pick afterwards
            // would leave the section short of the three it means to offer.
            candidates = pool(links, feedPosts, feeds.feeds).filterNot { it.url in exclude },
            signals = signals,
            now = Clock.System.now().toEpochMilliseconds(),
            seed = day + shuffles,
            focusMinutes = focusMinutes,
        )
    }

    // At most one row per source.
    val picks = remember(ranked) { topPicks(ranked, count = 3) }

    // Opening a pick is the moment it becomes the reader's own — the save is not a
    // convenience — and that has to hold for the second and third as much as the first.
    val queue = remember(picks) {
        ReadingQueue(
            entries = picks.map { ReadingQueueEntry(it.candidate.url, it.candidate.title, it.candidate.host, it.candidate.tag) },
            source = "Recommended",
            record = OpenRecord.SaveAndMarkRead,
        )
    }
    val hero = picks.firstOrNull()
    val runnersUp = picks.drop(1)

    Column(modifier.fillMaxWidth().padding(bottom = SectionGap)) {
        EyebrowHeader(
            text = "RECOMMENDED",
            trailing = if (ranked.size > 1) {
                {
                    Icon(
                        imageVector = DuskReadIcons.Shuffle,
                        contentDescription = "Show a different pick",
                        modifier = Modifier
                            .size(26.dp)
                            .clickable {
                                // Recorded before the re-seed: the skip is about the
                                // thing that was on screen.
                                hero?.let { signals.recordSkip(it.candidate.url) }
                                shuffles++
                            }
                            .padding(6.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
        )
        Spacer(Modifier.height(12.dp))

        if (hero == null) {
            CompactEmptyState(
                title = if (links.links.isEmpty()) "Nothing saved yet" else "All caught up",
                message = if (links.links.isEmpty()) {
                    "Share an article to DuskRead, or paste its address in the Saved tab."
                } else {
                    "Every saved link has been read, and no followed blog has anything new."
                },
                onClick = if (links.links.isEmpty()) null else onOpenSaved,
            )
        } else {
            NextUpRow(
                scored = hero,
                hero = true,
                last = runnersUp.isEmpty(),
                onOpen = { open(queue.at(0)) },
            )
            runnersUp.forEachIndexed { index, scored ->
                NextUpRow(
                    scored = scored,
                    hero = false,
                    last = index == runnersUp.lastIndex,
                    onOpen = { open(queue.at(index + 1)) },
                )
            }
        }
    }
}

/**
 * One row of the section.
 */
@Composable
private fun NextUpRow(scored: Scored, hero: Boolean, last: Boolean, onOpen: () -> Unit) {
    ListRow(
        host = scored.candidate.host,
        title = scored.candidate.title,
        last = last,
        onClick = onOpen,
        titleMaxLines = if (hero) 2 else 1,
    ) {
        RowMeta(scored.candidate.host)
        scored.candidate.tag?.let { RowMeta(it.lowercase()) }
        RowMeta("${scored.minutes} min")
    }
}
