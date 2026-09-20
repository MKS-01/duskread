package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.links.Candidate
import dev.mks.duskread.links.Scored
import dev.mks.duskread.links.pool
import dev.mks.duskread.links.rank
import dev.mks.duskread.links.topPicks

/**
 * What the reader has actually read, and the ranking built on top of it.
 */
class SignalsBridge internal constructor(private val graph: AppGraph) {
    fun recordRead(url: String) = graph.signals.recordRead(url)

    fun recordOpen(url: String) = graph.signals.recordOpen(url)

    fun recordSkip(url: String) = graph.signals.recordSkip(url)

    fun recordTopicRead(tag: String) = graph.signals.recordTopicRead(tag)

    fun clear() = graph.signals.clear()

    /**
     * The ranked shortlist Home shows. [exclude] is whatever is already on the screen as
     * a card, dropped before the ranking rather than after — filtering the picks
     * afterwards leaves the section short of the [count] it means to offer.
     */
    fun nextUp(count: Int, now: Long, seed: Int, focusMinutes: Int?, exclude: Set<String>): List<Scored> {
        val candidates: List<Candidate> = pool(graph.links, graph.feedPosts, graph.feeds.feeds)
            .filterNot { it.url in exclude }
        return topPicks(rank(candidates, graph.signals, now, seed, focusMinutes), count)
    }
}
