package dev.mks.duskread.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.ReadingSignals
import dev.mks.duskread.links.Scored
import dev.mks.duskread.links.pool
import dev.mks.duskread.links.rank
import dev.mks.duskread.links.syncFeeds
import dev.mks.duskread.links.topPicks
import dev.mks.duskread.pomodoro.PickableMinutes
import dev.mks.duskread.pomodoro.clockLabel
import dev.mks.duskread.pomodoro.rememberPomodoroController
import dev.mks.duskread.reader.AudioPlayer
import dev.mks.duskread.reader.ReadItem
import dev.mks.duskread.reader.ReadSort
import dev.mks.duskread.reader.ReaderSource
import dev.mks.duskread.reader.readbackSupported
import dev.mks.duskread.reader.rememberReadRepository
import dev.mks.duskread.ui.common.CompactEmptyState
import dev.mks.duskread.ui.common.EyebrowHeader
import dev.mks.duskread.ui.common.ListRow
import dev.mks.duskread.ui.common.RowMeta
import dev.mks.duskread.ui.common.ToastRequest
import dev.mks.duskread.ui.common.WaveformMeter
import dev.mks.duskread.ui.reader.formatDuration
import dev.mks.duskread.ui.rememberUrlOpener
import dev.mks.duskread.ui.theme.CodeStyle
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.SectionLabel
import dev.mks.duskread.ui.theme.Stroke
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Home: a dashboard rather than a list. Four sections that earn their own
 * weight instead of four identically-boxed cards — each opens with the same
 * label-and-rule cadence and sits flush on the background, the way every
 * other screen in the Amplitude direction does. Something saved to read and
 * something already turned into audio lead the screen, ahead of the focus
 * timer: the content pick answers "what am I opening this for", and the
 * timer is only useful once that's decided.
 */
@Composable
fun DashboardTab(
    onOpenFocus: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenReadback: () -> Unit,
    onOpenFollowing: () -> Unit,
    links: LinkLibrary,
    signals: ReadingSignals,
    player: AudioPlayer,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    feedClient: HttpClient,
    greeting: String?,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            // The one thing on this screen that can go stale without the
            // reader doing anything — saved links and the readback library
            // are both re-read reactively the moment their own tab opens,
            // but nobody re-fetches a followed blog until Sync now is
            // tapped. This is the same call that button makes.
            if (feeds.feeds.isEmpty()) return@PullToRefreshBox
            scope.launch {
                refreshing = true
                val synced = syncFeeds(feedClient, feeds.feeds, feedPosts)
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
            // The greeting only appears if a name was given — no "Hello, there"
            // fallback, which reads worse than nothing — but the row itself
            // always shows, since Settings needs somewhere to live either way.
            // The same hairline-and-softened-corner language as the sort
            // chips, not a circle button — a bare glyph read as too slight
            // next to them, and this keeps it one system rather than two.
            item("head") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 22.dp)) {
                    greeting?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                    } ?: Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = DuskReadIcons.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(Radius.Chip))
                            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Chip))
                            .clickable(onClick = onOpenSettings)
                            .padding(9.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Leads the screen — a specific thing to read, ahead of the general
            // habit prompt below it.
            item("next-up") {
                NextUpSection(links = links, signals = signals, feeds = feeds, feedPosts = feedPosts, onOpenSaved = onOpenSaved)
            }
            item("readback") { ReadbackSection(player = player, onOpen = onOpenReadback) }
            item("focus") { FocusSection(onOpen = onOpenFocus) }
            item("following") {
                FollowingShortcut(feeds = feeds, feedPosts = feedPosts, links = links, onOpen = onOpenFollowing)
            }
        }
    }
}

/** Vertical gap between one flat section and the next. */
private val SectionGap = 28.dp

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
 * The newest read, or the one actually playing — and playable from here.
 *
 * It used to show `listReads().first()` with a meter hardcoded to zero and a
 * tap that only opened the tab, which meant the one section on Home about
 * audio was the one section that could not tell you any audio was running.
 * Start something from the Readback tab, come back, and this still showed a
 * flat bar under a different title.
 *
 * So: whatever is playing wins over whatever is newest — the read in your
 * ears is more "today" than the newest file on disk — the meter follows the
 * real position, and the row plays rather than navigates. Tapping a read to
 * play it is what the same row does in the Readback tab, and two rows that
 * look identical should not do different things.
 */
@Composable
private fun ReadbackSection(
    player: AudioPlayer,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = rememberReadRepository()
    val source by repository.source.collectAsState()
    val playback by player.state.collectAsState()
    var newest by remember { mutableStateOf<ReadItem?>(null) }

    LaunchedEffect(source) {
        newest = if (source == ReaderSource.READY) {
            repository.listReads(query = "", sort = ReadSort.NEWEST).firstOrNull()
        } else {
            null
        }
    }

    val item = playback.item ?: newest
    val playing = item != null && playback.item?.id == item.id

    Column(modifier.fillMaxWidth().padding(bottom = SectionGap)) {
        EyebrowHeader(
            text = "TODAY'S READBACK",
            icon = DuskReadIcons.Waveform,
            // The way to the full library, kept off the row now that the row
            // itself plays.
            trailing = if (item != null) {
                {
                    Icon(
                        imageVector = DuskReadIcons.Chevron,
                        contentDescription = "Open Readback",
                        modifier = Modifier
                            .size(26.dp)
                            .clickable(onClick = onOpen)
                            .padding(7.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
        )
        Spacer(Modifier.height(12.dp))

        when {
            // Said plainly rather than as a prompt: on a platform with no
            // folder to point at, "connect your library" is an instruction
            // that cannot be followed, and a reader who tries it finds the
            // Readback tab explaining the same thing from the other end.
            !readbackSupported() -> {
                CompactEmptyState(
                    title = "Readback needs a device",
                    message = "Reads are audio files synced onto a phone or a Mac. There's nowhere here to keep them.",
                )
            }

            source != ReaderSource.READY -> {
                CompactEmptyState(
                    title = "Connect your readback library",
                    message = "Point the Readback tab at a synced readback-audio-db folder to see your latest reads here.",
                )
            }

            item == null -> CompactEmptyState(title = "Nothing read yet", message = null)

            else -> Column(
                Modifier.fillMaxWidth().clickable {
                    if (playing) player.togglePlayPause() else player.play(item)
                },
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        // The playing row is the only coloured thing on the
                        // screen, the same rule the Readback tab follows.
                        color = if (playing) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        imageVector = if (playing && playback.playing) DuskReadIcons.Pause else DuskReadIcons.Play,
                        contentDescription = if (playing && playback.playing) "Pause" else "Play",
                        modifier = Modifier.size(15.dp).padding(top = 2.dp),
                        tint = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (playing && playback.durationSec > 0f) {
                        "${formatDuration(playback.positionSec.toDouble())} / ${formatDuration(playback.durationSec.toDouble())}"
                    } else {
                        formatDuration(item.durationSec)
                    },
                    fontFamily = Mono,
                    fontSize = 10.5.sp,
                    color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(9.dp))
                WaveformMeter(
                    progress = if (playing && playback.durationSec > 0f) {
                        (playback.positionSec / playback.durationSec).coerceIn(0f, 1f)
                    } else {
                        0f
                    },
                    modifier = Modifier.height(15.dp),
                    seed = item.id.hashCode(),
                )
            }
        }
    }
}

/**
 * The door to the Following tab: a summary, not the digest that used to sit
 * here.
 *
 * That digest — every followed feed, each expandable into its own posts — is
 * a real screen's worth of content once more than a couple of blogs are
 * followed, and Home is not where a screen's worth of anything belongs. What
 * stays here is the shape of what's waiting, not all of it: the total, and
 * the two or three feeds actually carrying it. A single count line said the
 * same thing in fewer words but read as an afterthought under Focus; naming
 * the blogs is what makes this a summary rather than a number.
 */
@Composable
private fun FollowingShortcut(
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    links: LinkLibrary,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Sorted rather than filtered to exactly the unread: a feed with nothing
    // new yet is still worth naming if it's one of only two followed, so the
    // cutoff is "the top few" rather than "only the ones with something new".
    val byNewest = remember(feeds.feeds, feedPosts.postsByFeed, links.links) {
        feeds.feeds.map { feed ->
            feed to feedPosts.postsByFeed[feed.id].orEmpty().count { !links.isSaved(it.url) }
        }.sortedByDescending { it.second }
    }
    val newCount = byNewest.sumOf { it.second }

    Column(modifier.fillMaxWidth().padding(bottom = SectionGap)) {
        EyebrowHeader(
            text = "FOLLOWING",
            trailing = if (feeds.feeds.isNotEmpty()) {
                {
                    Text(
                        text = if (newCount > 0) "$newCount new" else "—",
                        fontFamily = Mono,
                        fontSize = 12.sp,
                        fontWeight = if (newCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (newCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
        )
        Spacer(Modifier.height(12.dp))

        if (feeds.feeds.isEmpty()) {
            CompactEmptyState(
                title = "Follow a blog",
                message = "Its new posts will show up here, and in full on the Following tab.",
                onClick = onOpen,
            )
        } else {
            Column(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
                Text(
                    text = "${feeds.feeds.size} feed${if (feeds.feeds.size == 1) "" else "s"} followed",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))

                byNewest.take(FollowingPreviewRows).forEach { (feed, count) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            // Jost, not mono — a feed's name is a name, and
                            // the tokens doc reserves Inconsolata for a
                            // reported value, not a section's own label. See
                            // the matching note on `DigestLine` in
                            // FollowingSection.kt.
                            text = feed.label,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (count > 0) "$count new" else "—",
                            fontFamily = Mono,
                            fontSize = 11.sp,
                            color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (feeds.feeds.size > FollowingPreviewRows) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${feeds.feeds.size - FollowingPreviewRows} more",
                            style = SectionLabel,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = DuskReadIcons.Chevron,
                            contentDescription = "Open Following",
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/** How many feeds [FollowingShortcut] names before handing over to [FollowingTab]. */
private const val FollowingPreviewRows = 3

/**
 * The one section on Home that makes a *choice* rather than reporting local
 * state — and now it chooses over both halves of the app.
 *
 * The pool is every unread saved link plus every cached post from a followed
 * blog that is not already saved, ranked by [rank]. That merge is the point:
 * `FeedPostCache` holds dozens of real, dated posts that were previously
 * reachable only by tapping a digest line open, so the app's best content sat
 * one deliberate gesture away from the section meant to say "here, read this".
 *
 * A hero and two runners-up rather than a single pick, because one row reads
 * as a decree and a list reads as a browse. Three is enough to feel chosen
 * from without the section becoming a fourth list.
 *
 * No boxes: rows sit flush on the background under their own hairline, the
 * same skeleton every other list in the app uses. A recommendation is not a
 * different kind of thing and should not look like one.
 */
@Composable
private fun NextUpSection(
    links: LinkLibrary,
    signals: ReadingSignals,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val open = rememberUrlOpener()

    // Only the *length*, not the countdown: mapped and de-duplicated so a
    // running timer does not recompose this section once a second for a
    // number it does not draw. A five-minute session should not be offered a
    // twenty-minute essay; nothing else about the timer matters here.
    val controller = rememberPomodoroController()
    val focusMinutes by remember(controller) {
        controller.state.map { it.totalSeconds.takeIf { seconds -> seconds > 0 }?.div(60) }.distinctUntilChanged()
    }.collectAsState(initial = null)

    // The seed *is* the shuffle. Re-seeding re-ranks without abandoning the
    // ranking, so a shuffle offers something else good rather than anything
    // at all. Started from the day so the pick is stable across a morning's
    // worth of openings rather than re-rolling on every recomposition.
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
    ) {
        rank(
            candidates = pool(links, feedPosts, feeds.feeds),
            signals = signals,
            now = Clock.System.now().toEpochMilliseconds(),
            seed = day + shuffles,
            focusMinutes = focusMinutes,
        )
    }

    // At most one row per source. Two followed blogs can be near-duplicates by
    // design — JetBrains publishes both a general and a Kotlin feed — so a
    // straight take(3) can spend the whole section on one publisher.
    val picks = remember(ranked) { topPicks(ranked, count = 3) }
    val hero = picks.firstOrNull()
    val runnersUp = picks.drop(1)

    Column(modifier.fillMaxWidth().padding(bottom = SectionGap)) {
        EyebrowHeader(
            text = "NEXT UP",
            trailing = if (ranked.size > 1) {
                {
                    Icon(
                        imageVector = DuskReadIcons.Shuffle,
                        contentDescription = "Show a different pick",
                        modifier = Modifier
                            .size(26.dp)
                            .clickable {
                                // Recorded before the re-seed: the skip is
                                // about the thing that was on screen.
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
                onOpen = { openCandidate(hero, links, signals, open) },
            )
            runnersUp.forEachIndexed { index, scored ->
                NextUpRow(
                    scored = scored,
                    hero = false,
                    last = index == runnersUp.lastIndex,
                    onOpen = { openCandidate(scored, links, signals, open) },
                )
            }
        }
    }
}

/**
 * Opening a candidate is also the moment its signal is recorded, and for a
 * feed post it is the moment it becomes the reader's own.
 *
 * That save is not a convenience. `FeedPostCache` is replaced wholesale on
 * the next sync, so a post read straight out of it would be read and
 * forgotten — the record of having read it would go with the cache.
 */
private fun openCandidate(
    scored: Scored,
    links: LinkLibrary,
    signals: ReadingSignals,
    open: (String) -> Unit,
) {
    val candidate = scored.candidate
    open(candidate.url)

    val id = candidate.savedId ?: links.save(candidate.url, candidate.title, candidate.tag)?.id
    id?.let { links.toggleRead(it) }
    signals.recordRead(candidate.url)
    // The other half of the topic term: without this, tags are read on every
    // candidate and never credited to anything, and topic affinity stays the
    // zero it has always been.
    candidate.tag?.let { signals.recordTopicRead(it) }
}

/**
 * One row of the section. The hero gets two lines of title, a runner-up one —
 * the difference in weight is what makes the first one a recommendation and
 * the rest alternatives, without either of them needing a label saying so.
 *
 * The meta line is the house's two facts, three when a topic is known: host,
 * subject, and how long this will take. The subject takes the middle slot
 * because it is the one that says *why this*, and it is simply absent for a
 * candidate from a feed Notion has not filed.
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
