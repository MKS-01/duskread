package dev.mks.blogmark.links

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/**
 * One post as the feed describes it.
 *
 * [content] is the publisher's own markup for the post where the feed carries
 * it — `<content:encoded>` on RSS, `<content>` on Atom. Plenty of feeds ship
 * only a teaser there, so this is a candidate for the reader rather than a
 * promise; [articleFromFeed] is what decides whether it is the whole post.
 */
data class FeedEntry(
    val url: String,
    val title: String?,
    val content: String? = null,
    val imageUrl: String? = null,
)

/**
 * Reads [url] as RSS or Atom and pulls out its entries.
 *
 * Regex over tags, the same call [fetchLinkMetadata] makes: a feed is
 * machine-generated XML, well-formed often enough that a real parser buys
 * nothing a five-target KMP project doesn't already pay for as a dependency,
 * and the two formats only need two tag shapes told apart.
 */
suspend fun fetchFeed(client: HttpClient, url: String): List<FeedEntry> {
    val xml = client.get(url) {
        header(HttpHeaders.UserAgent, UserAgent)
        header(HttpHeaders.Accept, "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
    }.bodyAsText().take(MaxBytesScanned)

    return parseFeed(xml)
}

/**
 * RSS wraps posts in `<item>`, Atom in `<entry>` — never both in one
 * document, so trying `<item>` first and falling back to `<entry>` picks the
 * right shape without needing to read the root element.
 */
fun parseFeed(xml: String): List<FeedEntry> {
    val blocks = ItemPattern.findAll(xml).map { it.groupValues[1] }.toList()
        .ifEmpty { EntryPattern.findAll(xml).map { it.groupValues[1] }.toList() }

    return blocks.mapNotNull { block ->
        val url = RssLinkPattern.find(block)?.groupValues?.get(1)?.tidy() ?: block.atomEntryUrl()
        if (url.isNullOrBlank()) return@mapNotNull null

        val content = ContentPatterns.firstNotNullOfOrNull { pattern -> pattern.find(block)?.groupValues?.get(1) }?.unescapeMarkup()

        FeedEntry(
            url = url,
            title = FeedTitlePattern.find(block)?.groupValues?.get(1)?.tidy(),
            content = content?.takeIf { it.isNotBlank() },
            imageUrl = block.entryImage()?.let { resolveAgainst(url, it) },
        )
    }
}

/**
 * An Atom entry can carry several `<link>` tags — Blogger's feed puts out
 * `replies`, `edit` and `self` alongside the one a reader would actually
 * click. `rel="alternate"` is that one, so it has to be matched by name
 * rather than taken as the first `<link>` in the block; only a feed that
 * omits `rel` (legal — it defaults to alternate) falls through to that.
 */
private fun String.atomEntryUrl(): String? = AtomAlternateLinkPattern.firstNotNullOfOrNull { it.find(this)?.groupValues?.get(1)?.tidy() }
    ?: AtomAnyLinkPattern.find(this)?.groupValues?.get(1)?.tidy()

/**
 * A feed's own copy of the post, unwrapped.
 *
 * Two encodings, one meaning: a feed either wraps the post's HTML in CDATA,
 * where it is already literal and must be left alone, or escapes it into the
 * XML text, where every tag arrives as `&lt;p&gt;` and has to be turned back.
 * Running the entity pass over CDATA content would corrupt any post that
 * legitimately *displays* an escaped tag — a code sample about HTML — so the
 * two cases stay separate rather than both being run through the same filter.
 */
private fun String.unescapeMarkup(): String {
    CdataPattern.find(this)?.let { return it.groupValues[1] }

    return replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&#x27;", "'")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&") // last: everything above may itself have been double-escaped through it
}

/**
 * The post's picture as the feed states it, before falling back to the first
 * one in the body. The three tags are three different generators' answers to
 * the same question and no feed emits more than one of them.
 */
private fun String.entryImage(): String? = EntryImagePatterns.firstNotNullOfOrNull { pattern ->
    pattern.find(this)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
}

/**
 * Resolves whatever the reader typed — a blog's homepage, or the feed
 * address itself — to an actual RSS/Atom URL.
 *
 * Asking for the exact feed address up front is how a follow can end up
 * pointed at an HTML page with nothing to parse: a blog's homepage is what
 * people actually have on hand, not its `/rss.xml`. So the address itself is
 * tried first — most people who *do* paste a real feed URL should not pay for
 * a discovery round trip — and only on a feed that parses to nothing does
 * this fall back to reading the page for the `<link rel="alternate">` a
 * publisher points feed readers at, then a handful of conventional paths.
 * Whatever is tried last, successful or not, is what gets followed — a
 * result the reader can inspect and fix beats silently failing.
 */
suspend fun discoverFeedUrl(client: HttpClient, rawUrl: String): String {
    if (!runCatching { fetchFeed(client, rawUrl) }.getOrNull().isNullOrEmpty()) return rawUrl

    val html = runCatching {
        client.get(rawUrl) { header(HttpHeaders.UserAgent, UserAgent) }.bodyAsText().take(MaxBytesScanned)
    }.getOrNull()

    val linked = html?.let { page ->
        FeedLinkPattern.firstNotNullOfOrNull { it.find(page)?.groupValues?.get(1)?.tidy() }?.let { href -> resolveAgainst(rawUrl, href) }
    }
    if (linked != null && !runCatching { fetchFeed(client, linked) }.getOrNull().isNullOrEmpty()) return linked

    val origin = rawUrl.substringBefore("://") + "://" + rawUrl.substringAfter("://").substringBefore("/")
    for (path in CommonFeedPaths) {
        val candidate = origin + path
        if (!runCatching { fetchFeed(client, candidate) }.getOrNull().isNullOrEmpty()) return candidate
    }

    return rawUrl
}

/**
 * A `href` from a `<link>` tag, which publishers write both root-relative and
 * absolute. Internal rather than private: [extractArticle] resolves an
 * article's own images and links against its page URL by the same three rules.
 */
internal fun resolveAgainst(pageUrl: String, href: String): String = when {
    href.startsWith("http://") || href.startsWith("https://") -> href
    href.startsWith("/") -> pageUrl.substringBefore("://") + "://" + pageUrl.substringAfter("://").substringBefore("/") + href
    else -> pageUrl.substringBeforeLast("/") + "/" + href
}

/**
 * Fetches every followed feed and, for each one that actually yields posts,
 * replaces its slot in [cache] — the source Home's topic rows read from. A
 * feed that fails to load — down, moved, not actually a feed — keeps
 * whatever it last had cached rather than going blank, and does not block
 * the rest from syncing.
 *
 * An empty parse is treated the same as a failed fetch, not a real "no
 * posts" answer: a 200 response with no `<item>`/`<entry>` tags is far more
 * often a rate limit or an interstitial page served instead of the feed than
 * an actually empty blog, and overwriting a good cache with that would throw
 * away real posts over a transient hiccup. Returns how many feeds actually
 * yielded posts, for the caller to report.
 */
suspend fun syncFeeds(client: HttpClient, feeds: List<Feed>, cache: FeedPostCache): Int {
    var synced = 0

    for (feed in feeds) {
        val entries = runCatching { fetchFeed(client, feed.url) }.getOrNull()
        if (entries.isNullOrEmpty()) continue
        cache.replace(feed.id, entries.take(EntriesPerFeed).mapIndexed { index, entry -> entry.asPost(feed.id, index) })
        synced++
    }

    return synced
}

/**
 * Full post bodies are cached only for the newest few entries, and truncated
 * even then.
 *
 * The cache is one string in a key/value store that is read into memory whole
 * at launch — fine for a list of titles, ruinous for fifteen full articles per
 * feed across a dozen feeds. The newest handful is what anyone actually opens
 * from a feed list, and everything past it simply falls through to fetching
 * and extracting the page, which is the same path a saved link takes.
 */
private fun FeedEntry.asPost(feedId: String, index: Int): FeedPost = FeedPost(
    feedId = feedId,
    url = url,
    title = title ?: titleFromUrl(url),
    imageUrl = imageUrl,
    content = content?.take(MaxCachedContentChars)?.takeIf { index < EntriesWithContent },
)

private val CdataPattern = Regex("""<!\[CDATA\[(.*?)]]>""", RegexOption.DOT_MATCHES_ALL)

// `content:encoded` first: a feed that has both is using <description> for the
// teaser and this for the post.
private val ContentPatterns = listOf(
    Regex("""<content:encoded[^>]*>(.*?)</content:encoded>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
    Regex("""<content[^>]*>(.*?)</content>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
    Regex("""<description[^>]*>(.*?)</description>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
    Regex("""<summary[^>]*>(.*?)</summary>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
)

private val EntryImagePatterns = listOf(
    Regex("""<media:thumbnail[^>]+url\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
    Regex("""<media:content[^>]+url\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
    Regex("""<enclosure[^>]+url\s*=\s*["']([^"']+)["'][^>]*type\s*=\s*["']image/""", RegexOption.IGNORE_CASE),
    Regex("""<img[^>]+src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
)

private val ItemPattern = Regex("""<item[^>]*>(.*?)</item>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val EntryPattern = Regex("""<entry[^>]*>(.*?)</entry>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val FeedTitlePattern = Regex(
    """<title[^>]*>(?:<!\[CDATA\[)?(.*?)(?:]]>)?</title>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val RssLinkPattern = Regex("""<link[^>]*>([^<]+)</link>""", RegexOption.IGNORE_CASE)

// Matched in either attribute order — real feeds put rel before href and
// after it both, the same reason LinkMetadata.metaContent tries both orders.
private val AtomAlternateLinkPattern = listOf(
    Regex("""<link[^>]+rel\s*=\s*["']alternate["'][^>]*href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
    Regex("""<link[^>]+href\s*=\s*["']([^"']+)["'][^>]*rel\s*=\s*["']alternate["']""", RegexOption.IGNORE_CASE),
)
private val AtomAnyLinkPattern = Regex("""<link[^>]+href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

// The tag a publisher's <head> uses to point feed readers at the real feed —
// matched in either attribute order, same reasoning as the two patterns above.
private val FeedLinkPattern = listOf(
    Regex(
        """<link[^>]+type\s*=\s*["']application/(?:rss|atom)\+xml["'][^>]*href\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE,
    ),
    Regex(
        """<link[^>]+href\s*=\s*["']([^"']+)["'][^>]*type\s*=\s*["']application/(?:rss|atom)\+xml["']""",
        RegexOption.IGNORE_CASE,
    ),
)

// Tried in this order once neither the address itself nor a discovery link
// worked — the paths enough blogging platforms default to that it is worth
// trying before giving up.
private val CommonFeedPaths = listOf("/feed", "/feed/", "/rss.xml", "/rss", "/atom.xml", "/index.xml")

// A feed with a thousand-item archive should not flood the list on first
// sync — the point of following a blog is what's new, not its backlog.
private const val EntriesPerFeed = 15

// See [asPost]: what the cache is allowed to hold.
private const val EntriesWithContent = 6
private const val MaxCachedContentChars = 24_000
private const val MaxBytesScanned = 500_000
