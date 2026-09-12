package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.FeedPost
import dev.mks.duskread.links.discoverFeedUrl
import dev.mks.duskread.links.syncFeeds

/**
 * Followed blogs and their cached posts.
 *
 * Every sync entry point in `FeedSync` takes an `HttpClient` as its first
 * argument; the whole point of this class is that Swift never sees one. The
 * graph's single client is supplied here instead, which also means iOS gets
 * the same one connection pool the rest of the app uses.
 */
class FeedsBridge internal constructor(private val graph: AppGraph) {
    fun observeFeeds(onEach: (List<Feed>) -> Unit): Cancellable = graph.feeds.feedsUpdates.watch(onEach)

    fun observePosts(onEach: (Map<String, List<FeedPost>>) -> Unit): Cancellable = graph.feedPosts.postsByFeedUpdates.watch(onEach)

    fun currentFeeds(): List<Feed> = graph.feeds.feeds

    fun currentPosts(): Map<String, List<FeedPost>> = graph.feedPosts.postsByFeed

    fun remove(id: String) {
        graph.feeds.remove(id)
        graph.feedPosts.removeFeed(id)
    }

    fun clear() {
        graph.feeds.clear()
        graph.feedPosts.clear()
    }

    /**
     * Resolves whatever was pasted to a real feed address, then follows it.
     *
     * Discovery and adding are one call because they are one intent: a reader
     * pastes a blog's home page, not its `atom.xml`, and splitting the two
     * across the bridge would only give Swift a chance to do half of it.
     */
    suspend fun follow(rawUrl: String, title: String?, topic: String?): Feed? {
        val resolved = runCatching { discoverFeedUrl(graph.http, rawUrl) }.getOrNull() ?: rawUrl
        return graph.feeds.add(resolved, title, topic)
    }

    /** Returns how many new posts landed, so Swift can say so. */
    suspend fun sync(): Int = syncFeeds(graph.http, graph.feeds.feeds, graph.feedPosts)
}
