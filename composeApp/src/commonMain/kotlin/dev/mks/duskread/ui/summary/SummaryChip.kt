package dev.mks.duskread.ui.summary

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
 * The bordered pill this feature says everything with — the length choices in
 * Settings, and the button that starts a download.
 *
 * One shape rather than two that merely resemble each other: Settings stacks
 * the download action directly under the length chips, and the two reading as
 * different orders of control is what would make that section look assembled
 * rather than designed.
 *
 * Selection and action are both carried by [tone] alone — border and ink,
 * never a fill. That is the rule Readback's sort control follows, and it is
 * what keeps the accent meaning "this one" where several pills sit together.
 */
@Composable
internal fun SummaryChip(label: String, tone: Color, onClick: () -> Unit) {
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

/** The pill that does something, as opposed to the one that selects something. */
@Composable
internal fun SummaryActionChip(label: String, onClick: () -> Unit) = SummaryChip(label = label, tone = MaterialTheme.colorScheme.primary, onClick = onClick)
