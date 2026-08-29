package dev.mks.duskread.notion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.rememberKeyValueStore

/**
 * Everything about the Notion connection except the credential.
 *
 * Deliberately a sibling of `UserPrefs` rather than four more properties on
 * it: this is the only state in the app that describes a connection to
 * somewhere else, and it is the state a disconnect has to clear wholesale.
 * Keeping it separate means [clear] is one obvious call rather than four
 * assignments someone will one day only do three of.
 *
 * The token is not here. It lives in `SecretStore`, and the split is the
 * point — see `SecretStore.kt`.
 */
class NotionPrefs(private val store: KeyValueStore) {
    /** The `Sources` database the sync reads. */
    var sourcesDatabaseId: String? by mutableStateOf(store.getString(SourcesKey))
        private set

    /**
     * The `Reading List` database saved links sync against.
     *
     * Optional and separate from [sourcesDatabaseId]: the two halves are
     * independent, and a reader who only wants their followed blogs pulled
     * down should not have to configure the half that writes.
     */
    var readingDatabaseId: String? by mutableStateOf(store.getString(ReadingKey))
        private set

    /** When the last successful pull finished, for the "synced 2m ago" line. */
    var lastSyncAt: Long? by mutableStateOf(store.getString(LastSyncKey)?.toLongOrNull())
        private set

    /**
     * The name Notion gave the database, cached from the last successful call.
     *
     * Held so Settings can say "Sources · 18 feeds" on a cold start without
     * making a network request to re-learn a string that does not change.
     */
    var databaseName: String? by mutableStateOf(store.getString(NameKey))
        private set

    fun updateDatabaseId(id: String?) {
        val trimmed = id?.trim()?.takeIf { it.isNotBlank() }
        sourcesDatabaseId = trimmed
        store.putString(SourcesKey, trimmed)
    }

    fun updateReadingDatabaseId(id: String?) {
        val trimmed = id?.trim()?.takeIf { it.isNotBlank() }
        readingDatabaseId = trimmed
        store.putString(ReadingKey, trimmed)
    }

    fun recordConnection(name: String) {
        databaseName = name
        store.putString(NameKey, name)
    }

    fun recordSync(at: Long) {
        lastSyncAt = at
        store.putString(LastSyncKey, at.toString())
    }

    /**
     * Whether an automatic sync is due.
     *
     * False when nothing is configured, so a reader who has never connected
     * never pays for a network call they did not ask for.
     *
     * [hasUnpushedWork] overrides the timer, and that is the point of it: the
     * clock is there to stop four openings in an evening costing four syncs,
     * not to make something just pasted in wait four hours to exist anywhere
     * else. Fresh feeds can wait; a link the reader deliberately saved cannot.
     */
    fun dueForSync(now: Long, hasUnpushedWork: Boolean): Boolean {
        if (sourcesDatabaseId.isNullOrBlank()) return false
        val last = lastSyncAt ?: return true
        return hasUnpushedWork || now - last >= AutoSyncAfterMs
    }

    /** The other half of a disconnect; the token half is `NotionAuth.disconnect`. */
    fun clear() {
        sourcesDatabaseId = null
        readingDatabaseId = null
        lastSyncAt = null
        databaseName = null
        listOf(SourcesKey, ReadingKey, LastSyncKey, NameKey).forEach { store.putString(it, null) }
    }

    private companion object {
        const val SourcesKey = "notion.database.sources"
        const val ReadingKey = "notion.database.reading"
        const val LastSyncKey = "notion.sync.last"
        const val NameKey = "notion.database.name"

        /**
         * Four hours. Long enough that opening the app repeatedly in an
         * evening costs one sync, short enough that a morning's reading is
         * current. A feed publishes a few times a week; there is nothing to
         * gain from checking more often than a meal.
         */
        const val AutoSyncAfterMs = 4L * 60 * 60 * 1000
    }
}

@Composable
fun rememberNotionPrefs(): NotionPrefs {
    val store = rememberKeyValueStore()
    return remember(store) { NotionPrefs(store) }
}
