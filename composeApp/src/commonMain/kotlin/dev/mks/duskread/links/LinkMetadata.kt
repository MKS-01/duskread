package dev.mks.duskread.links

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/** What a page can tell us about itself. Both fields are optional — plenty of pages say neither. */
data class LinkMetadata(val title: String?, val description: String?)

/**
 * Each target brings its own engine (OkHttp, CIO, Darwin, JS) because no single one
 * covers all five.
 */
expect fun createHttpClient(): HttpClient

/**
 * Reads the page and pulls out its title. Deliberately a regex over the first stretch of
 * HTML rather than a parser.
 */
suspend fun fetchLinkMetadata(client: HttpClient, url: String): LinkMetadata {
    val html = client.get(url) {
        // Some publishers serve a stub or a challenge page to clients that send no
        // User-Agent at all.
        header(HttpHeaders.UserAgent, UserAgent)
        header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
    }.bodyAsText().take(MaxBytesScanned)

    return LinkMetadata(
        title = html.metaContent("og:title") ?: html.titleTag(),
        description = html.metaContent("og:description") ?: html.metaContent("description"),
    )
}

/** `<title>…</title>`, whitespace collapsed — HTML wraps titles across lines freely. */
private fun String.titleTag(): String? = TitlePattern.find(this)?.groupValues?.get(1)?.tidy()

/**
 * A `<meta>` tag's content, matched in either attribute order — `name` and `property`
 * before `content` or after it, which real pages do both ways.
 */
internal fun String.metaContent(key: String): String? {
    val escaped = Regex.escape(key)
    val patterns = listOf(
        """<meta[^>]+(?:property|name)\s*=\s*["']$escaped["'][^>]*content\s*=\s*["']([^"']*)["']""",
        """<meta[^>]+content\s*=\s*["']([^"']*)["'][^>]*(?:property|name)\s*=\s*["']$escaped["']""",
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        Regex(pattern, RegexOption.IGNORE_CASE).find(this)?.groupValues?.get(1)?.tidy()
    }
}

// Internal rather than private: FeedSync.kt cleans the same kind of tag soup out of RSS
// and Atom titles, and a second entity table would only drift from this one.
internal fun String.tidy(): String? {
    var text = replace(Whitespace, " ").trim()
    for ((entity, char) in Entities) text = text.replace(entity, char, ignoreCase = true)
    return text.decodeNumericEntities().takeIf { it.isNotBlank() }
}

/**
 * `&#8217;` and `&#x2019;`, which no table can enumerate — a publisher writing prose
 * reaches for a curly quote or a dash far more often than for anything named.
 */
private fun String.decodeNumericEntities(): String = replace(NumericEntity) { match ->
    val radix = if (match.groupValues[1].isEmpty()) 10 else 16
    val code = match.groupValues[2].toIntOrNull(radix)
    // Left as written rather than turned into a replacement glyph: an entity on screen
    // says what went wrong, a black diamond does not. Astral planes are out of reach of
    // a single Char, and nothing in prose needs one.
    if (code == null || code !in 1..0xFFFF) match.value else code.toChar().toString()
}

private val TitlePattern = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val Whitespace = Regex("""\s+""")

// The handful that actually turn up in titles.
private val Entities = listOf(
    "&amp;" to "&",
    "&#38;" to "&",
    "&lt;" to "<",
    "&gt;" to ">",
    "&quot;" to "\"",
    "&#39;" to "'",
    "&apos;" to "'",
    "&#x27;" to "'",
    "&nbsp;" to " ",
    "&mdash;" to "—",
    "&ndash;" to "–",
    "&hellip;" to "…",
    // Prose, unlike a title, is full of these.
    "&rsquo;" to "\u2019",
    "&lsquo;" to "\u2018",
    "&rdquo;" to "\u201D",
    "&ldquo;" to "\u201C",
)

private val NumericEntity = Regex("""&#(x?)([0-9a-fA-F]+);""", RegexOption.IGNORE_CASE)

internal const val UserAgent = "Mozilla/5.0 (compatible; DuskRead/1.0; +https://github.com/MKS-01)"

/** The head is all we need, and some pages are megabytes. */
private const val MaxBytesScanned = 200_000
