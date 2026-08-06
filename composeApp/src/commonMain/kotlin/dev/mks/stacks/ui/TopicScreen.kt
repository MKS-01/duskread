package dev.mks.stacks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.stacks.content.topicById
import dev.mks.stacks.model.Lang
import dev.mks.stacks.model.Reference
import dev.mks.stacks.model.Topic
import dev.mks.stacks.ui.code.CodeBlock
import dev.mks.stacks.ui.code.markup
import dev.mks.stacks.ui.theme.Layout
import dev.mks.stacks.ui.theme.LocalVizPalette
import dev.mks.stacks.ui.theme.Radius
import dev.mks.stacks.ui.theme.SectionLabel
import dev.mks.stacks.ui.viz.ScenePlayer

@Composable
fun TopicScreen(
    topic: Topic,
    lang: Lang,
    onLangChange: (Lang) -> Unit,
    onOpenTopic: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Reset scroll when the topic changes, otherwise you land mid-article.
    val listState = rememberLazyListState()
    val scene = remember(topic.id) { topic.scene?.invoke() }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Layout.ReadingGutter,
            end = Layout.ReadingGutter,
            top = 8.dp,
            bottom = Layout.BarClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item("head") { TopicHeader(topic) }

        scene?.let {
            item("scene") {
                Column {
                    SectionHeading("How it works")
                    ScenePlayer(it)
                }
            }
        }

        item("note") {
            Column {
                SectionHeading("Note")
                BulletList(topic.note)
            }
        }

        topic.readMore?.let { readMore ->
            item("read-more") {
                Column {
                    SectionHeading("Read more")
                    ReferenceCard(readMore)
                }
            }
        }

        item("code") {
            Column {
                SectionHeading("Implementation")
                CodeBlock(topic.code, lang, onLangChange)
            }
        }

        if (topic.references.isNotEmpty()) {
            item("refs") {
                Column {
                    SectionHeading("Further reading")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        topic.references.forEach { reference -> ReferenceCard(reference) }
                    }
                }
            }
        }

        if (topic.related.isNotEmpty()) {
            item("related") {
                Column {
                    SectionHeading("Related")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        topic.related.mapNotNull { topicById(it) }.forEach { related ->
                            Text(
                                text = related.title,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(999.dp),
                                    )
                                    .clickable { onOpenTopic(related.id) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicHeader(topic: Topic) {
    val palette = LocalVizPalette.current

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.of(topic.level)),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = topic.level.label.uppercase(),
                style = SectionLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = topic.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = topic.tagline,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReferenceCard(reference: Reference) {
    val open = rememberUrlOpener()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Inline))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Inline))
            .clickable { open(reference.url) }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = reference.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            reference.source?.let { source ->
                Text(
                    text = source,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text.uppercase(),
        style = SectionLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun BulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { line ->
            Row {
                Box(
                    Modifier
                        .padding(top = 8.dp, end = 11.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant),
                )
                Text(
                    text = markup(line),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
