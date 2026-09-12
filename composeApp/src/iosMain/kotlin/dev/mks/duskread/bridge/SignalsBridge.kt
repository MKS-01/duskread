package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.links.Candidate
import dev.mks.duskread.links.Scored
import dev.mks.duskread.links.pool
import dev.mks.duskread.links.rank
import dev.mks.duskread.links.topPicks

/**
 * What the reader has actually read, and the ranking built on top of it.
 *
 * The recommender is here rather than in its own bridge because it is useless
 * without these signals — `rank` takes them directly — and because Swift would
 * otherwise have to hold `LinkLibrary` and `FeedPostCache` just to pass them
 * back in. [nextUp] is the whole of Home's "Next up" in one call.
 */
class SignalsBridge internal constructor(private val graph: AppGraph) {
    fun recordRead(url: String) = graph.signals.recordRead(url)

    fun recordOpen(url: String) = graph.signals.recordOpen(url)

    fun recordSkip(url: String) = graph.signals.recordSkip(url)

    fun recordTopicRead(tag: String) = graph.signals.recordTopicRead(tag)

    fun clear() = graph.signals.clear()

    /**
     * The ranked shortlist Home shows.
     *
     * [seed] is the shuffle: re-seeding re-ranks rather than re-randomising, so
     * tapping shuffle means "something else good" rather than "anything at all".
     */
    fun nextUp(count: Int, now: Long, seed: Int, focusMinutes: Int?): List<Scored> {
        val candidates: List<Candidate> = pool(graph.links, graph.feedPosts, graph.feeds.feeds)
        return topPicks(rank(candidates, graph.signals, now, seed, focusMinutes), count)
    }
}
