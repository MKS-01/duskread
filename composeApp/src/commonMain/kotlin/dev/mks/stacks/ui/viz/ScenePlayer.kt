package dev.mks.stacks.ui.viz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.stacks.model.GraphFrame
import dev.mks.stacks.model.MatrixFrame
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.SeqFrame
import dev.mks.stacks.ui.theme.Mono
import dev.mks.stacks.ui.theme.Motion
import kotlinx.coroutines.delay

/**
 * Plays a [Scene] back frame by frame.
 *
 * The scene is fully computed before playback, so scrubbing backwards is exact
 * rather than a re-simulation, and pausing mid-run costs nothing.
 */
@Composable
fun ScenePlayer(scene: Scene, modifier: Modifier = Modifier) {
    val frames = scene.frames
    if (frames.isEmpty()) return

    var index by remember(scene) { mutableIntStateOf(0) }
    var playing by remember(scene) { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }

    val atEnd = index >= frames.lastIndex

    LaunchedEffect(playing, index, speed, frames.size) {
        if (!playing) return@LaunchedEffect
        if (index >= frames.lastIndex) {
            playing = false
            return@LaunchedEffect
        }
        delay((Motion.FrameDwell / speed).toLong())
        index++
    }

    val frame = frames[index]

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Stage
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 20.dp, horizontal = 8.dp)
                .defaultMinSize(minHeight = 190.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (scene) {
                is Scene.Cells, is Scene.Chain -> CellsView(frame as SeqFrame)
                is Scene.Bars -> BarsView(frame as SeqFrame)
                is Scene.Graph -> GraphView(scene, frame as GraphFrame)
                is Scene.Matrix -> MatrixView(frame as MatrixFrame)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Caption — cross-faded so the text change reads as a step, not a flicker.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "${index + 1}/${frames.size}",
                fontFamily = Mono,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 10.dp, top = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            // Deliberately not cross-faded: captions differ in length, so
            // overlapping two of them mid-fade just looks like double vision.
            Text(
                text = frame.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (frame.aux.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                frame.aux.forEach { value ->
                    Text(
                        text = "${value.label} ${value.value}",
                        fontFamily = Mono,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Transport
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (index > 0) index-- }, enabled = index > 0) {
                Icon(Icons.Filled.SkipPrevious, "Previous step", Modifier.size(20.dp))
            }

            IconButton(
                onClick = {
                    if (atEnd) {
                        index = 0
                        playing = true
                    } else {
                        playing = !playing
                    }
                },
            ) {
                Icon(
                    imageVector = when {
                        atEnd -> Icons.Filled.Refresh
                        playing -> Icons.Filled.Pause
                        else -> Icons.Filled.PlayArrow
                    },
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }

            IconButton(
                onClick = { if (index < frames.lastIndex) index++ },
                enabled = index < frames.lastIndex,
            ) {
                Icon(Icons.Filled.SkipNext, "Next step", Modifier.size(20.dp))
            }

            Slider(
                value = index.toFloat(),
                onValueChange = {
                    playing = false
                    index = it.toInt().coerceIn(0, frames.lastIndex)
                },
                valueRange = 0f..frames.lastIndex.toFloat(),
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp).height(24.dp),
            )

            TextButton(
                onClick = { speed = if (speed >= 2f) 0.5f else speed * 2f },
                modifier = Modifier.defaultMinSize(minWidth = 44.dp),
            ) {
                Text(
                    text = if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x",
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
