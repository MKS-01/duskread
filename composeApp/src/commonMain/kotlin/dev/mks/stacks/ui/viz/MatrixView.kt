package dev.mks.stacks.ui.viz

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.stacks.model.MatrixFrame
import dev.mks.stacks.model.Tone
import dev.mks.stacks.ui.theme.LocalVizPalette
import dev.mks.stacks.ui.theme.Mono
import dev.mks.stacks.ui.theme.Motion

/**
 * A grid with optional row and column headers — the natural shape for a DP
 * table, where the interesting thing is watching cells fill in and seeing which
 * earlier cells each new one depends on.
 */
@Composable
fun MatrixView(frame: MatrixFrame, modifier: Modifier = Modifier) {
    val palette = LocalVizPalette.current
    val rows = frame.grid.size
    if (rows == 0) return
    val cols = frame.grid.maxOf { it.size }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val available = maxWidth - 16.dp
        val header = if (frame.rowLabels.isEmpty()) 0.dp else 34.dp
        val gap = 3.dp
        val cell = ((available - header - gap * (cols - 1)) / cols).coerceIn(26.dp, 46.dp)
        val total = header + cell * cols + gap * (cols - 1)
        val needsScroll = total > available

        Box(
            Modifier
                .fillMaxWidth()
                .then(if (needsScroll) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Column(Modifier.width(total), verticalArrangement = Arrangement.spacedBy(gap)) {
                if (frame.colLabels.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        if (header > 0.dp) Box(Modifier.width(header))
                        frame.colLabels.forEach { label ->
                            Text(
                                text = label,
                                modifier = Modifier.width(cell),
                                textAlign = TextAlign.Center,
                                fontFamily = Mono,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                frame.grid.forEachIndexed { r, row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (header > 0.dp) {
                            Text(
                                text = frame.rowLabels.getOrElse(r) { "" },
                                modifier = Modifier.width(header),
                                textAlign = TextAlign.Center,
                                fontFamily = Mono,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        for (c in 0 until cols) {
                            val tone = frame.marks["$r,$c"] ?: Tone.IDLE
                            val bg by animateColorAsState(
                                palette.bg(tone),
                                tween(Motion.Tone),
                                label = "mcellBg",
                            )
                            val fg by animateColorAsState(
                                palette.fg(tone),
                                tween(Motion.Tone),
                                label = "mcellFg",
                            )
                            val value = row.getOrNull(c)

                            Box(
                                Modifier
                                    .size(width = cell, height = cell * 0.78f)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (value == null) MaterialTheme.colorScheme.surface else bg)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(5.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (value != null) {
                                    Text(
                                        text = value,
                                        color = fg,
                                        fontFamily = Mono,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
