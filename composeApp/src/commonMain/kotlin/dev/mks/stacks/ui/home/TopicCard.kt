package dev.mks.stacks.ui.home

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
import dev.mks.stacks.model.Topic
import dev.mks.stacks.ui.theme.LocalVizPalette
import dev.mks.stacks.ui.theme.Mono
import dev.mks.stacks.ui.theme.Radius
import dev.mks.stacks.ui.theme.StacksIcons

/**
 * One topic as a tappable card, kept to a single compact row: title, one-line
 * tagline, and a meta strip (level, problem count, whether there is an
 * animation to watch). Enough to choose what to read next without opening
 * anything. No per-question preview any more — the curriculum keeps growing,
 * and a shorter card means less scrolling per topic; the meta strip's problem
 * count is enough of a signal, and Practice detail lives one tap away.
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
            .height(IntrinsicSize.Min)
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
        // A stronger at-a-glance signal than the meta chip's small dot alone —
        // clipped by the card's own rounded corners since it sits inside them.
        Box(Modifier.fillMaxHeight().width(3.dp).background(levelColor))

        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
        ) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 14.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = topic.tagline,
                fontSize = 11.5.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(6.dp))
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
            StacksIcons.Chevron,
            contentDescription = null,
            modifier = Modifier.padding(end = 10.dp).size(16.dp),
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
