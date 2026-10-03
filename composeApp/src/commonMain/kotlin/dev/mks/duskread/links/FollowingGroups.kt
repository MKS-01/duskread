package dev.mks.duskread.links

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

/**
 * Following split three ways: blogs with something unsaved, most recent first; the rest
 * A–Z; and any no sync has reached yet, which are not "caught up" with anything. Shared
 * so both phones order a long list the same way.
 */
data class FollowingGroups(
    val fresh: List<DigestRow>,
    val caughtUp: List<DigestRow>,
    val unsynced: List<DigestRow>,
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
    )
}

/** "4h", "3d", "2w" — [savedAgo] without the "ago", for a row with little room. */
fun shortAgo(at: Long, now: Long): String = savedAgo(at, now).removeSuffix(" ago").replace("just now", "now")

private fun Feed.matches(query: String): Boolean = label.contains(query, ignoreCase = true) ||
    host.contains(query, ignoreCase = true) ||
    topic?.contains(query, ignoreCase = true) == true

/** Past this many blogs, search stays open rather than behind an icon. */
const val SearchAlwaysShownAt = 12
