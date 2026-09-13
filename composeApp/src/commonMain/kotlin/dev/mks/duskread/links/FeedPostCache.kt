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
 * What the last successful sync of each feed found, keyed by feed.
 */
class FeedPostCache(private val store: KeyValueStore) {
    // Snapshot state and a StateFlow in one, so Compose and the iOS bridge read the same
    // value.
    private val observedPostsByFeed = Observed(load())

    var postsByFeed: Map<String, List<FeedPost>> by observedPostsByFeed
        private set

    /** [postsByFeed] for observers outside a composition; see [Observed]. */
    val postsByFeedUpdates: StateFlow<Map<String, List<FeedPost>>> get() = observedPostsByFeed.updates

    fun replace(feedId: String, posts: List<FeedPost>) {
        postsByFeed = postsByFeed + (feedId to posts)
        persist()
    }

    /**
     * Every feed that answered, in one write.
     */
    fun replaceAll(byFeed: Map<String, List<FeedPost>>) {
        if (byFeed.isEmpty()) return
        postsByFeed = postsByFeed + byFeed
        persist()
    }

    /**
     * Every feed's posts at once, for the reset in Settings.
     */
    fun clear() {
        postsByFeed = emptyMap()
        store.putString(Key, null)
    }

    /** Drops a feed's cached posts once it's unfollowed — nothing should surface for a blog no longer synced. */
    fun removeFeed(feedId: String) {
        postsByFeed = postsByFeed - feedId
        persist()
    }

    private fun persist() = store.putString(Key, encode(postsByFeed.values.flatten()).takeIf { it.isNotEmpty() })

    private fun load(): Map<String, List<FeedPost>> = store.getString(Key)?.split(RecordSeparator)?.mapNotNull(::decode)?.groupBy { it.feedId }.orEmpty()

    private fun encode(posts: List<FeedPost>): String = posts.joinToString(RecordSeparator.toString()) { post ->
        listOf(
            post.feedId,
            post.url,
            post.title.clean(),
            post.imageUrl.orEmpty(),
            post.content.orEmpty().clean(),
            post.publishedAt?.toString().orEmpty(),
            post.words?.toString().orEmpty(),
            if (post.offline) "1" else "0",
        ).joinToString(FieldSeparator.toString())
    }

    private fun decode(record: String): FeedPost? {
        // Still three, not seven: records written before posts carried an image, a body,
        // a date or a word count decode as they always did rather than being dropped.
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

    private fun String.clean() = filterNot { it == FieldSeparator || it == RecordSeparator }.trim()

    private companion object {
        const val Key = "feeds.posts"
        const val FieldSeparator = ''
        const val RecordSeparator = ''
    }
}

@Composable
fun rememberFeedPostCache(): FeedPostCache = LocalAppGraph.current.feedPosts
