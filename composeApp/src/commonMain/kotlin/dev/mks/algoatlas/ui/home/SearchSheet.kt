package dev.mks.algoatlas.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.algoatlas.content.searchTopics
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.ui.theme.LocalVizPalette
import dev.mks.algoatlas.ui.theme.Mono

/**
 * Search that grows out of the floating bar rather than dropping from the top.
 *
 * The input stays pinned above the keyboard and results stack upward, so your
 * thumb never leaves the bottom of the screen.
 */
@Composable
fun SearchSheet(
    visible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(160)),
    ) {
        Box(Modifier.fillMaxSize()) {
            // Scrim. Tapping anywhere outside the sheet closes it.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    ),
            )

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val results = searchTopics(query)

                AnimatedVisibility(
                    visible = query.isNotBlank(),
                    enter = slideInVertically(tween(220)) { it / 4 } + fadeIn(tween(180)),
                    exit = slideOutVertically(tween(180)) { it / 4 } + fadeOut(tween(140)),
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shadowElevation = 10.dp,
                    ) {
                        if (results.isEmpty()) {
                            Text(
                                text = "Nothing matches “$query”.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 320.dp),
                                contentPadding = PaddingValues(vertical = 6.dp),
                            ) {
                                results.forEach { topic ->
                                    item(topic.id) {
                                        ResultRow(topic, query) { onSelect(topic.id) }
                                    }
                                }
                            }
                        }
                    }
                }

                SearchField(query, onQueryChange, onDismiss)
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 12.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(11.dp))

            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search topics and problems",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.5.sp,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.5.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
            }

            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable { if (query.isEmpty()) onDismiss() else onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = if (query.isEmpty()) "Close search" else "Clear",
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ResultRow(topic: Topic, query: String, onClick: () -> Unit) {
    // Show which question matched, when the hit came from the question list
    // rather than the title — otherwise the result looks arbitrary.
    val matchedQuestion = topic.questions.firstOrNull {
        it.title.contains(query.trim(), ignoreCase = true)
    }?.takeUnless { topic.title.contains(query.trim(), ignoreCase = true) }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(LocalVizPalette.current.of(topic.level)),
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = matchedQuestion?.let { "matches ${it.title}" } ?: topic.tagline,
                fontSize = 11.5.sp,
                maxLines = 1,
                fontFamily = if (matchedQuestion != null) Mono else null,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
