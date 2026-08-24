package dev.mks.duskread.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * No synced-folder mechanism exists on iOS yet — the sandbox has no
 * equivalent of Android's SAF folder grant or a plain desktop path, and this
 * app is read mostly on a phone that already has Android covered. Revisit if
 * that changes; for now the Readback tab just explains why.
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
        text = "The Reader isn't available on iOS yet — there's no way here to point at a " +
            "synced readback-audio-db folder. It works on Android today.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
