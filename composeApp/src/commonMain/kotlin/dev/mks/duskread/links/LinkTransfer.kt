package dev.mks.duskread.links

/*
 * Getting a reading list into the app, as text.
 *
 * The carrier is the clipboard, because it is universal: a list of links
 * pastes in from wherever it already lives — a notes app, a bookmarks dump, a
 * mail from a friend — without either end needing to agree a format first.
 *
 * There used to be an export beside this, writing the library back out as
 * Markdown. It was removed once Notion became where a reading list is kept:
 * a second, hand-driven copy of the same list is a thing to keep in sync
 * rather than a safety net.
 *
 * [parseImport] is deliberately permissive. It pulls links out of *any* text,
 * including the Markdown the old export produced, so nothing already saved
 * out of the app has become unreadable. Anything it recognises as a title it
 * keeps, anything it does not it drops, so the worst outcome of pasting the
 * wrong thing is nothing happening.
 */

/** One link recovered from pasted text, before the library has decided what to do with it. */
data class ImportedLink(
    val url: String,
    /** Null when the line was a bare URL and the title will have to be fetched. */
    val title: String?,
    val read: Boolean,
)

/** What an import did, so the screen can say so rather than silently changing under the reader. */
data class ImportSummary(
    val found: Int,
    val added: Int,
) {
    val duplicates: Int
        get() = found - added
}

/**
 * Every link the text contains, in the order it contains them.
 *
 * Duplicates within the paste are collapsed here rather than left to the
 * library, so a summary of "found 24" means twenty-four distinct articles and
 * not twenty-four lines.
 */
fun parseImport(text: String): List<ImportedLink> {
    var inReadSection = false
    val seen = mutableSetOf<String>()

    return text.lineSequence().mapNotNull { rawLine ->
        val line = rawLine.trim()

        // A "## Read" heading marks everything below it as read, which is how
        // an export round-trips. Lines carrying their own "[x]" do not need it.
        if (line.startsWith("#")) {
            inReadSection = line.trimStart('#', ' ').equals("Read", ignoreCase = true)
            return@mapNotNull null
        }
        if (line.isEmpty()) return@mapNotNull null

        // The raw token, not the normalised URL, is what the title is measured
        // against: a line may well say "example.com/x" where the saved link
        // will say "https://example.com/x".
        val token = extractUrl(line) ?: return@mapNotNull null
        val url = normaliseUrl(token)
        if (!seen.add(url.lowercase())) return@mapNotNull null

        val marked = TickMarks.any { line.contains(it, ignoreCase = true) }
        ImportedLink(
            url = url,
            title = titleFrom(line, token),
            read = marked || inReadSection,
        )
    }.toList()
}

/**
 * The prose before the URL, if there is any worth keeping.
 *
 * Everything is stripped conservatively: list bullets and checkboxes because
 * every Markdown source has them, a trailing dash or bullet because that is
 * our own separator, and nothing else. A line whose text is only decoration
 * yields null and the page gets fetched instead, which is the better answer
 * than a title of "-".
 */
private fun titleFrom(line: String, token: String): String? {
    val before = line.substringBefore(token).let { if (it.length == line.length) "" else it }

    var title = before.trim()
    ListMarkers.forEach { marker -> title = title.removePrefix(marker).trim() }
    TickMarks.forEach { mark -> title = title.removePrefix(mark).trim() }
    title = title.trim('—', '–', '-', '·', ':', '|', ' ')

    // A bare "https://…" as its own title is what a line like "- https://x/y"
    // leaves behind on some sources; it tells the reader nothing the row does
    // not already show.
    return title.takeIf { it.isNotBlank() && it.length > 1 && !looksLikeUrl(it) }
}

private val ListMarkers = listOf("- ", "* ", "+ ", "• ")
private val TickMarks = listOf("[x]", "[X]", "[✓]")
