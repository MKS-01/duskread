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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.mks.duskread.data.LocalAppGraph
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LatestTickMs
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.ReadingSignals
import dev.mks.duskread.links.latestPosts
import dev.mks.duskread.pomodoro.PickableMinutes
import dev.mks.duskread.pomodoro.clockLabel
import dev.mks.duskread.pomodoro.elapsedFraction
import dev.mks.duskread.pomodoro.rememberPomodoroController
import dev.mks.duskread.speech.SpeechSession
import dev.mks.duskread.speech.speechSupported
import dev.mks.duskread.speech.weekListenQueue
import dev.mks.duskread.ui.common.EyebrowHeader
import dev.mks.duskread.ui.common.WaveformMeter
import dev.mks.duskread.ui.common.rememberOffMain
import dev.mks.duskread.ui.rememberArticleOpener
import dev.mks.duskread.ui.theme.CodeStyle
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.Space
import dev.mks.duskread.ui.theme.Stroke
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Home: a dashboard rather than a list.
 */
@Composable
fun DashboardTab(
    onOpenFocus: () -> Unit,
    onOpenFollowing: () -> Unit,
    links: LinkLibrary,
    signals: ReadingSignals,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    greeting: String?,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val feedSync = LocalAppGraph.current.feedSync
    val open = rememberArticleOpener()

    // A minute's resolution, re-read on every return to the app: the week's edge and each
    // card's age move while Home stays open, without recomputing on every frame.
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LifecycleResumeEffect(feeds.feeds) {
        // Launched apart from the ticker: pausing mid-fetch must not throw the fetch away.
        scope.launch { feedSync.syncIfStale(Clock.System.now().toEpochMilliseconds()) }
        val ticker = scope.launch {
            while (true) {
                now = Clock.System.now().toEpochMilliseconds()
                delay(LatestTickMs)
            }
        }
        onPauseOrDispose { ticker.cancel() }
    }
    val latest = rememberOffMain(feedPosts.postsByFeed, feeds.feeds, links.links, now / LatestTickMs) {
        latestPosts(feeds = feeds.feeds, cache = feedPosts, links = links, now = now)
    }
    val bodies = rememberCardBodies(latest, feedPosts)

    val http = LocalAppGraph.current.http
    val speech by SpeechSession.state.collectAsState()
    // A listen-through on one of these cards, as opposed to a single read from elsewhere.
    val listeningUrl = speech?.takeIf { it.inQueue }?.key?.takeIf { url -> latest.any { it.url == url } }
    val canListen = speechSupported() && latest.any { !it.read }

    PullToRefreshBox(
        // Never held: the pull hands off to Latest's header, which fills as feeds answer,
        // rather than parking a spinner over the list for the whole sync.
        isRefreshing = false,
        onRefresh = { scope.launch { feedSync.sync() } },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
        ) {
            // Settings lives here, top-right, not on the bar: it is opened rarely, so it
            // gives up the thumb's reach to the tabs. The row stays with no name to show.
            item("head") {
                Row(Modifier.fillMaxWidth().padding(bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = greeting.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
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
                sync = feedSync.state,
                playingUrl = listeningUrl,
                listening = listeningUrl != null,
                onListen = when {
                    listeningUrl != null -> SpeechSession::stop
                    canListen -> fun() {
                        weekListenQueue(latest, feeds, feedPosts, links, signals, http)?.let(SpeechSession::listen)
                    }
                    else -> null
                },
                now = now,
                onOpen = open,
                onFollow = onOpenFollowing,
            )
        }
    }
}

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

        // One strip, clock left and controls right, so the section costs a single line
        // of height and the week below it starts higher.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (state.idle) Modifier else Modifier.clickable(onClick = onOpen)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                // Idle, the middle length stands in as what a start would count down from.
                text = if (state.idle) "${PickableMinutes[PickableMinutes.size / 2]}:00" else state.clockLabel,
                style = CodeStyle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    state.idle -> MaterialTheme.colorScheme.onSurfaceVariant
                    state.running -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(Modifier.weight(1f))
            if (state.idle) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.ChipGap)) {
                    PickableMinutes.forEach { minutes ->
                        PillButton(text = "$minutes min") { controller.start(minutes) }
                    }
                }
            } else {
                WaveformMeter(
                    progress = state.elapsedFraction,
                    modifier = Modifier.height(18.dp),
                    barCount = 22,
                )
                Spacer(Modifier.width(14.dp))
                PillButton(text = "Open", active = true, onClick = onOpen)
            }
        }
    }
}

/** A small bordered pill, the same `.pill` shape as the sort chips on Readback — never filled. */
@Composable
private fun PillButton(text: String, active: Boolean = false, onClick: () -> Unit) {
    val tone = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = text.uppercase(),
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        color = tone,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.Chip))
            .border(
                Stroke.Hairline,
                if (active) tone else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(Radius.Chip),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
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
