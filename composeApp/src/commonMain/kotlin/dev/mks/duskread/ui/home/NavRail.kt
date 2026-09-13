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
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Layout
import dev.mks.duskread.ui.theme.Motion
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.Stroke

/**
 * The wide-window replacement for [FloatingBar]'s tab face. The floating bar sits at the
 * bottom of a phone because that is where the thumb is.
 */
@Composable
fun NavRail(
    selected: HomeTab,
    tabs: List<HomeTab>,
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
            // [tabs], not `HomeTab.entries`: Readback is hidden until it is switched on.
            // See `UserPrefs.readbackEnabled`.
            tabs.forEach { tab ->
                RailButton(tab.icon, tab.label, active = tab == selected) { onSelect(tab) }
            }

            // Pushes the two non-destinations to the far end. A divider would be
            // redundant once a whole column of empty rail separates them.
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

        // The rail's whole chrome.
        Box(Modifier.fillMaxHeight().width(Stroke.Hairline).background(scheme.outlineVariant))
    }
}

/**
 * A rail stop: a squared-off tap target, not the bar's circular disc. The disc reads as a
 * thumb target, which is exactly what it is on a phone.
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

        // Outer edge, not inner: it reads as the window's own margin marking the stop,
        // rather than a divider between the rail and the content.
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
 */
@Composable
fun TransportBar(
    nowPlaying: NowPlaying?,
    onTogglePlay: () -> Unit,
    /** A fraction, 0f–1f — not seconds. */
    onSeek: (Float) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    // Held past the end of playback for the same reason the floating bar holds it:
    // `nowPlaying` goes null the instant stop lands.
    val shown = remember { mutableStateOf<NowPlaying?>(null) }
    nowPlaying?.let { shown.value = it }
    val current = shown.value

    val fraction = current?.fraction ?: 0f
    val seekable = current?.seekable ?: false

    Column(modifier.fillMaxWidth().background(scheme.background)) {
        // The seek line doubles as the bar's top hairline — one 2dp rule instead of a
        // border and a track stacked on each other.
        Box(
            Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(seekable) {
                    if (seekable) {
                        detectHorizontalDragGestures { change, _ ->
                            onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                        }
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
                icon = if (current?.playing == true) DuskReadIcons.Pause else DuskReadIcons.Play,
                label = if (current?.playing == true) "Pause" else "Play",
                onClick = onTogglePlay,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = current?.title.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                fontSize = 13.sp,
                color = scheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = current?.wideLabel.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 84.dp),
            )
            Spacer(Modifier.width(6.dp))
            RailButton(icon = DuskReadIcons.Close, label = "Stop", onClick = onStop)
        }
    }
}
