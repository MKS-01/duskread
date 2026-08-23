package dev.mks.blogmark.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.mks.blogmark.reader.PlaybackState
import dev.mks.blogmark.reader.ReadItem
import dev.mks.blogmark.ui.reader.formatDuration
import dev.mks.blogmark.ui.theme.BlogmarkIcons
import dev.mks.blogmark.ui.theme.Motion

enum class HomeTab(val label: String, val icon: ImageVector) {
    HOME("Home", BlogmarkIcons.Home),
    READBACK("Readback", BlogmarkIcons.Waveform),
    SAVED("Saved", BlogmarkIcons.Bookmark),
}

/** Every face of the bar is this tall; only the width changes between them. */
private val BarHeight = 56.dp

/** How far a downward run has to travel before the bar gives up its width. */
private val CollapseRun = 52.dp

/** And how far back up to earn it again — deliberately shorter, see [BarCollapse]. */
private val ExpandRun = 18.dp

/**
 * Which of the bar's three faces is on screen.
 *
 * They are mutually exclusive by design: the whole point of the swap is that
 * there is only ever one floating object above the nav bar, never a stack of
 * them competing for the same thumb.
 */
private enum class BarFace { TABS, PLAYER, PUCK }

/**
 * Tracks scroll direction so the bar can shrink out of the way while reading.
 *
 * Hysteretic rather than a plain sign check on each delta: a fling delivers
 * alternating small deltas as its curve flattens out, and reacting to every
 * one of them makes the bar flicker. So a *run* in one direction has to build
 * up before anything happens, and the two thresholds are asymmetric on
 * purpose — losing the controls should take a deliberate scroll, getting them
 * back should feel like it costs nothing.
 */
@Stable
class BarCollapse(private val collapseRun: Float, private val expandRun: Float) : NestedScrollConnection {
    var collapsed by mutableStateOf(false)
        private set

    private var run = 0f

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        // Direction change restarts the run rather than merely subtracting
        // from it, otherwise a long scroll down leaves a debt that swallows
        // the first flick back up.
        run = if ((run > 0f) != (dy > 0f)) dy else run + dy
        if (run < -collapseRun) collapsed = true
        if (run > expandRun) collapsed = false
        return Offset.Zero
    }

    /** For the tap on the collapsed puck, which has no scroll to ride back on. */
    fun expand() {
        collapsed = false
        run = 0f
    }
}

@Composable
fun rememberBarCollapse(): BarCollapse {
    val density = LocalDensity.current
    return remember(density) {
        with(density) { BarCollapse(CollapseRun.toPx(), ExpandRun.toPx()) }
    }
}

/**
 * The floating pill at the bottom of the home screen.
 *
 * It sits within thumb reach, which is the whole argument for moving
 * navigation and search down here from a top app bar.
 *
 * Icons carry it alone — with three destinations and a search button there is
 * nothing to disambiguate, and the labels were costing width on the one axis a
 * phone cannot spare. The active tab is marked by a filled disc instead.
 * Labels remain in [HomeTab] for the accessibility contentDescription.
 *
 * The transport lives *inside* this pill rather than on a second pill above
 * it. Two stacked glass slabs ate a third of the reachable zone and pushed
 * every list's bottom padding around as playback started and stopped; one pill
 * that swaps its contents costs no height at all. Playback wins the bar by
 * default because it is the transient thing — navigation is one tap away
 * behind the trailing tab icon, and reappears on its own as soon as you use it.
 *
 * The bar blurs whatever scrolls beneath it rather than sitting on an opaque
 * slab, so the list stays visible as it passes underneath.
 */
@Composable
fun FloatingBar(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    hazeState: HazeState,
    nowPlaying: ReadItem?,
    playback: PlaybackState,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onStop: () -> Unit,
    mono: Boolean,
    onToggleTheme: () -> Unit,
    collapse: BarCollapse,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    // Peeking at the tabs is a momentary thing, not a mode: any new read, and
    // any tab actually chosen, hands the bar back to the transport.
    var peekingTabs by remember { mutableStateOf(false) }
    LaunchedEffect(nowPlaying?.id) { peekingTabs = false }

    // Held past the end of playback: `nowPlaying` goes null the instant you
    // hit stop, and reading it directly would blank the title and duration
    // while the player face was still animating out.
    val shown = remember { mutableStateOf<ReadItem?>(null) }
    nowPlaying?.let { shown.value = it }

    val face = when {
        collapse.collapsed -> BarFace.PUCK
        nowPlaying != null && !peekingTabs -> BarFace.PLAYER
        else -> BarFace.TABS
    }

    Box(
        modifier = modifier
            .height(BarHeight)
            .clip(CircleShape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = scheme.background,
                    tints = listOf(HazeTint(scheme.surface.copy(alpha = 0.62f))),
                    blurRadius = 28.dp,
                    // A little grain stops large flat areas from banding.
                    noiseFactor = 0.04f,
                ),
            )
            // A brighter top edge is what actually sells glass: real glass
            // catches light where it curves away from you.
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    0f to scheme.onSurface.copy(alpha = 0.22f),
                    0.5f to scheme.onSurface.copy(alpha = 0.07f),
                    1f to scheme.onSurface.copy(alpha = 0.04f),
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Progress as a tint filling the pill from the left, not a track under
        // the row. Deliberately faint, and deliberately behind everything —
        // this is an ambient cue you read at a glance, not a control you aim
        // at. Costs no height, which is the only reason the transport fits in
        // a 56dp bar at all.
        if (face == BarFace.PLAYER) {
            val duration = playback.durationSec.takeIf { it > 0f } ?: 1f
            Box(Modifier.matchParentSize()) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((playback.positionSec / duration).coerceIn(0f, 1f))
                        .background(scheme.primary.copy(alpha = 0.16f)),
                )
            }
        }

        AnimatedContent(
            targetState = face,
            transitionSpec = { fadeIn(tween(Motion.Chip)) togetherWith fadeOut(tween(Motion.Fade)) },
            label = "bar-face",
        ) { current ->
            when (current) {
                BarFace.TABS -> TabsFace(
                    selected = selected,
                    onSelect = {
                        onSelect(it)
                        peekingTabs = false
                    },
                    mono = mono,
                    onToggleTheme = onToggleTheme,
                )

                BarFace.PLAYER -> PlayerFace(
                    item = shown.value,
                    playback = playback,
                    selected = selected,
                    onTogglePlay = onTogglePlay,
                    onSeek = onSeek,
                    onStop = onStop,
                    onShowTabs = { peekingTabs = true },
                )

                BarFace.PUCK -> PuckFace(
                    icon = when {
                        nowPlaying == null || peekingTabs -> selected.icon
                        playback.playing -> BlogmarkIcons.Pause
                        else -> BlogmarkIcons.Play
                    },
                    onClick = collapse::expand,
                )
            }
        }
    }
}

/**
 * The theme toggle rides at the trailing end, behind [BarDivider] — it isn't
 * a destination like the three tabs before it, so it doesn't get to look like
 * one. This is also the only place it lives now: reachable from every tab
 * rather than stranded at the top of Home alone, in keeping with this bar's
 * whole reason for existing.
 */
@Composable
private fun TabsFace(selected: HomeTab, onSelect: (HomeTab) -> Unit, mono: Boolean, onToggleTheme: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HomeTab.entries.forEach { tab ->
            BarButton(tab.icon, tab.label, active = tab == selected) { onSelect(tab) }
        }
        BarDivider()
        BarButton(
            icon = BlogmarkIcons.Contrast,
            label = if (mono) "Switch to the colour theme" else "Switch to the monochrome theme",
            onClick = onToggleTheme,
        )
    }
}

/**
 * The transport, sized to fill the bar's whole width — unlike [TabsFace],
 * which wraps its icons. A [Box] takes the larger of its children, so this is
 * what makes the pill itself widen and narrow as playback comes and goes.
 */
@Composable
private fun PlayerFace(
    item: ReadItem?,
    playback: PlaybackState,
    selected: HomeTab,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onStop: () -> Unit,
    onShowTabs: () -> Unit,
) {
    val duration = playback.durationSec.takeIf { it > 0f } ?: 1f

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarButton(
            icon = if (playback.playing) BlogmarkIcons.Pause else BlogmarkIcons.Play,
            label = if (playback.playing) "Pause" else "Play",
            tint = MaterialTheme.colorScheme.primary,
            onClick = onTogglePlay,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = item?.title.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                // Drag, not tap: a tap here would fire on every mis-aimed
                // press at the pause button next to it, and scrubbing to a
                // random position is a worse accident than doing nothing.
                .pointerInput(duration) {
                    detectHorizontalDragGestures { change, _ ->
                        onSeek((change.position.x / size.width).coerceIn(0f, 1f) * duration)
                    }
                },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatDuration((playback.durationSec - playback.positionSec).toDouble()),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        BarButton(BlogmarkIcons.Close, "Stop", diameter = 34.dp, iconSize = 13.dp, onClick = onStop)
        BarDivider()
        // The way back to navigation. It shows the tab you are already on, so
        // it reads as "return to where you were" rather than as a fourth
        // destination.
        BarButton(selected.icon, "Show tabs", onClick = onShowTabs)
    }
}

/** What is left of the bar while you scroll: the one control worth keeping. */
@Composable
private fun PuckFace(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(BarHeight).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = "Expand bar",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BarDivider() {
    Box(
        Modifier
            .padding(horizontal = 5.dp)
            .size(1.dp, 22.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
    )
}

/**
 * Every touch target in the bar: a circular tap area with an optional filled
 * disc behind it. One composable rather than a tab variant and a plain-icon
 * variant, because both faces need both behaviours and the 42dp target is the
 * thing that must not drift between them.
 */
@Composable
private fun BarButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    tint: Color? = null,
    diameter: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    onClick: () -> Unit,
) {
    val discAlpha by animateFloatAsState(
        if (active) 1f else 0f,
        tween(Motion.Chip),
        label = "disc",
    )
    val content by animateColorAsState(
        tint ?: if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(Motion.Chip),
        label = "barFg",
    )

    Box(
        Modifier
            .size(diameter)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(2.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = discAlpha * 0.9f),
                ),
        )
        Icon(icon, contentDescription = label, Modifier.size(iconSize), tint = content)
    }
}

/** Kept for the search field, which wants the same treatment on its own page. */
@Composable
fun glassStyle(): HazeStyle {
    val scheme = MaterialTheme.colorScheme
    return HazeStyle(
        backgroundColor = scheme.background,
        tints = listOf(HazeTint(scheme.surface.copy(alpha = 0.62f))),
        blurRadius = 28.dp,
        noiseFactor = 0.04f,
    )
}
