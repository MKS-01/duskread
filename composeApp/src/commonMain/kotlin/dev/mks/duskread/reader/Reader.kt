package dev.mks.duskread.reader

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

/**
 * One past read from readback (github.com/MKS-01/readback) — a personal text-to-speech
 * reader that generates a WAV plus a SQLite row per article or book scan.
 */
data class ReadItem(
    val id: String,
    val title: String,
    val summary: String?,
    val excerpt: String,
    val sourceUrl: String,
    val mode: String,
    val voice: String,
    val durationSec: Double,
    val wordCount: Int,
    val audioFilename: String,
    val createdAt: String,
)

enum class ReadSort { NEWEST, OLDEST }

/** Whether the reader has been pointed at a synced `readback-audio-db` folder yet. */
enum class ReaderSource { NOT_CONFIGURED, READY }

/**
 * Read-only access to a readback library.
 */
interface ReadRepository {
    val source: StateFlow<ReaderSource>

    suspend fun listReads(query: String, sort: ReadSort): List<ReadItem>
}

@Composable
expect fun rememberReadRepository(): ReadRepository

/**
 * Whether this platform can reach a readback library at all — not whether one has been
 * configured, which only [ReadRepository.source] can answer.
 */
expect fun readbackSupported(): Boolean

/**
 * Platform-specific UI for pointing the repository at its data — a
 * Storage-Access-Framework folder picker on Android, a plain path field on desktop.
 */
@Composable
expect fun ReaderSourcePicker(repository: ReadRepository, compact: Boolean = false)
