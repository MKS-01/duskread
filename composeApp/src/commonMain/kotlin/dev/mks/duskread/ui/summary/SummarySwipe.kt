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
 * What a row reveals when it is swiped the other way.
 *
 * Saved teaches one swipe already — pull a row and it says what it will do in
 * words before it does it. This is the mirror of that gesture, and stating its
 * intent the same way is what keeps the row itself clean: a list row in this
 * app carries a title and two facts, and hanging controls off every one of
 * them to reach a feature used occasionally is how that stops being true.
 *
 * It opens one panel that both summarises and reads aloud — [default] only
 * decides whether it starts speaking the instant it opens or waits for the
 * play button, and the label says which so the gesture is never a surprise.
 * They were briefly two outcomes on two depths of the same pull, which was a
 * mistake twice over: it asked the reader to meter a gesture to choose
 * between them, and it split two things that want the same fetched article
 * across two panels that could not share it.
 *
 * Drawn in `surfaceContainerHigh` rather than the accent container Remove
 * uses. One is destructive and should look like it; this one is not, and the
 * quieter ground is the difference a thumb halfway through a swipe can read.
 *
 * Aligned to the end, because this is now the leftward pull — the background
 * is uncovered from the right edge, and a label starting at the far left
 * would sit under the row for most of the gesture.
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
            // The panel always does both; only the tense changes, naming the
            // action before release and promising it after — and, when the
            // default is read-aloud, saying which half fires the instant it
            // opens rather than waiting on the play button.
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

// Where the label changes from naming the action to promising it. Matches the
// point Saved's remove background switches, so both swipes commit alike.
private const val ReleasePoint = 0.4f
