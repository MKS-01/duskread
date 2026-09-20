package dev.mks.duskread.links

/*
 * What the followed blogs published this week, as Home's own list.
 *
 * Pure, and in `commonMain` rather than in either UI, because Compose and
 * SwiftUI both draw these cards and a second reckoning of "latest" would put
 * a different week on each phone.
 */

/**
 * One card on Home. [excerpt] is never blank — a post whose body yields nothing to quote
 * is not built into an item at all, which is how a blog with no readable content drops
 * out of this section rather than showing an empty card.
 */
data class LatestItem(
    val url: String,
    val title: String,
    val host: String,
    val feedId: String,
    val publishedAt: Long,
    val excerpt: String,
    val minutes: Int,
    /** The blog's subject, where its feed carries one. */
    val topic: String? = null,
    /** Shown recessed rather than dropped: a list called Latest that hides things is a puzzle. */
    val read: Boolean = false,
)

/**
 * The week's posts, newest first.
 */
fun latestPosts(
    feeds: List<Feed>,
    cache: FeedPostCache,
    links: LinkLibrary,
    now: Long,
    windowMs: Long = LatestWindowMs,
    limit: Int = LatestLimit,
): List<LatestItem> {
    val topicByFeed = feeds.associate { it.id to it.topic }
    val saved = links.links.associateBy { canonicalUrl(it.url) }

    val recent = cache.postsByFeed.values.asSequence().flatten()
        .filter { post ->
            val published = post.publishedAt ?: return@filter false
            // The upper bound is not pedantry: a post dated next year outranks everything
            // for as long as the feed carries it.
            published <= now + FutureSlackMs && now - published <= windowMs
        }
        .sortedByDescending { it.publishedAt }

    val items = mutableListOf<LatestItem>()
    val seen = mutableSetOf<String>()
    val perFeed = mutableMapOf<String, Int>()

    for (post in recent) {
        if (items.size == limit) break

        // One prolific blog posting daily would otherwise be the whole section.
        val taken = perFeed[post.feedId] ?: 0
        if (taken == MaxPerFeed) continue

        val key = canonicalUrl(post.url)
        if (!seen.add(key)) continue

        val published = post.publishedAt ?: continue
        val excerpt = post.content?.let(::excerptOf) ?: continue

        items += LatestItem(
            url = post.url,
            title = post.title,
            host = hostOf(post.url),
            feedId = post.feedId,
            publishedAt = published,
            excerpt = excerpt,
            minutes = estimatedMinutes(post.words, post.content).toInt().coerceAtLeast(1),
            topic = topicByFeed[post.feedId],
            read = saved[key]?.read == true,
        )
        perFeed[post.feedId] = taken + 1
    }

    return items
}

/**
 * The opening of a post as prose: tags out, entities decoded, cut at the last sentence
 * that fits. Null for a body with too little in it to be worth a card.
 */
fun excerptOf(markup: String, maxChars: Int = ExcerptChars): String? {
    val text = markup.textOf()
    if (text.length < MinExcerptChars) return null
    if (text.length <= maxChars) return text

    val window = text.take(maxChars)
    val stop = window.lastIndexOfAny(SentenceEnd)
    // A mid-sentence cut is marked; a clean one is not, because nothing was lost from it.
    if (stop >= MinExcerptChars) return window.take(stop + 1)

    return window.substringBeforeLast(' ').trimEnd(',', ';', ':', '—', '–', '-') + "…"
}

/** Home shows the week, and only the week. */
const val LatestWindowMs = 7L * 24 * 60 * 60 * 1000

/** Enough to be worth scrolling, few enough to stay a shortlist rather than an inbox. */
const val LatestLimit = 10

/** See the loop above: no blog gets more than this many of the [LatestLimit] cards. */
const val MaxPerFeed = 3

/** A publisher scheduling an hour ahead is dating in good faith; a year ahead is not. */
private const val FutureSlackMs = 24L * 60 * 60 * 1000

/** Roughly three lines on a phone, which is the card's own budget for it. */
private const val ExcerptChars = 220

/** Below this there is nothing to read, only a byline or a "read more". */
private const val MinExcerptChars = 80

private val SentenceEnd = charArrayOf('.', '!', '?')
