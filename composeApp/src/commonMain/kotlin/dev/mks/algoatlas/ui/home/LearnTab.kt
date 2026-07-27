package dev.mks.algoatlas.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.algoatlas.content.AllTopics
import dev.mks.algoatlas.content.Chapters
import dev.mks.algoatlas.ui.theme.SectionLabel

/**
 * The curriculum, chapter by chapter.
 *
 * There is no fixed app bar — the title scrolls away with the content, which
 * buys back a chunk of vertical space on a phone.
 */
@Composable
fun LearnTab(
    onOpenTopic: (String) -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item("head") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Algo Atlas",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "${AllTopics.size} topics · ${Chapters.size} chapters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(onClick = onToggleTheme),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = if (isDark) "Switch to light theme" else "Switch to dark theme",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Chapters.forEach { chapter ->
            item("${chapter.id}-head") {
                Column(Modifier.padding(top = 14.dp, bottom = 2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = chapter.title.uppercase(),
                            style = SectionLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(9.dp))
                        Box(
                            Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = chapter.blurb,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                }
            }

            chapter.topics.forEach { topic ->
                item(topic.id) {
                    TopicCard(topic, onClick = { onOpenTopic(topic.id) })
                }
            }
        }
    }
}
