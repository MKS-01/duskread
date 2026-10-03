package dev.mks.duskread.links

/**
 * A blog followed for its RSS or Atom feed, not for one article — the address itself is
 * the whole record.
 */
data class Feed(
    val id: String,
    val url: String,
    val addedAt: Long,
    /**
     * The publisher's own name, when something knew it — a Notion `Sources` row does, a
     * URL typed into Following does not.
     */
    val title: String? = null,
    /**
     * The subject this blog is about, curated in Notion's `Sources` table rather than
     * inferred here — "Security", "Languages & Tooling".
     */
    val topic: String? = null,
) {
    /** "arstechnica.com" — the label a topic row shows itself under. */
    val host: String
        get() = hostOf(url)

    /**
     * What to put on screen: the real name if there is one, the host if not, less the
     * `www.` / `feeds.` that say nothing about the blog.
     */
    val label: String
        // Unwrapped here too: names saved before the parser learned to strip CDATA.
        get() = title?.withoutCdata()?.takeIf { it.isNotBlank() } ?: host.removePrefix("www.").removePrefix("feeds.")

    /**
     * The name a grid tile has room for: "Android" for "Android Developers Blog",
     * "Spotify" for "Spotify Engineering". Falls back to [label] rather than to nothing.
     */
    val shortLabel: String
        get() {
            var name = label.substringBefore(" — ").substringBefore(" – ").substringBefore(" | ")
                .substringBefore(" - ").substringBefore(": ").trim()
            name = name.removePrefix("Articles on ").removePrefix("The blog of ").trim()
            while (true) {
                val trimmed = ShortLabelSuffixes.fold(name) { acc, word -> acc.removeSuffix(" $word") }
                    .removeSuffix("’s").removeSuffix("'s").trim()
                if (trimmed == name || trimmed.isEmpty()) break
                name = trimmed
            }
            return name.ifEmpty { label }
        }
}

// Words that describe the kind of site rather than which one it is.
private val ShortLabelSuffixes = listOf("Blog", "blog", "Weblog", "Engineering", "Developers", "Developer", "News", "Feed")
