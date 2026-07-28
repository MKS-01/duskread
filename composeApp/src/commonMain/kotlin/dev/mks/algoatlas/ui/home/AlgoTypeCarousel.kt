package dev.mks.algoatlas.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.mks.algoatlas.model.Tone
import dev.mks.algoatlas.ui.theme.LocalVizPalette
import dev.mks.algoatlas.ui.theme.Motion
import dev.mks.algoatlas.ui.theme.VizPalette
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * The empty search state: a slow carousel of algorithm families, each drawn
 * running rather than described.
 *
 * This is the app's whole argument in miniature — you understand a sort by
 * watching things move into place, not by reading the word "sorting". Tapping a
 * panel searches for that family, so the animation is also the suggestion.
 *
 * Each illustration is a pure function of one looping 0..1 progress value, the
 * same idea as the frame generators, just continuous instead of stepped.
 */
@Composable
fun AlgoTypeCarousel(onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState { Families.size }
    val loop = rememberInfiniteTransition(label = "carousel")
    val progress by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "progress",
    )

    // Advance on its own, but never fight a finger that is already dragging.
    LaunchedEffect(pager) {
        while (true) {
            delay(4200)
            if (!pager.isScrollInProgress) {
                pager.animateScrollToPage((pager.currentPage + 1) % Families.size)
            }
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().height(158.dp),
        ) { page ->
            val family = Families[page]
            val palette = LocalVizPalette.current

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onPick(family.query) }
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.fillMaxWidth(0.6f).height(72.dp)) {
                    family.draw(this, progress, palette)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = family.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = family.blurb,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(Families.size) { index ->
                val active = index == pager.currentPage
                val width by animateFloatAsState(
                    if (active) 18f else 6f,
                    tween(Motion.Chip),
                    label = "dot",
                )
                Box(
                    Modifier
                        .height(6.dp)
                        .width(width.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                )
            }
        }
    }
}

private class Family(
    val title: String,
    val blurb: String,
    val query: String,
    val draw: DrawScope.(Float, VizPalette) -> Unit,
)

private val Families = listOf(
    Family(
        title = "Sorting",
        blurb = "Getting things into order, and what that costs",
        query = "sort",
        draw = { t, palette -> drawSorting(t, palette) },
    ),
    Family(
        title = "Searching",
        blurb = "Throwing away half the answers at a time",
        query = "search",
        draw = { t, palette -> drawSearching(t, palette) },
    ),
    Family(
        title = "Graphs",
        blurb = "Exploring outward, one ring at a time",
        query = "graph",
        draw = { t, palette -> drawGraph(t, palette) },
    ),
    Family(
        title = "Hashing",
        blurb = "Letting the key compute its own address",
        query = "hash",
        draw = { t, palette -> drawHashing(t, palette) },
    ),
    Family(
        title = "Linked structures",
        blurb = "Follow the pointer to find the next one",
        query = "linked",
        draw = { t, palette -> drawLinked(t, palette) },
    ),
)

/* ----------------------------------------------------------------------------
 * The drawings. Each is a pure function of loop progress.
 * ------------------------------------------------------------------------- */

/** Bars rise into sorted order, hold, then fall back to where they started. */
private fun DrawScope.drawSorting(t: Float, palette: VizPalette) {
    val start = listOf(0.55f, 0.25f, 0.95f, 0.4f, 0.75f, 0.15f, 0.6f)
    val end = start.sorted()
    val settle = ease(triangle(t))

    val gap = size.width * 0.02f
    val barWidth = (size.width - gap * (start.size - 1)) / start.size
    start.indices.forEach { index ->
        val height = lerp(start[index], end[index], settle) * size.height
        val settled = settle > 0.92f
        drawRoundRectCompat(
            colour = if (settled) palette.good else palette.active,
            topLeft = Offset(index * (barWidth + gap), size.height - height),
            size = Size(barWidth, height),
        )
    }
}

/** The live range halves, step by step, until one cell is left. */
private fun DrawScope.drawSearching(t: Float, palette: VizPalette) {
    val count = 8
    val step = (t * 4f).toInt().coerceAtMost(3)
    var lo = 0
    var hi = count - 1
    repeat(step) {
        val mid = (lo + hi) / 2
        lo = mid + 1
    }
    val mid = (lo + hi) / 2

    val gap = size.width * 0.02f
    val cell = (size.width - gap * (count - 1)) / count
    val height = size.height * 0.52f
    repeat(count) { index ->
        val tone = when {
            index == mid -> palette.active
            index in lo..hi -> palette.info
            else -> palette.idle
        }
        drawRoundRectCompat(
            colour = tone,
            topLeft = Offset(index * (cell + gap), (size.height - height) / 2f),
            size = Size(cell, height),
        )
    }
}

/** Nodes light up in rings, the way breadth-first search actually spreads. */
private fun DrawScope.drawGraph(t: Float, palette: VizPalette) {
    val nodes = listOf(
        Offset(0.5f, 0.12f) to 0,
        Offset(0.22f, 0.5f) to 1,
        Offset(0.78f, 0.5f) to 1,
        Offset(0.08f, 0.9f) to 2,
        Offset(0.42f, 0.9f) to 2,
        Offset(0.92f, 0.9f) to 2,
    )
    val edges = listOf(0 to 1, 0 to 2, 1 to 3, 1 to 4, 2 to 5)
    val ring = (t * 3.4f).toInt().coerceAtMost(2)
    fun at(o: Offset) = Offset(o.x * size.width, o.y * size.height)

    edges.forEach { (a, b) ->
        val lit = nodes[b].second <= ring
        drawLine(
            color = if (lit) palette.info else palette.idle,
            start = at(nodes[a].first),
            end = at(nodes[b].first),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    nodes.forEach { (position, depth) ->
        val lit = depth <= ring
        drawCircle(
            color = if (lit) palette.good else palette.idle,
            radius = 11.dp.toPx(),
            center = at(position),
        )
    }
}

/** A key drops, and lands in the bucket its hash picked. */
private fun DrawScope.drawHashing(t: Float, palette: VizPalette) {
    val buckets = 4
    val target = (t * buckets).toInt().coerceAtMost(buckets - 1)
    val within = (t * buckets) - target

    val gap = size.width * 0.04f
    val width = (size.width - gap * (buckets - 1)) / buckets
    val bucketHeight = size.height * 0.34f
    val bucketTop = size.height - bucketHeight

    repeat(buckets) { index ->
        drawRoundRectCompat(
            colour = if (index == target && within > 0.8f) palette.good else palette.idle,
            topLeft = Offset(index * (width + gap), bucketTop),
            size = Size(width, bucketHeight),
        )
    }

    val x = index(target, width, gap) + width / 2f
    val fall = ease(within.coerceIn(0f, 0.8f) / 0.8f)
    drawCircle(
        color = palette.warn,
        radius = 8.dp.toPx(),
        center = Offset(x, lerp(0f, bucketTop - 10.dp.toPx(), fall) + 8.dp.toPx()),
    )
}

/** A pointer walks the chain, one node at a time. */
private fun DrawScope.drawLinked(t: Float, palette: VizPalette) {
    val count = 4
    val active = (t * count).toInt().coerceAtMost(count - 1)
    val gap = size.width * 0.09f
    val box = (size.width - gap * (count - 1)) / count
    val height = size.height * 0.44f
    val top = (size.height - height) / 2f

    repeat(count) { index ->
        drawRoundRectCompat(
            colour = if (index == active) palette.active else palette.idle,
            topLeft = Offset(index * (box + gap), top),
            size = Size(box, height),
        )
        if (index < count - 1) {
            val x = index * (box + gap) + box
            val y = top + height / 2f
            drawLine(
                color = palette.info,
                start = Offset(x + 4.dp.toPx(), y),
                end = Offset(x + gap - 4.dp.toPx(), y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/* ----------------------------------------------------------------------------
 * Helpers
 * ------------------------------------------------------------------------- */

private fun DrawScope.drawRoundRectCompat(colour: Color, topLeft: Offset, size: Size) {
    drawRoundRect(
        color = colour,
        topLeft = topLeft,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
    )
}

private fun index(i: Int, width: Float, gap: Float) = i * (width + gap)

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** 0 → 1 → 0, so every loop settles and then resets without a jump. */
private fun triangle(t: Float) = 1f - abs(t * 2f - 1f)

private fun ease(t: Float) = t * t * (3f - 2f * t)
