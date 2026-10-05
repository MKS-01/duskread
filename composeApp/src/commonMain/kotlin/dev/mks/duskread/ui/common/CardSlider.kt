package dev.mks.duskread.ui.common

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.mks.duskread.ui.theme.Space

/**
 * Cards side by side, one at a time, with the next peeking in so the row reads as
 * swipeable. Shared by Following and Saved so the two sliders cannot drift apart.
 */
@Composable
fun <T> CardSlider(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    /** Draws one card; apply the given modifier, which sets its width. */
    card: @Composable (index: Int, item: T, cardModifier: Modifier) -> Unit,
) {
    val state = rememberLazyListState()
    LazyRow(
        modifier = modifier,
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Start),
        horizontalArrangement = Arrangement.spacedBy(Space.CardGap),
    ) {
        itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
            card(index, item, Modifier.fillParentMaxWidth(CardWidth))
        }
    }
}

// Short of the full width, so the next card shows at the edge.
private const val CardWidth = 0.86f
