package dev.mks.duskread.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.LocalAppGraph
import dev.mks.duskread.data.Observed
import dev.mks.duskread.data.rememberKeyValueStore
import dev.mks.duskread.db.DuskReadDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import dev.mks.duskread.db.FeedPost as FeedPostRow

/**
 * One post as it appeared in a feed the last time that feed synced.
 */
data class FeedPost(
    val feedId: String,
    val url: String,
    val title: String,
    val imageUrl: String? = null,
    val content: String? = null,
    /** When the publisher dated it, or null for a feed that dates nothing. */
    val publishedAt: Long? = null,
    /**
     * Whether this post can be read with no network.
     */
    val offline: Boolean = false,
    /**
     * How long the article is, counted once at sync time.
     */
    val words: Int? = null,
)

/** The cached post for [url], if some followed feed carried it. */
fun Map<String, List<FeedPost>>.postFor(url: String): FeedPost? = values.asSequence().flatten().firstOrNull { it.url == url }

/**
 * What the last successful sync of each feed found, keyed by feed. Rows live in SQLite
 * so a sync writes only the feeds that answered; the map is the in-memory read side.
 */
class FeedPostCache(
    private val store: KeyValueStore,
    private val db: DuskReadDatabase,
) {
    // Snapshot state and a StateFlow in one, so Compose and the iOS bridge read the same
    // value.
    private val observedPostsByFeed = Observed(load())

    var postsByFeed: Map<String, List<FeedPost>> by observedPostsByFeed
        private set

    /** [postsByFeed] for observers outside a composition; see [Observed]. */
    val postsByFeedUpdates: StateFlow<Map<String, List<FeedPost>>> get() = observedPostsByFeed.updates

    /** When some feed last answered, so Home can tell a stale week from a fresh one. */
    var syncedAt: Long? = store.getString(SyncedAtKey)?.toLongOrNull()
        private set

    /**
     * Every feed that answered, in one transaction on Default — only those feeds' rows are
     * touched, where the old store re-encoded the whole cache.
     */
    suspend fun replaceAll(byFeed: Map<String, List<FeedPost>>) {
        if (byFeed.isEmpty()) return
        withContext(Dispatchers.Default) { write(byFeed) }
        postsByFeed = postsByFeed + byFeed
        syncedAt = Clock.System.now().toEpochMilliseconds()
        store.putString(SyncedAtKey, syncedAt.toString())
    }

    /**
     * Every feed's posts at once, for the reset in Settings.
     */
    fun clear() {
        postsByFeed = emptyMap()
        db.feedPostQueries.deleteAll()
        syncedAt = null
        store.putString(SyncedAtKey, null)
    }

    /** Drops a feed's cached posts once it's unfollowed — nothing should surface for a blog no longer synced. */
    fun removeFeed(feedId: String) {
        postsByFeed = postsByFeed - feedId
        db.feedPostQueries.deleteFeed(feedId)
    }

    private fun write(byFeed: Map<String, List<FeedPost>>) = db.feedPostQueries.transaction {
        byFeed.forEach { (feedId, posts) ->
            db.feedPostQueries.deleteFeed(feedId)
            posts.forEachIndexed { index, post -> db.feedPostQueries.insert(post.asRow(index)) }
        }
    }

    private fun load(): Map<String, List<FeedPost>> {
        importLegacy()
        return db.feedPostQueries.selectAll().executeAsList().map { it.asPost() }.groupBy { it.feedId }
    }

    /**
     * Moves a cache written by an older build into an empty table. Never over rows: a
     * removal lost to an early kill can bring the stale string back.
     */
    private fun importLegacy() {
        val legacy = store.getString(Key) ?: return
        if (db.feedPostQueries.count().executeAsOne() == 0L) {
            write(legacy.split(RecordSeparator).mapNotNull(::decode).groupBy { it.feedId })
        }
        store.putString(Key, null)
    }

    private fun decode(record: String): FeedPost? {
        val fields = record.split(FieldSeparator)
        if (fields.size < 3) return null

        return FeedPost(
            feedId = fields[0].ifBlank { return null },
            url = fields[1].ifBlank { return null },
            title = fields[2],
            imageUrl = fields.getOrNull(3)?.takeIf { it.isNotBlank() },
            content = fields.getOrNull(4)?.takeIf { it.isNotBlank() },
            publishedAt = fields.getOrNull(5)?.toLongOrNull(),
            words = fields.getOrNull(6)?.toIntOrNull(),
            offline = fields.getOrNull(7) == "1",
        )
    }

    private companion object {
        /** Where an older build kept the cache; read once by [importLegacy]. */
        const val Key = "feeds.posts"
        const val SyncedAtKey = "feeds.syncedAt"
        const val FieldSeparator = '\u001F'
        const val RecordSeparator = '\u001E'
    }
}

private fun FeedPost.asRow(position: Int) = FeedPostRow(
    feedId = feedId,
    url = url,
    title = title,
    imageUrl = imageUrl,
    content = content,
    publishedAt = publishedAt,
    words = words?.toLong(),
    offline = if (offline) 1L else 0L,
    position = position.toLong(),
)

private fun FeedPostRow.asPost() = FeedPost(
    feedId = feedId,
    url = url,
    title = title,
    imageUrl = imageUrl,
    content = content,
    publishedAt = publishedAt,
    words = words?.toInt(),
    offline = offline == 1L,
)

@Composable
fun rememberFeedPostCache(): FeedPostCache = LocalAppGraph.current.feedPosts
