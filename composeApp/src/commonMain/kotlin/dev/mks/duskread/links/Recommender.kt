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

/** Deliberately small: a skip is "not right now", not a dislike. See [HostSignal]. */
private const val SkipPenaltyWeight = 0.3f

/** And it wears off. A week is long enough to have meant it, short enough to forgive. */
private const val SkipHalfLifeMs = 7L * 24 * 60 * 60 * 1000

/** The shuffle. Large enough to reorder the top of a close field, too small to promote a bad candidate. */
private const val JitterWeight = 0.35f

/** Freshness half-life. About a fortnight — a blog post is still worth reading a week later. */
private const val FreshnessHalfLifeMs = 14L * 24 * 60 * 60 * 1000

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
 *
 * The merged pool is the part that does not exist anywhere else: a saved link
 * and a post from a followed blog are different records with different
 * lifetimes, and the whole point of ranking is that they compete on equal
 * terms. [savedId] is what tells them apart afterwards — non-null means the
 * reader already owns this one, null means opening it has to save it first or
 * the signal is lost with the next sync.
 */
data class Candidate(
    val url: String,
    val title: String,
    val host: String,
    /** Published, or saved, or null for a feed that dates nothing. */
    val date: Long?,
    /** The publisher's body or the link's description — only ever used to estimate length. */
    val body: String?,
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
 *
 * A feed post already in the library is dropped rather than merged: the saved
 * copy carries the read state, and two rows for one article would be a bug
 * the reader can see.
 */
fun pool(links: LinkLibrary, cache: FeedPostCache, tagFor: (String) -> String? = { null }): List<Candidate> {
    val saved = links.links.filterNot { it.read }.map { link ->
        Candidate(
            url = link.url,
            title = link.title,
            host = link.host,
            date = link.savedAt,
            body = link.description,
            tag = tagFor(link.url),
            savedId = link.id,
        )
    }

    val known = links.links.mapTo(mutableSetOf()) { it.url.lowercase() }
    val posts = cache.postsByFeed.values.asSequence().flatten()
        .filterNot { it.url.lowercase() in known }
        .distinctBy { it.url.lowercase() }
        .map { post ->
            Candidate(
                url = post.url,
                title = post.title,
                host = hostOf(post.url),
                date = post.publishedAt,
                body = post.content,
                tag = tagFor(post.url),
            )
        }

    return saved + posts
}

/**
 * Score every candidate and return them best-first.
 *
 * Pure: no Compose, no I/O, no clock of its own. [now] and [seed] are passed
 * in so the same pool ranks the same way twice, which is what makes the
 * Discovery block in Settings worth anything.
 *
 * [seed] is the shuffle. Re-seeding re-ranks without abandoning the ranking,
 * so shuffle means "something else good" rather than "anything at all" —
 * which is the whole difference from the `random()` this replaces.
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

        // Only a saved link can be stale — a feed post the reader has never
        // seen is not something they are forgetting. Ramps in over the month
        // after the threshold rather than switching on, so nothing jumps to
        // the top of the list on one particular morning.
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

        // Saturating, so the tenth skip of a host costs barely more than the
        // third, and decaying, so a host skipped past last week is not still
        // being punished for it today.
        val skip = signal?.let { it ->
            val raw = 1f - 1f / (1f + it.skips)
            raw * (it.lastSkipAt?.let { at -> decay(now - at, SkipHalfLifeMs) } ?: 1f)
        } ?: 0f

        val terms = mapOf(
            "freshness" to freshness * FreshnessWeight,
            "stale" to stale * StaleRescueWeight,
            "source" to sourceAffinity * SourceAffinityWeight,
            "topic" to topicAffinity * TopicAffinityWeight,
            "fit" to fit * FitWeight,
            "skip" to -skip * SkipPenaltyWeight,
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
 * Stable per-candidate noise in 0..1.
 *
 * Derived from the url so it does not move under a scroll, and from the seed
 * so a shuffle moves all of them at once.
 */
private fun jitter(url: String, seed: Int): Float {
    var h = url.hashCode() * 31 + seed
    h = h xor (h shr 16)
    h *= 0x7feb352d
    h = h xor (h shr 15)
    return (h.toLong() and 0xFFFFL).toFloat() / 0xFFFF
}

/** Body words over [WordsPerMinute], falling back to a flat guess for a candidate with no body cached. */
internal fun estimatedMinutes(candidate: Candidate): Float {
    val words = candidate.body?.split(' ', '\n', '\t')?.count { it.isNotBlank() } ?: 0
    if (words < 40) return DefaultMinutes
    return max(1f, words.toFloat() / WordsPerMinute)
}
