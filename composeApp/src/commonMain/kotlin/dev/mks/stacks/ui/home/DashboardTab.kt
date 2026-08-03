package dev.mks.stacks.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.stacks.content.AllTopics
import dev.mks.stacks.content.Chapters
import dev.mks.stacks.model.Topic
import dev.mks.stacks.pomodoro.PickableMinutes
import dev.mks.stacks.pomodoro.clockLabel
import dev.mks.stacks.pomodoro.rememberPomodoroController
import dev.mks.stacks.reader.ReadItem
import dev.mks.stacks.reader.ReadSort
import dev.mks.stacks.reader.ReaderSource
import dev.mks.stacks.reader.rememberReadRepository
import dev.mks.stacks.ui.reader.formatDuration
import dev.mks.stacks.ui.rememberUrlOpener
import dev.mks.stacks.ui.theme.LocalVizPalette
import dev.mks.stacks.ui.theme.Radius
import dev.mks.stacks.ui.theme.SectionLabel
import dev.mks.stacks.ui.theme.Space
import dev.mks.stacks.ui.theme.StacksIcons
import dev.mks.stacks.ui.theme.Stroke
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke

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
                        text = "Stacks",
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

        item("library-pick") {
            LibraryPickCard(modifier = Modifier.padding(top = 6.dp))
        }

        item("algo-of-day") {
            AlgoOfDayCard(onOpen = onOpenTopic)
        }

        // Ahead of Readback, not at the very top — today's topic stays the
        // first thing seen, but the timer still surfaces before whatever
        // happens to be queued up in the reader.
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
                imageVector = if (!state.idle && state.running) StacksIcons.Pause else StacksIcons.Play,
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
    Row(
        Modifier
            .clip(RoundedCornerShape(Radius.Pill))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = StacksIcons.Clock,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
                imageVector = StacksIcons.Waveform,
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

/**
 * A random pick from the reader's own library rather than anything from the
 * network — every other card on the dashboard is either bundled content or
 * local state, and a "trending" claim sourced from an outside API was the odd
 * one out. Renders nothing until the library has at least one read, so it
 * never duplicates the "Connect your library" prompt [ReadbackOfDayCard]
 * already shows.
 */
@Composable
private fun LibraryPickCard(modifier: Modifier = Modifier) {
    val repository = rememberReadRepository()
    val source by repository.source.collectAsState()
    val open = rememberUrlOpener()
    // Candidates are loaded once per source change; shuffling re-picks from
    // this in memory rather than re-querying, since it is a local library and
    // the whole point of the button is an instant re-roll.
    var candidates by remember { mutableStateOf<List<ReadItem>>(emptyList()) }
    var item by remember { mutableStateOf<ReadItem?>(null) }

    LaunchedEffect(source) {
        candidates = if (source == ReaderSource.READY) {
            // Only reads with a real source link — the card's whole point is
            // "Read original", so one without a link is a dead end.
            repository.listReads(query = "", sort = ReadSort.NEWEST)
                .filter { it.sourceUrl.isNotBlank() }
        } else {
            emptyList()
        }
        item = candidates.randomOrNull()
    }

    val found = item ?: return

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Card))
            .clickable { open(found.sourceUrl) }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "FROM YOUR LIBRARY",
                style = SectionLabel,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (candidates.size > 1) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable {
                            item = candidates.filterNot { it.id == found.id }.random()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = StacksIcons.Shuffle,
                        contentDescription = "Show a different pick",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // No cover image comes from the library, unlike a web article — an
        // icon for what the read is actually about stands in, matched by
        // keyword against the title and excerpt rather than picked at random.
        Box(
            Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(Radius.Panel))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            LibraryPickArt(
                category = categorize("${found.title} ${found.summary ?: found.excerpt}"),
                modifier = Modifier.size(46.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = found.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        (found.summary ?: found.excerpt).let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Read original",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** What a read is actually about, matched by keyword rather than guessed at random. */
private enum class LibraryCategory { ANDROID, AI, HACKING, MOBILE, GADGET, TECH }

private fun categorize(text: String): LibraryCategory {
    val lower = text.lowercase()
    fun matches(vararg words: String) = words.any { it in lower }
    return when {
        matches("android", "jetpack", "google play", "kotlin multiplatform") -> LibraryCategory.ANDROID
        matches("llm", "large language model", "gpt", "chatgpt", "claude", "machine learning", "neural network", "artificial intelligence") ->
            LibraryCategory.AI
        matches("hack", "exploit", "vulnerab", "breach", "malware", "cve", "penetration test") -> LibraryCategory.HACKING
        matches("iphone", "smartphone", "ios ", "mobile app", "app store") -> LibraryCategory.MOBILE
        matches("gadget", "wearable", "smartwatch", "hardware review", "unboxing") -> LibraryCategory.GADGET
        else -> LibraryCategory.TECH
    }
}

/**
 * A stand-in for the cover image a library read doesn't have: a small stroked
 * glyph for the category [categorize] matched, in the same hand-drawn style
 * as [StacksIcons] rather than a stock illustration.
 */
@Composable
private fun LibraryPickArt(category: LibraryCategory, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        when (category) {
            LibraryCategory.ANDROID -> drawAndroidGlyph(color)
            LibraryCategory.AI -> drawAiGlyph(color)
            LibraryCategory.HACKING -> drawHackingGlyph(color)
            LibraryCategory.MOBILE -> drawMobileGlyph(color)
            LibraryCategory.GADGET -> drawGadgetGlyph(color)
            LibraryCategory.TECH -> drawTechGlyph(color)
        }
    }
}

private val GlyphStrokeWidth = 2.4.dp

/** A rounded head, two antennae and two eyes — the bugdroid, reduced to its simplest reading. */
private fun DrawScope.drawAndroidGlyph(color: Color) {
    val stroke = GlyphStrokeWidth.toPx()
    val headTop = size.height * 0.32f
    val headBottom = size.height * 0.88f
    val headLeft = size.width * 0.16f
    val headRight = size.width * 0.84f
    val sideTop = headTop + (headBottom - headTop) * 0.375f

    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(headLeft, headTop),
        size = Size(headRight - headLeft, (headBottom - headTop) * 0.75f),
        style = DrawStroke(stroke, cap = StrokeCap.Round),
    )
    drawLine(color, Offset(headLeft, sideTop), Offset(headLeft, headBottom), stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(headRight, sideTop), Offset(headRight, headBottom), stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(headLeft, headBottom), Offset(headRight, headBottom), stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.32f, headTop), Offset(size.width * 0.22f, size.height * 0.1f), stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.68f, headTop), Offset(size.width * 0.78f, size.height * 0.1f), stroke, cap = StrokeCap.Round)
    val eyeY = headTop + (headBottom - headTop) * 0.3f
    drawCircle(color, radius = 2.dp.toPx(), center = Offset(size.width * 0.38f, eyeY))
    drawCircle(color, radius = 2.dp.toPx(), center = Offset(size.width * 0.62f, eyeY))
}

/** Three outer nodes and a centre one, all connected — the smallest thing that reads as "network". */
private fun DrawScope.drawAiGlyph(color: Color) {
    val stroke = GlyphStrokeWidth.toPx()
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) * 0.38f
    val outer = listOf(-90f, 30f, 150f).map { degrees ->
        val radians = degrees * (kotlin.math.PI / 180.0)
        Offset(
            center.x + radius * kotlin.math.cos(radians).toFloat(),
            center.y + radius * kotlin.math.sin(radians).toFloat(),
        )
    }
    outer.forEach { point -> drawLine(color, center, point, stroke, cap = StrokeCap.Round) }
    outer.forEach { point -> drawCircle(color, radius = 4.dp.toPx(), center = point) }
    drawCircle(color, radius = 4.dp.toPx(), center = center)
}

/** A shackle over a body — a padlock, the plainest way to say "security". */
private fun DrawScope.drawHackingGlyph(color: Color) {
    val stroke = GlyphStrokeWidth.toPx()
    val bodyTop = size.height * 0.48f
    val bodyLeft = size.width * 0.2f
    val bodyRight = size.width * 0.8f
    val bodyBottom = size.height * 0.85f

    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(size.width * 0.3f, size.height * 0.12f),
        size = Size(size.width * 0.4f, size.height * 0.5f),
        style = DrawStroke(stroke, cap = StrokeCap.Round),
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = DrawStroke(stroke),
    )
    drawCircle(color, radius = 2.6.dp.toPx(), center = Offset(size.width / 2f, (bodyTop + bodyBottom) / 2f))
}

/** A tall rounded rectangle with a home-indicator line — a phone, not a tablet or a watch. */
private fun DrawScope.drawMobileGlyph(color: Color) {
    val stroke = GlyphStrokeWidth.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.3f, size.height * 0.08f),
        size = Size(size.width * 0.4f, size.height * 0.84f),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = DrawStroke(stroke),
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.44f, size.height * 0.84f),
        end = Offset(size.width * 0.56f, size.height * 0.84f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

/** A watch face and strap — the shorthand for a wearable, the most common gadget in a reading list. */
private fun DrawScope.drawGadgetGlyph(color: Color) {
    val stroke = GlyphStrokeWidth.toPx()
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) * 0.28f
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - size.width * 0.12f, size.height * 0.04f),
        size = Size(size.width * 0.24f, size.height * 0.2f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = DrawStroke(stroke),
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - size.width * 0.12f, size.height * 0.76f),
        size = Size(size.width * 0.24f, size.height * 0.2f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = DrawStroke(stroke),
    )
    drawCircle(color, radius = radius, center = center, style = DrawStroke(stroke))
}

/** A chip body with pins on either side — the fallback when nothing more specific matched. */
private fun DrawScope.drawTechGlyph(color: Color) {
    val stroke = GlyphStrokeWidth.toPx()
    val left = size.width * 0.28f
    val right = size.width * 0.72f
    val top = size.height * 0.28f
    val bottom = size.height * 0.72f
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = DrawStroke(stroke),
    )
    val pinPositions = listOf(0.35f, 0.5f, 0.65f)
    pinPositions.forEach { fraction ->
        val y = top + (bottom - top) * fraction
        drawLine(color, Offset(left - size.width * 0.12f, y), Offset(left, y), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(right, y), Offset(right + size.width * 0.12f, y), stroke, cap = StrokeCap.Round)
        val x = left + (right - left) * fraction
        drawLine(color, Offset(x, top - size.height * 0.12f), Offset(x, top), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(x, bottom), Offset(x, bottom + size.height * 0.12f), stroke, cap = StrokeCap.Round)
    }
}
