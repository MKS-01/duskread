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
 * A small bordered pill — `.pill` in the mockup, not a filled chip. The one
 * way this app says "one of these, and this is the one": Readback's
 * Newest/Oldest, Following's sort, Saved's All/Unread/Read.
 *
 * Selection is carried by the border and the text alone. A filled chip would
 * be the only remaining Material surface on any of these screens, and next to
 * a hairline sourcechip it reads as a different app. The accent on the active
 * one is the design system's selected-control exception to the one-accent
 * rule: selection is its own state, not a competitor to whatever is playing.
 *
 * Lived as a private copy in `ReaderTab` and a second in `FollowingSection`
 * before Saved wanted a third; one component, so a change to how selection
 * looks happens once.
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
