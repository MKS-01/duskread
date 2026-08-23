package dev.mks.blogmark.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.sin

/**
 * A row of bars standing in for a progress track, used for both audio
 * playback and the focus timer — one visual language for "how much of this
 * has elapsed" rather than a bar for sound and a plain line for time.
 *
 * The bar heights are fixed and decorative, not a real amplitude reading —
 * there is no clip to sample for a focus session — but they are seeded once
 * per [barCount] rather than randomised on every call, so the shape does not
 * shuffle underneath a running timer as it recomposes each second.
 */
@Composable
fun WaveformMeter(
    progress: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    filledColor: Color = MaterialTheme.colorScheme.primary,
    dimColor: Color = MaterialTheme.colorScheme.outlineVariant,
    strokeWidth: Float = 3f,
) {
    val heights = remember(barCount) { waveformHeights(barCount) }
    val clamped = progress.coerceIn(0f, 1f)

    Canvas(modifier) {
        val gap = size.width / barCount
        val filledUpTo = (clamped * barCount)
        for (i in 0 until barCount) {
            val x = gap * i + gap / 2f
            val barHeight = size.height * heights[i]
            val fraction = (filledUpTo - i).coerceIn(0f, 1f)
            val color = if (fraction >= 1f) filledColor else lerpColor(dimColor, filledColor, fraction)
            drawLine(
                color = color,
                start = Offset(x, (size.height - barHeight) / 2f),
                end = Offset(x, (size.height + barHeight) / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Two overlapping sine waves rather than [kotlin.random.Random] — a genuinely
 * random set of heights tends to clump into a run of similar bars somewhere
 * in the row, which reads as a rendering glitch rather than a waveform. The
 * two frequencies keep every run visually irregular without that risk, and
 * being a pure function of `count` it is naturally stable across calls.
 */
private fun waveformHeights(count: Int): FloatArray = FloatArray(count) { i ->
    val t = i.toFloat()
    val wave = sin(t * 0.9f) * 0.5f + sin(t * 2.3f + 1.3f) * 0.3f
    0.32f + (wave + 0.8f) / 1.6f * 0.68f
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color = Color(
    red = from.red + (to.red - from.red) * fraction,
    green = from.green + (to.green - from.green) * fraction,
    blue = from.blue + (to.blue - from.blue) * fraction,
    alpha = from.alpha + (to.alpha - from.alpha) * fraction,
)
