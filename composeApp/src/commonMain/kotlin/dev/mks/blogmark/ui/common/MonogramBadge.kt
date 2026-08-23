package dev.mks.blogmark.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.blogmark.ui.theme.Mono
import dev.mks.blogmark.ui.theme.Radius
import dev.mks.blogmark.ui.theme.Stroke

/**
 * A followed blog's badge: the first letter of its host, not a guessed icon.
 *
 * This used to be a pattern match against the host string — an Android blog
 * got a phone glyph, "spotify" got a music note, anything else fell back to a
 * hashed pick from eight decorative icons — pulled from a third icon
 * vocabulary (Feather) on top of [dev.mks.blogmark.ui.theme.BlogmarkIcons] and
 * the platform's own favicon-style expectations. A guessed icon is also
 * simply wrong more often than it is right: most hosts match nothing and land
 * on an arbitrary fallback. A letter is honest about being a letter, needs no
 * guessing, and reads at this size better than a favicon ever fetched would.
 *
 * Drawn as the mockup's `.sourcechip`: a **square** with a 3dp radius and a
 * hairline border, holding one mono capital — not a filled circle. A filled
 * circle is an avatar, and an avatar promises a person or a brand mark; this
 * is a data cell, the same hairline-and-mono vocabulary as the meta line it
 * sits beside. Deliberately the quietest thing in the row.
 */
@Composable
fun MonogramBadge(
    host: String,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier
            .size(size)
            .border(Stroke.Hairline, borderColor, RoundedCornerShape(Radius.Chip)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = monogramOf(host),
            fontFamily = Mono,
            // The mockup's chip is 22px holding 10px type; keeping that ratio
            // means the letter still sits inside its square at 28dp.
            fontSize = (size.value * 0.45f).sp,
            color = contentColor,
        )
    }
}

/** The one letter a [MonogramBadge] shows: the host's own first letter, past any `www.`. */
fun monogramOf(host: String): String {
    val bare = host.removePrefix("www.")
    return bare.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
