package dev.mks.algoatlas.ui.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.mks.algoatlas.pomodoro.PomodoroState
import dev.mks.algoatlas.pomodoro.rememberPomodoroController
import dev.mks.algoatlas.ui.theme.AtlasIcons
import dev.mks.algoatlas.ui.theme.Radius
import dev.mks.algoatlas.ui.theme.Stroke

/**
 * A small always-reachable status pill — reachable from every screen rather
 * than folded into a single tab, since a running session should stay visible
 * no matter where in the app you wander off to. All the actual controls
 * (picking a duration, pausing, resetting) live in the big-timer focus mode
 * this opens, not here — the chip only ever needs to show at a glance whether
 * a session is running.
 */
@Composable
fun PomodoroChip(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val controller = rememberPomodoroController()
    val state by controller.state.collectAsState()

    Row(
        modifier
            .clip(RoundedCornerShape(Radius.Pill))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Pill))
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (!state.idle && state.running) AtlasIcons.Pause else AtlasIcons.Play,
            contentDescription = null,
            modifier = Modifier.padding(end = 6.dp).width(13.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = if (state.idle) "Focus" else state.clockLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

internal val PomodoroState.clockLabel: String
    get() {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }
