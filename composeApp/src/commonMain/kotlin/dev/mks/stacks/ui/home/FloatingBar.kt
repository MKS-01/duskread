package dev.mks.stacks.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.mks.stacks.ui.theme.Motion
import dev.mks.stacks.ui.theme.StacksIcons

enum class HomeTab(val label: String, val icon: ImageVector) {
    HOME("Home", StacksIcons.Home),
    LIBRARY("Library", StacksIcons.Steps),
    READER("Reader", StacksIcons.Waveform),
}

/**
 * The floating pill at the bottom of the home screen.
 *
 * It sits within thumb reach, which is the whole argument for moving
 * navigation and search down here from a top app bar.
 *
 * Icons carry it alone — with two destinations and a search button there is
 * nothing to disambiguate, and the labels were costing width on the one axis a
 * phone cannot spare. The active tab is marked by a filled disc instead.
 *
 * The bar blurs whatever scrolls beneath it rather than sitting on an opaque
 * slab, so the list stays visible as it passes underneath. Labels remain in
 * [HomeTab] for the accessibility contentDescription.
 */
@Composable
fun FloatingBar(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    onSearch: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .height(56.dp)
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
            )
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HomeTab.entries.forEach { tab ->
            TabButton(tab, tab == selected) { onSelect(tab) }
        }

        Box(
            Modifier
                .padding(horizontal = 5.dp)
                .size(1.dp, 22.dp)
                .background(scheme.onSurface.copy(alpha = 0.12f)),
        )

        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(onClick = onSearch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                StacksIcons.Search,
                contentDescription = "Search",
                modifier = Modifier.size(20.dp),
                tint = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TabButton(tab: HomeTab, active: Boolean, onClick: () -> Unit) {
    val discAlpha by animateFloatAsState(
        if (active) 1f else 0f,
        tween(Motion.Chip),
        label = "disc",
    )
    val content by animateColorAsState(
        if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(Motion.Chip),
        label = "tabFg",
    )

    Box(
        Modifier
            .size(42.dp)
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
        Icon(tab.icon, contentDescription = tab.label, Modifier.size(20.dp), tint = content)
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
