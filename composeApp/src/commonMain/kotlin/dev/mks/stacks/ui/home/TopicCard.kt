package dev.mks.stacks.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.stacks.model.Topic
import dev.mks.stacks.ui.theme.LocalVizPalette
import dev.mks.stacks.ui.theme.Mono
import dev.mks.stacks.ui.theme.Radius

/** Two lines of title at 17sp, the level line, and the padding around both. */
private val TileHeight = 70.dp

/**
 * One topic as a tappable tile, two to a row in [LibraryTab].
 *
 * The tagline used to sit under the title, and it is the obvious thing to miss
 * here. It could not come with: half a phone width leaves about 160dp of text
 * column, where "One unbroken block of memory — and everything that follows
 * from it." runs to three lines and the tile stops being a tile. Trading that
 * one-line hint for roughly twice as many topics per screen is the right way
 * round on a curriculum this size — the title is what you scan for, and the
 * tagline is the first thing the topic screen shows anyway.
 *
 * The chevron went for the same reason: at this width it is competing for
 * space with the meta line, and a tile that is entirely tappable does not need
 * an arrow to say so.
 *
 * The problem count went too, and not for space: every topic in the app has
 * two or three questions, so a number that is always 2 or 3 cannot help anyone
 * choose between them.
 *
 * Level is the only meta left, and it is one thing now instead of three. It
 * used to be a coloured stripe down the left edge *and* a dot *and* a word in
 * a pill; the stripe in particular read as a stray mark at this size, because
 * the card's 20dp corners clip everything but a stub in the middle of it. One
 * uppercase word, tinted, says the same thing without drawing a shape.
 */
@Composable
fun TopicCard(
    topic: Topic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVizPalette.current
    val levelColor = palette.of(topic.level)

    Row(
        modifier
            .fillMaxWidth()
            // Fixed, not intrinsic: a grid row is as tall as its tallest tile,
            // so the two-line titles ("Divide & Conquer") would otherwise leave
            // their one-line neighbours short and the row ragged. Tall enough
            // for two lines of title plus the meta row.
            .height(TileHeight)
            .clip(RoundedCornerShape(Radius.Card))
            // Flat neutral surface, same as every other card in the app
            // (Home's Algo of the Day, every Reader entry) — level is
            // carried by the small dot in the meta chip below, not a
            // full-card colour wash.
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Card))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 14.5.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Titles hang from the top, the level sits on the floor. Reserving
            // two title lines instead would leave a hole in every one-line
            // tile, which is nearly all of them.
            Spacer(Modifier.weight(1f))
            Text(
                text = topic.level.label.uppercase(),
                fontFamily = Mono,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                color = levelColor,
            )
        }
    }
}
