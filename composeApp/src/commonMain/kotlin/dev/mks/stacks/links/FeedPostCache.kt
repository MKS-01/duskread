package dev.mks.stacks.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.stacks.data.KeyValueStore
import dev.mks.stacks.data.rememberKeyValueStore

/** One post as it appeared in a feed the last time that feed synced. */
data class FeedPost(val feedId: String, val url: String, val title: String)

/**
 * What the last successful sync of each feed found, keyed by feed.
 *
 * Home reads straight from this rather than fetching on its own, so a
 * followed blog's posts are there the instant the app opens and only change
 * when the reader presses Sync. A feed that fails to load on a given sync
 * keeps whatever it last had here rather than going blank — [replace] is
 * only called for a feed that actually answered, so "cached until new data
 * arrives" is the default, not something callers have to arrange.
 */
class FeedPostCache(private val store: KeyValueStore) {
    var postsByFeed: Map<String, List<FeedPost>> by mutableStateOf(load())
        private set

    fun replace(feedId: String, posts: List<FeedPost>) {
        postsByFeed = postsByFeed + (feedId to posts)
        persist()
    }

    /** Drops a feed's cached posts once it's unfollowed — nothing should surface for a blog no longer synced. */
    fun removeFeed(feedId: String) {
        postsByFeed = postsByFeed - feedId
        persist()
    }

    private fun persist() = store.putString(Key, encode(postsByFeed.values.flatten()).takeIf { it.isNotEmpty() })

    private fun load(): Map<String, List<FeedPost>> = store.getString(Key)?.split(RecordSeparator)?.mapNotNull(::decode)?.groupBy { it.feedId }.orEmpty()

    private fun encode(posts: List<FeedPost>): String = posts.joinToString(RecordSeparator.toString()) { post ->
        listOf(post.feedId, post.url, post.title.clean()).joinToString(FieldSeparator.toString())
    }

    private fun decode(record: String): FeedPost? {
        val fields = record.split(FieldSeparator)
        if (fields.size < 3) return null

        return FeedPost(
            feedId = fields[0].ifBlank { return null },
            url = fields[1].ifBlank { return null },
            title = fields[2],
        )
    }

    private fun String.clean() = filterNot { it == FieldSeparator || it == RecordSeparator }.trim()

    private companion object {
        const val Key = "feeds.posts"
        const val FieldSeparator = ''
        const val RecordSeparator = ''
    }
}

@Composable
fun rememberFeedPostCache(): FeedPostCache {
    val store = rememberKeyValueStore()
    return remember(store) { FeedPostCache(store) }
}
