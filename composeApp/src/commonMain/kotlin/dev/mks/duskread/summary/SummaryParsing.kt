package dev.mks.duskread.summary

/*
 * Turning what the engine returns into what the panel draws.
 *
 * Pure functions, kept apart from the engine for the same reason
 * `extractArticle` is kept apart from its fetch: everything that decides
 * whether this feature is any good is here, and none of it needs a device.
 */

internal const val SummaryWordBudget = 2_500

/** The first [maxWords] words, marked as cut so the model knows the piece continues. */
internal fun truncateWords(text: String, maxWords: Int): String {
    val words = text.split(Whitespace).filter { it.isNotBlank() }
    if (words.size <= maxWords) return text

    return words.take(maxWords).joinToString(" ") + " […]"
}

/**
 * Reads back whatever the engine wrote, as prose.
 *
 * The engine returns a bulleted list, always — that is what it was built to
 * emit — so a list is the input here, not a failure. Markers, labels and the
 * article's own title come off, and the fragments are joined into a paragraph
 * with each closed so the seam does not show.
 *
 * Forgiving otherwise: stray emphasis, a lead-in above the answer, numbers
 * instead of dashes are all recoverable. An empty answer is not, and is the
 * only case this returns null for.
 */
internal fun parseSummary(raw: String, url: String, title: String, model: String, now: Long, length: SummaryLength): ArticleSummary? {
    val text = raw.replace("\r\n", "\n").split('\n')
        // Order matters, and getting it wrong cost a bug: the answer arrives
        // as `GIST: <title> — …`, so testing for the title while the label is
        // still attached matches nothing and the title reaches the panel.
        .map { it.trim().removeEmphasis().withoutBulletMarker().withoutLabel() }
        // The title is already on screen above this, so restating it spends
        // the first line on something the reader can see.
        .filterNot { it.isTitle(title) }
        .map { it.withoutTitlePrefix(title) }
        // A lead-in ("Here is the summary:") is not part of the summary.
        .filterNot { it.isBlank() || it.endsWith(":") }
        // Flattened into one paragraph, each fragment closed so the seam
        // does not show.
        .joinToString(" ") { it.closed() }
        .trim()

    if (text.isBlank()) return null

    return ArticleSummary(url = url, text = text, model = model, createdAt = now, length = length)
}

/** Bullets carry no full stop, and three unclosed ones read as one lost sentence. */
private fun String.closed(): String = if (isEmpty() || last() in SentenceEnd) this else "$this."

/** `GIST:`, `Summary:` and the other labels a model writes before answering. */
private fun String.withoutLabel(): String = replaceFirst(Label, "").trim()

/**
 * Compared on letters and digits alone: a restated title is rarely
 * character-identical — title-cased, or missing the site name, or with a full
 * stop the original never had.
 */
private fun String.isTitle(title: String): Boolean {
    val key = title.titleKey()
    return key.length >= MinTitleKey && titleKey() == key
}

/** The title used as an opening clause, trimmed back to the sentence after it. */
private fun String.withoutTitlePrefix(title: String): String {
    val key = title.titleKey()
    if (key.length < MinTitleKey) return this

    // Walk until the title's letters are covered, then drop what joins them.
    var covered = 0
    for ((index, character) in withIndex()) {
        if (character.isLetterOrDigit()) {
            if (covered >= key.length || character.lowercaseChar() != key[covered]) return this
            covered++
        }
        if (covered == key.length) return substring(index + 1).trimStart(' ', ':', '—', '–', '-', '.', ',').ifBlank { "" }
    }
    return this
}

private fun String.titleKey(): String = filter { it.isLetterOrDigit() }.lowercase()

// Below this a "title" is a word or two, and matching would eat a real sentence.
private const val MinTitleKey = 12

private val Label = Regex("""^(?:gist|summary|tl;?dr|overview)\s*[:\u2014-]\s*""", RegexOption.IGNORE_CASE)

private val SentenceEnd = charArrayOf('.', '!', '?', '…')

private fun String.withoutBulletMarker(): String = replaceFirst(BulletMarker, "").trim()

/** The panel draws plain text, so `**Netflix**` would arrive with its asterisks showing. */
private fun String.removeEmphasis(): String = replace(Emphasis) { match ->
    // Two alternatives, one of which is always empty — asterisks captured in
    // the first group, underscores in the second.
    match.groupValues[1].ifEmpty { match.groupValues[2] }
}

private val Whitespace = Regex("""\s+""")
private val BulletMarker = Regex("""^\s*(?:[-*•‣–]|\d+[.)])\s+""")
private val Emphasis = Regex("""\*{1,2}([^*]+)\*{1,2}|_{1,2}([^_]+)_{1,2}""")
