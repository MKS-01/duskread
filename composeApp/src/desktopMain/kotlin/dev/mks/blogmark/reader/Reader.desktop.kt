package dev.mks.blogmark.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.blogmark.data.KeyValueStore
import dev.mks.blogmark.data.rememberKeyValueStore
import dev.mks.blogmark.ui.common.AppTextField
import dev.mks.blogmark.ui.common.PrimaryButton
import dev.mks.blogmark.ui.theme.Mono
import dev.mks.blogmark.ui.theme.Radius
import dev.mks.blogmark.ui.theme.Stroke
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.DriverManager
import java.sql.SQLException

private const val FolderPathKey = "reader_folder_path"
private const val LibraryDbName = "library.db"
private const val AudioDirName = "audio"

/**
 * No Storage Access Framework on desktop — a plain path typed once and
 * remembered is the equivalent of the Android folder picker. JDBC against
 * the `library.db` file directly, no copying needed, since desktop already
 * has ordinary filesystem access to whatever the user points at.
 */
internal class DesktopReadRepository(private val store: KeyValueStore) : ReadRepository {
    private val _source = MutableStateFlow(ReaderSource.NOT_CONFIGURED)
    override val source: StateFlow<ReaderSource> = _source

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var folder: File? = null

    init {
        store.getString(FolderPathKey)?.let { folder = File(it) }
        if (folder?.let { File(it, LibraryDbName).exists() } == true) _source.value = ReaderSource.READY
    }

    /** Verifies `library.db` is directly inside before committing to a path — same reasoning as the Android picker. */
    fun setFolder(dir: File) {
        if (!File(dir, LibraryDbName).exists()) {
            _error.value = "That's not quite the right folder — point this at the main readback-audio-db " +
                "folder instead."
            return
        }

        folder = dir
        store.putString(FolderPathKey, dir.path)
        _error.value = null
        _source.value = ReaderSource.READY
    }

    fun currentPath(): String = folder?.path.orEmpty()

    override suspend fun listReads(query: String, sort: ReadSort): List<ReadItem> = withContext(Dispatchers.IO) {
        val dbFile = folder?.let { File(it, LibraryDbName) } ?: return@withContext emptyList()
        if (!dbFile.exists()) return@withContext emptyList()

        val orderBy = if (sort == ReadSort.NEWEST) "DESC" else "ASC"
        val trimmed = query.trim()
        val sql = buildString {
            append("SELECT * FROM reads")
            if (trimmed.isNotEmpty()) append(" WHERE title LIKE ? OR summary LIKE ? OR excerpt LIKE ? OR source_url LIKE ?")
            append(" ORDER BY created_at $orderBy")
        }

        // A stale connection, a mid-sync/half-written db, or an older or
        // newer readback build's schema missing a column this one expects
        // all throw SQLException here — a boundary this app doesn't control
        // the other side of, so it degrades to "no reads" rather than
        // crashing, same as the Android repository does.
        try {
            DriverManager.getConnection("jdbc:sqlite:${dbFile.path}").use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    if (trimmed.isNotEmpty()) {
                        val like = "%$trimmed%"
                        for (i in 1..4) stmt.setString(i, like)
                    }
                    stmt.executeQuery().use { rs ->
                        buildList {
                            while (rs.next()) {
                                add(
                                    ReadItem(
                                        id = rs.getString("id"),
                                        title = rs.getString("title"),
                                        summary = rs.getString("summary"),
                                        excerpt = rs.getString("excerpt"),
                                        sourceUrl = rs.getString("source_url"),
                                        mode = rs.getString("mode"),
                                        voice = rs.getString("voice"),
                                        durationSec = rs.getDouble("duration_sec"),
                                        wordCount = rs.getInt("word_count"),
                                        audioFilename = rs.getString("audio_filename"),
                                        createdAt = rs.getString("created_at"),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLException) {
            emptyList()
        }
    }

    fun audioFile(item: ReadItem): File? = folder?.let { File(File(it, AudioDirName), item.audioFilename) }
}

@Composable
actual fun rememberReadRepository(): ReadRepository {
    val store = rememberKeyValueStore()
    return remember { DesktopReadRepository(store) }
}

@Composable
actual fun ReaderSourcePicker(repository: ReadRepository, compact: Boolean) {
    val desktopRepository = repository as DesktopReadRepository
    val error by desktopRepository.error.collectAsState()
    var expanded by remember { mutableStateOf(!compact) }
    var path by remember { mutableStateOf(desktopRepository.currentPath()) }

    if (compact && !expanded) {
        // A bordered pill, the same `.pill` shape as the Newest/Oldest sort
        // chips beside it — plain text, no icon, so the three read as one
        // row of equal-weight controls rather than one of them standing out
        // as a button.
        Text(
            text = "FOLDER",
            fontFamily = Mono,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.Chip))
                .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Chip))
                .clickable { expanded = true }
                .padding(horizontal = 11.dp, vertical = 6.dp),
        )
        return
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        error?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        AppTextField(
            value = path,
            onValueChange = { path = it },
            placeholder = "/path/to/readback-audio-db",
        )
        PrimaryButton(
            label = "Use this folder",
            onClick = {
                desktopRepository.setFolder(File(path))
                if (compact && desktopRepository.source.value == ReaderSource.READY) expanded = false
            },
        )
    }
}
