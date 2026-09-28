package dev.mks.duskread.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.mks.duskread.ui.theme.Motion
import dev.mks.duskread.ui.theme.SectionLabel

/**
 * A section's small-caps label with a hairline trailing off to the right of it on the
 * *same* line — the one repeating way every section on Home, Readback and Saved opens.
 */
@Composable
fun EyebrowHeader(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    // Defaults to the accent, but a lower-priority section — read history under an unread
    // list, say — can ask for the quieter muted tone instead.
    tint: Color? = null,
    // Fills the rule itself, muted, while something behind the section is loading: the
    // line was already there, so a loader costs the layout nothing.
    progress: Float? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val color = tint ?: MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.height(14.dp),
                tint = color,
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(text = text, style = SectionLabel, color = color)
        Spacer(Modifier.width(8.dp))
        val fill = remember { Animatable(0f) }
        val shown = remember { Animatable(0f) }
        LaunchedEffect(progress == null) {
            if (progress != null) {
                fill.snapTo(0f)
                shown.animateTo(1f, tween(Motion.Fade))
            } else if (shown.value > 0f) {
                // Finish the line before letting it go, so the end reads as done, not dropped.
                fill.animateTo(1f, tween(Motion.Chip))
                shown.animateTo(0f, tween(Motion.Fade))
            }
        }
        LaunchedEffect(progress) {
            progress?.let { fill.animateTo(it, tween(Motion.Chip)) }
        }
        val fillColor = MaterialTheme.colorScheme.onSurfaceVariant
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
                .drawBehind {
                    drawRect(fillColor, size = Size(size.width * fill.value, size.height), alpha = shown.value)
                },
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}
