package dev.mks.duskread.ui.common

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
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.Stroke

/**
 * A followed blog's badge: the first letter of its host, not a guessed icon.
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
            // The mockup's chip is 22px holding 10px type; keeping that ratio means the
            // letter still sits inside its square at 28dp.
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
