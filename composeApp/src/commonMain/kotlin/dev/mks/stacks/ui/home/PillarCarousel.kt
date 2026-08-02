package dev.mks.stacks.ui.home

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.mks.stacks.ui.theme.LocalVizPalette
import dev.mks.stacks.ui.theme.Motion
import dev.mks.stacks.ui.theme.VizPalette
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * The empty search state: one slide per pillar of the app — Algo, Focus,
 * Reader — each drawn moving rather than described.
 *
 * This is the app's whole argument in miniature — you understand a sort by
 * watching things move into place, not by reading the word "sorting" — and it
 * now doubles as search's front door to the two destinations search cannot
 * index, since a Pomodoro session and a Reader library are not text to match
 * against. Tapping a slide acts on it directly, so the animation is the
 * suggestion rather than decoration around it.
 *
 * Each illustration is a pure function of one looping 0..1 progress value, the
 * same idea as the frame generators, just continuous instead of stepped.
 */
@Composable
fun PillarCarousel(
    onSearchAlgo: (String) -> Unit,
    onOpenFocus: () -> Unit,
    onOpenReader: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slides = remember(onSearchAlgo, onOpenFocus, onOpenReader) {
        listOf(
            Slide(
                title = "Topics",
                blurb = "Throwing away half the answers at a time",
                action = { onSearchAlgo("search") },
                draw = { t, palette -> drawSearching(t, palette) },
            ),
            Slide(
                title = "Focus",
                blurb = "Run a Pomodoro session while you study",
                action = onOpenFocus,
                draw = { t, palette -> drawFocus(t, palette) },
            ),
            Slide(
                title = "Reader",
                blurb = "Pick up your readback library where you left off",
                action = onOpenReader,
                draw = { t, palette -> drawReader(t, palette) },
            ),
        )
    }

    val pager = rememberPagerState { slides.size }
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
                pager.animateScrollToPage((pager.currentPage + 1) % slides.size)
            }
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().height(158.dp),
        ) { page ->
            val slide = slides[page]
            val palette = LocalVizPalette.current

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = slide.action,
                    )
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.fillMaxWidth(0.6f).height(72.dp)) {
                    slide.draw(this, progress, palette)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = slide.blurb,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(slides.size) { index ->
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

private class Slide(
    val title: String,
    val blurb: String,
    val action: () -> Unit,
    val draw: DrawScope.(Float, VizPalette) -> Unit,
)

/* ----------------------------------------------------------------------------
 * The drawings. Each is a pure function of loop progress.
 * ------------------------------------------------------------------------- */

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

/** A ring sweeps round once per loop, the same shape as the Focus timer. */
private fun DrawScope.drawFocus(t: Float, palette: VizPalette) {
    val strokeWidth = 5.dp.toPx()
    val diameter = size.height * 0.92f
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

    drawArc(
        color = palette.idle,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = Size(diameter, diameter),
        style = Stroke(strokeWidth, cap = StrokeCap.Round),
    )
    drawArc(
        color = palette.active,
        startAngle = -90f,
        sweepAngle = 360f * t,
        useCenter = false,
        topLeft = topLeft,
        size = Size(diameter, diameter),
        style = Stroke(strokeWidth, cap = StrokeCap.Round),
    )
}

/** Bars breathe up and down out of phase, like a played-back waveform. */
private fun DrawScope.drawReader(t: Float, palette: VizPalette) {
    val bars = 5
    val gap = size.width * 0.05f
    val barWidth = (size.width - gap * (bars - 1)) / bars
    val base = size.height * 0.2f

    repeat(bars) { index ->
        // Each bar rides its own phase of the same loop, so they never move
        // in lockstep — closer to a real waveform than a metronome would be.
        val phase = (t + index * 0.17f) % 1f
        val height = base + (size.height - base) * ease(triangle(phase))
        drawRoundRectCompat(
            colour = palette.active,
            topLeft = Offset(index * (barWidth + gap), size.height - height),
            size = Size(barWidth, height),
        )
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
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
}

/** 0 → 1 → 0, so every loop settles and then resets without a jump. */
private fun triangle(t: Float) = 1f - abs(t * 2f - 1f)

private fun ease(t: Float) = t * t * (3f - 2f * t)
