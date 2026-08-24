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
import dev.mks.blogmark.ui.common.ListRowBody
import dev.mks.blogmark.ui.common.ListRowDivider
import dev.mks.blogmark.ui.common.RowMeta
import dev.mks.blogmark.ui.common.RowTone
import dev.mks.blogmark.ui.common.WaveformMeter
import dev.mks.blogmark.ui.rememberUrlOpener
import dev.mks.blogmark.ui.theme.BlogmarkIcons
import dev.mks.blogmark.ui.theme.Mono
import dev.mks.blogmark.ui.theme.Radius
import dev.mks.blogmark.ui.theme.Stroke

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
    val openLink = rememberUrlOpener()

    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(ReadSort.NEWEST) }
    var items by remember { mutableStateOf<List<ReadItem>?>(null) }

    // `source`, `query` and `sort` are the only things a read list depends
    // on, and all three are already observed here — nothing about this list
    // can go stale in a way only a manual pull can catch, so there is no
    // refresh gesture to wire up.
    LaunchedEffect(source, query, sort) {
        items = if (source == ReaderSource.READY) repository.listReads(query, sort) else emptyList()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
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

        // The folder picker used to live at the end of the sort-chip row,
        // where it read as a fourth destination on par with Newest/Oldest
        // rather than the "change where the library comes from" setting it
        // actually is. It gets the same top-right corner Home gives Settings
        // instead — same idea, same weight, its own row.
        item("head") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ReaderSourcePicker(repository, compact = true)
            }
        }

        item("controls") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SortChip("Newest", sort == ReadSort.NEWEST) { sort = ReadSort.NEWEST }
                SortChip("Oldest", sort == ReadSort.OLDEST) { sort = ReadSort.OLDEST }
            }
        }

        // A pointer at the transport, not a second copy of it — the controls
        // themselves stay in the floating bar below, where they're always
        // reachable regardless of scroll position.
        playback.item?.let { nowPlaying ->
            item("now-playing") {
                NowPlayingTip(title = nowPlaying.title, modifier = Modifier.padding(bottom = 16.dp))
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
                        onOpenSource = { openLink(read.sourceUrl) },
                    )
                }
            }
        }
    }
}

/** A small bordered pill — `.pill` in the mockup, not a filled chip. */
@Composable
private fun SortChip(label: String, active: Boolean, onClick: () -> Unit) {
    val tone = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = label.uppercase(),
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        color = tone,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.Chip))
            // Selection is carried by the border and the text alone. A filled
            // chip is the only remaining Material surface on this screen, and
            // next to a hairline sourcechip it reads as a different app.
            .border(Stroke.Hairline, tone, RoundedCornerShape(Radius.Chip))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

/**
 * An acknowledgement that something is playing, not a second transport — the
 * controls stay in the floating bar, which is reachable from any scroll
 * position; this only exists so the list itself says which read that bar
 * belongs to right now. Wraps rather than truncating: a title long enough to
 * threaten the message is exactly the title a reader most needs to actually
 * read here.
 */
@Composable
private fun NowPlayingTip(title: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = BlogmarkIcons.Waveform,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(13.dp).padding(top = 1.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = "Playing “$title” — use the bar below to control it",
            fontFamily = Mono,
            fontSize = 10.5.sp,
            lineHeight = 15.sp,
            color = scheme.onSurfaceVariant,
        )
    }
}

/**
 * One read: a monogram, a title, two facts (duration, word count) and a
 * waveform — real data, not a decoration, since its filled fraction is the
 * clip's actual playback position. The playing row is the only coloured
 * thing here: title and duration switch to the accent, and its waveform
 * fills in from the left as the clip runs.
 *
 * [onOpenSource] is its own tap target, separate from [onTap]: the row
 * itself is "play this read", the source line underneath is "go to where it
 * came from" — the same in-app reader flow saved links and feed posts already
 * open into, so a read is read-back-then-verify rather than a dead end.
 */
@Composable
private fun ReadRow(
    item: ReadItem,
    playback: PlaybackState?,
    last: Boolean,
    onTap: () -> Unit,
    onOpenSource: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val playing = playback != null

    Column(Modifier.fillMaxWidth()) {
        ListRowBody(
            host = hostOf(item.sourceUrl),
            title = item.title,
            onClick = onTap,
            // The playing row is the only coloured thing on screen — title,
            // duration and the sourcechip's border all follow it.
            tone = if (playing) RowTone.Accent else RowTone.Normal,
            trailing = {
                if (playing) {
                    Icon(
                        imageVector = if (playback?.playing == true) BlogmarkIcons.Pause else BlogmarkIcons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp).padding(top = 2.dp),
                        tint = scheme.primary,
                    )
                }
            },
            content = {
                Spacer(Modifier.height(9.dp))
                WaveformMeter(
                    progress = if (playback != null && playback.durationSec > 0f) {
                        (playback.positionSec / playback.durationSec).coerceIn(0f, 1f)
                    } else {
                        0f
                    },
                    modifier = Modifier.height(15.dp),
                    seed = item.id.hashCode(),
                )
            },
        ) {
            RowMeta(
                text = if (playback != null && playback.durationSec > 0f) {
                    "${formatDuration(playback.positionSec.toDouble())} / ${formatDuration(playback.durationSec.toDouble())}"
                } else {
                    formatDuration(item.durationSec)
                },
                accent = playing,
            )
            // The word count stays muted even on the playing row: the accent
            // has to mean "this one is playing", and a second coloured fact
            // that has nothing to do with playback dilutes it.
            RowMeta("${item.wordCount}w")
        }

        // Outside the row's own tap target, deliberately: the row is "play
        // this read", this is "go to where it came from".
        Spacer(Modifier.height(9.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.Chip))
                .clickable(onClick = onOpenSource)
                .padding(vertical = 4.dp),
        ) {
            Icon(
                imageVector = BlogmarkIcons.External,
                contentDescription = null,
                modifier = Modifier.size(11.dp),
                // The one glyph on an otherwise-muted row that's allowed a
                // permanent hint of the accent — it marks "this leaves the
                // app" regardless of playback state, so it stays legible even
                // when nothing on the row is playing.
                tint = scheme.primary.copy(alpha = 0.75f),
            )
            Spacer(Modifier.width(5.dp))
            RowMeta(hostOf(item.sourceUrl))
        }
        // 6, not the divider's usual 15: the source row above carries 4dp of
        // its own padding, and the gap that matters is the optical one.
        ListRowDivider(last, topSpacing = 6.dp)
    }
}

internal fun formatDuration(seconds: Double): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val minutes = total / 60
    val secs = total % 60
    return "$minutes:${secs.toString().padStart(2, '0')}"
}
