package dev.mks.duskread.links

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/*
 * The weights. They are wrong at first and the only way to fix them is on the
 * phone, which is why they are one block at the top with a line each rather
 * than scattered through the arithmetic.
 *
 * Every term below is bounded to roughly 0..1 before it is weighted, so a
 * weight reads as "how much this matters relative to the others" and nothing
 * else. Change one and the comparison stays honest.
 */

/** New things surface. The largest single term, because recency is the one signal that is never wrong. */
private const val FreshnessWeight = 1.0f

/** A saved link the reader has forgotten. Without this the section only ever offers the last thing saved. */
private const val StaleRescueWeight = 0.55f

/** Reads from this host over all reads. Smoothed, so one read of one blog does not swamp the pool. */
private const val SourceAffinityWeight = 0.7f

/** The same over tags. Zero until something tags the candidates. */
private const val TopicAffinityWeight = 0.7f

/** Length against the focus timer. A five-minute timer should not be offered a twenty-minute essay. */
private const val FitWeight = 0.45f

/**
 * A weak hint that a source is not landing. Halved when the per-post term below arrived
 * and took over the job this was being asked to do alone.
 */
private const val SkipPenaltyWeight = 0.15f

/** And it wears off. A week is long enough to have meant it, short enough to forgive. */
private const val SkipHalfLifeMs = 7L * 24 * 60 * 60 * 1000

/**
 * Stepping past *this* post. Large enough to sink it outright, because that is exactly
 * what the reader just asked for by tapping shuffle.
 */
private const val PostSkipWeight = 1.4f

/** Two days: long enough that a shuffle-through does not loop, short enough that nothing is buried. */
private const val PostSkipHalfLifeMs = 2L * 24 * 60 * 60 * 1000

/**
 * The shuffle. Large enough to reorder a genuinely close field, small enough not to
 * outvote freshness.
 */
private const val JitterWeight = 0.18f

/**
 * Freshness half-life. Four days, not the fortnight this started at.
 */
private const val FreshnessHalfLifeMs = 4L * 24 * 60 * 60 * 1000

/** How long a saved link has to sit untouched before the rescue term starts paying out. */
private const val StaleAfterMs = 30L * 24 * 60 * 60 * 1000

/** Laplace smoothing for both affinity terms. Higher means more reads are needed before a host stands out. */
private const val AffinitySmoothing = 4f

/** Words a minute, for turning a body into an estimate. Deliberately unhurried; this is evening reading. */
private const val WordsPerMinute = 200

/** What a candidate with no body at all is assumed to cost. */
private const val DefaultMinutes = 7f

/**
 * One thing Home could offer, from either half of the app.
 */
data class Candidate(
    val url: String,
    val title: String,
    val host: String,
    /** Published, or saved, or null for a feed that dates nothing. */
    val date: Long?,
    /** The publisher's body or the link's description — only ever used to estimate length. */
    val body: String?,
    /** Counted at sync time where a feed supplied one; see [FeedPost.words]. */
    val words: Int? = null,
    /** From the tagging layer, absent until it exists. */
    val tag: String? = null,
    /** Non-null when this is already a [SavedLink]. */
    val savedId: String? = null,
)

/** A candidate with its score and the arithmetic that produced it, kept for the Discovery block in Settings. */
data class Scored(
    val candidate: Candidate,
    val score: Float,
    /** Term name to its weighted contribution. Every candidate ranking system is opaque exactly when it misbehaves. */
    val terms: Map<String, Float>,
) {
    /** Body words over [WordsPerMinute], for the meta line. */
    val minutes: Int get() = estimatedMinutes(candidate).toInt().coerceAtLeast(1)
}

/**
 * Everything unread the app knows about, as one pool.
 */
fun pool(links: LinkLibrary, cache: FeedPostCache, feeds: List<Feed> = emptyList()): List<Candidate> {
    val topicByFeed = feeds.associate { it.id to it.topic }
    // A saved link belongs to no feed, but it often comes from a blog that is followed —
    // so fall back to matching on host.
    val topicByHost = feeds.filter { it.topic != null }.associate { it.host to it.topic }

    val saved = links.links.filterNot { it.read }.map { link ->
        Candidate(
            url = link.url,
            title = link.title,
            host = link.host,
            date = link.savedAt,
            body = link.description,
            // What Notion filed beats what the host implies: a newsletter arriving by
            // mail has a topic and no followed feed to match on.
            tag = link.topic ?: topicByHost[link.host],
            savedId = link.id,
        )
    }

    // Canonical, so a feed post already saved under a slightly different address is
    // recognised rather than offered a second time.
    val known = links.links.mapTo(mutableSetOf()) { canonicalUrl(it.url) }
    val posts = cache.postsByFeed.values.asSequence().flatten()
        .filterNot { canonicalUrl(it.url) in known }
        .distinctBy { canonicalUrl(it.url) }
        .map { post ->
            Candidate(
                url = post.url,
                title = post.title,
                host = hostOf(post.url),
                date = post.publishedAt,
                body = post.content,
                words = post.words,
                tag = topicByFeed[post.feedId],
            )
        }

    return saved + posts
}

/**
 * Score every candidate and return them best-first. Pure: no Compose, no I/O, no clock of
 * its own.
 */
fun rank(
    candidates: List<Candidate>,
    signals: ReadingSignals,
    now: Long,
    seed: Int,
    focusMinutes: Int?,
): List<Scored> {
    val totalReads = signals.totalReads
    val totalTopicReads = signals.topicReads.values.sum()

    return candidates.map { candidate ->
        val signal = signals.byHost[candidate.host]

        val freshness = candidate.date?.let { decay(now - it, FreshnessHalfLifeMs) } ?: 0f

        // Only a saved link can be stale — a feed post the reader has never seen is not
        // something they are forgetting.
        val stale = if (candidate.savedId != null && candidate.date != null) {
            val idle = now - candidate.date
            if (idle <= StaleAfterMs) 0f else 1f - decay(idle - StaleAfterMs, StaleAfterMs)
        } else {
            0f
        }

        val sourceAffinity = affinity(signal?.reads ?: 0, totalReads)
        val topicAffinity = candidate.tag?.let { affinity(signals.topicReads[it] ?: 0, totalTopicReads) } ?: 0f

        // No timer set is not a reason to prefer any particular length.
        val fit = focusMinutes?.let { target ->
            val estimate = estimatedMinutes(candidate)
            (1f - abs(estimate - target) / max(target.toFloat(), estimate)).coerceIn(0f, 1f)
        } ?: 0f

        // Saturating, so the tenth skip of a host costs barely more than the third, and
        // decaying.
        val skip = signal?.let { it ->
            val raw = 1f - 1f / (1f + it.skips)
            raw * (it.lastSkipAt?.let { at -> decay(now - at, SkipHalfLifeMs) } ?: 1f)
        } ?: 0f

        // The one signal here about an article rather than a source.
        val postSkip = signals.skippedPosts[candidate.url]?.let { at -> decay(now - at, PostSkipHalfLifeMs) } ?: 0f

        val terms = mapOf(
            "freshness" to freshness * FreshnessWeight,
            "stale" to stale * StaleRescueWeight,
            "source" to sourceAffinity * SourceAffinityWeight,
            "topic" to topicAffinity * TopicAffinityWeight,
            "fit" to fit * FitWeight,
            "skip" to -skip * SkipPenaltyWeight,
            "skipped" to -postSkip * PostSkipWeight,
            "jitter" to jitter(candidate.url, seed) * JitterWeight,
        )

        Scored(candidate, terms.values.sum(), terms)
    }.sortedByDescending { it.score }
}

/** Exponential decay to 0.5 at [halfLife]. Bounded 0..1 by construction. */
private fun decay(age: Long, halfLife: Long): Float {
    if (age <= 0L) return 1f
    return exp(-0.693147f * age.toFloat() / halfLife.toFloat())
}

/** Laplace-smoothed share. One read of one host cannot reach 1.0, which is the point. */
private fun affinity(count: Int, total: Int): Float {
    if (total <= 0) return 0f
    return count / (total + AffinitySmoothing)
}

/**
 * Stable per-candidate noise in 0..1. Derived from the url so it does not move under a
 * scroll, and from the seed so a shuffle moves all of them at once.
 */
private fun jitter(url: String, seed: Int): Float {
    var h = url.hashCode() * 31 + seed
    h = h xor (h shr 16)
    h *= 0x7feb352d
    h = h xor (h shr 15)
    return (h.toLong() and 0xFFFFL).toFloat() / 0xFFFF
}

/**
 * Body words over [WordsPerMinute], falling back to a flat guess for a candidate whose
 * length nothing knows.
 */
internal fun estimatedMinutes(candidate: Candidate): Float {
    val words = candidate.words ?: candidate.body?.split(' ', '\n', '\t')?.count { it.isNotBlank() } ?: 0
    if (words < 40) return DefaultMinutes
    return max(1f, words.toFloat() / WordsPerMinute)
}

/**
 * The [count] to actually show, at most one per source. Applied *after* ranking rather
 * than as another term inside it, on purpose.
 */
fun topPicks(ranked: List<Scored>, count: Int): List<Scored> {
    val seen = mutableSetOf<String>()
    val picked = mutableListOf<Scored>()

    for (scored in ranked) {
        if (picked.size == count) break
        if (seen.add(scored.candidate.host)) picked += scored
    }

    if (picked.size < count) {
        val taken = picked.mapTo(mutableSetOf()) { it.candidate.url }
        picked += ranked.filterNot { it.candidate.url in taken }.take(count - picked.size)
    }

    return picked
}
