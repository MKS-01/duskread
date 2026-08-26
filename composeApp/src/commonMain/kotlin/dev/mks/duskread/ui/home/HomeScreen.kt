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
import dev.mks.duskread.data.UserPrefs
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.SharedLinkRequest
import dev.mks.duskread.links.createHttpClient
import dev.mks.duskread.links.rememberFeedLibrary
import dev.mks.duskread.links.rememberFeedPostCache
import dev.mks.duskread.links.rememberLinkLibrary
import dev.mks.duskread.reader.rememberAudioPlayer
import dev.mks.duskread.reader.rememberReadRepository
import dev.mks.duskread.ui.common.ToastHost
import dev.mks.duskread.ui.common.ToastRequest
import dev.mks.duskread.ui.layout.LocalWindowClass
import dev.mks.duskread.ui.links.LinksTab
import dev.mks.duskread.ui.reader.ReaderTab
import dev.mks.duskread.ui.settings.SettingsScreen
import dev.mks.duskread.ui.theme.Layout
import dev.mks.duskread.ui.theme.Motion

/**
 * Home: tabs and a floating bar.
 *
 * Everything reachable sits in the lower third of the screen — this is a
 * phone-first app and the top of a 6-inch display is a stretch for one thumb.
 */
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

    // Owned here rather than inside ReaderTab so there is exactly one player
    // for the whole of Home. On Android that is belt and braces — playback is
    // a foreground service behind singleton state, so a second instance would
    // still find the same session — but on desktop the player *is* the
    // session, and two of them would mean the floating bar controlling a
    // different clip from the one the list started.
    val readRepository = rememberReadRepository()
    val player = rememberAudioPlayer(readRepository)
    val playback by player.state.collectAsState()

    // Lets a tapped Readback notification land on the Readback tab
    // specifically, rather than just reopening the app onto whatever tab it
    // last showed.
    val requestedTab by HomeTabRequest.target.collectAsState()
    LaunchedEffect(requestedTab) {
        requestedTab?.let {
            onTabChange(it)
            HomeTabRequest.consume()
        }
    }

    // A link shared into the app from a browser. Saved here rather than in the
    // Saved tab so the share lands whichever tab happens to be showing —
    // the tab switch that follows is a courtesy, not what makes it work.
    val links = rememberLinkLibrary()
    val sharedUrl by SharedLinkRequest.url.collectAsState()
    LaunchedEffect(sharedUrl) {
        sharedUrl?.let {
            links.save(it)
            ToastRequest.show("Saved")
            SharedLinkRequest.consume()
        }
    }

    // The followed-blogs thread on Home: a cache of the last sync plus a
    // client to run the next one, both owned here so they survive a tab
    // switch instead of re-fetching every time the dashboard recomposes.
    val feeds = rememberFeedLibrary()
    val feedPosts = rememberFeedPostCache()
    val feedClient = remember { createHttpClient() }
    DisposableEffect(feedClient) { onDispose { feedClient.close() } }

    // Wide windows get the rail-and-transport plan instead of the floating
    // bar; see `ui/layout/WindowClass.kt` and the design system's "Wide"
    // section. Read once here and passed down as a boolean rather than read
    // again in each branch, so the two layouts can never disagree about
    // which one is running.
    val wide = LocalWindowClass.current.isWide

    // One fixed clearance for the last card. It used to grow while the player
    // was docked above the bar; now that the transport lives inside the bar,
    // the bar is the same height whether anything is playing or not, and the
    // padding no longer has to animate underneath a scrolling list.
    //
    // `top` is more generous than it used to be: with no per-tab title left
    // above the first card, that clearance is the only thing keeping content
    // off the status bar.
    //
    // Wide drops most of the bottom clearance: the transport is a sibling in
    // a Column there rather than something floating over the list, so the
    // list already ends where the transport begins and padding for it would
    // be a second gap under the first.
    val listPadding = PaddingValues(
        start = if (wide) Layout.WideListGutter else 16.dp,
        end = if (wide) Layout.WideListGutter else 16.dp,
        top = 24.dp,
        bottom = if (wide) 28.dp else 104.dp,
    )

    // Shared by all three tabs so the bar's collapse survives switching
    // between them — each tab owns its own scroll position, but the bar is one
    // object and should not pop back open just because you changed lists.
    val collapse = rememberBarCollapse()

    // Owned here rather than in `App.kt`, unlike Focus mode: Settings needs
    // `links`, which already lives at this level, and threading a whole
    // `LinkLibrary` up to `App.kt` and back down would exist only to move
    // this one flag up alongside it.
    var showSettings by remember { mutableStateOf(false) }

    // The feed whose posts fill [TopicsScreen], or null when nobody has
    // opened one. Owned here for the same reason as `showSettings`: the
    // screen needs `feedPosts` and `links`, both of which already live at
    // this level, and lifting it to `App.kt` would mean threading them up
    // there and straight back down.
    var topicsFeed by remember { mutableStateOf<Feed?>(null) }

    // The last feed opened, kept after `topicsFeed` clears. Without it the
    // screen would empty itself the instant Back is pressed and spend its
    // whole exit animation as a blank surface sliding away.
    var lastTopicsFeed by remember { mutableStateOf<Feed?>(null) }
    LaunchedEffect(topicsFeed) { topicsFeed?.let { lastTopicsFeed = it } }

    // Read as a boolean through `derivedStateOf` so the bar's visibility
    // recomposes once when the keyboard opens or closes, not on every frame
    // of the inset animation that carries it there.
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeVisible by remember(density) { derivedStateOf { imeInsets.getBottom(density) > 0 } }

    // Hoisted out of the layout branch below so the two plans share one
    // definition of "the tabs" — the rail layout and the floating-bar layout
    // differ in what surrounds the content, never in what the content is.
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
                // The measure cap, and the only thing standing between a
                // 1180dp window and a paste field a metre wide. Left-aligned
                // against the rail rather than centred in the window: the
                // eye returns to the same left edge on every line, and a
                // column floating in the middle of the ground has no edge to
                // return to. Below the breakpoint this is inert — the phone
                // is narrower than the cap by definition.
                .then(if (wide) Modifier.widthIn(max = Layout.ReadingMeasure) else Modifier)
                .nestedScroll(collapse)
                .hazeSource(hazeState)
                .statusBarsPadding()
                // Without this the keyboard draws over the tab rather than
                // shrinking it, so a field low in a list — the feed address
                // on Home is the worst case, it sits in the last section —
                // opens underneath the thing covering it. Resizing the tab
                // is what lets the list scroll the focused field into view.
                .imePadding(),
            label = "tab",
        ) { current ->
            when (current) {
                HomeTab.HOME -> DashboardTab(
                    greeting = prefs.name?.let { "Hello, $it" },
                    links = links,
                    feeds = feeds,
                    feedPosts = feedPosts,
                    feedClient = feedClient,
                    onOpenFocus = onOpenFocus,
                    onOpenSaved = { onTabChange(HomeTab.SAVED) },
                    onOpenReadback = { onTabChange(HomeTab.READBACK) },
                    onOpenSettings = { showSettings = true },
                    onOpenTopics = { topicsFeed = it },
                    contentPadding = listPadding,
                )

                HomeTab.READBACK -> ReaderTab(
                    repository = readRepository,
                    player = player,
                    contentPadding = listPadding,
                )

                HomeTab.SAVED -> LinksTab(
                    library = links,
                    contentPadding = listPadding,
                )
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        if (wide) {
            // Rail and transport are siblings of the content here, not
            // floating over it: with room to spare, furniture anchored to
            // the window's own edges beats anything that has to blur what it
            // covers. The transport keeps the bottom because it outlives
            // whichever pane is above it.
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    NavRail(
                        selected = tab,
                        onSelect = onTabChange,
                        mono = mono,
                        colourMode = prefs.colourMode,
                        onToggleTheme = onToggleTheme,
                        onOpenSettings = { showSettings = true },
                    )
                    tabs(Modifier.weight(1f).fillMaxHeight())
                }

                AnimatedVisibility(
                    visible = playback.item != null,
                    enter = expandVertically(tween(Motion.Chip)) + fadeIn(tween(Motion.Fade)),
                    exit = shrinkVertically(tween(Motion.Chip)) + fadeOut(tween(Motion.Fade)),
                ) {
                    TransportBar(
                        item = playback.item,
                        playback = playback,
                        onTogglePlay = { player.togglePlayPause() },
                        onSeek = { player.seekTo(it) },
                        onStop = { player.stop() },
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
            }
        } else {
            tabs(Modifier.fillMaxSize())
        }

        // There is no scrim gradient under the bar any more. The bar blurs
        // whatever passes beneath it, so fading that content to the
        // background colour would defeat the effect.
        //
        // The horizontal padding is what bounds the bar's width: the
        // transport face fills it, the tab face wraps and centres inside
        // it. Playback already outlives the Readback tab; keeping the
        // transport here rather than in the tab is what makes the controls
        // outlive it too, instead of sending you to the notification shade.
        //
        // It leaves entirely while the keyboard is up. Riding above the
        // keyboard was worse than hiding: it eats a bar's height out of an
        // already halved screen and puts a blurred pill against the
        // keyboard's own edge, and there is nothing on it worth reaching
        // mid-sentence — every field here commits with its own inline
        // action or the IME's Done key.
        AnimatedVisibility(
            visible = !wide && !imeVisible,
            enter = slideInVertically(tween(Motion.Chip)) { it } + fadeIn(tween(Motion.Fade)),
            exit = slideOutVertically(tween(Motion.Chip)) { it } + fadeOut(tween(Motion.Fade)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            FloatingBar(
                selected = tab,
                onSelect = onTabChange,
                hazeState = hazeState,
                nowPlaying = playback.item,
                playback = playback,
                onTogglePlay = { player.togglePlayPause() },
                onSeek = { player.seekTo(it) },
                onStop = { player.stop() },
                mono = mono,
                colourMode = prefs.colourMode,
                onToggleTheme = onToggleTheme,
                collapse = collapse,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp, start = 16.dp, end = 16.dp),
            )
        }

        // Top, not bottom: the floating bar already owns the bottom of the
        // screen, and a toast landing there would either sit on top of it or
        // shove it aside for two seconds every time a link is saved.
        ToastHost(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
        )

        // Same overlay shape as Focus mode in `App.kt`: a full-screen
        // destination on top of everything else, reached from a door in the
        // Saved tab rather than a fourth stop on the floating bar.
        // Same overlay shape again, one level down: opened from the all-posts
        // row at the foot of an expanded digest line on the dashboard
        // underneath.
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
            )
        }
    }
}
