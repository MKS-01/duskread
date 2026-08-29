package dev.mks.duskread.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.mks.duskread.reader.PlaybackState
import dev.mks.duskread.reader.ReadItem
import dev.mks.duskread.ui.reader.formatDuration
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Layout
import dev.mks.duskread.ui.theme.Motion

enum class HomeTab(val label: String, val icon: ImageVector) {
    HOME("Home", DuskReadIcons.Home),
    READBACK("Readback", DuskReadIcons.Waveform),
    SAVED("Saved", DuskReadIcons.Bookmark),
}

/** The visible scrub line at the foot of the player face. */
private val SeekTrackHeight = 2.5.dp

/** Its actual touch target — a 2.5dp line is not draggable with a thumb. */
private val SeekTouchHeight = 20.dp

/** How far a downward run has to travel before the bar gets out of the way. */
private val CollapseRun = 52.dp

/** And how far back up to earn its place again — deliberately shorter, see [BarCollapse]. */
private val ExpandRun = 18.dp

/**
 * Which of the bar's two faces is on screen.
 *
 * They are mutually exclusive by design: the whole point of the swap is that
 * there is only ever one floating object above the nav bar, never a stack of
 * them competing for the same thumb.
 *
 * Getting out of the way is no longer a third face. It used to collapse to a
 * bare icon, which cost the reader every control and the answer to "where am
 * I" for the sake of some pixels; the bar now slides down instead and keeps
 * both.
 */
private enum class BarFace { TABS, PLAYER }

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
        // A zero delta is not a direction. Nested scroll delivers them —
        // a fling settling, a list already at its end, a gesture that turns
        // out to be horizontal — and treating one as a reversal reset the run
        // to nothing, so a genuine scroll could fail to move the bar at all.
        // That was the bar feeling like it ignored you.
        if (dy == 0f) return Offset.Zero

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
        nowPlaying != null && !peekingTabs -> BarFace.PLAYER
        else -> BarFace.TABS
    }

    // Out of the way, not gone. Sliding beats shrinking here: the bar's
    // buttons are 42dp against a 56dp bar, so any real reduction in height
    // takes the touch targets under the size a thumb can hit. Translation
    // costs them nothing — what is still on screen is still the same size it
    // always was.
    //
    // Deliberate to leave, cheap to return, the same asymmetry [BarCollapse]
    // already applies to the scroll runs that trigger it.
    val drop by animateDpAsState(
        targetValue = if (collapse.collapsed) Layout.BarPeekDrop else 0.dp,
        animationSpec = tween(
            durationMillis = if (collapse.collapsed) Motion.Chip else Motion.Fade,
            easing = LinearOutSlowInEasing,
        ),
        label = "barPeek",
    )

    Box(
        modifier = modifier
            // `offset` with a lambda, not `graphicsLayer`: this has to move
            // the bar's hit area with it, or the buttons stay tappable at a
            // position they are no longer drawn in.
            .offset { IntOffset(0, drop.roundToPx()) }
            .height(Layout.BarHeight)
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
        // No meter behind the transport. The mockup's floating bar carries
        // icons and nothing else, and every pitch tried here either moirés
        // into a hatch across the pill or collides with the play control —
        // the remaining-time readout already answers "how much is left".

        AnimatedContent(
            targetState = face,
            // The size transform is stated rather than left to the default,
            // because the two faces are different widths on purpose — tabs
            // wrap their icons, the transport fills the bar — so this swap is
            // a resize as much as a crossfade. Unspecified, the width moved on
            // a spring while the opacity moved on a tween, and the pill
            // arrived at its new size before the face that wanted it had
            // finished appearing.
            //
            // clip = false because the Box already clips to CircleShape; a
            // second clip animating its own bounds inside that one is what
            // made the contents look sheared mid-swap.
            transitionSpec = {
                fadeIn(tween(Motion.Chip)) togetherWith fadeOut(tween(Motion.Fade)) using
                    SizeTransform(clip = false) { _, _ -> tween(Motion.Chip) }
            },
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
            }
        }

        // While peeked the whole bar is one target, not five. A third of a
        // 42dp button is 14dp, and a tap that lands on the wrong one of three
        // tabs is worse than a tap that just brings the bar back.
        if (collapse.collapsed) {
            Box(Modifier.matchParentSize().clickable(onClick = collapse::expand))
        }

        // Position, shown rather than left to be discovered by dragging: the
        // strip mirrors the same seek gesture that lives on the title above
        // it, so scrubbing works from either and this line is never lying
        // about where a drag on the title would land.
        if (face == BarFace.PLAYER) {
            val duration = playback.durationSec.takeIf { it > 0f } ?: 1f

            // Where the finger is, while it is down. Without this the fill is
            // drawn straight from the player's reported position, so a drag
            // fought the playhead: every pixel asked the player to seek, the
            // player answered a few frames later with wherever it had actually
            // landed, and the line snapped back and forth between the two.
            var scrub by remember { mutableStateOf<Float?>(null) }
            val fraction = scrub ?: (playback.positionSec / duration).coerceIn(0f, 1f)

            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(SeekTouchHeight)
                    .pointerInput(duration) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset -> scrub = (offset.x / size.width).coerceIn(0f, 1f) },
                            // One seek, on release. Asking a MediaPlayer to
                            // seek on every drag event is what makes a scrub
                            // stutter — each one interrupts the decode it just
                            // started for the last.
                            onDragEnd = {
                                scrub?.let { onSeek(it * duration) }
                                scrub = null
                            },
                            onDragCancel = { scrub = null },
                        ) { change, _ ->
                            // Consumed, or the list underneath treats the same
                            // drag as its own scroll and the bar collapses
                            // while you are scrubbing on it.
                            change.consume()
                            scrub = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    },
                contentAlignment = Alignment.BottomStart,
            ) {
                Box(Modifier.fillMaxWidth().height(SeekTrackHeight).background(scheme.onSurface.copy(alpha = 0.14f)))
                Box(Modifier.fillMaxWidth(fraction).height(SeekTrackHeight).background(scheme.primary))
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
            icon = DuskReadIcons.Contrast,
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
            icon = if (playback.playing) DuskReadIcons.Pause else DuskReadIcons.Play,
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
                // The visible line along the pill's foot is the discoverable
                // scrub target; this is the same gesture repeated over the
                // title so a drag doesn't have to land in a 20dp-tall strip.
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
        BarButton(DuskReadIcons.Close, "Stop", diameter = 34.dp, iconSize = 13.dp, onClick = onStop)
        BarDivider()
        // The way back to navigation. It shows the tab you are already on, so
        // it reads as "return to where you were" rather than as a fourth
        // destination.
        BarButton(selected.icon, "Show tabs", onClick = onShowTabs)
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
