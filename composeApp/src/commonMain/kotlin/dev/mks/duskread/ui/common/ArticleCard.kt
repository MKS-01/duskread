package dev.mks.duskread.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Motion
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.Stroke

/**
 * The bordered card Home's Latest section is built from: the same sourcechip, title and
 * mono meta line as [ListRow], given room to also say what the piece is about.
 *
 * Every card is the same height closed, whatever the length of its title or body — a
 * column of cards that each stop somewhere different reads as a mistake rather than as
 * variety. Room the text does not use is left empty, and text that does not fit is
 * behind "more".
 */
@Composable
fun ArticleCard(
    host: String,
    title: String,
    /** The article's own opening, or a summary of it. Crossfaded, because it can change under the reader. */
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** "2d ago", set apart from [meta] because it is the one fact the card is sorted by. */
    timeAgo: String? = null,
    /** Recession, not a strikethrough — the same thing [RowTone.Faded] means on a row. */
    faded: Boolean = false,
    meta: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    // Reset by the body it describes: a summary arriving where an excerpt was is a
    // different piece of text, and it has not been opened.
    var expanded by remember(body) { mutableStateOf(false) }

    // Only ever set while closed. Open, nothing overflows, and reading the flag then
    // would take "less" away the moment it was needed.
    var truncated by remember(body) { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxWidth()
            .alpha(if (faded) 0.5f else 1f)
            .clip(RoundedCornerShape(Radius.Card))
            .border(Stroke.Hairline, scheme.outlineVariant, RoundedCornerShape(Radius.Card))
            .clickable(onClick = onClick)
            // The card grows under the reader's own finger, so the growth is shown.
            .animateContentSize(tween(Motion.Chip))
            .padding(CardPadding),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonogramBadge(host = host, size = ChipSize)
            Spacer(Modifier.width(10.dp))
            RowMeta(host, modifier = Modifier.weight(1f))
            timeAgo?.let { RowMeta(it) }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            // Both, and equal: the second line is held open for a one-line title.
            minLines = TitleLines,
            maxLines = TitleLines,
            overflow = TextOverflow.Ellipsis,
            color = scheme.onSurface,
        )

        Spacer(Modifier.height(9.dp))
        // Keyed on the text itself: an excerpt replaced by a summary changed without the
        // reader asking, and a cut explains that better than a swap.
        Crossfade(targetState = body, animationSpec = tween(Motion.Fade), label = "card-body") { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                minLines = BodyLines,
                maxLines = if (expanded) Int.MAX_VALUE else BodyLines,
                overflow = TextOverflow.Ellipsis,
                color = scheme.onSurfaceVariant,
                onTextLayout = { if (!expanded) truncated = it.hasVisualOverflow },
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), content = meta)

            // Only offered when there is something behind it. A "more" that opens two
            // more words is a broken promise.
            if (truncated) {
                Text(
                    text = if (expanded) "LESS" else "MORE",
                    fontFamily = Mono,
                    fontSize = 10.5.sp,
                    color = scheme.onSurfaceVariant,
                    // Its own clickable, so opening the card's text is not opening the
                    // article — the inner one takes the tap.
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.Chip))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Generous next to a list row's, because the card's whole point is the room. */
private val CardPadding = 16.dp

/** Held open whether the title needs both or not; see the note on uniform height above. */
private const val TitleLines = 2

/** Four lines is about what [excerptOf]'s budget produces on a phone. */
private const val BodyLines = 4
