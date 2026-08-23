package dev.mks.blogmark.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import dev.mks.blogmark.data.UserPrefs
import dev.mks.blogmark.links.SharedLinkRequest
import dev.mks.blogmark.links.createHttpClient
import dev.mks.blogmark.links.rememberFeedLibrary
import dev.mks.blogmark.links.rememberFeedPostCache
import dev.mks.blogmark.links.rememberLinkLibrary
import dev.mks.blogmark.reader.rememberAudioPlayer
import dev.mks.blogmark.reader.rememberReadRepository
import dev.mks.blogmark.ui.common.ToastHost
import dev.mks.blogmark.ui.common.ToastRequest
import dev.mks.blogmark.ui.links.LinksTab
import dev.mks.blogmark.ui.reader.ReaderTab
import dev.mks.blogmark.ui.settings.SettingsScreen
import dev.mks.blogmark.ui.theme.Motion

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

    // One fixed clearance for the last card. It used to grow while the player
    // was docked above the bar; now that the transport lives inside the bar,
    // the bar is the same height whether anything is playing or not, and the
    // padding no longer has to animate underneath a scrolling list.
    //
    // `top` is more generous than it used to be: with no per-tab title left
    // above the first card, that clearance is the only thing keeping content
    // off the status bar.
    val listPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 24.dp,
        bottom = 104.dp,
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

    // Read as a boolean through `derivedStateOf` so the bar's visibility
    // recomposes once when the keyboard opens or closes, not on every frame
    // of the inset animation that carries it there.
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeVisible by remember(density) { derivedStateOf { imeInsets.getBottom(density) > 0 } }

    Box(modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val offset = if (forward) 1 else -1
                (slideInHorizontally(tween(240)) { it / 6 * offset } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 6 * offset } + fadeOut(tween(140)))
            },
            modifier = Modifier
                .fillMaxSize()
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
            visible = !imeVisible,
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
        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(tween(Motion.PushIn)) + slideInVertically(tween(Motion.PushIn)) { it / 8 },
            exit = fadeOut(tween(Motion.PopFade)),
        ) {
            SettingsScreen(library = links, prefs = prefs, onClose = { showSettings = false })
        }
    }
}
