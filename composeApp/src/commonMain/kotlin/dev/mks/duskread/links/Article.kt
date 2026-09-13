package dev.mks.duskread.links

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * A page reduced to the part worth reading: headline, lead image, body — with the
 * navigation, the share rail, the newsletter box.
 */
data class Article(
    val url: String,
    val title: String,
    val imageUrl: String?,
    val bodyHtml: String,
    val text: String,
)

/**
 * The article for [url], preferring what the feed already gave us.
 */
suspend fun loadArticle(
    client: HttpClient,
    url: String,
    feedTitle: String? = null,
    feedContent: String? = null,
): Article? = articleFromFeed(url, feedTitle, feedContent) ?: fetchArticle(client, url)

/** The feed's own copy of the post, if the feed carried the whole thing. */
fun articleFromFeed(url: String, title: String?, contentHtml: String?): Article? {
    if (contentHtml.isNullOrBlank()) return null

    val body = sanitiseHtml(contentHtml, url)
    val text = body.textOf()
    // A teaser is not an article.
    if (text.length < MinFeedArticleChars) return null

    return Article(
        url = url,
        title = title?.tidy() ?: titleFromUrl(url),
        imageUrl = body.firstImage(),
        bodyHtml = body,
        text = text,
    )
}

/** Fetches [url] and reduces it. Null when the page yields nothing that reads like an article. */
suspend fun fetchArticle(client: HttpClient, url: String): Article? {
    val html = runCatching {
        client.get(url) {
            header(HttpHeaders.UserAgent, UserAgent)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
        }.bodyAsText().take(MaxArticleBytes)
    }.getOrNull() ?: return null

    return extractArticle(html, url)
}

/**
 * Readability in miniature: throw away the tags that are never article body, score what
 * is left, take the winner.
 */
fun extractArticle(html: String, url: String): Article? {
    // Read before stripping: `og:` tags live in <head>, and <h1> is usually inside a
    // <header> that the strip below is about to remove.
    val title = html.metaContent("og:title")
        ?: html.firstMatch(HeadingPattern)?.textOf()?.takeIf { it.isNotBlank() }
        ?: html.firstMatch(DocTitlePattern)?.tidy()
        ?: titleFromUrl(url)
    val declaredImage = html.metaContent("og:image") ?: html.metaContent("twitter:image")

    val cleaned = html.stripNoise()
    val body = cleaned.bestBlock()?.let { sanitiseHtml(it, url) } ?: return null
    val text = body.textOf()
    if (text.length < MinArticleChars) return null

    return Article(
        url = url,
        title = title,
        imageUrl = declaredImage?.let { resolveAgainst(url, it) } ?: body.firstImage(),
        bodyHtml = body,
        text = text,
    )
}

/**
 * Removes what can never be the article, by name.
 */
private fun String.stripNoise(): String {
    var text = this
    for (pattern in NoisePatterns) text = text.replace(pattern, " ")
    return text
}

/**
 * The highest-scoring container, tightened.
 */
private fun String.bestBlock(): String? {
    val scored = scanBlocks(this)
        .map { block -> block to block.score(this) }
        .filter { (_, score) -> score > 0 }
        .sortedByDescending { (_, score) -> score }
        .take(MaxCandidates)
    val (best, bestScore) = scored.firstOrNull() ?: return null

    val tightest = scored
        .filter { (block, score) -> block.start >= best.start && block.end <= best.end && score >= bestScore * TightenTolerance }
        .minByOrNull { (block, _) -> block.end - block.start }
        ?.first ?: best

    return substring(tightest.start, tightest.end)
}

/** One container element and the span of its content. */
private class Block(val name: String, val attrs: String, val start: Int, val end: Int)

/**
 * Walks the tags keeping a stack, which is the one thing a regex cannot do: `<div>`
 * nests, so no pattern can say where a given one ends.
 */
private fun scanBlocks(html: String): List<Block> {
    val open = ArrayDeque<Block>()
    val blocks = mutableListOf<Block>()

    for (tag in TagPattern.findAll(html)) {
        val name = tag.groupValues[2].lowercase()
        if (name !in Containers) continue

        if (tag.groupValues[1] == "/") {
            while (open.isNotEmpty()) {
                val candidate = open.removeLast()
                if (candidate.name == name) {
                    blocks += Block(candidate.name, candidate.attrs, candidate.start, tag.range.first)
                    break
                }
            }
        } else if (!tag.groupValues[3].trimEnd().endsWith("/")) {
            open.addLast(Block(name, tag.groupValues[3], tag.range.last + 1, html.length))
        }
    }

    return blocks
}

/**
 * How much this block reads like prose.
 */
private fun Block.score(html: String): Double {
    // Cheapest possible reject first: a block whose *markup* is shorter than the text an
    // article needs cannot pass, and this runs for every <div> on the page.
    if (end - start < MinBlockChars) return 0.0

    val inner = html.substring(start, end)
    val text = inner.textOf()
    if (text.length < MinBlockChars) return 0.0

    val paragraphs = ParagraphPattern.findAll(inner).count()
    if (paragraphs < MinParagraphs) return 0.0

    val linked = LinkPattern.findAll(inner).sumOf { it.groupValues[1].textOf().length }
    val density = linked.toDouble() / text.length
    if (density > MaxLinkDensity) return 0.0

    var score = text.length * (1 - density) + paragraphs * ParagraphWeight
    val hint = attrs.lowercase()
    if (PositiveHint.containsMatchIn(hint)) score *= 1.25
    if (NegativeHint.containsMatchIn(hint)) score *= 0.4
    // <article> and <main> are the publisher saying it outright. Rare enough to be worth
    // trusting loudly when present.
    if (name == "article" || name == "main") score *= 1.6

    return score
}

/**
 * Keeps the tags that carry structure and drops the rest, unwrapping rather than deleting
 * so their text survives.
 */
private fun sanitiseHtml(fragment: String, baseUrl: String): String {
    val out = StringBuilder()
    var cursor = 0
    // A `<meta content="…">` sitting in the body is the publisher naming its social-card
    // image, and Blogger — which is a large share of the feeds anyone follows.
    val declared = MetaContentPattern.findAll(fragment).mapNotNull { it.groupValues[1].trim().takeIf(String::isNotEmpty) }.toSet()

    for (tag in TagPattern.findAll(fragment)) {
        out.append(fragment, cursor, tag.range.first)
        cursor = tag.range.last + 1

        val name = tag.groupValues[2].lowercase()
        if (name !in AllowedTags) continue

        if (tag.groupValues[1] == "/") {
            if (name !in VoidTags) out.append("</").append(name).append('>')
            continue
        }

        val attrs = tag.groupValues[3]
        when (name) {
            "a" -> attrs.attr(HrefPattern)?.let { out.append("""<a href="${resolveAgainst(baseUrl, it).escapeAttribute()}">""") }
            "img" -> attrs.imageSource()?.takeIf { it !in declared }?.let {
                val alt = attrs.attr(AltPattern).orEmpty().escapeAttribute()
                out.append("""<img src="${resolveAgainst(baseUrl, it).escapeAttribute()}" alt="$alt">""")
            }
            else -> out.append('<').append(name).append('>')
        }
    }

    out.append(fragment, cursor, fragment.length)
    return out.toString()
        .replace(EmptyParagraph, "")
        .replace(BreakRun, "<br>")
        .replace(BlankRun, "\n")
        .trim()
}

/**
 * Lazy-loading puts a placeholder — a spacer GIF, a blurred data URI — in `src` and the
 * real file in a `data-` attribute.
 */
private fun String.imageSource(): String? = attr(DataSrcPattern)
    ?: attr(SrcsetPattern)?.substringBefore(',')?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
    ?: attr(SrcPattern)

/** First image in an already-sanitised body, skipping the ones that are not pictures. */
private fun String.firstImage(): String? = ImgSrcPattern.findAll(this)
    .map { it.groupValues[1] }
    .firstOrNull { src -> NonPictureImage.containsMatchIn(src).not() }

/** Tags removed, entities decoded, whitespace collapsed — what the page actually says. */
internal fun String.textOf(): String = replace(TagPattern, " ").tidy().orEmpty()

private fun String.attr(pattern: Regex): String? = pattern.find(this)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

private fun String.firstMatch(pattern: Regex): String? = pattern.find(this)?.groupValues?.get(1)

private fun String.escapeAttribute(): String = replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;")

private val TagPattern = Regex("""<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>""")
private val Containers = setOf("div", "article", "main", "section")

// Unwrapped rather than kept: everything that shapes a paragraph, plus the two inline
// tags a body loses meaning without.
private val AllowedTags = setOf(
    "p", "h2", "h3", "h4", "ul", "ol", "li", "blockquote", "pre", "code",
    "em", "strong", "b", "i", "a", "img", "figure", "figcaption", "br", "hr",
)
private val VoidTags = setOf("img", "br", "hr")

private val NoisePatterns = listOf("script", "style", "noscript", "svg", "iframe", "form", "nav", "header", "footer", "aside", "button", "template")
    .map { tag -> Regex("""<$tag[^>]*>.*?</$tag>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)) } +
    Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)

private val ParagraphPattern = Regex("""<p[\s>]""", RegexOption.IGNORE_CASE)
private val LinkPattern = Regex("""<a[^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HeadingPattern = Regex("""<h1[^>]*>(.*?)</h1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val DocTitlePattern = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

private val PositiveHint = Regex("""article|content|post|entry|story|body|main|prose|markdown""")
private val NegativeHint = Regex("""comment|share|social|related|recommend|sidebar|widget|promo|newsletter|subscribe|banner|advert|sponsor|cookie|popup|modal|breadcrumb|pagination|meta""")

private val HrefPattern = Regex("""href\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
private val SrcPattern = Regex("""[^-]src\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
private val DataSrcPattern = Regex("""data-(?:src|original|lazy-src)\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
private val SrcsetPattern = Regex("""srcset\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
private val AltPattern = Regex("""alt\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
private val MetaContentPattern = Regex("""<meta[^>]+content\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
private val ImgSrcPattern = Regex("""<img[^>]+src="([^"]+)"""", RegexOption.IGNORE_CASE)

// Tracking pixels, spacers and icon sets, which are <img> tags but not pictures.
private val NonPictureImage = Regex("""\.svg|\.gif|/pixel|1x1|spacer|avatar|icon|logo|badge|emoji""", RegexOption.IGNORE_CASE)

// A paragraph holding only a line break or a non-breaking space is a publisher's spacer.
private val EmptyParagraph = Regex("""<p>(?:\s|<br>|&nbsp;)*</p>""", RegexOption.IGNORE_CASE)

// A stack of <br> is how a WYSIWYG editor writes a gap it has no style for.
private val BreakRun = Regex("""(?:<br>\s*){2,}""", RegexOption.IGNORE_CASE)
private val BlankRun = Regex("""\n{3,}""")

// A block has to be at least a few paragraphs before it is worth scoring, and the winner
// has to be a real read before it is worth showing.
private const val MinBlockChars = 250
private const val MinParagraphs = 2
private const val MinArticleChars = 400
private const val MinFeedArticleChars = 900
private const val MaxLinkDensity = 0.4
private const val ParagraphWeight = 40
private const val TightenTolerance = 0.9
private const val MaxCandidates = 40

/** Generous next to the 200KB a title scan needs — this one wants the whole body. */
private const val MaxArticleBytes = 800_000
