package dev.mks.duskread.links

/**
 * Reading a blog's index page as if it were a feed.
 *
 * Plenty of publishers ship no feed at all — a marketing site with a `/blog`
 * section built in a page builder is the common shape, and no amount of
 * guessing at `/rss.xml` will conjure one. The page itself still lists every
 * post with its headline, its date and a link, which is exactly what a feed
 * entry is; the only difference is that the structure has to be inferred
 * rather than read off a tag name.
 *
 * So this is the last resort behind [discoverFeedSource], never the first
 * choice: a real feed is authoritative about what a post *is*, whereas
 * everything below is a heuristic that a site redesign can break. It exists so
 * that "follow this blog" means any blog, not only the ones whose publisher
 * remembered to emit XML.
 *
 * The inference has one idea behind it. A listing page links to its posts many
 * times over and to everything else a handful of times, and its posts share a
 * shape — `/blog/<slug>`, `/2024/03/<slug>` — that the navigation, the footer
 * and the category chips do not. So: collect every plausible link, group them
 * by that shape, and take the largest group. Nothing here knows the name of a
 * single publisher or platform.
 */
fun parseIndexPage(html: String, pageUrl: String): List<FeedEntry> {
    val page = html.withoutScripts()
    val indexPath = pathSegmentsOf(pageUrl)
    val host = hostOf(pageUrl)

    val links = mutableListOf<IndexLink>()
    // Where the previous link ended, so a card's window never reaches back
    // past its neighbour and steals the headline belonging to that one.
    var previousEnd = 0

    for (anchor in AnchorPattern.findAll(page)) {
        val (attrs, inner) = anchor.destructured
        val href = HrefAttr.find(attrs)?.groupValues?.get(1)?.trim() ?: continue
        val url = postUrl(pageUrl, href) ?: continue
        if (hostOf(url) != host) continue

        val segments = pathSegmentsOf(url)
        // A post lives below the page listing it. This is what keeps a
        // masthead's "/pricing" and "/about" out without naming any of them.
        if (segments.size <= indexPath.size) continue
        if (AssetSuffix.containsMatchIn(segments.last())) continue

        val card = page.substring(maxOf(previousEnd, anchor.range.first - CardWindow), anchor.range.first)
        previousEnd = anchor.range.last + 1

        // A link with nothing to call it is either chrome or an image wrapper
        // whose sibling carries the headline; either way it is not the entry.
        val title = titleOf(inner, attrs, card) ?: continue

        links += IndexLink(
            shape = segments.dropLast(1).joinToString("/") { segment -> if (segment.all(Char::isDigit)) "#" else segment },
            url = url,
            title = title,
            imageUrl = ImgSrcAttr.find(inner)?.groupValues?.get(1)?.let { resolveAgainst(pageUrl, it) },
            publishedAt = dateOf(inner, card),
        )
    }

    val posts = links
        .groupBy { it.shape }
        // Size decides; sharing a path with the page settles a tie, so a
        // "/blog" listing prefers "/blog/*" over an equally large "/news/*".
        .maxWithOrNull(compareBy({ it.value.size }, { sharedDepth(it.key, indexPath) }))
        ?.value
        ?.mergedByUrl()
        .orEmpty()

    // Two links of the same shape is a coincidence — a footer pair, a
    // prev/next control. Below that bar this page is not a listing, and
    // saying so lets the caller report an honest failure.
    if (posts.size < MinIndexPosts) return emptyList()

    val entries = posts.take(MaxIndexPosts).map { link ->
        FeedEntry(url = link.url, title = link.title, imageUrl = link.imageUrl, publishedAt = link.publishedAt)
    }

    // Document order is already newest-first on most listings, so re-ordering
    // is only worth it — and only trustworthy — when the page dated the bulk
    // of what it showed.
    return if (entries.count { it.publishedAt != null } > entries.size / 2) {
        entries.sortedByDescending { it.publishedAt ?: 0L }
    } else {
        entries
    }
}

/** One link on the page that might be a post, and what its surroundings say about it. */
private class IndexLink(
    val shape: String,
    val url: String,
    val title: String,
    val imageUrl: String?,
    val publishedAt: Long?,
)

/**
 * One entry per post, keeping the best of what each link to it said.
 *
 * A card is routinely three links to the same URL — the image, the headline,
 * and a "Read more" — and each knows a different part of the answer. Merging
 * rather than taking the first means the picture from the image link and the
 * headline from the headline link end up on the same entry.
 */
private fun List<IndexLink>.mergedByUrl(): List<IndexLink> {
    val byUrl = LinkedHashMap<String, IndexLink>()

    for (link in this) {
        val seen = byUrl[link.url]
        byUrl[link.url] = if (seen == null) {
            link
        } else {
            IndexLink(
                shape = seen.shape,
                url = seen.url,
                // Longest wins: a headline says more than "Read more", and
                // the length is the only thing separating them here.
                title = if (link.title.length > seen.title.length) link.title else seen.title,
                imageUrl = seen.imageUrl ?: link.imageUrl,
                publishedAt = seen.publishedAt ?: link.publishedAt,
            )
        }
    }

    return byUrl.values.toList()
}

/**
 * What to call the post, in the order the answers are trustworthy.
 *
 * A heading inside the link is the publisher saying it outright — that is the
 * card-wrapping-anchor shape. Failing that, the link's own text, unless it is
 * a call to action, which says nothing about the post. Then whatever the link
 * was labelled with for screen readers. Last, the heading the card put *above*
 * the link: the "invisible overlay covering the card" pattern, where the
 * anchor contains only the words "Read more".
 *
 * No fall back to the URL's slug. A slug can name any link on the page, so
 * accepting one would let every nav item qualify as a post and drown the real
 * group; a post with no readable headline is better dropped here and left to
 * [titleFromUrl] downstream, where it is one entry rather than the grouping.
 */
private fun titleOf(inner: String, attrs: String, card: String): String? {
    val heading = HeadingPattern.find(inner)?.groupValues?.get(1)?.textOf()
    if (!heading.isNullOrBlank()) return heading.trimTitle()

    val text = inner.textOf().trimTitle()
    if (text != null && !CallToAction.matches(text) && text.length >= MinTitleChars) return text

    val labelled = LabelAttr.find(attrs)?.groupValues?.get(1)?.tidy()?.trimTitle()
    if (labelled != null && labelled.length >= MinTitleChars) return labelled

    return HeadingPattern.findAll(card).lastOrNull()?.groupValues?.get(1)?.textOf()?.trimTitle()
        ?.takeIf { it.length >= MinTitleChars }
}

/**
 * A headline as a list wants it: no leading "Read more", no wrapping quotes,
 * and short enough to be a title rather than the excerpt it sat next to.
 */
private fun String.trimTitle(): String? = removeCallToAction()
    .trim { it.isWhitespace() || it in TitleTrim }
    .take(MaxTitleChars)
    .trim()
    .takeIf { it.isNotBlank() }

private fun String.removeCallToAction(): String = CallToActionPrefix.find(this)?.let { drop(it.value.length) } ?: this

/**
 * When the card says the post went out.
 *
 * `<time datetime>` is the machine-readable answer and worth trying on both
 * the link and the card around it. Everything else is a date printed for
 * people — "March 4, 2024" — which is worth reading precisely because the
 * listing pages that have no feed are also the ones that date every card.
 */
private fun dateOf(inner: String, card: String): Long? {
    TimeAttr.find(inner)?.groupValues?.get(1)?.let(::parsePostDate)?.let { return it }
    TimeAttr.find(card)?.groupValues?.get(1)?.let(::parsePostDate)?.let { return it }

    // Last match, not first: on a card that carries both, the date sits
    // nearest the link, and whatever came earlier belongs to the piece above.
    val printed = WrittenDate.findAll(card.textOf()).lastOrNull() ?: WrittenDate.find(inner.textOf())
    return printed?.value?.let(::parsePostDate)
}

/**
 * The absolute URL a link points at, or null when it does not point at a page
 * on the web at all — an in-page anchor, a `mailto:`, a script handler.
 *
 * The fragment and any trailing slash go, because "/blog/x", "/blog/x/" and
 * "/blog/x#top" are one post appearing three times on the same page, and every
 * duplicate costs the group it belongs to nothing but noise.
 */
private fun postUrl(pageUrl: String, href: String): String? {
    if (href.isEmpty() || href.startsWith("#") || NonPageScheme.containsMatchIn(href)) return null

    val resolved = resolveAgainst(pageUrl, href).substringBefore('#')
    if (!resolved.startsWith("http")) return null

    return resolved.trimEnd('/').takeIf { it.isNotBlank() }
}

/** How many leading path segments two paths agree on. */
private fun sharedDepth(shape: String, indexPath: List<String>): Int {
    val segments = shape.split('/')
    var depth = 0
    while (depth < segments.size && depth < indexPath.size && segments[depth] == indexPath[depth]) depth++
    return depth
}

/**
 * Inline script and style go before anything is counted.
 *
 * Not the same list [extractArticle] strips: `<nav>`, `<header>` and
 * `<footer>` stay, because a listing card is free to be built out of them —
 * a `<header>` holding the headline above the link is exactly the shape
 * [titleOf] reaches for last. The chrome they also contain is handled by
 * grouping instead, which does not need to know what a tag is called.
 */
private fun String.withoutScripts(): String {
    var text = this
    for (pattern in ScriptPatterns) text = text.replace(pattern, " ")
    return text
}

private val ScriptPatterns = listOf("script", "style", "noscript", "svg", "template")
    .map { tag -> Regex("""<$tag[^>]*>.*?</$tag>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)) } +
    Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)

// Non-greedy, so a link ends at the first </a>: anchors cannot nest, and the
// inner group is the whole card on the sites that wrap one in a link.
private val AnchorPattern = Regex("""<a\b([^>]*)>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val HeadingPattern = Regex("""<h[1-4][^>]*>(.*?)</h[1-4]>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

private val HrefAttr = Regex("""href\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
private val LabelAttr = Regex("""(?:aria-label|title)\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val ImgSrcAttr = Regex("""<img[^>]+src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val TimeAttr = Regex("""<time[^>]+datetime\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

private val NonPageScheme = Regex("""^(?:mailto|tel|javascript|data|file):""", RegexOption.IGNORE_CASE)

// A file the listing links to rather than a post it lists. Matched on the last
// path segment only, so a post whose slug happens to contain ".js" survives.
private val AssetSuffix = Regex("""\.(?:jpe?g|png|gif|svg|webp|avif|ico|pdf|zip|gz|mp3|mp4|css|js|json|xml|rss|atom)$""", RegexOption.IGNORE_CASE)

// The words a card uses when the link itself is not the headline.
private val CallToAction = Regex(
    """(?:read|view|see|learn)(?:\s+(?:more|post|article|now|it))?|continue reading|more|details|link|→""",
    RegexOption.IGNORE_CASE,
)
private val CallToActionPrefix = Regex("""^(?:read more|read the (?:post|article)|continue reading|learn more|read)\s*[:–—-]?\s*""", RegexOption.IGNORE_CASE)

// "March 4, 2024", "4 Mar 2024", "2024-03-04" — the three ways a card prints
// a date. Anything else falls through to no date, which the list handles.
private val WrittenDate = Regex(
    """\b(?:[A-Z][a-z]{2,8}\.?\s+\d{1,2},?\s+\d{4}|\d{1,2}\s+[A-Z][a-z]{2,8}\.?,?\s+\d{4}|\d{4}-\d{2}-\d{2})\b""",
)

// Quotes and dashes a publisher wraps a headline in — "Read more “Title”".
private val TitleTrim = setOf('"', '\'', '“', '”', '‘', '’', '·', '|', '-', '–', '—', ':')

// How far back from a link to look for the headline and date belonging to it.
// A card's markup is a few hundred characters of wrappers; a couple of
// thousand covers the verbose ones without reaching the card above.
private const val CardWindow = 2_500

// Long enough not to be a chip or a byline. Short titles exist, but not on the
// links this has to tell apart from real posts.
private const val MinTitleChars = 15
private const val MaxTitleChars = 180

// Below this the page is not a listing; above it, the same cap a feed gets,
// since the list this feeds is trimmed to [EntriesPerFeed] anyway.
private const val MinIndexPosts = 3
private const val MaxIndexPosts = 40
