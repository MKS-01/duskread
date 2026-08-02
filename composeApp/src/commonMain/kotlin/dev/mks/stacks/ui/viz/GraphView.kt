package dev.mks.stacks.ui.viz

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.stacks.model.GraphFrame
import dev.mks.stacks.model.Scene
import dev.mks.stacks.model.Tone
import dev.mks.stacks.model.edgeKey
import dev.mks.stacks.ui.theme.LocalVizPalette
import dev.mks.stacks.ui.theme.Mono
import dev.mks.stacks.ui.theme.Motion

private val NodeSize = 38.dp

/**
 * Nodes and edges on a fixed normalised layout.
 *
 * Edges are drawn on a [Canvas] underneath, nodes are real composables on top
 * so their labels and badges get proper text layout. Positions are authored in
 * 0..1 space, so the whole thing scales to any width.
 */
@Composable
fun GraphView(scene: Scene.Graph, frame: GraphFrame, modifier: Modifier = Modifier) {
    val palette = LocalVizPalette.current
    val idleEdge = MaterialTheme.colorScheme.outline

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = maxWidth
        val height = (width / 2.1f).coerceIn(150.dp, 230.dp)
        val inset = NodeSize / 2 + 12.dp

        // Resolve every edge colour up front: the count is fixed, so these
        // animation states are stable across frames.
        val edgeColors = scene.edges.map { edge ->
            val tone = frame.edges[edgeKey(edge.from, edge.to)]
                ?: frame.edges[edgeKey(edge.to, edge.from)]
                ?: Tone.IDLE
            val target = if (tone == Tone.IDLE) idleEdge else palette.bg(tone)
            animateColorAsState(target, tween(Motion.Tone), label = "edge").value to (tone != Tone.IDLE)
        }

        Box(Modifier.fillMaxWidth().height(height)) {
            val nodeById = scene.nodes.associateBy { it.id }

            Canvas(Modifier.fillMaxSize()) {
                val usableW = size.width - inset.toPx() * 2
                val usableH = size.height - inset.toPx() * 2

                fun point(x: Float, y: Float) = Offset(
                    inset.toPx() + x * usableW,
                    inset.toPx() + y * usableH,
                )

                scene.edges.forEachIndexed { index, edge ->
                    val from = nodeById[edge.from] ?: return@forEachIndexed
                    val to = nodeById[edge.to] ?: return@forEachIndexed
                    val (color, highlighted) = edgeColors[index]

                    drawLine(
                        color = color,
                        start = point(from.x, from.y),
                        end = point(to.x, to.y),
                        strokeWidth = if (highlighted) 3.dp.toPx() else 1.5.dp.toPx(),
                    )
                }
            }

            scene.nodes.forEach { node ->
                val tone = frame.nodes[node.id] ?: Tone.IDLE
                val bg by animateColorAsState(palette.bg(tone), tween(Motion.Tone), label = "nodeBg")
                val fg by animateColorAsState(palette.fg(tone), tween(Motion.Tone), label = "nodeFg")

                val xDp = inset + (width - inset * 2) * node.x - NodeSize / 2
                val yDp = inset + (height - inset * 2) * node.y - NodeSize / 2

                Column(
                    Modifier.offset(x = xDp, y = yDp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(NodeSize)
                            .clip(CircleShape)
                            .background(bg)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = node.label,
                            color = fg,
                            fontFamily = Mono,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    frame.badges[node.id]?.let { badge ->
                        Text(
                            text = badge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = Mono,
                            fontSize = 9.5.sp,
                        )
                    }
                }
            }
        }
    }
}
