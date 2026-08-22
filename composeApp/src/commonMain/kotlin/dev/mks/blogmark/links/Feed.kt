package dev.mks.blogmark.links

/**
 * A blog followed for its RSS or Atom feed, not for one article — the address
 * itself is the whole record. What it has published is never stored here; a
 * sync reads the feed live and hands anything new to [LinkLibrary], which is
 * where an entry becomes a durable record.
 */
data class Feed(
    val id: String,
    val url: String,
    val addedAt: Long,
) {
    /** "arstechnica.com" — the label a topic row shows itself under. */
    val host: String
        get() = hostOf(url)
}
