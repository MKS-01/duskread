package dev.mks.blogmark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.mks.blogmark.data.rememberUserPrefs
import dev.mks.blogmark.ui.PlatformOverlay
import dev.mks.blogmark.ui.home.HomeScreen
import dev.mks.blogmark.ui.home.HomeTab
import dev.mks.blogmark.ui.onboarding.Onboarding
import dev.mks.blogmark.ui.pomodoro.FocusScreen
import dev.mks.blogmark.ui.theme.BlogmarkTheme
import dev.mks.blogmark.ui.theme.Motion

@Composable
fun App() {
    // Both themes are dark; this picks the colourless one. Not persisted —
    // it is a mood switch for the current sitting, not a setting.
    var mono by remember { mutableStateOf(false) }

    val prefs = rememberUserPrefs()

    BlogmarkTheme(mono = mono) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!prefs.introSeen) {
                Onboarding(
                    onDone = { name ->
                        prefs.updateName(name)
                        prefs.markIntroSeen()
                    },
                )
                return@Surface
            }

            var homeTab by remember { mutableStateOf(HomeTab.HOME) }
            var focusMode by remember { mutableStateOf(false) }

            Box(Modifier.fillMaxSize()) {
                HomeScreen(
                    onOpenFocus = { focusMode = true },
                    prefs = prefs,
                    mono = mono,
                    onToggleTheme = { mono = !mono },
                    tab = homeTab,
                    onTabChange = { homeTab = it },
                )

                // The big-timer mode: a full-screen destination for whenever
                // the point is to actually stare at the clock, not glance at a
                // corner. Closing it never stops the session underneath.
                AnimatedVisibility(
                    visible = focusMode,
                    enter = fadeIn(tween(Motion.PushIn)) + slideInVertically(tween(Motion.PushIn)) { it / 8 },
                    exit = fadeOut(tween(Motion.PopFade)),
                ) {
                    FocusScreen(onClose = { focusMode = false })
                }

                // Android's embedded reader browser; a no-op everywhere else.
                // See `PlatformOverlay`.
                PlatformOverlay()
            }
        }
    }
}
