package dev.mks.algoatlas.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class HomeTab(val label: String, val icon: ImageVector) {
    LEARN("Learn", Icons.Outlined.MenuBook),
    PRACTICE("Practice", Icons.Outlined.Bolt),
}

/**
 * The floating pill at the bottom of the home screen.
 *
 * It sits within thumb reach, which is the whole argument for moving
 * navigation and search down here from a top app bar.
 */
@Composable
fun FloatingBar(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(58.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HomeTab.entries.forEach { tab ->
                TabPill(tab, tab == selected) { onSelect(tab) }
            }

            Box(
                Modifier
                    .padding(start = 2.dp)
                    .size(1.dp, 24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )

            Box(
                Modifier
                    .padding(start = 2.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(21.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TabPill(tab: HomeTab, active: Boolean, onClick: () -> Unit) {
    // The label slides in only for the active tab, so the bar stays compact.
    val labelWidth by animateDpAsState(
        if (active) (tab.label.length * 8).dp + 6.dp else 0.dp,
        tween(260),
        label = "labelWidth",
    )
    val background by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        tween(220),
        label = "pillBg",
    )
    val content by animateColorAsState(
        if (active) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(220),
        label = "pillFg",
    )

    Row(
        Modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tab.icon, contentDescription = tab.label, Modifier.size(19.dp), tint = content)

        if (labelWidth > 0.dp) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.width(labelWidth - 6.dp)) {
                Text(
                    text = tab.label,
                    color = content,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}
