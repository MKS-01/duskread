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
     * An id of its own because there are two tables, not because either half
     * is optional: `provision` resolves both or neither, so this being set
     * with [sourcesDatabaseId] null — or the reverse — is not a state the app
     * reaches. It *was* optional while both ids were pasted in by hand, when
     * pulling your followed blogs and configuring nothing else was a
     * reasonable thing to want; `runFullSync` carried a branch for it.
     */
    var readingDatabaseId: String? by mutableStateOf(store.getString(ReadingKey))
        private set

    /**
     * The page the reader shared with the token, inside which [homePageId]
     * was created.
     *
     * Held so a later repair — a home page deleted in Notion, say — can rebuild
     * without asking the same question twice.
     */
    var parentPageId: String? by mutableStateOf(store.getString(ParentKey))
        private set

    /**
     * The `DuskRead` page the two databases live in.
     *
     * Separate from [parentPageId] because they answer different questions:
     * one is where the reader let the app in, the other is what the app built
     * there. Conflating them would mean a second connection creating a second
     * home page inside the first.
     */
    var homePageId: String? by mutableStateOf(store.getString(HomeKey))
        private set

    /** When the last successful pull finished, for the "synced 2m ago" line. */
    var lastSyncAt: Long? by mutableStateOf(store.getString(LastSyncKey)?.toLongOrNull())
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
     * Whether an automatic sync is due.
     *
     * Deliberately says nothing about whether the databases are known. It used
     * to refuse until [sourcesDatabaseId] was set, which was right when that id
     * was pasted in by hand and wrong the moment `provision` started resolving
     * it: a reader who had just connected would have been refused every sync
     * forever, because the sync is the only thing that would have found the id.
     * Whether there is a credential at all is the caller's check — see
     * `NotionAuth.bearer`.
     *
     * [hasUnpushedWork] overrides the timer, and that is the point of it: the
     * clock is there to stop four openings in an evening costing four syncs,
     * not to make something just pasted in wait four hours to exist anywhere
     * else. Fresh feeds can wait; a link the reader deliberately saved cannot.
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
        // [LegacyNameKey] has no field behind it any more, and is cleared
        // anyway: an install that ran the old code still has the string
        // sitting in its store, and a disconnect should not leave it there.
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
         * The cached name of *the* database, from when there was one of them
         * and its id was typed in by hand. Nothing writes it now; it survives
         * only so [clear] can remove what an older install left behind.
         */
        const val LegacyNameKey = "notion.database.name"

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
