package dev.mks.duskread.ui.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * How much of the bottom edge is already spoken for, so anything else
 * bottom-anchored can rest above it rather than under it.
 *
 * A published value rather than a token, because the answer is not constant:
 * the floating bar leaves entirely while the keyboard is up, the wide layout
 * has no floating bar at all, and a full-screen surface with nothing playing
 * hides the bar outright. `SummaryOverlay` is mounted in `App.kt`, a level
 * above the screen that draws the bar, so it has no other way to ask — and
 * guessing wrong is visible either way: too little and the panel is drawn
 * under the pill, too much and it floats in the middle of the screen with a
 * bar's worth of nothing beneath it.
 *
 * A singleton in the same shape as [dev.mks.duskread.summary.SummaryRequest]
 * and [HomeTabRequest], for the same reason: the one composable that knows
 * and the one that asks are in unrelated parts of the tree.
 */
object BottomFurniture {
    private val _clearance = MutableStateFlow(0.dp)

    /** Zero when the bottom edge is clear; otherwise the whole height to stay above, gap included. */
    val clearance: StateFlow<Dp> = _clearance

    /** Only for whichever screen owns the bar; nothing else should publish on its behalf. */
    internal fun publish(value: Dp) {
        _clearance.value = value
    }
}
