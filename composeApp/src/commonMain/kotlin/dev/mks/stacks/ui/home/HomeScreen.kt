package dev.mks.stacks.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.mks.stacks.ui.PlatformBackHandler
import dev.mks.stacks.ui.reader.ReaderTab
import dev.mks.stacks.ui.theme.Motion

/**
 * Home: two tabs and a floating bar, with search growing out of the bar.
 *
 * Everything reachable sits in the lower third of the screen — this is a
 * phone-first app and the top of a 6-inch display is a stretch for one thumb.
 */
@Composable
fun HomeScreen(
    onOpenTopic: (String) -> Unit,
    onOpenFocus: () -> Unit,
    greeting: String?,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    tab: HomeTab,
    onTabChange: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val hazeState = remember { HazeState() }

    // Lets a tapped Reader notification land on the Reader tab specifically,
    // rather than just reopening the app onto whatever tab it last showed.
    val requestedTab by HomeTabRequest.target.collectAsState()
    LaunchedEffect(requestedTab) {
        requestedTab?.let {
            onTabChange(it)
            HomeTabRequest.consume()
        }
    }

    fun closeSearch() {
        searching = false
        query = ""
    }

    PlatformBackHandler(enabled = searching) { closeSearch() }

    // Enough bottom room that the floating bar never covers the last card.
    val listPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp)

    Box(modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val offset = if (forward) 1 else -1
                (slideInHorizontally(tween(240)) { it / 6 * offset } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 6 * offset } + fadeOut(tween(140)))
            },
            modifier = Modifier.fillMaxSize().hazeSource(hazeState).statusBarsPadding(),
            label = "tab",
        ) { current ->
            when (current) {
                HomeTab.HOME -> DashboardTab(
                    greeting = greeting,
                    onOpenTopic = onOpenTopic,
                    onOpenFocus = onOpenFocus,
                    onOpenReader = { onTabChange(HomeTab.READER) },
                    isDark = isDark,
                    onToggleTheme = onToggleTheme,
                    contentPadding = listPadding,
                )

                HomeTab.LIBRARY -> LibraryTab(
                    onOpenTopic = onOpenTopic,
                    contentPadding = listPadding,
                )

                HomeTab.READER -> ReaderTab(contentPadding = listPadding)
            }
        }

        if (!searching) {
            // There is no scrim gradient under the bar any more. The bar blurs
            // whatever passes beneath it, so fading that content to the
            // background colour would defeat the effect.
            FloatingBar(
                selected = tab,
                onSelect = onTabChange,
                onSearch = { searching = true },
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp),
            )
        }
    }

    // Search is a screen, not a sheet over the list: it owns the whole display
    // while it is open, so results get the room to be read rather than being
    // squeezed into a strip above the keyboard.
    AnimatedVisibility(
        visible = searching,
        enter = fadeIn(tween(Motion.Fade)) + slideInVertically(tween(Motion.PushIn)) { it / 8 },
        exit = fadeOut(tween(Motion.PushOut)),
    ) {
        SearchScreen(
            query = query,
            onQueryChange = { query = it },
            onSelect = { id ->
                closeSearch()
                onOpenTopic(id)
            },
            onOpenFocus = {
                closeSearch()
                onOpenFocus()
            },
            onOpenReader = {
                closeSearch()
                onTabChange(HomeTab.READER)
            },
            onDismiss = ::closeSearch,
        )
    }
}
