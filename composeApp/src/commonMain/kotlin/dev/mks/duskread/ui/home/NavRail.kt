package dev.mks.duskread.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.reader.PlaybackState
import dev.mks.duskread.reader.ReadItem
import dev.mks.duskread.ui.reader.formatDuration
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Layout
import dev.mks.duskread.ui.theme.Motion
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.Stroke

/**
 * The wide-window replacement for [FloatingBar]'s tab face.
 *
 * The floating bar sits at the bottom of a phone because that is where the
 * thumb is. On a desktop there is no thumb — the pointer is already wherever
 * the eye is — and the bottom edge of an 800dp-tall window is simply the
 * longest journey from the list you are reading. So navigation goes to the
 * left edge, where a pointer costs nothing to reach and where the eye returns
 * between rows anyway.
 *
 * It does not float, blur or animate: glass earns its keep over content
 * scrolling beneath it, and nothing scrolls under a rail that owns its own
 * column. A hairline on the inner edge is the whole of its chrome.
 */
@Composable
fun NavRail(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    mono: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Row(modifier.fillMaxHeight()) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(Layout.RailWidth)
                .background(scheme.background)
                .statusBarsPadding()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HomeTab.entries.forEach { tab ->
                RailButton(tab.icon, tab.label, active = tab == selected) { onSelect(tab) }
            }

            // Pushes the two non-destinations to the far end. A divider would
            // be redundant once a whole column of empty rail separates them.
            Spacer(Modifier.weight(1f))

            RailButton(
                icon = DuskReadIcons.Contrast,
                label = if (mono) "Switch to the colour theme" else "Switch to the monochrome theme",
                onClick = onToggleTheme,
            )
            RailButton(
                icon = DuskReadIcons.Settings,
                label = "Settings",
                onClick = onOpenSettings,
            )
        }

        // The rail's whole chrome. Drawn here rather than as a border on the
        // Column so it is the full height of the window including under the
        // status bar, which the Column's own inset padding would otherwise
        // cut short.
        Box(Modifier.fillMaxHeight().width(Stroke.Hairline).background(scheme.outlineVariant))
    }
}

/**
 * A rail stop: a squared-off tap target, not the bar's circular disc.
 *
 * The disc reads as a thumb target, which is exactly what it is on a phone.
 * Against a straight rail edge the same shape looks like a stray bubble, so
 * this borrows [Radius.Chip] from the sort chips instead — the app's existing
 * answer for "bordered, squared-off, not a Material pill".
 *
 * The selected stop is marked twice over: the [Radius.Card]-less surface lift
 * a card already uses, plus a short accent tick on the outer edge. Two marks
 * rather than one because the rail is the only answer to "where am I?" once
 * the tab labels are gone, and in Ink the lift alone is a very quiet signal.
 */
@Composable
private fun RailButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    val lift by animateFloatAsState(if (active) 1f else 0f, tween(Motion.Chip), label = "railLift")
    val content by animateColorAsState(
        if (active) scheme.onSurface else scheme.onSurfaceVariant,
        tween(Motion.Chip),
        label = "railFg",
    )

    Box(Modifier.size(width = Layout.RailWidth, height = 44.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(Radius.Chip))
                .background(scheme.surfaceContainer.copy(alpha = lift))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, Modifier.size(20.dp), tint = content)
        }

        // Outer edge, not inner: it reads as the window's own margin marking
        // the stop, rather than a divider between the rail and the content.
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
                .size(width = 2.dp, height = 16.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(scheme.primary.copy(alpha = lift)),
        )
    }
}

/**
 * The transport, full-bleed along the bottom of a wide window.
 *
 * The one element that keeps its phone position, because it is the one that
 * outlives whatever is above it: a read started from the Readback list goes
 * on playing while you browse Saved, and a control that moved with the pane
 * would have to be somewhere else by the time you wanted it.
 *
 * Full-bleed rather than a floating pill. The pill floats on a phone so a
 * scrolling list stays visible through it in a screen with no room to spare;
 * a wide window has the room, and a 1180dp pill with a play button at one end
 * is a banner. Anchored to the window edge it is furniture instead — always
 * the same place, never in the way.
 */
@Composable
fun TransportBar(
    item: ReadItem?,
    playback: PlaybackState,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    // Held past the end of playback for the same reason the floating bar
    // holds it: `item` goes null the instant stop lands, and reading it
    // directly would blank the title mid-exit-animation.
    val shown = remember { mutableStateOf<ReadItem?>(null) }
    item?.let { shown.value = it }

    val duration = playback.durationSec.takeIf { it > 0f } ?: 1f
    val fraction = (playback.positionSec / duration).coerceIn(0f, 1f)

    Column(modifier.fillMaxWidth().background(scheme.background)) {
        // The seek line doubles as the bar's top hairline — one 2dp rule
        // instead of a border and a track stacked on each other.
        Box(
            Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(duration) {
                    detectHorizontalDragGestures { change, _ ->
                        onSeek((change.position.x / size.width).coerceIn(0f, 1f) * duration)
                    }
                },
            contentAlignment = Alignment.TopStart,
        ) {
            Box(Modifier.fillMaxWidth().height(Stroke.Hairline).background(scheme.outlineVariant))
            Box(Modifier.fillMaxWidth(fraction).height(2.dp).background(scheme.primary))
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RailButton(
                icon = if (playback.playing) DuskReadIcons.Pause else DuskReadIcons.Play,
                label = if (playback.playing) "Pause" else "Play",
                onClick = onTogglePlay,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = shown.value?.title.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                fontSize = 13.sp,
                color = scheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "${formatDuration(playback.positionSec.toDouble())} / " +
                    formatDuration(playback.durationSec.toDouble()),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 84.dp),
            )
            Spacer(Modifier.width(6.dp))
            RailButton(icon = DuskReadIcons.Close, label = "Stop", onClick = onStop)
        }
    }
}
