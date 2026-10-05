package dev.mks.duskread.links

import kotlin.random.Random

/**
 * One followed blog as the Following list shows it. [posts] is already narrowed by any
 * search, so a blog found only through one of its posts shows just that post.
 */
data class DigestRow(
    val feed: Feed,
    val posts: List<FeedPost>,
    /** Posts from this week not yet saved — what "3 new" counts. */
    val newCount: Int,
    val lastPostAt: Long?,
)

/** One card in Following's slider: a post, the blog it came from, and its opening. */
data class PostPick(val feed: Feed, val post: FeedPost, val excerpt: String?, val minutes: Int)

/**
 * Following split three ways: blogs with something unsaved, most recent first; the rest
 * A–Z; and any no sync has reached yet, which are not "caught up" with anything. Shared
 * so both phones order a long list the same way.
 */
data class FollowingGroups(
    val fresh: List<DigestRow>,
    val caughtUp: List<DigestRow>,
    val unsynced: List<DigestRow>,
    /** Recent unsaved posts for the slider under the cloud; empty while searching. */
    val picks: List<PostPick> = emptyList(),
) {
    val isEmpty: Boolean get() = fresh.isEmpty() && caughtUp.isEmpty() && unsynced.isEmpty()
}

fun followingGroups(feeds: List<Feed>, cache: FeedPostCache, links: LinkLibrary, query: String, now: Long): FollowingGroups {
    // Built once: `isSaved` walks the whole library, and a long list asks it hundreds of
    // times.
    val saved = links.links.mapTo(HashSet()) { canonicalUrl(it.url) }
    val rows = feeds.mapNotNull { feed ->
        val all = cache.postsByFeed[feed.id].orEmpty()
        val posts = when {
            query.isBlank() || feed.matches(query) -> all
            else -> all.filter { it.title.contains(query, ignoreCase = true) }.ifEmpty { return@mapNotNull null }
        }
        DigestRow(
            feed = feed,
            posts = posts,
            // This week's, not every unsaved post: a feed's last fifteen are all unsaved the
            // day it is followed, and a blog quiet for months is not news.
            newCount = posts.count { post ->
                val at = post.publishedAt ?: return@count false
                now - at in 0..LatestWindowMs && canonicalUrl(post.url) !in saved
            },
            lastPostAt = posts.mapNotNull { it.publishedAt }.maxOrNull(),
        )
    }
    val (withPosts, unsynced) = rows.partition { it.posts.isNotEmpty() }
    val (fresh, caughtUp) = withPosts.partition { it.newCount > 0 }
    return FollowingGroups(
        fresh = fresh.sortedByDescending { it.lastPostAt ?: 0L },
        caughtUp = caughtUp.sortedBy { it.feed.label.lowercase() },
        unsynced = unsynced.sortedBy { it.feed.label.lowercase() },
        picks = if (query.isNotBlank()) emptyList() else pickPosts(withPosts, saved, now),
    )
}

/** "4h", "3d", "2w" — [savedAgo] without the "ago", for a row with little room. */
fun shortAgo(at: Long, now: Long): String = savedAgo(at, now).removeSuffix(" ago").replace("just now", "now")

private fun Feed.matches(query: String): Boolean = label.contains(query, ignoreCase = true) ||
    host.contains(query, ignoreCase = true) ||
    topic?.contains(query, ignoreCase = true) == true

/**
 * One recent unsaved post from each of a few blogs, at random. Each blog draws from its
 * own seed of blog and day, so the slider holds still all day and saving one post
 * replaces that blog's card without reshuffling the others.
 */
private fun pickPosts(rows: List<DigestRow>, saved: Set<String>, now: Long): List<PostPick> {
    val day = now / DayMs
    return rows.mapNotNull { row ->
        val random = Random(day * 31 + row.feed.id.hashCode())
        // Drawn before the post, so a blog's place in the order does not move when its
        // own candidates change.
        val rank = random.nextInt()
        val candidates = row.posts.filter { post ->
            val at = post.publishedAt ?: return@filter false
            now - at in 0..PickWindowMs && canonicalUrl(post.url) !in saved
        }
        candidates.randomOrNull(random)?.let { post ->
            val minutes = estimatedMinutes(post.words, post.content).toInt().coerceAtLeast(1)
            // Longer than a Home card's: a card stretched to the bar has the room for it.
            val excerpt = post.content?.let { excerptOf(it, maxChars = PickExcerptChars) }
            rank to PostPick(row.feed, post, excerpt, minutes)
        }
    }.sortedBy { it.first }.take(PicksShown).map { it.second }
}

/** A taste under the cloud, not a second feed: the blogs stay the point of the tab. */
private const val PicksShown = 6
private const val PickExcerptChars = 1400
private const val PickWindowMs = 14L * 24 * 60 * 60 * 1000
private const val DayMs = 24L * 60 * 60 * 1000

/** Past this many blogs, search stays open rather than behind an icon. */
const val SearchAlwaysShownAt = 12

/**
 * How loud a blog's name is in the Following cloud, 0 (quiet) to 3. Shared so both phones
 * size a blog alike; indexes `DesignTokens.CloudNameSizes`.
 */
fun cloudTier(row: DigestRow): Int = when {
    row.newCount >= 10 -> 3
    row.newCount >= 4 -> 2
    row.newCount > 0 -> 1
    else -> 0
}
