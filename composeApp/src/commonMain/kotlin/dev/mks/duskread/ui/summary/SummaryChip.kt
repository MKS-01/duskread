package dev.mks.duskread.ui.summary

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.mks.duskread.ui.common.Chip

/**
 * The summary feature's name for the shared pill.
 *
 * Kept as a name rather than folded into every call site: this feature stacks
 * the download action directly under the length chips, and the two reading as
 * different orders of control is what would make that section look assembled
 * rather than designed. See [Chip] for the shape itself.
 */
@Composable
internal fun SummaryChip(label: String, tone: Color, onClick: () -> Unit) = Chip(label = label, tone = tone, onClick = onClick)

/** The pill that does something, as opposed to the one that selects something. */
@Composable
internal fun SummaryActionChip(label: String, onClick: () -> Unit) = SummaryChip(label = label, tone = MaterialTheme.colorScheme.primary, onClick = onClick)
