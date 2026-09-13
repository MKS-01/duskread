package dev.mks.duskread.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.summary.SwipeDefault
import dev.mks.duskread.ui.theme.DuskReadIcons

/**
 * What a row reveals when it is swiped the other way. Saved teaches one swipe already —
 * pull a row and it says what it will do in words before it does it.
 */
@Composable
fun SummariseBackground(progress: Float, default: SwipeDefault, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            // The panel always does both; only the tense changes, naming the action
            // before release and promising it after — and.
            text = when {
                progress > ReleasePoint && default == SwipeDefault.ReadAloud -> "Release to listen"
                progress > ReleasePoint -> "Release to summarise"
                else -> "Summarise & listen"
            },
            style = MaterialTheme.typography.labelLarge,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            if (default == SwipeDefault.ReadAloud) DuskReadIcons.Waveform else DuskReadIcons.Summary,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

// Where the label changes from naming the action to promising it. Matches the point
// Saved's remove background switches, so both swipes commit alike.
private const val ReleasePoint = 0.4f
