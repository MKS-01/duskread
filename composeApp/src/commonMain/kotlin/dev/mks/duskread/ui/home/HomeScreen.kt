package dev.mks.duskread.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.mks.duskread.data.LocalAppGraph
import dev.mks.duskread.data.UserPrefs
import dev.mks.duskread.data.rememberKeyValueStore
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.LinkInbox
import dev.mks.duskread.links.SharedLinkRequest
import dev.mks.duskread.links.rememberFeedLibrary
import dev.mks.duskread.links.rememberFeedPostCache
import dev.mks.duskread.links.rememberLinkLibrary
import dev.mks.duskread.links.rememberReadingSignals
import dev.mks.duskread.notion.rememberNotionPrefs
import dev.mks.duskread.notion.runFullSync
import dev.mks.duskread.reader.rememberAudioPlayer
import dev.mks.duskread.reader.rememberReadRepository
import dev.mks.duskread.speech.DriveSpeechSession
import dev.mks.duskread.speech.SpeechSession
import dev.mks.duskread.ui.common.ToastHost
import dev.mks.duskread.ui.common.ToastRequest
import dev.mks.duskread.ui.layout.LocalWindowClass
import dev.mks.duskread.ui.links.LinksTab
import dev.mks.duskread.ui.reader.ReaderTab
import dev.mks.duskread.ui.reader.formatDuration
import dev.mks.duskread.ui.rememberUrlOpener
import dev.mks.duskread.ui.settings.SettingsScreen
import dev.mks.duskread.ui.theme.Layout
import dev.mks.duskread.ui.theme.Motion
import dev.mks.duskread.ui.theme.Space
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Bottom inset for the three tab lists with the bar at rest. Derived rather than typed
 * out: the bar, the gap it keeps from the safe area, and a gap under the last row.
 */
private val FullClearance = Layout.BarHeight + Layout.BarInset + 32.dp

/**
 * Home: tabs and a floating bar.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun HomeScreen(
    onOpenFocus: () -> Unit,
    prefs: UserPrefs,
    mono: Boolean,
    onToggleTheme: () -> Unit,
    tab: HomeTab,
    onTabChange: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hazeState = remember { HazeState() }

    // Owned here rather than inside ReaderTab so there is exactly one player for the
    // whole of Home.
    val readRepository = rememberReadRepository()
    val player = rememberAudioPlayer(readRepository)
    val playback by player.state.collectAsState()

    // The speech half of the same idea, driven from here for the same reason the Readback
    // player is: this is the one place already alive for the life of the app.
    DriveSpeechSession()
    val speechNowPlaying by SpeechSession.state.collectAsState()

    // At most one of Readback and a live read ever has the floating transport, because
    // there is only one and it can only be about one thing.
    LaunchedEffect(speechNowPlaying?.playing) {
        if (speechNowPlaying?.playing == true) player.stop()
    }
    LaunchedEffect(playback.playing) {
        if (playback.playing) SpeechSession.stop()
    }

    // The single merged description of whatever the transport is about.
    val nowPlaying: NowPlaying? = speechNowPlaying?.let { speech ->
        NowPlaying(
            title = speech.title,
            playing = speech.playing,
            fraction = speech.fraction,
            compactLabel = "${(speech.fraction * 100).toInt()}%",
            wideLabel = "${(speech.fraction * 100).toInt()}%",
            seekable = false,
        )
    } ?: playback.item?.let { item ->
        val duration = playback.durationSec.takeIf { it > 0f } ?: 1f
        NowPlaying(
            title = item.title,
            playing = playback.playing,
            fraction = (playback.positionSec / duration).coerceIn(0f, 1f),
            compactLabel = formatDuration((playback.durationSec - playback.positionSec).toDouble()),
            wideLabel = "${formatDuration(playback.positionSec.toDouble())} / ${formatDuration(playback.durationSec.toDouble())}",
            seekable = true,
        )
    }

    val onTogglePlayTransport: () -> Unit = {
        if (speechNowPlaying != null) SpeechSession.stop() else player.togglePlayPause()
    }
    val onSeekTransport: (Float) -> Unit = { fraction ->
        // No-op for a live read: `NowPlaying.seekable` is false for one, so neither
        // transport ever calls this for it in the first place.
        if (speechNowPlaying == null) {
            val duration = playback.durationSec.takeIf { it > 0f } ?: 1f
            player.seekTo(fraction * duration)
        }
    }
    val onStopTransport: () -> Unit = {
        if (speechNowPlaying != null) SpeechSession.stop() else player.stop()
    }

    // Lets a tapped Readback notification land on the Readback tab specifically, rather
    // than just reopening the app onto whatever tab it last showed.
    val requestedTab by HomeTabRequest.target.collectAsState()
    LaunchedEffect(requestedTab) {
        requestedTab?.let {
            onTabChange(it)
            HomeTabRequest.consume()
        }
    }

    // Readback is a destination only once it has been switched on, so the tab list is
    // derived rather than fixed — see `UserPrefs.readbackEnabled`.
    val visibleTabs = remember(prefs.readbackEnabled) {
        HomeTab.entries.filter { it != HomeTab.READBACK || prefs.readbackEnabled }
    }

    // Switching it off while standing on it would leave the selection pointing at a tab
    // with no way back to it.
    LaunchedEffect(visibleTabs) {
        if (tab !in visibleTabs) onTabChange(HomeTab.HOME)
    }

    // A link shared into the app from a browser.
    val links = rememberLinkLibrary()

    // Hoisted beside the library it describes, and passed down rather than re-remembered
    // per screen: what gets read is one record, and two copies of it would disagree.
    val signals = rememberReadingSignals()

    val sharedUrl by SharedLinkRequest.url.collectAsState()
    LaunchedEffect(sharedUrl) {
        sharedUrl?.let {
            links.save(it)
            ToastRequest.show("Saved")
            SharedLinkRequest.consume()
        }
    }

    // A pick tapped from the reading-suggestion widget.
    val openSuggestion = rememberUrlOpener()
    val pendingSuggestion by SuggestionOpenRequest.pending.collectAsState()
    LaunchedEffect(pendingSuggestion) {
        pendingSuggestion?.let { pick ->
            openSuggestion(pick.url)
            val id = links.save(pick.url, pick.title, pick.topic)?.id
            id?.let { links.toggleRead(it) }
            signals.recordRead(pick.url)
            pick.topic?.let { signals.recordTopicRead(it) }
            SuggestionOpenRequest.consume()
        }
    }

    // Links captured from the home-screen widget while the app was closed.
    val store = rememberKeyValueStore()
    val inboxPokes by LinkInbox.pokes.collectAsState()
    LaunchedEffect(inboxPokes) {
        val captured = LinkInbox.drain(store)
        captured.forEach { links.save(it) }
        if (captured.isNotEmpty()) {
            ToastRequest.show(if (captured.size == 1) "Saved" else "Saved ${captured.size} links")
        }
    }

    // The followed-blogs thread on Home: a cache of the last sync plus a client to run
    // the next one.
    val feeds = rememberFeedLibrary()
    val feedPosts = rememberFeedPostCache()
    val feedClient = LocalAppGraph.current.http

    // Passed down rather than reached for per screen, for the same reason the libraries
    // are: a sync prunes it, and Home's cards read it.
    val summaries = LocalAppGraph.current.summaries

    // Hoisted here and passed into Settings rather than built there, for the same reason
    // FeedLibrary is: NotionPrefs writes `notion.sync.last`.
    val notionPrefs = rememberNotionPrefs()
    val notionAuth = LocalAppGraph.current.notionAuth
    val notionApi = LocalAppGraph.current.notionApi

    /*
     * The sync that happens without being asked.
     *
     * Once per launch, and only if the last one is older than
     * `AutoSyncAfterMs` — so opening the app four times in an evening costs
     * one sync, not four. It runs in the background with no spinner and no
     * toast: a reader who opens the app to read should not be shown the
     * machinery, and the result appears as feeds and posts simply being
     * current.
     *
     * Failures are silent on purpose. There is nothing useful to say about a
     * sync nobody asked for, and a network error banner on launch would be
     * the first thing a reader sees on a train. Settings' own button is where
     * a sync reports for itself.
     */
    LaunchedEffect(Unit) {
        val now = Clock.System.now().toEpochMilliseconds()

        // Anything saved, read or retitled since the last sync has not reached Notion
        // yet.
        val since = notionPrefs.lastSyncAt ?: 0L
        val unpushed = links.links.any { it.changedAt > since } || links.removedUrls.values.any { it > since }
        if (!notionPrefs.dueForSync(now, unpushed)) return@LaunchedEffect
        if (notionAuth.bearer() == null) return@LaunchedEffect

        runCatching {
            runFullSync(
                api = notionApi,
                prefs = notionPrefs,
                library = links,
                feeds = feeds,
                feedPosts = feedPosts,
                summaries = summaries,
                http = feedClient,
                recordSync = notionPrefs::recordSync,
            )
        }
    }

    // Wide windows get the rail-and-transport plan instead of the floating bar; see
    // `ui/layout/WindowClass.kt` and the design system's "Wide" section.
    val wide = LocalWindowClass.current.isWide

    // Shared by all three tabs so the bar's collapse survives switching between them —
    // each tab owns its own scroll position.
    val collapse = rememberBarCollapse()

    // Clearance for the last card, and the one thing here that does animate.
    val listPadding = PaddingValues(
        start = if (wide) Layout.WideListGutter else 16.dp,
        end = if (wide) Layout.WideListGutter else 16.dp,
        top = 24.dp,
        bottom = if (wide) 28.dp else FullClearance,
    )

    // Owned here rather than in `App.kt`, unlike Focus mode: Settings needs `links`,
    // which already lives at this level.
    var showSettings by remember { mutableStateOf(false) }

    // The feed whose posts fill [TopicsScreen], or null when nobody has opened one.
    var topicsFeed by remember { mutableStateOf<Feed?>(null) }

    // The last feed opened, kept after `topicsFeed` clears.
    var lastTopicsFeed by remember { mutableStateOf<Feed?>(null) }
    LaunchedEffect(topicsFeed) { topicsFeed?.let { lastTopicsFeed = it } }

    // Read as a boolean through `derivedStateOf` so the bar's visibility recomposes once
    // when the keyboard opens or closes.
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeVisible by remember(density) { derivedStateOf { imeInsets.getBottom(density) > 0 } }

    // Hoisted out of the layout branch below so the two plans share one definition of
    // "the tabs".
    val tabs: @Composable (Modifier) -> Unit = { tabModifier ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val offset = if (forward) 1 else -1
                (slideInHorizontally(tween(240)) { it / 6 * offset } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 6 * offset } + fadeOut(tween(140)))
            },
            modifier = tabModifier
                // The measure cap, and the only thing standing between a 1180dp window
                // and a paste field a metre wide.
                .then(if (wide) Modifier.widthIn(max = Layout.ReadingMeasure) else Modifier)
                .nestedScroll(collapse)
                .hazeSource(hazeState)
                .statusBarsPadding()
                // Without this the keyboard draws over the tab rather than shrinking it,
                // so a field low in a list — the feed address on Home is the worst case.
                .imePadding(),
            label = "tab",
        ) { current ->
            when (current) {
                HomeTab.HOME -> DashboardTab(
                    greeting = prefs.name?.let { "Hello, $it" },
                    links = links,
                    signals = signals,
                    feeds = feeds,
                    feedPosts = feedPosts,
                    feedClient = feedClient,
                    onOpenFocus = onOpenFocus,
                    onOpenSaved = { onTabChange(HomeTab.SAVED) },
                    onOpenFollowing = { onTabChange(HomeTab.FOLLOWING) },
                    contentPadding = listPadding,
                )

                HomeTab.FOLLOWING -> FollowingTab(
                    feeds = feeds,
                    feedPosts = feedPosts,
                    links = links,
                    client = feedClient,
                    onOpenTopics = { topicsFeed = it },
                    contentPadding = listPadding,
                )

                HomeTab.SAVED -> LinksTab(
                    library = links,
                    signals = signals,
                    contentPadding = listPadding,
                )

                HomeTab.READBACK -> ReaderTab(
                    repository = readRepository,
                    player = player,
                    contentPadding = listPadding,
                )
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        if (wide) {
            // Rail and transport are siblings of the content here, not floating over it:
            // with room to spare.
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    NavRail(
                        selected = tab,
                        tabs = visibleTabs,
                        onSelect = onTabChange,
                        mono = mono,
                        onToggleTheme = onToggleTheme,
                        onOpenSettings = { showSettings = true },
                    )
                    tabs(Modifier.weight(1f).fillMaxHeight())
                }

                AnimatedVisibility(
                    visible = nowPlaying != null,
                    enter = expandVertically(tween(Motion.Chip)) + fadeIn(tween(Motion.Fade)),
                    exit = shrinkVertically(tween(Motion.Chip)) + fadeOut(tween(Motion.Fade)),
                ) {
                    TransportBar(
                        nowPlaying = nowPlaying,
                        onTogglePlay = onTogglePlayTransport,
                        onSeek = onSeekTransport,
                        onStop = onStopTransport,
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
            }
        } else {
            tabs(Modifier.fillMaxSize())
        }

        // Top, not bottom: the floating bar already owns the bottom of the screen.
        ToastHost(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
        )

        // Same overlay shape as Focus mode in `App.kt`: a full-screen destination on top
        // of everything else.
        AnimatedVisibility(
            visible = topicsFeed != null,
            enter = fadeIn(tween(Motion.PushIn)) + slideInVertically(tween(Motion.PushIn)) { it / 8 },
            exit = fadeOut(tween(Motion.PopFade)),
        ) {
            val feed = lastTopicsFeed ?: return@AnimatedVisibility
            TopicsScreen(
                feed = feed,
                posts = feedPosts.postsByFeed[feed.id].orEmpty(),
                linkLibrary = links,
                onClose = { topicsFeed = null },
            )
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(tween(Motion.PushIn)) + slideInVertically(tween(Motion.PushIn)) { it / 8 },
            exit = fadeOut(tween(Motion.PopFade)),
        ) {
            SettingsScreen(
                library = links,
                prefs = prefs,
                mono = mono,
                onToggleTheme = onToggleTheme,
                onClose = { showSettings = false },
                // The same three Home's own sync uses, not fresh copies.
                feeds = feeds,
                feedPosts = feedPosts,
                feedClient = feedClient,
                // Same instance Home ranks with.
                signals = signals,
                notion = notionPrefs,
                auth = notionAuth,
                api = notionApi,
            )
        }

        // There is no scrim gradient under the bar any more.
        val coveredByASurface = topicsFeed != null || showSettings
        val barVisible = !wide && !imeVisible && (!coveredByASurface || nowPlaying != null)

        // What anything else bottom-anchored has to clear — see [BottomFurniture].
        val furniture = when {
            barVisible -> Layout.BarInset + Layout.BarHeight + Space.CardGap
            wide && nowPlaying != null -> Layout.BarHeight + Space.CardGap
            else -> 0.dp
        }
        LaunchedEffect(furniture) { BottomFurniture.publish(furniture) }
        DisposableEffect(Unit) { onDispose { BottomFurniture.publish(0.dp) } }

        AnimatedVisibility(
            visible = barVisible,
            enter = slideInVertically(tween(Motion.Chip)) { it } + fadeIn(tween(Motion.Fade)),
            exit = slideOutVertically(tween(Motion.Chip)) { it } + fadeOut(tween(Motion.Fade)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            FloatingBar(
                selected = tab,
                tabs = visibleTabs,
                onSelect = onTabChange,
                hazeState = hazeState,
                nowPlaying = nowPlaying,
                onTogglePlay = onTogglePlayTransport,
                onSeek = onSeekTransport,
                onStop = onStopTransport,
                mono = mono,
                onToggleTheme = onToggleTheme,
                onOpenSettings = { showSettings = true },
                // The tabs are behind whatever is covering the bar.
                tabsAvailable = !coveredByASurface,
                collapse = collapse,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = Layout.BarInset, start = 16.dp, end = 16.dp),
            )
        }
    }
}
