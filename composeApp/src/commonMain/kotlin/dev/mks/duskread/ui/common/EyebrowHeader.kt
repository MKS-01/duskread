package dev.mks.duskread.ui.common

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
import dev.mks.duskread.ui.theme.SectionLabel

/**
 * A section's small-caps label with a hairline trailing off to the right of
 * it on the *same* line — the one repeating way every section on Home,
 * Readback and Saved opens, so the screen reads as rule-and-rhythm rather
 * than a stack of boxes. This is not a rule sitting under the label: the
 * line is what is left of the row after the label and any trailing content,
 * the way a printed section head trails a line off into the margin.
 */
@Composable
fun EyebrowHeader(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    // Defaults to the accent, but a lower-priority section — read history
    // under an unread list, say — can ask for the quieter muted tone instead.
    tint: Color? = null,
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
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}
