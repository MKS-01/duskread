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
 * One topic as a tappable card.
 *
 * The metadata strip along the bottom is the point: level, how many practice
 * questions, and whether there is an animation to watch. Enough to choose what
 * to read next without opening anything.
 *
 * [showQuestions] adds one of the topic's question titles beneath the meta
 * row — Library wants that preview so browsing and practice read as one
 * list; the two-pane list rail does not have the width to spare for it. Kept
 * to one line rather than two now that the card itself is compact, since the
 * curriculum keeps growing and a shorter card means less scrolling per topic.
 */
@Composable
fun TopicCard(
    topic: Topic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showQuestions: Boolean = false,
) {
    val palette = LocalVizPalette.current
    val levelColor = palette.of(topic.level)

    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(Radius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Card))
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
                .clip(RoundedCornerShape(Radius.Marker))
                .background(levelColor),
        )

        Column(
            Modifier
                .weight(1f)
                .padding(top = 10.dp, bottom = 10.dp, end = 8.dp),
        ) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = topic.tagline,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(7.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MetaChip(topic.level.label, levelColor)
                MetaChip("${topic.questions.size} problems", null)
                if (topic.scene != null) MetaChip("visual", null)
            }

            if (showQuestions && topic.questions.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    topic.questions.take(1).forEach { question ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(palette.of(question.difficulty)),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = question.title,
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    val remaining = topic.questions.size - 1
                    if (remaining > 0) {
                        Text(
                            text = "+$remaining more",
                            fontSize = 10.5.sp,
                            fontFamily = Mono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
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
