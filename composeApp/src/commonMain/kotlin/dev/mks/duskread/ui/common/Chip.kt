package dev.mks.duskread.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.Stroke

/**
 * The bordered pill, wherever one is needed.
 *
 * Lived in `ui/summary` until a second feature wanted the same shape — the
 * summary length choices, the download action, and now a saved link's topic.
 * Three copies of a pill is how a design system stops being one, so it moved
 * here rather than being duplicated.
 *
 * Selection and action are both carried by [tone] alone — border and ink,
 * never a fill. That is the rule Readback's sort control follows too, and it
 * is what keeps the accent meaning "this one" where several pills sit
 * together.
 */
@Composable
fun Chip(label: String, tone: Color, onClick: () -> Unit) {
    Text(
        text = label.uppercase(),
        fontFamily = Mono,
        fontSize = 10.5.sp,
        letterSpacing = 0.4.sp,
        color = tone,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.Chip))
            .border(Stroke.Hairline, tone, RoundedCornerShape(Radius.Chip))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
