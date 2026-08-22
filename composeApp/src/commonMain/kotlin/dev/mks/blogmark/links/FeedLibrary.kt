package dev.mks.blogmark.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.blogmark.data.KeyValueStore
import dev.mks.blogmark.data.rememberKeyValueStore
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The blogs followed for [syncFeeds] to pull from, persisted through
 * [KeyValueStore] with the same delimited-string encoding [LinkLibrary] uses —
 * a handful of feed addresses earns a real database no more than a reading
 * list does.
 */
@OptIn(ExperimentalTime::class)
class FeedLibrary(private val store: KeyValueStore) {
    var feeds: List<Feed> by mutableStateOf(load())
        private set

    /** Follows [rawUrl], or returns the existing feed if it's already followed. Null if not a link at all. */
    fun add(rawUrl: String): Feed? {
        if (!looksLikeUrl(rawUrl)) return null

        val url = normaliseUrl(rawUrl).trim()
        feeds.firstOrNull { it.url.equals(url, ignoreCase = true) }?.let { return it }

        val feed = Feed(
            id = Clock.System.now().toEpochMilliseconds().toString(36) + "-" + feeds.size,
            url = url,
            addedAt = Clock.System.now().toEpochMilliseconds(),
        )
        feeds = feeds + feed
        persist()
        return feed
    }

    fun remove(id: String) {
        feeds = feeds.filterNot { it.id == id }
        persist()
    }

    private fun persist() = store.putString(Key, encode(feeds).takeIf { it.isNotEmpty() })

    private fun load(): List<Feed> = store.getString(Key)?.split(RecordSeparator)?.mapNotNull(::decode).orEmpty()

    private fun encode(feeds: List<Feed>): String = feeds.joinToString(RecordSeparator.toString()) { feed ->
        listOf(feed.id, feed.url, feed.addedAt.toString()).joinToString(FieldSeparator.toString())
    }

    private fun decode(record: String): Feed? {
        val fields = record.split(FieldSeparator)
        if (fields.size < 3) return null

        return Feed(
            id = fields[0],
            url = fields[1].ifBlank { return null },
            addedAt = fields[2].toLongOrNull() ?: 0L,
        )
    }

    private companion object {
        const val Key = "feeds.followed"
        const val FieldSeparator = ''
        const val RecordSeparator = ''
    }
}

@Composable
fun rememberFeedLibrary(): FeedLibrary {
    val store = rememberKeyValueStore()
    return remember(store) { FeedLibrary(store) }
}
