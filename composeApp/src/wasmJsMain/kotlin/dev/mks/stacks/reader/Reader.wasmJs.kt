package dev.mks.stacks.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A browser sandbox has no filesystem access to a synced folder at all —
 * not a missing feature to add later so much as a real platform ceiling.
 * The Reader is Android (and desktop, for testing) only.
 */
private class UnavailableReadRepository : ReadRepository {
    override val source: StateFlow<ReaderSource> = MutableStateFlow(ReaderSource.NOT_CONFIGURED)

    override suspend fun listReads(query: String, sort: ReadSort): List<ReadItem> = emptyList()
}

@Composable
actual fun rememberReadRepository(): ReadRepository = remember { UnavailableReadRepository() }

@Composable
actual fun ReaderSourcePicker(repository: ReadRepository, compact: Boolean) {
    Text(
        text = "The Reader isn't available on the web — a browser has no access to a " +
            "synced folder on your device. It works on Android today.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
