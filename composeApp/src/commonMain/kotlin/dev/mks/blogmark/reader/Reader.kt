package dev.mks.blogmark.reader

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

/**
 * One past read from readback (github.com/MKS-01/readback) — a personal
 * text-to-speech reader that generates a WAV plus a SQLite row per article
 * or book scan. Field names mirror the `reads` table exactly; this app never
 * writes to that table, only reads it.
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
 * Read-only access to a readback library. This app is never the writer —
 * readback's own CLI generates reads, and a separate sync step (the user's
 * own script, run periodically) is what gets `library.db` and the `audio/`
 * folder onto this device. Resolving audio must go through
 * [audioFilename][ReadItem.audioFilename] joined against the configured
 * folder, never the `audio_path` column readback itself stores — that path
 * is absolute on the machine that generated the file, not this one.
 */
interface ReadRepository {
    val source: StateFlow<ReaderSource>

    suspend fun listReads(query: String, sort: ReadSort): List<ReadItem>
}

@Composable
expect fun rememberReadRepository(): ReadRepository

/**
 * Platform-specific UI for pointing the repository at its data — a
 * Storage-Access-Framework folder picker on Android, a plain path field on
 * desktop. Full prompt when [compact] is false (rendered wherever
 * [ReadRepository.source] is `NOT_CONFIGURED`); a small "change folder"
 * affordance when true, so a wrong pick doesn't require clearing app data.
 */
@Composable
expect fun ReaderSourcePicker(repository: ReadRepository, compact: Boolean = false)
