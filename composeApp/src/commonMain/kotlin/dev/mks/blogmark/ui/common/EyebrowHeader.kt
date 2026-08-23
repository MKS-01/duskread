package dev.mks.blogmark.ui.common

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.mks.blogmark.ui.theme.SectionLabel

/**
 * A card's small-caps label, an optional glyph beside it, and a hairline rule
 * underneath — the one repeating cadence every card on Home and Following
 * opens with, so the screen reads as one system instead of a stack of
 * differently-built boxes. The rule is what a bare label-then-content jump
 * was missing: it gives the eyebrow somewhere to land before the title
 * starts, the same way a printed page rules off a section head.
 */
@Composable
fun EyebrowHeader(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    // Defaults to the accent, but a lower-priority section — read history
    // under an unread list, say — can ask for the quieter muted tone instead,
    // the same distinction those sections already drew before this existed.
    tint: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val color = tint ?: MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.height(16.dp),
                tint = color,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = SectionLabel,
            color = color,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
    Spacer(Modifier.height(8.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
    Spacer(Modifier.height(10.dp))
}
