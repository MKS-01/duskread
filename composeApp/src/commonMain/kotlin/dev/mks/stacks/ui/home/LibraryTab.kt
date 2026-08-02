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
import androidx.compose.foundation.lazy.LazyColumn
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
import dev.mks.stacks.content.AllQuestions
import dev.mks.stacks.content.Chapters
import dev.mks.stacks.model.Difficulty
import dev.mks.stacks.ui.theme.LocalVizPalette
import dev.mks.stacks.ui.theme.SectionLabel
import dev.mks.stacks.ui.theme.Space

/**
 * The curriculum and its practice questions, together.
 *
 * This used to be two tabs — Learn to browse, Practice to drill. Splitting
 * them meant a topic's questions were never visible next to the topic itself,
 * so browsing told you nothing about what you would be asked. Each card now
 * carries a short question preview; the difficulty filter narrows which
 * topics show rather than flattening everything into a second list.
 */
@Composable
fun LibraryTab(
    onOpenTopic: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf<Difficulty?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.CardGap),
    ) {
        item("head") {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${AllQuestions.size} problems across ${Chapters.sumOf { it.topics.size }} topics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item("filters") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                DifficultyChip("All", filter == null) { filter = null }
                Difficulty.entries.forEach { difficulty ->
                    val count = AllQuestions.count { it.question.difficulty == difficulty }
                    DifficultyChip(
                        label = "${difficulty.label} · $count",
                        active = filter == difficulty,
                        accent = LocalVizPalette.current.of(difficulty),
                    ) {
                        filter = if (filter == difficulty) null else difficulty
                    }
                }
            }
        }

        Chapters.forEach { chapter ->
            val topics = chapter.topics.filter { topic ->
                filter == null || topic.questions.any { it.difficulty == filter }
            }
            if (topics.isEmpty()) return@forEach

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

            topics.forEach { topic ->
                item(topic.id) {
                    TopicCard(topic, onClick = { onOpenTopic(topic.id) }, showQuestions = true)
                }
            }
        }
    }
}

@Composable
private fun DifficultyChip(
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
