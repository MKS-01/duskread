package dev.mks.blogmark.links

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/** What a page can tell us about itself. Both fields are optional — plenty of pages say neither. */
data class LinkMetadata(val title: String?, val description: String?)

/**
 * Each target brings its own engine (OkHttp, CIO, Darwin, JS) because no
 * single one covers all five. The client is created once and kept for the
 * app's lifetime rather than per fetch: engines own a connection pool and a
 * thread pool, and building one per pasted URL would be the expensive part of
 * the operation.
 */
expect fun createHttpClient(): HttpClient

/**
 * Reads the page and pulls out its title.
 *
 * Deliberately a regex over the first stretch of HTML rather than a parser.
 * The whole job is three well-known tags that sit in `<head>`, real-world HTML
 * is too malformed for a strict parser to be an advantage, and a parser would
 * be a second dependency for a feature that already added one.
 *
 * Open Graph wins over `<title>` where both exist: `og:title` is the headline
 * as the publisher wants it shared, whereas `<title>` usually carries the site
 * name and section as well ("How heaps work | Example Blog").
 */
suspend fun fetchLinkMetadata(client: HttpClient, url: String): LinkMetadata {
    val html = client.get(url) {
        // Some publishers serve a stub or a challenge page to clients that
        // send no User-Agent at all.
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
 * A `<meta>` tag's content, matched in either attribute order — `name` and
 * `property` before `content` or after it, which real pages do both ways.
 */
private fun String.metaContent(key: String): String? {
    val escaped = Regex.escape(key)
    val patterns = listOf(
        """<meta[^>]+(?:property|name)\s*=\s*["']$escaped["'][^>]*content\s*=\s*["']([^"']*)["']""",
        """<meta[^>]+content\s*=\s*["']([^"']*)["'][^>]*(?:property|name)\s*=\s*["']$escaped["']""",
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        Regex(pattern, RegexOption.IGNORE_CASE).find(this)?.groupValues?.get(1)?.tidy()
    }
}

// Internal rather than private: FeedSync.kt cleans the same kind of tag soup
// out of RSS and Atom titles, and a second entity table would only drift from
// this one.
internal fun String.tidy(): String? {
    var text = replace(Whitespace, " ").trim()
    for ((entity, char) in Entities) text = text.replace(entity, char, ignoreCase = true)
    return text.takeIf { it.isNotBlank() }
}

private val TitlePattern = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val Whitespace = Regex("""\s+""")

// The handful that actually turn up in titles. A full entity table would be
// pages of it for no gain — anything unlisted survives as its escape, which is
// ugly but readable, and the title is editable anyway.
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
)

internal const val UserAgent = "Mozilla/5.0 (compatible; Blogmark/1.0; +https://github.com/MKS-01)"

/** The head is all we need, and some pages are megabytes. */
private const val MaxBytesScanned = 200_000
