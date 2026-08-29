package dev.mks.duskread.links

/**
 * An article saved from outside the app — pasted in, or shared to DuskRead from
 * a browser.
 *
 * This is the one thing in the app the *user* writes. Readback is a read-only
 * view of a synced audio library, and a followed feed is re-fetched from its
 * publisher — a saved link is the only record the reader made themselves. It
 * has a copy in Notion once the reading-list sync has run, but the phone is
 * where it is created and it has to stand on its own without a network.
 *
 * [title] and [description] start as whatever the URL alone can tell us and
 * are replaced once the page has been fetched, so a link is usable the instant
 * it is saved and improves a second later rather than blocking on the network.
 */
data class SavedLink(
    val id: String,
    val url: String,
    val title: String,
    val description: String? = null,
    val savedAt: Long,
    /** When it was read, or null if it has not been. The record outlives the reading. */
    val readAt: Long? = null,
    /** False until the page itself has answered; the title is a guess from the URL until then. */
    val fetched: Boolean = false,
    /** Whether the last fetch attempt couldn't reach the page — [fetched] is still true, so the loop won't retry it on its own. */
    val fetchFailed: Boolean = false,
    /**
     * When anything about this link last changed here.
     *
     * Exists for one job: deciding who wins when the same link changed on the
     * phone and in Notion. [savedAt] says when it arrived and [readAt] is null
     * on exactly the rows that need comparing, so neither can answer it.
     * Compared against Notion's `last_edited_time`, newest taking the row.
     */
    val changedAt: Long = 0L,
    /**
     * The subject, when Notion supplied one.
     *
     * The ranking otherwise infers a topic by matching [host] against a
     * followed feed, which works for a blog post and fails for exactly the
     * newsletters that arrive by mail and are filed in Notion rather than
     * fetched from a feed.
     */
    val topic: String? = null,
) {
    val read: Boolean
        get() = readAt != null

    /** "arstechnica.com" — the one piece of provenance worth showing in a list. */
    val host: String
        get() = hostOf(url)
}

/** The bare host out of any URL — shared with [Feed], which wants the same reduction. */
fun hostOf(url: String): String = url
    .substringAfter("://", url)
    .substringBefore('/')
    .substringBefore('?')
    .removePrefix("www.")

/**
 * Best guess at a title before the network answers: the last meaningful path
 * segment, un-slugged. "…/blog/how-heaps-actually-work?ref=x" → "How heaps
 * actually work". Falls back to the host, which is never wrong, only vague.
 */
fun titleFromUrl(url: String): String {
    val path = url.substringAfter("://", url).substringBefore('?').substringBefore('#')
    val slug = path.split('/')
        .drop(1)
        .lastOrNull { segment -> segment.isNotBlank() && segment.any { it.isLetter() } }
        ?.substringBeforeLast('.') // strip .html, .php and friends

    if (slug.isNullOrBlank()) return path.substringBefore('/').removePrefix("www.")

    val words = slug.replace('-', ' ').replace('_', ' ').trim()
    return words.replaceFirstChar { it.uppercaseChar() }
}

/**
 * Whether this looks enough like a link to save. Deliberately permissive: the
 * cost of accepting something odd is one bad row the user can delete, and the
 * cost of rejecting a real URL is that the feature appears broken.
 */
fun looksLikeUrl(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return false

    val host = trimmed.substringAfter("://", trimmed).substringBefore('/')
    return host.contains('.') && host.substringAfterLast('.').length >= 2
}

/** Adds the scheme a pasted "example.com/x" is missing, so it can actually be fetched or opened. */
fun normaliseUrl(text: String): String {
    val trimmed = text.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
}
