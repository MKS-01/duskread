package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.FeedPost
import dev.mks.duskread.links.FeedSyncResult
import dev.mks.duskread.links.FeedSyncState
import dev.mks.duskread.links.FollowingGroups
import dev.mks.duskread.links.LatestItem
import dev.mks.duskread.links.discoverFeedUrl
import dev.mks.duskread.links.followingGroups
import dev.mks.duskread.links.latestPosts
import dev.mks.duskread.links.pruneSummaries

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

    /** Following's NEW / CAUGHT UP / NO POSTS YET split, cut by the shared side. */
    fun groups(query: String, now: Long): FollowingGroups = followingGroups(graph.feeds.feeds, graph.feedPosts, graph.links, query, now)

    fun shortAgo(at: Long, now: Long): String = dev.mks.duskread.links.shortAgo(at, now)

    /** The Latest header's count, worded by the shared side so both homes agree. */
    fun latestCountLabel(items: List<LatestItem>): String = dev.mks.duskread.links.latestCountLabel(items)

    fun syncState(): FeedSyncState = graph.feedSync.state

    fun observeSync(onEach: (FeedSyncState) -> Unit): Cancellable = graph.feedSync.stateUpdates.watch(onEach)

    /** Every finished sync worth a toast, whichever side started it. */
    fun observeSyncResults(onEach: (FeedSyncResult) -> Unit): Cancellable = graph.feedSync.results.watch { if (it.worthSaying) onEach(it) }

    /** Joins a sync already running rather than starting a second. */
    suspend fun sync() {
        graph.feedSync.sync()
    }

    /** Home coming into view; does nothing while the last sync is recent. */
    suspend fun syncIfStale(now: Long) {
        graph.feedSync.syncIfStale(now)
    }
}
