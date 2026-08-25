package dev.mks.duskread.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import dev.mks.duskread.summary.SummaryRequest
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
    val requested by SummaryRequest.target.collectAsState()

    // Held past the request going null so the exit animation still has a
    // target to draw — same reason `PlatformOverlay` and `FloatingBar` do it.
    val shown = remember { mutableStateOf(requested) }
    requested?.let { shown.value = it }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = requested != null,
            enter = fadeIn(tween(Motion.Chip)) + slideInVertically(tween(Motion.Chip)) { it / 3 },
            exit = fadeOut(tween(Motion.Fade)) + slideOutVertically(tween(Motion.Fade)) { it / 3 },
        ) {
            shown.value?.let { target ->
                SummaryPanel(
                    target = target,
                    onClose = SummaryRequest::consume,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = Layout.ListGutter)
                        // Clear of the floating bar, not clear of the whole
                        // list inset: [Layout.BarClearance] is what the last
                        // row of a list needs to be scrollable past the bar,
                        // and a panel resting just above it needs less.
                        .padding(top = 12.dp, bottom = Layout.BarClearance - 12.dp),
                )
            }
        }
    }
}
