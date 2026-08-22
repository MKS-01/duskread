package dev.mks.blogmark.ui.reader

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.blogmark.reader.AudioPlayer
import dev.mks.blogmark.reader.PlaybackState
import dev.mks.blogmark.reader.ReadItem
import dev.mks.blogmark.reader.ReadRepository
import dev.mks.blogmark.reader.ReadSort
import dev.mks.blogmark.reader.ReaderSource
import dev.mks.blogmark.reader.ReaderSourcePicker
import dev.mks.blogmark.ui.rememberUrlOpener
import dev.mks.blogmark.ui.theme.BlogmarkIcons
import dev.mks.blogmark.ui.theme.Radius
import dev.mks.blogmark.ui.theme.Stroke

/**
 * Past reads from readback (github.com/MKS-01/readback) — a personal
 * text-to-speech reader whose library this app only ever reads, never
 * writes. Mirrors the shape of readback's own dashboard (search, sort,
 * card-per-read, tap to expand a player) translated into this app's own
 * Material design system rather than readback's CSS tokens — two design
 * systems in one screen would be the mistake, not the fix.
 */
@Composable
fun ReaderTab(
    repository: ReadRepository,
    player: AudioPlayer,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val source by repository.source.collectAsState()
    val playback by player.state.collectAsState()

    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(ReadSort.NEWEST) }
    var items by remember { mutableStateOf<List<ReadItem>?>(null) }

    LaunchedEffect(source, query, sort) {
        items = if (source == ReaderSource.READY) repository.listReads(query, sort) else emptyList()
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item("head") {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "Readback",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Past reads, synced onto this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // No now-playing bar in the list any more — the transport is a face of
        // the floating nav bar in HomeScreen, where it survives both scrolling
        // this list and leaving the tab entirely. See `FloatingBar`.

        if (source == ReaderSource.NOT_CONFIGURED) {
            item("picker") {
                // Fills the rest of the viewport below the header so the empty
                // state centres in the space actually available, rather than
                // sitting pinned under the header the way a plain list item would.
                Box(Modifier.fillMaxWidth().fillParentMaxHeight(0.85f), contentAlignment = Alignment.Center) {
                    ReaderEmptyState(
                        icon = BlogmarkIcons.FolderConnect,
                        title = "Connect your library",
                        message = "Choose the readback-audio-db folder synced onto this device — the main " +
                            "folder itself, not one of the folders inside it.",
                    ) {
                        ReaderSourcePicker(repository)
                    }
                }
            }
            return@LazyColumn
        }

        item("controls") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                // The icon chip from ReaderSourcePicker is taller than the
                // plain-text sort chips beside it — without this they default
                // to top-aligned instead of sharing a centre line.
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SortChip("Newest", sort == ReadSort.NEWEST) { sort = ReadSort.NEWEST }
                SortChip("Oldest", sort == ReadSort.OLDEST) { sort = ReadSort.OLDEST }
                ReaderSourcePicker(repository, compact = true)
            }
        }

        val loaded = items
        if (loaded == null) {
            item("loading") {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (loaded.isEmpty()) {
            item("empty") {
                ReaderEmptyState(
                    icon = BlogmarkIcons.Waveform,
                    title = "Nothing here yet",
                    message = "Generate a read with the readback CLI, sync it onto this device, " +
                        "and it will show up here.",
                )
            }
        } else {
            loaded.forEach { read ->
                item(read.id) {
                    ReadCard(
                        item = read,
                        playback = if (playback.item?.id == read.id) playback else null,
                        onTap = {
                            if (playback.item?.id == read.id) player.togglePlayPause() else player.play(read)
                        },
                    )
                }
            }
        }
    }
}

/**
 * A centered illustration for a state with nothing else on screen yet —
 * built from the same hand-drawn [BlogmarkIcons] vocabulary as the rest of the
 * app rather than a third-party illustration pack, which would clash with
 * it the same way a filled Material icon does.
 */
@Composable
private fun ReaderEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    content: (@Composable () -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ReaderEmptyIcon(icon)
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        content?.let {
            Spacer(Modifier.height(20.dp))
            it()
        }
    }
}

/** Two layered rings around the icon — plainer, and it reads as decoration rather than a real badge. */
@Composable
private fun ReaderEmptyIcon(icon: ImageVector) {
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)),
        )
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f))
                .border(Stroke.Hairline, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SortChip(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 12.5.sp,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    )
}

@Composable
private fun ReadCard(
    item: ReadItem,
    playback: PlaybackState?,
    onTap: () -> Unit,
) {
    val open = rememberUrlOpener()

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Card))
            .background(MaterialTheme.colorScheme.surface)
            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Card))
            .clickable(onClick = onTap)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (playback?.playing == true) BlogmarkIcons.Pause else BlogmarkIcons.Play,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 14.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "${item.createdAt.take(10)} · ${formatDuration(item.durationSec)} · ${item.mode} · ${item.voice} · ${item.wordCount} words",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        val text = item.summary ?: item.excerpt
        // The card for the active item expands to the full text — safe now
        // that the slider lives in the pinned bar above instead of inline
        // here. It was *that* Row appearing and disappearing that jumped
        // the list around before, not the text growing on its own.
        if (playback != null && playback.durationSec > 0f) {
            // No per-word timestamps exist (readback's own dashboard fakes
            // this the same way) — the "read" fraction is elapsed ÷ total,
            // walked across the text weighted by character length rather
            // than word count, so long words take proportionally longer.
            Text(
                text = highlightedText(
                    text = text,
                    fraction = (playback.positionSec / playback.durationSec).coerceIn(0f, 1f),
                    readColor = MaterialTheme.colorScheme.onSurface,
                    unreadColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(9.dp))
        Row(
            Modifier.clickable { open(item.sourceUrl) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Read original",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun highlightedText(
    text: String,
    fraction: Float,
    readColor: androidx.compose.ui.graphics.Color,
    unreadColor: androidx.compose.ui.graphics.Color,
) = buildAnnotatedString {
    val words = text.split(" ")
    val totalChars = words.sumOf { it.length + 1 }.coerceAtLeast(1)
    val readChars = (totalChars * fraction).toInt()

    var consumed = 0
    words.forEachIndexed { index, word ->
        withStyle(SpanStyle(color = if (consumed < readChars) readColor else unreadColor)) {
            append(word)
        }
        if (index != words.lastIndex) append(" ")
        consumed += word.length + 1
    }
}

internal fun formatDuration(seconds: Double): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val minutes = total / 60
    val secs = total % 60
    return "$minutes:${secs.toString().padStart(2, '0')}"
}
