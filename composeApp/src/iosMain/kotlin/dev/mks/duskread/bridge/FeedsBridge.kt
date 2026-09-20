package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.FeedPost
import dev.mks.duskread.links.LatestItem
import dev.mks.duskread.links.discoverFeedUrl
import dev.mks.duskread.links.latestPosts
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

    /**
     * The week's posts, as Home's cards. The window and the caps are the shared
     * function's, not Swift's — both homes show the same week.
     */
    fun latest(now: Long): List<LatestItem> = latestPosts(
        feeds = graph.feeds.feeds,
        cache = graph.feedPosts,
        links = graph.links,
        now = now,
    )

    /** Returns how many new posts landed, so Swift can say so. */
    suspend fun sync(): Int = syncFeeds(graph.http, graph.feeds.feeds, graph.feedPosts, graph.links, graph.summaries)
}
