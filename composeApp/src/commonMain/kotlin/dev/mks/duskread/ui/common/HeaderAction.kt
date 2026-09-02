package dev.mks.duskread.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.mks.duskread.ui.theme.SectionLabel

/**
 * A word in the trailing slot of an [EyebrowHeader] — Sync now, Manage, Add.
 * Quiet on purpose: it sits on the section's own rule, so it has to read as
 * part of the heading rather than as a button parked on top of it. A toggle
 * says what the next tap does ("Add" / "Done"), never what state it is in.
 */
@Composable
fun HeaderAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = SectionLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** The icon-only sibling — Search sits with the other actions but has no word for one. */
@Composable
fun HeaderAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Icon(
        imageVector = icon,
        contentDescription = label,
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(4.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
