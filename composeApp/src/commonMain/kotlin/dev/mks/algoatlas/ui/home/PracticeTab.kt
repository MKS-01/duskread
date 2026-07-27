package dev.mks.algoatlas.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.mks.algoatlas.content.AllQuestions
import dev.mks.algoatlas.content.PracticeItem
import dev.mks.algoatlas.model.Difficulty
import dev.mks.algoatlas.ui.theme.LocalVizPalette
import dev.mks.algoatlas.ui.theme.Mono

/**
 * Every interview question across the curriculum, in one list.
 *
 * A different session shape from Learn: many short items you skim to check
 * recall, rather than one topic you sit with. Filtering by difficulty is what
 * makes it usable once this reaches a hundred-odd problems.
 */
@Composable
fun PracticeTab(
    onOpenTopic: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf<Difficulty?>(null) }
    val items = remember(filter) {
        AllQuestions.filter { filter == null || it.question.difficulty == filter }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("head") {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "Practice",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${AllQuestions.size} problems from the topics you have read.",
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
                FilterChip("All", filter == null) { filter = null }
                Difficulty.entries.forEach { difficulty ->
                    val count = AllQuestions.count { it.question.difficulty == difficulty }
                    FilterChip(
                        label = "${difficulty.label} · $count",
                        active = filter == difficulty,
                        accent = LocalVizPalette.current.of(difficulty),
                    ) {
                        filter = if (filter == difficulty) null else difficulty
                    }
                }
            }
        }

        items.forEach { item ->
            item("${item.topic.id}-${item.question.title}") {
                QuestionRow(item) { onOpenTopic(item.topic.id) }
            }
        }
    }
}

@Composable
private fun FilterChip(
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

@Composable
private fun QuestionRow(item: PracticeItem, onClick: () -> Unit) {
    val palette = LocalVizPalette.current
    val difficultyColor = palette.of(item.question.difficulty)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            item.question.id?.let { id ->
                Text(
                    text = "#$id",
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 7.dp),
                )
            }
            Text(
                text = item.question.title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 14.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = item.question.difficulty.label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = difficultyColor,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(difficultyColor.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }

        Spacer(Modifier.height(7.dp))
        Text(
            text = item.question.idea,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(palette.of(item.topic.level)),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = item.topic.title,
                fontSize = 11.sp,
                fontFamily = Mono,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
