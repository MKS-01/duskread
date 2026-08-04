package dev.mks.stacks.reader

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import dev.mks.stacks.data.KeyValueStore
import dev.mks.stacks.data.rememberKeyValueStore
import dev.mks.stacks.ui.theme.Radius
import dev.mks.stacks.ui.theme.StacksIcons
import dev.mks.stacks.ui.theme.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

private const val FolderUriKey = "reader_folder_uri"
private const val LibraryDbName = "library.db"
private const val AudioDirName = "audio"

/**
 * Reads a synced readback library through Android's Storage Access
 * Framework — scoped storage means an arbitrary external path cannot just be
 * opened, so the user grants a persistent read permission to the folder once
 * via the system picker, and everything after that goes through
 * [DocumentFile] rather than a raw filesystem path.
 *
 * `library.db` cannot be queried directly from a SAF stream, so it is copied
 * into the app's cache on every load. That is deliberate, not a shortcut:
 * sync is manual and infrequent (the user's own script, run periodically),
 * so a fresh copy per open is simpler than file-watching a tree that rarely
 * changes, and the db is at most a few hundred KB.
 */
internal class AndroidReadRepository(private val context: Context, private val store: KeyValueStore) : ReadRepository {
    private val _source = MutableStateFlow(ReaderSource.NOT_CONFIGURED)
    override val source: StateFlow<ReaderSource> = _source

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var treeUri: Uri? = null

    init {
        store.getString(FolderUriKey)?.let { saved ->
            val uri = Uri.parse(saved)
            if (context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }) {
                treeUri = uri
                _source.value = ReaderSource.READY
            }
        }
    }

    /**
     * The picker returns whatever folder the user tapped "use this folder" on
     * — nothing stops that being a subfolder (e.g. `audio/` itself) rather
     * than the `readback-audio-db` root. Verifying `library.db` is actually
     * there before committing to it turns that mistake into a clear message
     * instead of a silent, permanently-empty "no reads found".
     */
    fun onFolderPicked(uri: Uri) {
        val tree = DocumentFile.fromTreeUri(context, uri)
        if (tree?.findFile(LibraryDbName) == null) {
            _error.value = "That's not quite the right folder — go up one level and choose the main " +
                "readback-audio-db folder instead."
            return
        }

        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        store.putString(FolderUriKey, uri.toString())
        treeUri = uri
        _error.value = null
        _source.value = ReaderSource.READY
    }

    override suspend fun listReads(query: String, sort: ReadSort): List<ReadItem> = withContext(Dispatchers.IO) {
        val uri = treeUri ?: return@withContext emptyList()
        val tree = DocumentFile.fromTreeUri(context, uri) ?: return@withContext emptyList()
        val dbDoc = tree.findFile(LibraryDbName) ?: return@withContext emptyList()

        val cacheFile = File(context.cacheDir, LibraryDbName)
        context.contentResolver.openInputStream(dbDoc.uri)?.use { input ->
            cacheFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return@withContext emptyList()

        val orderBy = if (sort == ReadSort.NEWEST) "created_at DESC" else "created_at ASC"
        val trimmed = query.trim()
        val selection = if (trimmed.isEmpty()) {
            null
        } else {
            "title LIKE ? OR summary LIKE ? OR excerpt LIKE ? OR source_url LIKE ?"
        }
        val args = if (trimmed.isEmpty()) null else Array(4) { "%$trimmed%" }

        // A folder can pass the onFolderPicked check (library.db exists) and
        // still not have synced a single read yet, in which case readback
        // hasn't created the `reads` table at all — that's "no reads", not
        // an error, so it degrades to an empty list rather than crashing.
        try {
            SQLiteDatabase.openDatabase(cacheFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.query("reads", null, selection, args, null, null, orderBy).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(
                                ReadItem(
                                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                                    summary = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("summary")),
                                    excerpt = cursor.getString(cursor.getColumnIndexOrThrow("excerpt")),
                                    sourceUrl = cursor.getString(cursor.getColumnIndexOrThrow("source_url")),
                                    mode = cursor.getString(cursor.getColumnIndexOrThrow("mode")),
                                    voice = cursor.getString(cursor.getColumnIndexOrThrow("voice")),
                                    durationSec = cursor.getDouble(cursor.getColumnIndexOrThrow("duration_sec")),
                                    wordCount = cursor.getInt(cursor.getColumnIndexOrThrow("word_count")),
                                    audioFilename = cursor.getString(cursor.getColumnIndexOrThrow("audio_filename")),
                                    createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                                ),
                            )
                        }
                    }
                }
            }
        } catch (_: SQLiteException) {
            emptyList()
        }
    }

    /** Resolved fresh each call rather than cached — cheap, and the tree can change between reads. */
    fun audioUri(item: ReadItem): Uri? {
        val uri = treeUri ?: return null
        val tree = DocumentFile.fromTreeUri(context, uri) ?: return null
        val audioDoc = tree.findFile(AudioDirName)?.findFile(item.audioFilename)
        return audioDoc?.uri
    }
}

private fun android.database.Cursor.getStringOrNull(index: Int): String? = if (isNull(index)) null else getString(index)

@Composable
actual fun rememberReadRepository(): ReadRepository {
    val context = LocalContext.current
    val store = rememberKeyValueStore()
    return remember(context) { AndroidReadRepository(context, store) }
}

@Composable
actual fun ReaderSourcePicker(repository: ReadRepository, compact: Boolean) {
    val androidRepository = repository as AndroidReadRepository
    val error by androidRepository.error.collectAsState()

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) androidRepository.onFolderPicked(uri)
    }

    if (compact) {
        Column {
            // A folder icon plus a pill background, rather than plain text —
            // this sits next to the Newest/Oldest sort chips and needs to
            // read as its own tappable action, not a stray hyperlink.
            Row(
                Modifier
                    .clip(RoundedCornerShape(Radius.Pill))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Pill))
                    .clickable { pickFolder.launch(null) }
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = StacksIcons.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Change folder",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 13.dp),
                )
            }
        }
        return
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        error?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = { pickFolder.launch(null) },
            modifier = Modifier.clip(RoundedCornerShape(Radius.Pill)),
        ) {
            Text("Choose folder")
        }
    }
}
