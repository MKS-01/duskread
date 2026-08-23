package dev.mks.blogmark.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Nothing here yet", for a whole screen with nothing else on it.
 *
 * No icon badge — that used to be two layered rings around a hand-drawn
 * glyph, sitting dead centre a third of the way down a mostly blank screen.
 * In its place, a flat waveform at zero height: the same meter every row
 * with content uses, saying "no signal" literally rather than decorating the
 * absence with a badge. Left-aligned and meant to be placed low on the
 * screen by the caller (see the `Box(..., contentAlignment = BottomStart)`
 * wrappers at each call site) — an empty state sitting dead centre is the
 * exact thing this replaces.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        WaveformMeter(
            progress = 0f,
            modifier = Modifier.fillMaxWidth().height(14.dp),
            barCount = 22,
            dimColor = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content?.let {
            Spacer(Modifier.height(20.dp))
            it()
        }
    }
}

/**
 * The inline equivalent of [EmptyState] for a section of Home rather than a
 * whole screen — just the two lines, no waveform and no badge. A section
 * this small has no "lower third" problem to solve; it only needs to say
 * what's missing without competing with the eyebrow header right above it.
 */
@Composable
fun CompactEmptyState(
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        message?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
