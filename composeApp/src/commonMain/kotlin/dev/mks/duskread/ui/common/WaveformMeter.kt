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
 * A row of bars standing in for a progress track, used for both audio playback and the
 * focus timer.
 */
@Composable
fun WaveformMeter(
    progress: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    seed: Int = 0,
    // "No signal", drawn as the same meter flattened to a line of dots rather than as a
    // separate empty-state ornament — see [EmptyState].
    flat: Boolean = false,
    filledColor: Color = MaterialTheme.colorScheme.primary,
    // The mockup's off-state bar (`--a-wave-off`) sits between its hairline and its
    // meta-text grey.
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
        // Whole bars only.
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
 * The mockup writes each row's bar heights out by hand, and no two rows share a sequence
 * — that is the whole reason the meter reads as a clip rather than as a texture.
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
