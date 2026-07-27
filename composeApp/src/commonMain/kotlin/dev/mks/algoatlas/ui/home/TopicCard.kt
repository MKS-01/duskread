package dev.mks.algoatlas.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.ui.theme.LocalVizPalette
import dev.mks.algoatlas.ui.theme.Mono

/**
 * One topic as a tappable card.
 *
 * The metadata strip along the bottom is the point: level, how many practice
 * questions, and whether there is an animation to watch. Enough to choose what
 * to read next without opening anything.
 */
@Composable
fun TopicCard(topic: Topic, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalVizPalette.current
    val levelColor = palette.of(topic.level)

    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Level stripe — a spine down the full left edge, so it lines up with
        // the title rather than floating beside the middle of the card.
        Box(
            Modifier
                .padding(vertical = 12.dp, horizontal = 10.dp)
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(levelColor),
        )

        Column(
            Modifier
                .weight(1f)
                .padding(top = 14.dp, bottom = 13.dp, end = 8.dp),
        ) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 15.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = topic.tagline,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(9.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MetaChip(topic.level.label, levelColor)
                MetaChip("${topic.questions.size} problems", null)
                if (topic.scene != null) MetaChip("visual", null)
            }
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.padding(end = 10.dp).size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun MetaChip(label: String, accent: androidx.compose.ui.graphics.Color?) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accent != null) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = label,
            fontFamily = Mono,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
