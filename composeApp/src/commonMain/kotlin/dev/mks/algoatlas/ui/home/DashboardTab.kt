package dev.mks.algoatlas.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import dev.mks.algoatlas.content.AllTopics
import dev.mks.algoatlas.content.Chapters
import dev.mks.algoatlas.model.Topic
import dev.mks.algoatlas.pomodoro.PickableMinutes
import dev.mks.algoatlas.pomodoro.clockLabel
import dev.mks.algoatlas.pomodoro.rememberPomodoroController
import dev.mks.algoatlas.reader.ReadItem
import dev.mks.algoatlas.reader.ReadSort
import dev.mks.algoatlas.reader.ReaderSource
import dev.mks.algoatlas.reader.rememberReadRepository
import dev.mks.algoatlas.ui.reader.formatDuration
import dev.mks.algoatlas.ui.theme.AtlasIcons
import dev.mks.algoatlas.ui.theme.LocalVizPalette
import dev.mks.algoatlas.ui.theme.Radius
import dev.mks.algoatlas.ui.theme.SectionLabel
import dev.mks.algoatlas.ui.theme.Space
import dev.mks.algoatlas.ui.theme.Stroke
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Home: a dashboard rather than another list — the curriculum browser moved
 * to [LibraryTab]. What is left is the small set of things worth seeing every
 * time the app opens: one topic to read today, the focus timer, and whatever
 * is queued up in the reader.
 */
@Composable
fun DashboardTab(
    onOpenTopic: (String) -> Unit,
    onOpenFocus: () -> Unit,
    onOpenReader: () -> Unit,
    greeting: String?,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.CardGap),
    ) {
        item("head") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    // The greeting only appears if a name was given — no
                    // "Hello, there" fallback, which reads worse than nothing.
                    greeting?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
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

        item("algo-of-day") {
            AlgoOfDayCard(
                modifier = Modifier.padding(top = 6.dp),
                onOpen = onOpenTopic,
            )
        }

        item("focus") {
            FocusCard(onOpen = onOpenFocus)
        }

        item("readback") {
            ReadbackOfDayCard(onOpen = onOpenReader)
        }
    }
}

/** Stable across a single calendar day, so it does not change on every recompose. */
@OptIn(ExperimentalTime::class)
private fun algoOfTheDay(): Topic {
    val dayIndex = Clock.System.now().toEpochMilliseconds() / 86_400_000L
    return AllTopics[(dayIndex % AllTopics.size).toInt()]
}

@Composable
private fun AlgoOfDayCard(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val topic = remember { algoOfTheDay() }
    val palette = LocalVizPalette.current
    val levelColor = palette.of(topic.level)

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Card))
            .clickable { onOpen(topic.id) }
            .padding(16.dp),
    ) {
        Text(
            text = "ALGO OF THE DAY",
            style = SectionLabel,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = topic.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = topic.tagline,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(levelColor))
            Spacer(Modifier.width(6.dp))
            Text(
                text = topic.level.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FocusCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val controller = rememberPomodoroController()
    val state by controller.state.collectAsState()

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Card))
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (!state.idle && state.running) AtlasIcons.Pause else AtlasIcons.Play,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "FOCUS",
                style = SectionLabel,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (state.idle) "Start a session" else state.clockLabel,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (state.idle) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickableMinutes.forEach { minutes ->
                    QuickStartChip(text = "$minutes min") { controller.start(minutes) }
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (state.running) "Running — tap to open" else "Paused — tap to open",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickStartChip(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.Pill))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    )
}

@Composable
private fun ReadbackOfDayCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val repository = rememberReadRepository()
    val source by repository.source.collectAsState()
    var latest by remember { mutableStateOf<ReadItem?>(null) }

    LaunchedEffect(source) {
        latest = if (source == ReaderSource.READY) {
            repository.listReads(query = "", sort = ReadSort.NEWEST).firstOrNull()
        } else {
            null
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Card))
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = AtlasIcons.Waveform,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "TODAY'S READBACK",
                style = SectionLabel,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(6.dp))

        val item = latest
        when {
            source != ReaderSource.READY -> {
                Text(
                    text = "Connect your readback library",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Point the Reader tab at a synced readback-audio-db folder to see your latest reads here.",
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item == null -> {
                Text(
                    text = "Nothing read yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            else -> {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                item.summary?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formatDuration(item.durationSec),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
