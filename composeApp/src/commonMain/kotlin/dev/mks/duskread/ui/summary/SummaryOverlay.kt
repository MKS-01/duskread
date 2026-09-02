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
import dev.mks.duskread.speech.SpeechSession
import dev.mks.duskread.summary.SummaryRequest
import dev.mks.duskread.summary.SwipeDefault
import dev.mks.duskread.ui.theme.Layout
import dev.mks.duskread.ui.theme.Motion
import dev.mks.duskread.ui.theme.Space

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

    // Held past the request going null so the exit animation still has a
    // target to draw — same reason `PlatformOverlay` and `FloatingBar` do it.
    val shown = remember { mutableStateOf(requested) }
    requested?.let { shown.value = it }

    // Pressing play does not close this panel — the summary text is still
    // worth reading while it plays — so a read started from here leaves two
    // floating things at the bottom of the screen at once: this card, and
    // the transport `HomeScreen` raises to show it. Without extra clearance
    // they crowd the same few rows of screen; this is what keeps them
    // stacked with a real gap between them instead.
    val readingAloud by SpeechSession.state.collectAsState()

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
                        // Clear of the floating bar, not clear of the whole
                        // list inset: [Layout.BarClearance] is what the last
                        // row of a list needs to be scrollable past the bar,
                        // and a panel resting just above it needs less. A
                        // second bar's worth on top of that when something is
                        // actually playing — the transport is showing, not
                        // just reserving its usual quiet strip of icons.
                        .padding(
                            top = 12.dp,
                            bottom = Layout.BarClearance - 12.dp + if (readingAloud != null) Layout.BarHeight + Space.CardGap else 0.dp,
                        ),
                )
            }
        }
    }
}
