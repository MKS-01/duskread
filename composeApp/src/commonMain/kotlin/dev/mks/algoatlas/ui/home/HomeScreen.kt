package dev.mks.algoatlas.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.mks.algoatlas.ui.PlatformBackHandler

/**
 * Home: two tabs and a floating bar, with search growing out of the bar.
 *
 * Everything reachable sits in the lower third of the screen — this is a
 * phone-first app and the top of a 6-inch display is a stretch for one thumb.
 */
@Composable
fun HomeScreen(
    onOpenTopic: (String) -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(HomeTab.LEARN) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

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
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            label = "tab",
        ) { current ->
            when (current) {
                HomeTab.LEARN -> LearnTab(
                    onOpenTopic = onOpenTopic,
                    isDark = isDark,
                    onToggleTheme = onToggleTheme,
                    contentPadding = listPadding,
                )

                HomeTab.PRACTICE -> PracticeTab(
                    onOpenTopic = onOpenTopic,
                    contentPadding = listPadding,
                )
            }
        }

        if (!searching) {
            // Content fades out under the bar instead of running into it.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.background,
                        ),
                    ),
            )

            FloatingBar(
                selected = tab,
                onSelect = { tab = it },
                onSearch = { searching = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp),
            )
        }

        SearchSheet(
            visible = searching,
            query = query,
            onQueryChange = { query = it },
            onSelect = { id ->
                closeSearch()
                onOpenTopic(id)
            },
            onDismiss = ::closeSearch,
        )
    }
}
