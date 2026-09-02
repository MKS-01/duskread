package dev.mks.duskread.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mks.duskread.data.rememberUserPrefs
import dev.mks.duskread.summary.SummaryRequest
import dev.mks.duskread.summary.SwipeDefault
import dev.mks.duskread.ui.home.BottomFurniture
import dev.mks.duskread.ui.theme.Layout
import dev.mks.duskread.ui.theme.Motion

/**
 * The panel a swiped row opens, mounted once above the tabs.
 *
 * Bottom-anchored and clear of the floating bar, like everything else this app
 * expects a thumb to reach. It floats over the list rather than replacing it:
 * the point of summarising from a row is to decide whether to open the thing,
 * and a full-screen destination would have already answered that question by
 * taking you away from the list you were triaging.
 */
@Composable
fun SummaryOverlay(modifier: Modifier = Modifier) {
    val prefs = rememberUserPrefs()
    val requested by SummaryRequest.target.collectAsState()
    val furniture by BottomFurniture.clearance.collectAsState()

    // Held past the request going null so the exit animation still has a
    // target to draw — same reason `PlatformOverlay` and `FloatingBar` do it.
    val shown = remember { mutableStateOf(requested) }
    requested?.let { shown.value = it }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // Tapping the list dismisses the panel. No ripple and no scrim — the
        // rows underneath stay legible, which is the point of floating over
        // them; the only sign this layer exists is that the first tap closes.
        if (requested != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = SummaryRequest::consume,
                    ),
            )
        }

        AnimatedVisibility(
            visible = requested != null,
            enter = fadeIn(tween(Motion.Chip)) + slideInVertically(tween(Motion.Chip)) { it / 3 },
            exit = fadeOut(tween(Motion.Fade)) + slideOutVertically(tween(Motion.Fade)) { it / 3 },
        ) {
            shown.value?.let { target ->
                SummaryPanel(
                    target = target,
                    onClose = SummaryRequest::consume,
                    // The swipe is the one place this is a preference rather
                    // than a decision made at the tap: the gesture itself
                    // already told the reader which it would be, in
                    // `SummariseBackground`'s own label.
                    autoPlay = prefs.swipeDefault == SwipeDefault.ReadAloud,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = Layout.ListGutter)
                        // Asked, not assumed — see [BottomFurniture]. This
                        // used to be measured off [Layout.BarClearance],
                        // which is a *scroll* inset (what a list's last row
                        // needs to be pushed clear of the bar) and lands 20dp
                        // short of where the bar actually starts, so the
                        // panel's bottom edge was drawn under the pill. The
                        // opposite failure is just as visible: over a
                        // full-screen surface with nothing playing there is
                        // no bar at all, and reserving one leaves the panel
                        // floating in the middle of the screen.
                        .padding(top = 12.dp, bottom = maxOf(furniture, EdgeInset)),
                )
            }
        }
    }
}

/** What the panel keeps between itself and the edge when nothing else is down there. */
private val EdgeInset = 16.dp
