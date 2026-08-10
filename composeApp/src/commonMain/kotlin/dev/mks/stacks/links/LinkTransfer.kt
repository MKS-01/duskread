package dev.mks.stacks.links

/*
 * Getting the reading list out of the app and back in, as text.
 *
 * The store is a private key/value blob on one device, which makes the saved
 * links the only thing in the app that cannot be recovered if that device is
 * wiped — the readback library syncs, but a saved link has no copy anywhere
 * else. So there has to be a way out.
 *
 * The carrier is the clipboard and the format is a Markdown list, for the same
 * reason: both are universal. Text pastes into whatever the reader already
 * keeps notes in, survives being mailed to yourself, and can be read and
 * hand-edited without this app present — which a file format, however tidy,
 * cannot promise a year from now.
 *
 * [parseImport] is deliberately far more permissive than [exportLinks] is
 * strict. It pulls links out of *any* text: an export from here, a bookmarks
 * dump, a list a friend sent, a page of prose with three URLs in it. Anything
 * it recognises as a title it keeps, and anything it does not it drops, so the
 * worst outcome of pasting the wrong thing is nothing happening.
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
 * The whole list as Markdown, newest first, unread above read.
 *
 * The header line is a comment to the parser and a label to a human — pasted
 * into a notes app six months later it still says what this is and how much of
 * it there was.
 */
fun exportLinks(links: List<SavedLink>): String {
    val (read, unread) = links.partition { it.read }
    val header = "# Stacks — ${links.size} saved, ${unread.size} unread"

    val body = buildList {
        add(header)
        if (unread.isNotEmpty()) {
            add("")
            unread.forEach { add(it.asLine()) }
        }
        if (read.isNotEmpty()) {
            add("")
            add("## Read")
            read.sortedByDescending { it.readAt ?: it.savedAt }.forEach { add(it.asLine()) }
        }
    }

    return body.joinToString("\n")
}

// The em dash separates title from URL because it is the one character that
// reads as punctuation to a person and never appears inside a URL, so the
// parser can split on it without escaping anything.
private fun SavedLink.asLine(): String = if (read) "- [x] $title — $url" else "- $title — $url"

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
