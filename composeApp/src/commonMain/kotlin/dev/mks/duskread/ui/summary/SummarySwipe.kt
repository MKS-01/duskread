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
import dev.mks.duskread.ui.theme.DuskReadIcons

/**
 * What a row reveals when it is swiped the other way.
 *
 * Saved already teaches one swipe — pull a row left and it says "Remove" in
 * words before it does anything. Summarising is the mirror of that gesture,
 * and stating its intent the same way is what keeps the row itself clean: a
 * list row in this app carries a title and two facts, and hanging a third
 * control off every one of them to reach a feature used occasionally is how
 * that stops being true.
 *
 * Drawn in `surfaceContainerHigh` rather than the accent container Remove
 * uses. One is destructive and should look like it; this one is not, and the
 * quieter ground is the difference a thumb halfway through a swipe can read.
 */
@Composable
fun SummariseBackground(progress: Float, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            DuskReadIcons.Summary,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (progress > ReleasePoint) "Release to summarise" else "Summarise",
            style = MaterialTheme.typography.labelLarge,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// Where the label changes from naming the action to promising it. Matches the
// point Saved's remove background switches, so both swipes commit alike.
private const val ReleasePoint = 0.4f
