package dev.mks.algoatlas.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.algoatlas.content.Chapters
import dev.mks.algoatlas.content.searchTopics
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.ui.theme.LocalVizPalette
import dev.mks.algoatlas.ui.theme.SectionLabel

/**
 * The curriculum list. On a phone this is a full screen; on a wide window it is
 * the left pane. Same composable either way — only the width changes.
 */
@Composable
fun TopicListPane(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searching = query.isNotBlank()
    val results = if (searching) searchTopics(query) else emptyList()

    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            placeholder = { Text("Search topics and problems", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(18.dp)) },
            trailingIcon = {
                if (searching) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, "Clear search", Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (searching) {
                if (results.isEmpty()) {
                    item {
                        Text(
                            text = "Nothing matches \"$query\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
                results.forEach { topic ->
                    item(topic.id) {
                        TopicRow(topic, topic.id == selectedId) { onSelect(topic.id) }
                    }
                }
            } else {
                Chapters.forEach { chapter ->
                    item("${chapter.id}-head") {
                        Column(Modifier.padding(start = 8.dp, top = 16.dp, bottom = 6.dp)) {
                            Text(
                                text = chapter.title.uppercase(),
                                style = SectionLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = chapter.blurb,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }

                    chapter.topics.forEach { topic ->
                        item(topic.id) {
                            TopicRow(topic, topic.id == selectedId) { onSelect(topic.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicRow(topic: Topic, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalVizPalette.current

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.background.copy(alpha = 0f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(palette.of(topic.level)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = topic.tagline,
                fontSize = 11.5.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
