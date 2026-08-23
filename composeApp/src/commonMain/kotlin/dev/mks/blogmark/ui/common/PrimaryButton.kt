package dev.mks.blogmark.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.mks.blogmark.ui.theme.Radius

/**
 * The one filled call-to-action shape in the app: `Radius.Inline` corners,
 * the same as every bordered control, rather than a fully rounded pill.
 *
 * Material's default `Button` renders as a pill in the current M3 spec, and
 * the two platform folder pickers plus onboarding's Next/Get started each
 * used to draw their own — one landed on `Radius.Pill` explicitly, the other
 * two inherited Material's default. All three read as a different design
 * system from the rest of the app, which draws nothing else fully round
 * except an actual circular icon button. One component fixes all three at
 * once.
 */
@Composable
fun PrimaryButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.Inline))
            .background(MaterialTheme.colorScheme.primary)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 12.dp),
    )
}
