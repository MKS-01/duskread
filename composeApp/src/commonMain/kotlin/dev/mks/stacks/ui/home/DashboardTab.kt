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
import dev.mks.stacks.links.FeedLibrary
import dev.mks.stacks.links.FeedPostCache
import dev.mks.stacks.links.LinkLibrary
import dev.mks.stacks.pomodoro.PickableMinutes
import dev.mks.stacks.pomodoro.clockLabel
import dev.mks.stacks.pomodoro.rememberPomodoroController
import dev.mks.stacks.reader.ReadItem
import dev.mks.stacks.reader.ReadSort
import dev.mks.stacks.reader.ReaderSource
import dev.mks.stacks.reader.rememberReadRepository
import dev.mks.stacks.ui.reader.formatDuration
import dev.mks.stacks.ui.rememberUrlOpener
import dev.mks.stacks.ui.theme.Radius
import dev.mks.stacks.ui.theme.SectionLabel
import dev.mks.stacks.ui.theme.Space
import dev.mks.stacks.ui.theme.StacksIcons
import dev.mks.stacks.ui.theme.Stroke
import io.ktor.client.HttpClient
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke

/**
 * Home: a dashboard rather than a list. Two doors into the app's actual
 * content — something saved to read, something already turned into audio —
 * lead the screen, with the focus timer ahead of both because it is the one
 * habit worth reaching for before picking either.
 */
@Composable
fun DashboardTab(
    onOpenFocus: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenReadback: () -> Unit,
    links: LinkLibrary,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    feedClient: HttpClient,
    greeting: String?,
    mono: Boolean,
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
                        imageVector = StacksIcons.Contrast,
                        contentDescription = if (mono) "Switch to the colour theme" else "Switch to the monochrome theme",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Leads the screen — the focus timer is the habit this app wants
        // reached for every time it opens, ahead of any specific content pick.
        item("focus") {
            FocusCard(onOpen = onOpenFocus, modifier = Modifier.padding(top = 6.dp))
        }

        item("saved-pick") {
            SavedPickCard(links = links, onOpenSaved = onOpenSaved)
        }

        item("readback") {
            ReadbackOfDayCard(onOpen = onOpenReadback)
        }

        item("following") {
            FollowingSection(
                feedLibrary = feeds,
                postCache = feedPosts,
                linkLibrary = links,
                client = feedClient,
                modifier = Modifier.padding(top = 4.dp),
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
            // A badge rather than a bare glyph — this card now leads the
            // screen, and the other lead-in eyebrows (theme toggle, Reader's
            // empty state) all give their icon a filled circle of its own.
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (!state.idle && state.running) StacksIcons.Pause else StacksIcons.Play,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(10.dp))
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
            // The other dashboard cards all pair their title with a line of
            // supporting text before anything else — bare title straight
            // into the chips was what made this one read as plain next to them.
            Spacer(Modifier.height(4.dp))
            Text(
                text = "One read, no distractions, until the timer runs out.",
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                    text = "Point the Readback tab at a synced readback-audio-db folder to see your latest reads here.",
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
 * A pick from the saved links rather than the readback library — every other
 * card on the dashboard is either bundled content or local state, and this is
 * where "something to read" comes from now that there is no curriculum to
 * browse instead. Only unread links are candidates, so this never repeats
 * [ReadbackOfDayCard]'s job of surfacing something already finished.
 */
@Composable
private fun SavedPickCard(links: LinkLibrary, onOpenSaved: () -> Unit, modifier: Modifier = Modifier) {
    val open = rememberUrlOpener()
    val unread = links.links.filterNot { it.read }
    var pick by remember(unread.map { it.id }) { mutableStateOf(unread.randomOrNull()) }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Card))
            .clickable {
                val found = pick
                if (found == null) {
                    onOpenSaved()
                } else {
                    open(found.url)
                    links.toggleRead(found.id)
                }
            }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "FROM SAVED",
                style = SectionLabel,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (unread.size > 1) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable {
                            pick = unread.filterNot { it.id == pick?.id }.random()
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

        val found = pick
        if (found == null) {
            Text(
                text = if (links.links.isEmpty()) "Nothing saved yet" else "All caught up",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (links.links.isEmpty()) {
                    "Share an article to Stacks, or paste its address in the Saved tab."
                } else {
                    "Every saved link has been read — tap to see them."
                },
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // No cover image comes with a saved link, unlike a bundled read —
            // an icon for what the article is actually about stands in,
            // matched by keyword against the title and description rather
            // than picked at random.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(Radius.Panel))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                SavedPickArt(
                    category = categorize("${found.title} ${found.description.orEmpty()}"),
                    modifier = Modifier.size(46.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = found.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            found.description?.let {
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
                text = found.host,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** What a saved article is actually about, matched by keyword rather than guessed at random. */
private enum class SavedCategory { ANDROID, AI, HACKING, MOBILE, GADGET, TECH }

private fun categorize(text: String): SavedCategory {
    val lower = text.lowercase()
    fun matches(vararg words: String) = words.any { it in lower }
    return when {
        matches("android", "jetpack", "google play", "kotlin multiplatform") -> SavedCategory.ANDROID
        matches("llm", "large language model", "gpt", "chatgpt", "claude", "machine learning", "neural network", "artificial intelligence") ->
            SavedCategory.AI
        matches("hack", "exploit", "vulnerab", "breach", "malware", "cve", "penetration test") -> SavedCategory.HACKING
        matches("iphone", "smartphone", "ios ", "mobile app", "app store") -> SavedCategory.MOBILE
        matches("gadget", "wearable", "smartwatch", "hardware review", "unboxing") -> SavedCategory.GADGET
        else -> SavedCategory.TECH
    }
}

/**
 * A stand-in for the cover image a saved link doesn't have: a small stroked
 * glyph for the category [categorize] matched, in the same hand-drawn style
 * as [StacksIcons] rather than a stock illustration.
 */
@Composable
private fun SavedPickArt(category: SavedCategory, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        when (category) {
            SavedCategory.ANDROID -> drawAndroidGlyph(color)
            SavedCategory.AI -> drawAiGlyph(color)
            SavedCategory.HACKING -> drawHackingGlyph(color)
            SavedCategory.MOBILE -> drawMobileGlyph(color)
            SavedCategory.GADGET -> drawGadgetGlyph(color)
            SavedCategory.TECH -> drawTechGlyph(color)
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
