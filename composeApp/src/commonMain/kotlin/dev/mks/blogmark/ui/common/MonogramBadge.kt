package dev.mks.blogmark.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
 */
@Composable
fun MonogramBadge(
    host: String,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = monogramOf(host),
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.42f).sp,
            color = contentColor,
        )
    }
}

/** The one letter a [MonogramBadge] shows: the host's own first letter, past any `www.`. */
fun monogramOf(host: String): String {
    val bare = host.removePrefix("www.")
    return bare.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
