package dev.mks.duskread.notion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.LocalAppGraph
import dev.mks.duskread.data.Observed
import dev.mks.duskread.data.rememberKeyValueStore
import kotlinx.coroutines.flow.StateFlow

/**
 * Everything about the Notion connection except the credential.
 */
class NotionPrefs(private val store: KeyValueStore) {
    // Snapshot state and a StateFlow in one, so Compose and the iOS bridge read the same
    // value.
    private val observedSourcesDatabaseId = Observed(store.getString(SourcesKey))
    private val observedReadingDatabaseId = Observed(store.getString(ReadingKey))
    private val observedParentPageId = Observed(store.getString(ParentKey))
    private val observedHomePageId = Observed(store.getString(HomeKey))
    private val observedLastSyncAt = Observed(store.getString(LastSyncKey)?.toLongOrNull())

    /** The `Sources` database the sync reads. */
    var sourcesDatabaseId: String? by observedSourcesDatabaseId
        private set

    /** [sourcesDatabaseId] for observers outside a composition; see [Observed]. */
    val sourcesDatabaseIdUpdates: StateFlow<String?> get() = observedSourcesDatabaseId.updates

    /**
     * The `Reading List` database saved links sync against.
     */
    var readingDatabaseId: String? by observedReadingDatabaseId
        private set

    /** [readingDatabaseId] for observers outside a composition; see [Observed]. */
    val readingDatabaseIdUpdates: StateFlow<String?> get() = observedReadingDatabaseId.updates

    /**
     * The page the reader shared with the token, inside which [homePageId] was created.
     */
    var parentPageId: String? by observedParentPageId
        private set

    /** [parentPageId] for observers outside a composition; see [Observed]. */
    val parentPageIdUpdates: StateFlow<String?> get() = observedParentPageId.updates

    /**
     * The `DuskRead` page the two databases live in.
     */
    var homePageId: String? by observedHomePageId
        private set

    /** [homePageId] for observers outside a composition; see [Observed]. */
    val homePageIdUpdates: StateFlow<String?> get() = observedHomePageId.updates

    /** When the last successful pull finished, for the "synced 2m ago" line. */
    var lastSyncAt: Long? by observedLastSyncAt
        private set

    /** [lastSyncAt] for observers outside a composition; see [Observed]. */
    val lastSyncAtUpdates: StateFlow<Long?> get() = observedLastSyncAt.updates

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

    fun updateParentPageId(id: String?) {
        val trimmed = id?.trim()?.takeIf { it.isNotBlank() }
        parentPageId = trimmed
        store.putString(ParentKey, trimmed)
    }

    fun updateHomePageId(id: String?) {
        val trimmed = id?.trim()?.takeIf { it.isNotBlank() }
        homePageId = trimmed
        store.putString(HomeKey, trimmed)
    }

    fun recordSync(at: Long) {
        lastSyncAt = at
        store.putString(LastSyncKey, at.toString())
    }

    /**
     * Whether an automatic sync is due. Deliberately says nothing about whether the
     * databases are known.
     */
    fun dueForSync(now: Long, hasUnpushedWork: Boolean): Boolean {
        val last = lastSyncAt ?: return true
        return hasUnpushedWork || now - last >= AutoSyncAfterMs
    }

    /** The other half of a disconnect; the token half is `NotionAuth.disconnect`. */
    fun clear() {
        sourcesDatabaseId = null
        readingDatabaseId = null
        parentPageId = null
        homePageId = null
        lastSyncAt = null
        // [LegacyNameKey] has no field behind it any more, and is cleared anyway: an
        // install that ran the old code still has the string sitting in its store.
        listOf(SourcesKey, ReadingKey, ParentKey, HomeKey, LastSyncKey, LegacyNameKey)
            .forEach { store.putString(it, null) }
    }

    private companion object {
        const val SourcesKey = "notion.database.sources"
        const val ReadingKey = "notion.database.reading"
        const val ParentKey = "notion.page.parent"
        const val HomeKey = "notion.page.home"
        const val LastSyncKey = "notion.sync.last"

        /**
         * The cached name of *the* database, from when there was one of them and its id
         * was typed in by hand.
         */
        const val LegacyNameKey = "notion.database.name"

        /**
         * Four hours. Long enough that opening the app repeatedly in an evening costs one
         * sync, short enough that a morning's reading is current.
         */
        const val AutoSyncAfterMs = 4L * 60 * 60 * 1000
    }
}

@Composable
fun rememberNotionPrefs(): NotionPrefs = LocalAppGraph.current.notionPrefs
