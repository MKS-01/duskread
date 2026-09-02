package dev.mks.duskread.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.rememberKeyValueStore

/**
 * One post as it appeared in a feed the last time that feed synced.
 *
 * [content] is the publisher's own markup for the post, present only for the
 * newest few entries of each feed and truncated even there — see `asPost` in
 * `FeedSync.kt` for why. Null means "not cached", never "the post is empty";
 * the reader fetches and extracts the page in that case.
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
     *
     * Decided at sync time by [dev.mks.duskread.links.articleFromFeed] — the
     * *same* function the reader calls, given the same truncated body it will
     * be given. A cheaper approximation would eventually disagree with it, and
     * a badge that lies about what opens offline is worse than no badge.
     *
     * False for a feed that publishes only a teaser: three of the followed
     * blogs do, and no amount of caching at sync time can fix that.
     */
    val offline: Boolean = false,
    /**
     * How long the article is, counted once at sync time.
     *
     * Kept as a number rather than recomputed from [content] because the
     * ranking needs it for every candidate on every re-rank, and splitting a
     * couple of megabytes of cached markup on the draw path is what a shuffle
     * tap used to cost.
     *
     * It is also more accurate than [content] could be: this is counted from
     * the publisher's whole body, before the cache truncates it and before it
     * is dropped entirely for all but the newest few per feed.
     */
    val words: Int? = null,
)

/** The cached post for [url], if some followed feed carried it. */
fun Map<String, List<FeedPost>>.postFor(url: String): FeedPost? = values.asSequence().flatten().firstOrNull { it.url == url }

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

    /**
     * Every feed that answered, in one write.
     *
     * [persist] re-encodes the whole catalogue, so calling [replace] once per
     * feed meant a fourteen-feed sync serialised roughly a megabyte fourteen
     * times over to store it once. Merged rather than assigned, because a feed
     * that failed this time is absent from [byFeed] and has to keep what it
     * last had — the same contract [replace] has always honoured by being
     * called only for a feed that actually answered.
     */
    fun replaceAll(byFeed: Map<String, List<FeedPost>>) {
        if (byFeed.isEmpty()) return
        postsByFeed = postsByFeed + byFeed
        persist()
    }

    /**
     * Every feed's posts at once, for the reset in Settings.
     *
     * [replaceAll] cannot do this: it merges, and it returns early on an empty
     * map precisely so a sync where nothing answered leaves the cache alone.
     * Erasing needs the opposite of both, and calling the merge with an empty
     * map — which is what the reset used to do — cleared nothing at all, so
     * NEXT UP went on offering posts from blogs that no longer existed.
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
        // Still three, not seven: records written before posts carried an
        // image, a body, a date or a word count decode as they always did
        // rather than being dropped, so a reader who updates the app keeps
        // their feed lists until the next sync fills the new fields in.
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
fun rememberFeedPostCache(): FeedPostCache {
    val store = rememberKeyValueStore()
    return remember(store) { FeedPostCache(store) }
}
