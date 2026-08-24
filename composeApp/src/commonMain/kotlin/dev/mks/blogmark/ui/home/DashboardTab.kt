package dev.mks.blogmark.ui.home

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
import dev.mks.blogmark.links.Feed
import dev.mks.blogmark.links.FeedLibrary
import dev.mks.blogmark.links.FeedPostCache
import dev.mks.blogmark.links.LinkLibrary
import dev.mks.blogmark.links.syncFeeds
import dev.mks.blogmark.pomodoro.PickableMinutes
import dev.mks.blogmark.pomodoro.clockLabel
import dev.mks.blogmark.pomodoro.rememberPomodoroController
import dev.mks.blogmark.reader.ReadItem
import dev.mks.blogmark.reader.ReadSort
import dev.mks.blogmark.reader.ReaderSource
import dev.mks.blogmark.reader.rememberReadRepository
import dev.mks.blogmark.ui.common.ChipSize
import dev.mks.blogmark.ui.common.CompactEmptyState
import dev.mks.blogmark.ui.common.EyebrowHeader
import dev.mks.blogmark.ui.common.MonogramBadge
import dev.mks.blogmark.ui.common.ToastRequest
import dev.mks.blogmark.ui.common.WaveformMeter
import dev.mks.blogmark.ui.reader.formatDuration
import dev.mks.blogmark.ui.rememberUrlOpener
import dev.mks.blogmark.ui.theme.BlogmarkIcons
import dev.mks.blogmark.ui.theme.CodeStyle
import dev.mks.blogmark.ui.theme.Mono
import dev.mks.blogmark.ui.theme.Radius
import dev.mks.blogmark.ui.theme.Stroke
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch

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
    links: LinkLibrary,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    feedClient: HttpClient,
    greeting: String?,
    onOpenSettings: () -> Unit,
    onOpenTopics: (Feed) -> Unit,
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
            // A bare glyph, not a circle button: the greeting row is chrome,
            // not a card, and nothing else on it has a background either.
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
                        imageVector = BlogmarkIcons.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier
                            .size(34.dp)
                            .clickable(onClick = onOpenSettings)
                            .padding(9.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Leads the screen — a specific thing to read, ahead of the general
            // habit prompt below it.
            item("saved-pick") { SavedPickSection(links = links, onOpenSaved = onOpenSaved) }
            item("readback") { ReadbackSection(onOpen = onOpenReadback) }
            item("focus") { FocusSection(onOpen = onOpenFocus) }
            item("following") {
                FollowingDigest(
                    feedLibrary = feeds,
                    postCache = feedPosts,
                    linkLibrary = links,
                    client = feedClient,
                    onOpenTopics = onOpenTopics,
                )
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
            icon = if (!state.idle && state.running) BlogmarkIcons.Pause else BlogmarkIcons.Play,
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

@Composable
private fun ReadbackSection(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val repository = rememberReadRepository()
    val source by repository.source.collectAsState()
    var latest by remember { mutableStateOf<ReadItem?>(null) }

    LaunchedEffect(source) {
        latest = if (source == ReaderSource.READY) {
            repository.listReads(query = "", sort = ReadSort.NEWEST).firstOrNull()
        } else {
            null
        }
    }

    Column(modifier.fillMaxWidth().padding(bottom = SectionGap)) {
        EyebrowHeader(text = "TODAY'S READBACK", icon = BlogmarkIcons.Waveform)
        Spacer(Modifier.height(12.dp))

        val item = latest
        when {
            source != ReaderSource.READY -> {
                CompactEmptyState(
                    title = "Connect your readback library",
                    message = "Point the Readback tab at a synced readback-audio-db folder to see your latest reads here.",
                )
            }

            item == null -> CompactEmptyState(title = "Nothing read yet", message = null)

            else -> Column(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatDuration(item.durationSec),
                    fontFamily = Mono,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(9.dp))
                WaveformMeter(
                    progress = 0f,
                    modifier = Modifier.height(15.dp),
                    seed = item.id.hashCode(),
                )
            }
        }
    }
}

/**
 * A pick from the saved links rather than the readback library — every other
 * section is either bundled content or local state, and this is where
 * "something to read" comes from now that there is no curriculum to browse
 * instead. Only unread links are candidates, so this never repeats
 * [ReadbackSection]'s job of surfacing something already finished.
 */
@Composable
private fun SavedPickSection(links: LinkLibrary, onOpenSaved: () -> Unit, modifier: Modifier = Modifier) {
    val open = rememberUrlOpener()
    val unread = links.links.filterNot { it.read }
    var pick by remember(unread.map { it.id }) { mutableStateOf(unread.randomOrNull()) }

    Column(modifier.fillMaxWidth().padding(bottom = SectionGap)) {
        EyebrowHeader(
            text = "FROM SAVED",
            trailing = if (unread.size > 1) {
                {
                    Icon(
                        imageVector = BlogmarkIcons.Shuffle,
                        contentDescription = "Show a different pick",
                        modifier = Modifier
                            .size(26.dp)
                            .clickable { pick = unread.filterNot { it.id == pick?.id }.random() }
                            .padding(6.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
        )
        Spacer(Modifier.height(12.dp))

        val found = pick
        if (found == null) {
            CompactEmptyState(
                title = if (links.links.isEmpty()) "Nothing saved yet" else "All caught up",
                message = if (links.links.isEmpty()) {
                    "Share an article to Blogmark, or paste its address in the Saved tab."
                } else {
                    "Every saved link has been read — tap to see them."
                },
                onClick = if (links.links.isEmpty()) null else onOpenSaved,
            )
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        open(found.url)
                        links.toggleRead(found.id)
                    },
                verticalAlignment = Alignment.Top,
            ) {
                MonogramBadge(host = found.host, size = ChipSize)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = found.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = found.host,
                        fontFamily = Mono,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
