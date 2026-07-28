package dev.mks.algoatlas.ui.viz

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.algoatlas.model.SeqFrame
import dev.mks.algoatlas.model.Tone
import dev.mks.algoatlas.ui.theme.LocalVizPalette
import dev.mks.algoatlas.ui.theme.Mono

private val Gap = 6.dp
private const val MotionMs = 320

/**
 * Boxed values in a row.
 *
 * Cell colours cross-fade and pointers slide between indices rather than
 * jumping, which is the difference between a slideshow and something you can
 * actually follow.
 */
@Composable
fun CellsView(frame: SeqFrame, modifier: Modifier = Modifier) {
    val palette = LocalVizPalette.current
    val n = frame.values.size
    if (n == 0) return

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val available = maxWidth - 16.dp
        val cell = ((available - Gap * (n - 1)) / n).coerceIn(30.dp, 48.dp)
        val stride = cell + Gap
        val total = cell * n + Gap * (n - 1)
        val needsScroll = total > available

        val aboveRows = frame.pointers.count { !it.below }.coerceAtMost(1)
        val topPad = if (aboveRows > 0) 30.dp else 0.dp

        Box(
            Modifier
                .fillMaxWidth()
                .then(if (needsScroll) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.width(total).padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                // Pointers above
                Box(Modifier.fillMaxWidth().height(topPad)) {
                    frame.pointers.filter { !it.below }.forEach { pointer ->
                        PointerMarker(pointer.label, pointer.index, stride, cell, palette.bg(pointer.tone), pointsDown = true)
                    }
                }

                // The span outline sits behind the cells and resizes with the range.
                Box(Modifier.fillMaxWidth()) {
                    frame.span?.let { span ->
                        val x by animateDpAsState(stride * span.from, tween(MotionMs), label = "spanX")
                        val w by animateDpAsState(
                            cell + stride * (span.to - span.from),
                            tween(MotionMs),
                            label = "spanW",
                        )
                        Box(
                            Modifier
                                .offset(x = x - 4.dp, y = (-4).dp)
                                .width(w + 8.dp)
                                .height(cell + 8.dp)
                                .dashedBorder(palette.bg(span.tone)),
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                        frame.values.forEachIndexed { index, value ->
                            Cell(value, frame.marks[index] ?: Tone.IDLE, cell)
                        }
                    }
                }

                // Index / custom sub-labels
                Row(
                    Modifier.padding(top = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(Gap),
                ) {
                    frame.values.indices.forEach { index ->
                        Text(
                            text = frame.subLabels[index] ?: index.toString(),
                            modifier = Modifier.width(cell),
                            textAlign = TextAlign.Center,
                            fontFamily = Mono,
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (frame.pointers.any { it.below }) {
                    Box(Modifier.fillMaxWidth().height(28.dp)) {
                        frame.pointers.filter { it.below }.forEach { pointer ->
                            PointerMarker(pointer.label, pointer.index, stride, cell, palette.bg(pointer.tone), pointsDown = false)
                        }
                    }
                }

                frame.span?.label?.let { label ->
                    Text(
                        text = label,
                        modifier = Modifier.padding(top = 6.dp),
                        fontFamily = Mono,
                        fontSize = 10.5.sp,
                        color = palette.bg(frame.span!!.tone),
                    )
                }
            }
        }
    }
}

@Composable
private fun Cell(value: String, tone: Tone, size: Dp) {
    val palette = LocalVizPalette.current
    val bg by animateColorAsState(palette.bg(tone), tween(MotionMs), label = "cellBg")
    val fg by animateColorAsState(palette.fg(tone), tween(MotionMs), label = "cellFg")

    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value,
            color = fg,
            fontFamily = Mono,
            fontSize = if (value.length > 3) 11.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A labelled arrow that slides horizontally to whichever index it points at. */
@Composable
private fun PointerMarker(
    label: String,
    index: Int,
    stride: Dp,
    cell: Dp,
    color: Color,
    pointsDown: Boolean,
) {
    val x by animateDpAsState(stride * index, tween(MotionMs), label = "ptr-$label")

    Column(
        Modifier.offset(x = x).width(cell),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!pointsDown) Triangle(color, up = true)
        Text(
            text = label,
            color = color,
            fontFamily = Mono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        if (pointsDown) Triangle(color, up = false)
    }
}

@Composable
private fun Triangle(color: Color, up: Boolean) {
    Box(
        Modifier
            .size(width = 9.dp, height = 6.dp)
            .drawBehind {
                val path = Path().apply {
                    if (up) {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                    } else {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height)
                    }
                    close()
                }
                drawPath(path, color)
            },
    )
}

private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    drawRoundRectDashed(color)
}

private fun DrawScope.drawRoundRectDashed(color: Color) {
    drawRoundRect(
        color = color,
        style = Stroke(width = 2.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
    )
}

/**
 * Histogram form — used where relative magnitude carries the meaning, which is
 * to say sorting. Bar heights animate, so a swap or a write-back reads as
 * motion rather than a jump cut.
 */
@Composable
fun BarsView(frame: SeqFrame, modifier: Modifier = Modifier) {
    val palette = LocalVizPalette.current
    val values = frame.values.map { it.toIntOrNull() ?: 0 }
    val n = values.size
    if (n == 0) return
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    val chartHeight = 150.dp

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val available = maxWidth - 16.dp
        val bar = ((available - Gap * (n - 1)) / n).coerceIn(16.dp, 40.dp)
        val total = bar * n + Gap * (n - 1)
        val needsScroll = total > available

        Box(
            Modifier
                .fillMaxWidth()
                .then(if (needsScroll) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier.width(total),
                horizontalArrangement = Arrangement.spacedBy(Gap),
                verticalAlignment = Alignment.Bottom,
            ) {
                values.forEachIndexed { index, value ->
                    val tone = frame.marks[index] ?: Tone.IDLE
                    val bg by animateColorAsState(palette.bg(tone), tween(MotionMs), label = "barBg")
                    val fraction by animateFloatAsState(
                        value.toFloat() / max,
                        tween(MotionMs),
                        label = "barH",
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = value.toString(),
                            fontFamily = Mono,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // The outline keeps untouched bars legible against the
                        // stage background, which is close to the idle tone.
                        Box(
                            Modifier
                                .width(bar)
                                .height((chartHeight * fraction).coerceAtLeast(6.dp))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(bg)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                ),
                        )
                        Text(
                            text = index.toString(),
                            fontFamily = Mono,
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
