package dev.mks.duskread.ui.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * How much of the bottom edge is already spoken for, so anything else bottom-anchored can
 * rest above it rather than under it.
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
