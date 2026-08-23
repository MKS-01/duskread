package dev.mks.blogmark.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.blogmark.links.hostOf
import dev.mks.blogmark.reader.AudioPlayer
import dev.mks.blogmark.reader.PlaybackState
import dev.mks.blogmark.reader.ReadItem
import dev.mks.blogmark.reader.ReadRepository
import dev.mks.blogmark.reader.ReadSort
import dev.mks.blogmark.reader.ReaderSource
import dev.mks.blogmark.reader.ReaderSourcePicker
import dev.mks.blogmark.ui.common.EmptyState
import dev.mks.blogmark.ui.common.MonogramBadge
import dev.mks.blogmark.ui.common.WaveformMeter
import dev.mks.blogmark.ui.theme.BlogmarkIcons
import dev.mks.blogmark.ui.theme.Mono
import dev.mks.blogmark.ui.theme.Radius
import kotlinx.coroutines.launch

/**
 * Past reads from readback (github.com/MKS-01/readback) — a personal
 * text-to-speech reader whose library this app only ever reads, never
 * writes. This is the signature screen of the Amplitude direction: two facts
 * per row instead of five, and the third fact — how long this actually is —
 * is drawn as a waveform rather than written out. Rows sit flush on the
 * background with a hairline underneath each one; nothing here is boxed.
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

    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    suspend fun reload() {
        items = if (source == ReaderSource.READY) repository.listReads(query, sort) else emptyList()
    }

    LaunchedEffect(source, query, sort) { reload() }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            // A manual re-list rather than waiting on `source`/`query`/`sort`
            // to change — the one thing that can go stale on its own is the
            // readback folder growing new reads on disk, which none of those
            // three notice by themselves.
            scope.launch {
                refreshing = true
                reload()
                refreshing = false
            }
        },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            // No now-playing bar in the list any more — the transport is a face of
            // the floating nav bar in HomeScreen, where it survives both scrolling
            // this list and leaving the tab entirely. See `FloatingBar`.

            if (source == ReaderSource.NOT_CONFIGURED) {
                item("picker") {
                    Box(Modifier.fillMaxWidth().fillParentMaxHeight(0.85f), contentAlignment = Alignment.BottomStart) {
                        EmptyState(
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
                        .padding(bottom = 20.dp),
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
                    Box(Modifier.fillMaxWidth().fillParentMaxHeight(0.75f), contentAlignment = Alignment.BottomStart) {
                        EmptyState(
                            title = "Nothing here yet",
                            message = "Generate a read with the readback CLI, sync it onto this device, " +
                                "and it will show up here.",
                        )
                    }
                }
            } else {
                loaded.forEachIndexed { index, read ->
                    item(read.id) {
                        ReadRow(
                            item = read,
                            playback = if (playback.item?.id == read.id) playback else null,
                            last = index == loaded.lastIndex,
                            onTap = {
                                if (playback.item?.id == read.id) player.togglePlayPause() else player.play(read)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** A small bordered pill — `.pill` in the mockup, not a filled chip. */
@Composable
private fun SortChip(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = label.uppercase(),
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.Inline))
            .background(if (active) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    )
}

/**
 * One read: a monogram, a title, two facts (duration, word count) and a
 * waveform — real data, not a decoration, since its filled fraction is the
 * clip's actual playback position. The playing row is the only coloured
 * thing here: title and duration switch to the accent, and its waveform
 * fills in from the left as the clip runs.
 */
@Composable
private fun ReadRow(
    item: ReadItem,
    playback: PlaybackState?,
    last: Boolean,
    onTap: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val playing = playback != null

    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onTap)) {
            Row(verticalAlignment = Alignment.Top) {
                MonogramBadge(
                    host = hostOf(item.sourceUrl),
                    size = 22.dp,
                    background = scheme.surfaceContainer,
                    contentColor = if (playing) scheme.primary else scheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (playing) scheme.primary else scheme.onSurface,
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (playback != null && playback.durationSec > 0f) {
                                "${formatDuration(playback.positionSec.toDouble())} / ${formatDuration(playback.durationSec.toDouble())}"
                            } else {
                                formatDuration(item.durationSec)
                            },
                            fontFamily = Mono,
                            fontSize = 10.5.sp,
                            color = if (playing) scheme.primary else scheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${item.wordCount}w",
                            fontFamily = Mono,
                            fontSize = 10.5.sp,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                if (playing) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = if (playback?.playing == true) BlogmarkIcons.Pause else BlogmarkIcons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp).padding(top = 2.dp),
                        tint = scheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            WaveformMeter(
                progress = if (playback != null && playback.durationSec > 0f) {
                    (playback.positionSec / playback.durationSec).coerceIn(0f, 1f)
                } else {
                    0f
                },
                modifier = Modifier.fillMaxWidth().height(15.dp),
                barCount = 26,
                dimColor = scheme.outlineVariant,
            )
        }
        Spacer(Modifier.height(15.dp))
        if (!last) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant))
            Spacer(Modifier.height(15.dp))
        }
    }
}

internal fun formatDuration(seconds: Double): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val minutes = total / 60
    val secs = total % 60
    return "$minutes:${secs.toString().padStart(2, '0')}"
}
