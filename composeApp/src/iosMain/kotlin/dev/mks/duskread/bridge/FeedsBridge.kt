package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.FeedPost
import dev.mks.duskread.links.discoverFeedUrl
import dev.mks.duskread.links.pruneSummaries
import dev.mks.duskread.links.syncFeeds

/**
 * Followed blogs and their cached posts.
 */
class FeedsBridge internal constructor(private val graph: AppGraph) {
    fun observeFeeds(onEach: (List<Feed>) -> Unit): Cancellable = graph.feeds.feedsUpdates.watch(onEach)

    fun observePosts(onEach: (Map<String, List<FeedPost>>) -> Unit): Cancellable = graph.feedPosts.postsByFeedUpdates.watch(onEach)

    fun currentFeeds(): List<Feed> = graph.feeds.feeds

    fun currentPosts(): Map<String, List<FeedPost>> = graph.feedPosts.postsByFeed

    fun remove(id: String) {
        graph.feeds.remove(id)
        graph.feedPosts.removeFeed(id)
        pruneSummaries(graph.summaries, graph.links, graph.feedPosts)
    }

    fun clear() {
        graph.feeds.clear()
        graph.feedPosts.clear()
    }

    /**
     * Resolves whatever was pasted to a real feed address, then follows it.
     */
    suspend fun follow(rawUrl: String, title: String?, topic: String?): Feed? {
        val resolved = runCatching { discoverFeedUrl(graph.http, rawUrl) }.getOrNull() ?: rawUrl
        return graph.feeds.add(resolved, title, topic)
    }

    /** Returns how many new posts landed, so Swift can say so. */
    suspend fun sync(): Int = syncFeeds(graph.http, graph.feeds.feeds, graph.feedPosts, graph.links, graph.summaries)
}
