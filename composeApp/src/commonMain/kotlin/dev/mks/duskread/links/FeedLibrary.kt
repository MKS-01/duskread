package dev.mks.duskread.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.LocalAppGraph
import dev.mks.duskread.data.Observed
import dev.mks.duskread.data.rememberKeyValueStore
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The blogs followed for [syncFeeds] to pull from, persisted through [KeyValueStore] with
 * the same delimited-string encoding [LinkLibrary] uses.
 */
@OptIn(ExperimentalTime::class)
class FeedLibrary(private val store: KeyValueStore) {
    // Snapshot state and a StateFlow in one, so Compose and the iOS bridge read the same
    // value.
    private val observedFeeds = Observed(load())

    var feeds: List<Feed> by observedFeeds
        private set

    /** [feeds] for observers outside a composition; see [Observed]. */
    val feedsUpdates: StateFlow<List<Feed>> get() = observedFeeds.updates

    /**
     * Follows [rawUrl], or returns the existing feed if it's already followed. Null if
     * not a link at all.
     */
    fun add(rawUrl: String, title: String? = null, topic: String? = null): Feed? {
        if (!looksLikeUrl(rawUrl)) return null

        val url = normaliseUrl(rawUrl).trim()
        // Same rule as a saved link: `…/feed` and `…/feed/` are one blog, and following
        // it twice would double every post it publishes.
        feeds.firstOrNull { sameArticle(it.url, url) }?.let { existing ->
            val filled = existing.copy(
                title = existing.title?.takeIf { it.isNotBlank() } ?: title?.takeIf { it.isNotBlank() },
                topic = existing.topic?.takeIf { it.isNotBlank() } ?: topic?.takeIf { it.isNotBlank() },
            )
            if (filled == existing) return existing

            feeds = feeds.map { if (it.id == existing.id) filled else it }
            persist()
            return filled
        }

        val feed = Feed(
            id = Clock.System.now().toEpochMilliseconds().toString(36) + "-" + feeds.size,
            url = url,
            addedAt = Clock.System.now().toEpochMilliseconds(),
            title = title?.takeIf { it.isNotBlank() },
            topic = topic?.takeIf { it.isNotBlank() },
        )
        feeds = feeds + feed
        persist()
        return feed
    }

    fun remove(id: String) {
        feeds = feeds.filterNot { it.id == id }
        persist()
    }

    /**
     * Unfollows everything at once, for the reset in Settings — the only caller.
     */
    fun clear() {
        feeds = emptyList()
        store.putString(Key, null)
    }

    private fun persist() = store.putString(Key, encode(feeds).takeIf { it.isNotEmpty() })

    private fun load(): List<Feed> = store.getString(Key)?.split(RecordSeparator)?.mapNotNull(::decode).orEmpty()

    private fun encode(feeds: List<Feed>): String = feeds.joinToString(RecordSeparator.toString()) { feed ->
        listOf(feed.id, feed.url, feed.addedAt.toString(), feed.title.orEmpty(), feed.topic.orEmpty())
            .joinToString(FieldSeparator.toString())
    }

    private fun decode(record: String): Feed? {
        val fields = record.split(FieldSeparator)
        if (fields.size < 3) return null

        return Feed(
            id = fields[0],
            url = fields[1].ifBlank { return null },
            addedAt = fields[2].toLongOrNull() ?: 0L,
            // Positional and appended last, so records written before feeds carried a
            // name or a topic still decode — the same tolerance SavedLink uses.
            title = fields.getOrNull(3)?.takeIf { it.isNotBlank() },
            topic = fields.getOrNull(4)?.takeIf { it.isNotBlank() },
        )
    }

    private companion object {
        const val Key = "feeds.followed"
        const val FieldSeparator = ''
        const val RecordSeparator = ''
    }
}

@Composable
fun rememberFeedLibrary(): FeedLibrary = LocalAppGraph.current.feeds
