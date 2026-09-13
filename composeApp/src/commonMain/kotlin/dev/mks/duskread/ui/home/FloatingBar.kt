package dev.mks.duskread.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Layout
import dev.mks.duskread.ui.theme.Motion

enum class HomeTab(val label: String, val icon: ImageVector) {
    HOME("Home", DuskReadIcons.Home),
    FOLLOWING("Following", DuskReadIcons.Feed),
    SAVED("Saved", DuskReadIcons.Bookmark),
    READBACK("Readback", DuskReadIcons.Waveform),
}

/** The visible scrub line at the foot of the player face. */
private val SeekTrackHeight = 2.5.dp

/** Its actual touch target — a 2.5dp line is not draggable with a thumb. */
private val SeekTouchHeight = 20.dp

/** How far a downward run has to travel before the bar gets out of the way. */
private val CollapseRun = 52.dp

/** And how far back up to earn its place again — deliberately shorter, see [BarCollapse]. */
private val ExpandRun = 18.dp

/** How small the collapsed pill gets — a shrink you register, not a shrink you have to squint for. */
private const val CollapsedScale = 0.82f

/**
 * Which of the bar's two faces is on screen.
 */
private enum class BarFace { TABS, PLAYER }

/**
 * What the floating transport shows, merged from whichever of Readback or a live read is
 * actually playing — see `HomeScreen`.
 */
data class NowPlaying(
    val title: String,
    val playing: Boolean,
    /** 0f–1f. Always known: Readback derives it from position over duration, a live read reports it directly. */
    val fraction: Float,
    /** What the phone's floating bar prints — remaining time for Readback, a percentage for a live read. */
    val compactLabel: String,
    /** What the wide layout's transport prints — elapsed / total for Readback, a percentage for a live read. */
    val wideLabel: String,
    /** False for a live read: there is nowhere to drag to, only somewhere it has already been. */
    val seekable: Boolean,
)

/**
 * Tracks scroll direction so the bar can shrink out of the way while reading.
 */
@Stable
class BarCollapse(private val collapseRun: Float, private val expandRun: Float) : NestedScrollConnection {
    var collapsed by mutableStateOf(false)
        private set

    private var run = 0f

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        // A zero delta is not a direction.
        if (dy == 0f) return Offset.Zero

        // Direction change restarts the run rather than merely subtracting from it.
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
 * The floating pill at the bottom of the home screen. It sits within thumb reach, which
 * is the whole argument for moving navigation down here from a top app bar.
 */
@Composable
fun FloatingBar(
    selected: HomeTab,
    tabs: List<HomeTab>,
    onSelect: (HomeTab) -> Unit,
    hazeState: HazeState,
    nowPlaying: NowPlaying?,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onStop: () -> Unit,
    mono: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    collapse: BarCollapse,
    modifier: Modifier = Modifier,
    /**
     * False where the tabs are behind something — a full-screen surface covering the
     * screen the bar belongs to.
     */
    tabsAvailable: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme

    // Peeking at the tabs is a momentary thing, not a mode: any new read, and any tab
    // actually chosen, hands the bar back to the transport.
    var peekingTabs by remember { mutableStateOf(false) }
    LaunchedEffect(nowPlaying?.title) { peekingTabs = false }

    // Held past the end of playback: `nowPlaying` goes null the instant you hit stop.
    val shown = remember { mutableStateOf<NowPlaying?>(null) }
    nowPlaying?.let { shown.value = it }

    val face = when {
        nowPlaying != null && (!peekingTabs || !tabsAvailable) -> BarFace.PLAYER
        else -> BarFace.TABS
    }

    // Out of the way, not gone: it shrinks in place rather than sliding down toward the
    // edge.
    val scale by animateFloatAsState(
        targetValue = if (collapse.collapsed) CollapsedScale else 1f,
        animationSpec = tween(
            durationMillis = if (collapse.collapsed) Motion.Chip else Motion.Fade,
            easing = LinearOutSlowInEasing,
        ),
        label = "barShrink",
    )

    Box(
        modifier = modifier
            .height(Layout.BarHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // Bottom-centre, not the pill's own centre: shrinking toward the middle
                // reads as the bar deflating in place.
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
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
            // A brighter top edge is what actually sells glass: real glass catches light
            // where it curves away from you.
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
        // No meter behind the transport.

        AnimatedContent(
            targetState = face,
            // The size transform is stated rather than left to the default, because the
            // two faces are different widths on purpose — tabs wrap their icons.
            transitionSpec = {
                fadeIn(tween(Motion.Chip)) togetherWith fadeOut(tween(Motion.Fade)) using
                    SizeTransform(clip = false) { _, _ -> tween(Motion.Chip) }
            },
            label = "bar-face",
        ) { current ->
            when (current) {
                BarFace.TABS -> TabsFace(
                    selected = selected,
                    tabs = tabs,
                    onSelect = {
                        onSelect(it)
                        peekingTabs = false
                    },
                    mono = mono,
                    onToggleTheme = onToggleTheme,
                    onOpenSettings = onOpenSettings,
                )

                BarFace.PLAYER -> shown.value?.let { current ->
                    PlayerFace(
                        nowPlaying = current,
                        selected = selected,
                        onTogglePlay = onTogglePlay,
                        onSeek = onSeek,
                        onStop = onStop,
                        onShowTabs = { peekingTabs = true },
                        tabsAvailable = tabsAvailable,
                    )
                }
            }
        }

        // While peeked the whole bar is one target, not five.
        if (collapse.collapsed) {
            Box(Modifier.matchParentSize().clickable(onClick = collapse::expand))
        }

        // Position, shown rather than left to be discovered by dragging: the strip
        // mirrors the same seek gesture that lives on the title above it.
        if (face == BarFace.PLAYER) {
            val seekable = shown.value?.seekable == true

            // Where the finger is, while it is down.
            var scrub by remember { mutableStateOf<Float?>(null) }
            val fraction = scrub ?: (shown.value?.fraction ?: 0f)

            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(SeekTouchHeight)
                    .pointerInput(seekable) {
                        if (!seekable) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = { offset -> scrub = (offset.x / size.width).coerceIn(0f, 1f) },
                            // One seek, on release.
                            onDragEnd = {
                                scrub?.let { onSeek(it) }
                                scrub = null
                            },
                            onDragCancel = { scrub = null },
                        ) { change, _ ->
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
 * The theme toggle and Settings ride at the trailing end, behind [BarDivider] — neither
 * is a destination like the tabs before it, so neither gets to look like one.
 */
@Composable
private fun TabsFace(
    selected: HomeTab,
    tabs: List<HomeTab>,
    onSelect: (HomeTab) -> Unit,
    mono: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        Modifier.padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tabs.forEach { tab ->
            BarButton(tab.icon, tab.label, active = tab == selected) { onSelect(tab) }
        }
        BarDivider()
        BarButton(
            icon = DuskReadIcons.Contrast,
            label = if (mono) "Switch to the colour theme" else "Switch to the monochrome theme",
            onClick = onToggleTheme,
        )
        BarButton(icon = DuskReadIcons.Settings, label = "Settings", onClick = onOpenSettings)
    }
}

/**
 * The transport, sized to fill the bar's whole width — unlike [TabsFace], which wraps its
 * icons.
 */
@Composable
private fun PlayerFace(
    nowPlaying: NowPlaying,
    selected: HomeTab,
    onTogglePlay: () -> Unit,
    /** A fraction, 0f–1f — not seconds. The caller knows what that means for whichever source is actually playing. */
    onSeek: (Float) -> Unit,
    onStop: () -> Unit,
    onShowTabs: () -> Unit,
    /** False where the tabs are behind a full-screen surface — see [FloatingBar]'s own parameter. */
    tabsAvailable: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarButton(
            icon = if (nowPlaying.playing) DuskReadIcons.Pause else DuskReadIcons.Play,
            label = if (nowPlaying.playing) "Pause" else "Play",
            tint = MaterialTheme.colorScheme.primary,
            onClick = onTogglePlay,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = nowPlaying.title,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                // The visible line along the pill's foot is the discoverable scrub
                // target.
                .pointerInput(nowPlaying.seekable) {
                    if (nowPlaying.seekable) {
                        detectHorizontalDragGestures { change, _ ->
                            onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                        }
                    }
                },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = nowPlaying.compactLabel,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        BarButton(DuskReadIcons.Close, "Stop", diameter = 34.dp, iconSize = 13.dp, onClick = onStop)
        // The way back to navigation.
        if (tabsAvailable) {
            BarDivider()
            BarButton(selected.icon, "Show tabs", onClick = onShowTabs)
        }
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
 * Every touch target in the bar: a circular tap area with an optional filled disc behind
 * it.
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
