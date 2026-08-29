package dev.mks.duskread.links

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
    /**
     * The publisher's own name, when something knew it — a Notion `Sources`
     * row does, a URL typed into Following does not. Null rather than
     * defaulted to the host so the two cases stay distinguishable.
     */
    val title: String? = null,
) {
    /** "arstechnica.com" — the label a topic row shows itself under. */
    val host: String
        get() = hostOf(url)

    /** What to put on screen: the real name if there is one, the host if not. */
    val label: String
        get() = title?.takeIf { it.isNotBlank() } ?: host
}
