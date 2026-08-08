package dev.mks.stacks.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.stacks.content.Chapters
import dev.mks.stacks.model.Level
import dev.mks.stacks.ui.theme.LocalVizPalette
import dev.mks.stacks.ui.theme.SectionLabel
import dev.mks.stacks.ui.theme.Space

/**
 * The curriculum: chapters, and the topics inside them.
 *
 * Practice questions are deliberately absent from this screen. The library is
 * where you go to find *the thing itself* — what a heap is, how Dijkstra
 * works — and framing that browse around LeetCode problems answered a question
 * nobody was asking here: the subtitle counted problems rather than topics, and
 * the filter sorted topics by the difficulty of questions attached to them,
 * which is not a property of the topic at all. A topic's own level is, so that
 * is what filters now. The questions still live on the topic screen, one tap
 * away, next to the algorithm they belong to.
 *
 * Two columns, because the curriculum outgrew a single one: 41 topics over 11
 * chapters at four-and-a-half full-width cards per screen is a lot of
 * scrolling to find anything. The tile that pays for it is in [TopicCard] —
 * the tagline had to go. Chapter headers still span the full width, so the
 * chapter rhythm survives the change.
 */
@Composable
fun LibraryTab(
    onOpenTopic: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf<Level?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.CardGap),
        horizontalArrangement = Arrangement.spacedBy(Space.CardGap),
    ) {
        item("head", span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${Chapters.sumOf { it.topics.size }} topics across ${Chapters.size} chapters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item("filters", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                // "All" has no accent dot, the rest do — without this they
                // default to top-aligned instead of sharing a centre line.
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LevelChip("All", filter == null) { filter = null }
                Level.entries.forEach { level ->
                    val count = Chapters.sumOf { chapter -> chapter.topics.count { it.level == level } }
                    LevelChip(
                        label = "${level.label} · $count",
                        active = filter == level,
                        accent = LocalVizPalette.current.of(level),
                    ) {
                        filter = if (filter == level) null else level
                    }
                }
            }
        }

        Chapters.forEach { chapter ->
            val topics = chapter.topics.filter { filter == null || it.level == filter }
            if (topics.isEmpty()) return@forEach

            item("${chapter.id}-head", span = { GridItemSpan(maxLineSpan) }) {
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

            topics.forEach { topic ->
                item(topic.id) {
                    TopicCard(topic, onClick = { onOpenTopic(topic.id) })
                }
            }
        }
    }
}

@Composable
private fun LevelChip(
    label: String,
    active: Boolean,
    accent: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accent != null) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
