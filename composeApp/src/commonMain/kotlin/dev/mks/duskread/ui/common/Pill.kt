package dev.mks.duskread.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.Stroke

/**
 * A small bordered pill — `.pill` in the mockup, not a filled chip.
 */
@Composable
fun Pill(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tone = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = label.uppercase(),
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        color = tone,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.Chip))
            .border(Stroke.Hairline, tone, RoundedCornerShape(Radius.Chip))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}
