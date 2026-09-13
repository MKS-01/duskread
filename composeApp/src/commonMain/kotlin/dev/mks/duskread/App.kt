package dev.mks.duskread

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.mks.duskread.data.ProvideAppGraph
import dev.mks.duskread.data.rememberUserPrefs
import dev.mks.duskread.ui.PlatformOverlay
import dev.mks.duskread.ui.home.HomeScreen
import dev.mks.duskread.ui.home.HomeTab
import dev.mks.duskread.ui.layout.WindowClassProvider
import dev.mks.duskread.ui.onboarding.Onboarding
import dev.mks.duskread.ui.pomodoro.FocusRequest
import dev.mks.duskread.ui.pomodoro.FocusScreen
import dev.mks.duskread.ui.summary.SummaryOverlay
import dev.mks.duskread.ui.theme.DuskReadTheme
import dev.mks.duskread.ui.theme.Motion

@Composable
fun App() = ProvideAppGraph { DuskRead() }

/**
 * Split from [App] only so the graph is in scope: every `rememberX()` below now resolves
 * through `LocalAppGraph`.
 */
@Composable
private fun DuskRead() {
    val prefs = rememberUserPrefs()

    // Both themes are dark; this picks the colourless one.
    val mono = prefs.mono

    DuskReadTheme(mono = mono) {
        // Outermost, so every screen — onboarding and the overlays included — reads the
        // same window class.
        WindowClassProvider(Modifier.fillMaxSize()) {
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

                // Tapping a running session on the home-screen widget should land on the
                // timer rather than on whichever tab the app was last showing.
                val focusRequested by FocusRequest.open.collectAsState()
                LaunchedEffect(focusRequested) {
                    if (focusRequested) {
                        focusMode = true
                        FocusRequest.consume()
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    HomeScreen(
                        onOpenFocus = { focusMode = true },
                        prefs = prefs,
                        mono = mono,
                        onToggleTheme = { prefs.updateMono(!mono) },
                        tab = homeTab,
                        onTabChange = { homeTab = it },
                    )

                    // The big-timer mode: a full-screen destination for whenever the
                    // point is to actually stare at the clock, not glance at a corner.
                    AnimatedVisibility(
                        visible = focusMode,
                        enter = fadeIn(tween(Motion.PushIn)) + slideInVertically(tween(Motion.PushIn)) { it / 8 },
                        exit = fadeOut(tween(Motion.PopFade)),
                    ) {
                        FocusScreen(onClose = { focusMode = false })
                    }

                    // A summary asked for by swiping a row, wherever that row was. Under
                    // the reader below, which hosts its own panel over its own article.
                    SummaryOverlay()

                    // Android's embedded reader browser; a no-op everywhere else. See
                    // `PlatformOverlay`.
                    PlatformOverlay(mono = mono)
                }
            }
        }
    }
}
