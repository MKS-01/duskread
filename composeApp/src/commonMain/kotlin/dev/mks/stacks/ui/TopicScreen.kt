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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.stacks.content.topicById
import dev.mks.stacks.model.ComplexityRow
import dev.mks.stacks.model.Lang
import dev.mks.stacks.model.Question
import dev.mks.stacks.model.Reference
import dev.mks.stacks.model.Topic
import dev.mks.stacks.ui.code.CodeBlock
import dev.mks.stacks.ui.code.markup
import dev.mks.stacks.ui.theme.Layout
import dev.mks.stacks.ui.theme.LocalVizPalette
import dev.mks.stacks.ui.theme.Mono
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
    // A topic with no quickSummary has nothing shorter to show, so it opens
    // straight into the full notes as before.
    val expanded = remember(topic.id) { mutableStateOf(topic.quickSummary.isEmpty()) }

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

        // Shown in both modes — a topic never ships without its visualisation,
        // condensed or not.
        scene?.let {
            item("scene") {
                Column {
                    SectionHeading("How it works")
                    ScenePlayer(it)
                }
            }
        }

        if (!expanded.value) {
            item("quick") {
                Column {
                    SectionHeading("Quick notes")
                    BulletList(topic.quickSummary)
                }
            }

            item("key") {
                Column {
                    SectionHeading("What to remember")
                    BulletList(topic.keyPoints)
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

            item("expand") {
                Text(
                    text = "Show full notes",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                        .clickable { expanded.value = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        } else {
            item("intuition") {
                Column {
                    SectionHeading("Intuition")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        topic.intuition.forEach { paragraph ->
                            Text(
                                text = markup(paragraph),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }

            topic.origin?.let { origin ->
                item("origin") { OriginCard(origin) }
            }

            item("key") {
                Column {
                    SectionHeading("What to remember")
                    BulletList(topic.keyPoints)
                }
            }

            if (topic.steps.isNotEmpty()) {
                item("steps") {
                    Column {
                        SectionHeading("Walkthrough")
                        NumberedList(topic.steps)
                    }
                }
            }

            item("complexity") {
                Column {
                    SectionHeading("Complexity")
                    ComplexityTable(topic.complexity)
                }
            }

            item("code") {
                Column {
                    SectionHeading("Implementation")
                    CodeBlock(topic.code, lang, onLangChange)
                }
            }

            if (topic.pitfalls.isNotEmpty()) {
                item("pitfalls") {
                    Column {
                        SectionHeading("Where it goes wrong")
                        BulletList(topic.pitfalls, bulletColor = LocalVizPalette.current.bad)
                    }
                }
            }

            item("q-head") { SectionHeading("Practice") }

            items(topic.questions, key = { it.title }) { question ->
                QuestionCard(question)
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

/** The "where this came from" aside — visually distinct from the body text. */
@Composable
private fun OriginCard(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Panel))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
    ) {
        Box(
            Modifier
                .padding(end = 12.dp)
                .width(3.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LocalVizPalette.current.warn),
        )
        Column {
            Text(
                text = "ORIGIN STORY",
                style = SectionLabel,
                color = LocalVizPalette.current.warn,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = markup(text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
private fun BulletList(items: List<String>, bulletColor: androidx.compose.ui.graphics.Color? = null) {
    val dot = bulletColor ?: MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { line ->
            Row {
                Box(
                    Modifier
                        .padding(top = 8.dp, end = 11.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(dot),
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

@Composable
private fun NumberedList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, line ->
            Row {
                Box(
                    Modifier
                        .padding(top = 1.dp, end = 11.dp)
                        .size(19.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    text = markup(line),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun ComplexityTable(rows: List<ComplexityRow>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = row.time,
                        fontFamily = Mono,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = " · ",
                        fontFamily = Mono,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = row.space,
                        fontFamily = Mono,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                row.note?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = "time · space",
            fontFamily = Mono,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 14.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun QuestionCard(question: Question) {
    val palette = LocalVizPalette.current

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Panel))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Panel))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            question.id?.let { id ->
                Text(
                    text = "#$id",
                    fontFamily = Mono,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 7.dp),
                )
            }
            Text(
                text = question.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = question.difficulty.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = palette.of(question.difficulty),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.of(question.difficulty).copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }

        Spacer(Modifier.height(7.dp))
        Text(
            text = question.idea,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        question.askedAt?.let { asked ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = asked,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }
    }
}
