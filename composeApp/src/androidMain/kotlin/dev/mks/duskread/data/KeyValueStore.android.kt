package dev.mks.duskread.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * `SharedPreferences` rather than DataStore. DataStore is the modern answer for anything
 * sizeable, but it is asynchronous and would add a dependency to store two strings.
 */
private class AndroidStore(
    private val prefs: SharedPreferences,
    private val bulk: SharedPreferences,
) : KeyValueStore {
    override fun getString(key: String): String? {
        if (key !in BulkKeys) return prefs.getString(key, null)
        bulk.getString(key, null)?.let { return it }
        // Written to the shared file by an older build: moved across once, on first read.
        val legacy = prefs.getString(key, null) ?: return null
        bulk.edit().putString(key, legacy).apply()
        prefs.edit().remove(key).apply()
        return legacy
    }

    override fun putString(key: String, value: String?) {
        val target = if (key in BulkKeys) bulk else prefs
        target.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}

/**
 * Where an older build kept the multi-megabyte feed cache, apart from small prefs; read
 * once now, for the import into SQLite (`FeedPostCache.importLegacy`).
 */
private val BulkKeys = setOf("feeds.posts")

/**
 * The same store, reachable without composition.
 */
fun keyValueStore(context: Context): KeyValueStore = AndroidStore(
    context.getSharedPreferences("algo_atlas", Context.MODE_PRIVATE),
    context.getSharedPreferences("duskread_bulk", Context.MODE_PRIVATE),
)

@Composable
actual fun rememberKeyValueStore(): KeyValueStore {
    val context = LocalContext.current
    return remember(context) { keyValueStore(context) }
}
