package dev.mks.duskread.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.ui.theme.Mono

/**
 * How much of the screen's one accent a row is allowed to take.
 */
enum class RowTone { Normal, Accent, Faded }

/**
 * The list row every screen in this app is built from: sourcechip, title, a mono meta
 * line, an optional trailing glyph, and its own bottom hairline.
 */
@Composable
fun ListRow(
    host: String,
    title: String,
    last: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: RowTone = RowTone.Normal,
    titleMaxLines: Int = 2,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    meta: @Composable RowScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        ListRowBody(
            host = host,
            title = title,
            onClick = onClick,
            tone = tone,
            titleMaxLines = titleMaxLines,
            trailing = trailing,
            content = content,
            meta = meta,
        )
        ListRowDivider(last)
    }
}

/**
 * The row without its divider, for the one caller that cannot use [ListRow] whole: Saved
 * wraps its rows in a swipe-to-remove box.
 */
@Composable
fun ListRowBody(
    host: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: RowTone = RowTone.Normal,
    titleMaxLines: Int = 2,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    meta: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val accented = tone == RowTone.Accent

    Column(
        modifier
            .fillMaxWidth()
            // Recession, not a strikethrough: a done row is the same row with less of it.
            .alpha(if (tone == RowTone.Faded) 0.5f else 1f)
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            MonogramBadge(
                host = host,
                size = ChipSize,
                borderColor = if (accented) scheme.primary else scheme.outlineVariant,
                contentColor = if (accented) scheme.primary else scheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(ChipGap))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    color = if (accented) scheme.primary else scheme.onSurface,
                )
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), content = meta)
            }

            trailing?.let {
                Spacer(Modifier.width(8.dp))
                it()
            }
        }

        content?.invoke(this)
    }
}

/**
 * The gap and hairline that separate one row from the next, and only the gap when there
 * is no next.
 */
@Composable
fun ListRowDivider(last: Boolean, topSpacing: Dp = 15.dp) {
    Spacer(Modifier.height(topSpacing))
    if (!last) {
        HairlineDivider()
        Spacer(Modifier.height(15.dp))
    }
}

/**
 * The 1dp hairline itself, with no baked-in spacing — callers that already own their own
 * gaps (a row with vertical padding, say) want just the line.
 */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

/**
 * One fact on a row's meta line — a host, a duration, a word count, a time-ago. Mono,
 * because everything in this app that is data rather than prose is.
 */
@Composable
fun RowMeta(text: String, accent: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontFamily = Mono,
        fontSize = 10.5.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** The host chip's footprint — shared with any row that places one outside [ListRow] itself. */
val ChipSize = 22.dp
private val ChipGap = 10.dp
