package dev.mks.stacks.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.mks.stacks.links.SharedLinkRequest
import dev.mks.stacks.links.rememberLinkLibrary
import dev.mks.stacks.reader.rememberAudioPlayer
import dev.mks.stacks.reader.rememberReadRepository
import dev.mks.stacks.ui.links.LinksTab
import dev.mks.stacks.ui.reader.ReaderTab

/**
 * Home: tabs and a floating bar.
 *
 * Everything reachable sits in the lower third of the screen — this is a
 * phone-first app and the top of a 6-inch display is a stretch for one thumb.
 */
@Composable
fun HomeScreen(
    onOpenFocus: () -> Unit,
    greeting: String?,
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
            SharedLinkRequest.consume()
        }
    }

    // One fixed clearance for the last card. It used to grow while the player
    // was docked above the bar; now that the transport lives inside the bar,
    // the bar is the same height whether anything is playing or not, and the
    // padding no longer has to animate underneath a scrolling list.
    val listPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 12.dp,
        bottom = 104.dp,
    )

    // Shared by all three tabs so the bar's collapse survives switching
    // between them — each tab owns its own scroll position, but the bar is one
    // object and should not pop back open just because you changed lists.
    val collapse = rememberBarCollapse()

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
                .statusBarsPadding(),
            label = "tab",
        ) { current ->
            when (current) {
                HomeTab.HOME -> DashboardTab(
                    greeting = greeting,
                    links = links,
                    onOpenFocus = onOpenFocus,
                    onOpenSaved = { onTabChange(HomeTab.SAVED) },
                    onOpenReadback = { onTabChange(HomeTab.READBACK) },
                    mono = mono,
                    onToggleTheme = onToggleTheme,
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
        FloatingBar(
            selected = tab,
            onSelect = onTabChange,
            hazeState = hazeState,
            nowPlaying = playback.item,
            playback = playback,
            onTogglePlay = { player.togglePlayPause() },
            onSeek = { player.seekTo(it) },
            onStop = { player.stop() },
            collapse = collapse,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp, start = 16.dp, end = 16.dp),
        )
    }
}
