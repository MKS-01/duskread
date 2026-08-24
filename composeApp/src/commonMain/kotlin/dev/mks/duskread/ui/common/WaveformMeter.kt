package dev.mks.duskread.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A row of bars standing in for a progress track, used for both audio
 * playback and the focus timer — one visual language for "how much of this
 * has elapsed" rather than a bar for sound and a plain line for time.
 *
 * The meter is a **mark of a fixed size, not a full-width track**, which is
 * the thing the first pass got wrong: the mockup's `.wave` is a flex row of
 * a fixed [barCount] of [barWidth] bars separated by [barGap], so it takes
 * roughly a fifth of a row's width and stops. Stretched to fill the row
 * instead it becomes two hundred bars at a two-pixel pitch — a hatch, in
 * which the height variation averages out and nothing reads as a waveform
 * any more. Hence the intrinsic width here, and callers passing only a
 * height.
 *
 * Bar heights are seeded from [seed] so two reads listed above each other do
 * not draw the same shape. They are still not a real amplitude reading —
 * nothing here decodes the clip — but a per-item shape is what makes a row's
 * meter read as *this* clip's.
 */
@Composable
fun WaveformMeter(
    progress: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    seed: Int = 0,
    // "No signal", drawn as the same meter flattened to a line of dots
    // rather than as a separate empty-state ornament — see [EmptyState].
    flat: Boolean = false,
    filledColor: Color = MaterialTheme.colorScheme.primary,
    // The mockup's off-state bar (`--a-wave-off`) sits between its hairline
    // and its meta-text grey. `outlineVariant` (built for a 1px divider only
    // ever seen edge-on) all but vanishes at this width, and meta-text grey
    // at full strength competes with the title; the meta tone held back is
    // the tone the mockup actually specifies.
    dimColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
    barWidth: Dp = 2.dp,
    barGap: Dp = 1.5.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val width = barWidth * barCount + barGap * (barCount - 1)

    Canvas(modifier.width(width)) {
        val bar = barWidth.toPx()
        val pitch = bar + barGap.toPx()
        val radius = CornerRadius(1.dp.toPx())
        // Whole bars only. The mockup colours a bar or it does not — a
        // half-tinted boundary bar reads as an anti-aliasing artefact at
        // 2dp, not as sub-bar precision.
        val filledUpTo = clamped * barCount
        val heights = waveformHeights(barCount, seed)

        for (i in 0 until barCount) {
            val barHeight = if (flat) bar.coerceAtMost(size.height) else size.height * heights[i]
            drawRoundRect(
                color = if (i < filledUpTo) filledColor else dimColor,
                topLeft = Offset(pitch * i, (size.height - barHeight) / 2f),
                size = Size(bar, barHeight),
                cornerRadius = radius,
            )
        }
    }
}

/**
 * The mockup writes each row's bar heights out by hand, and no two rows share
 * a sequence — that is the whole reason the meter reads as a clip rather than
 * as a texture. So: a cheap integer hash of (seed, index) quantised onto the
 * mockup's own eleven steps (5px to 15px in a 15px box), which gives every
 * seed its own silhouette and, being a pure function of its arguments, never
 * shuffles underneath a timer that recomposes every second.
 *
 * The one thing plain randomness gets wrong is clumping: a run of three
 * similar bars reads as a rendering glitch. Hence the nudge forcing each bar
 * at least three steps away from its neighbour.
 */
private fun waveformHeights(count: Int, seed: Int): FloatArray {
    val steps = IntArray(count)
    var previous = -9
    for (i in 0 until count) {
        var h = seed * 0x9E3779B9.toInt() + i * 0x85EBCA6B.toInt()
        h = h xor (h ushr 15)
        h *= 0x2545F491
        h = h xor (h ushr 13)
        var step = (h ushr 1) % 11
        if (kotlin.math.abs(step - previous) < 3) step = (step + 5) % 11
        steps[i] = step
        previous = step
    }
    return FloatArray(count) { (5 + steps[it]) / 15f }
}
