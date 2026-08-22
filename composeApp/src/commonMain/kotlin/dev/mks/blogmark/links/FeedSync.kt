package dev.mks.blogmark.links

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

/** One post found in a feed — enough to save it as a link, nothing more. */
data class FeedEntry(val url: String, val title: String?)

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

        FeedEntry(url = url, title = FeedTitlePattern.find(block)?.groupValues?.get(1)?.tidy())
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

/** A `href` from a `<link>` tag, which publishers write both root-relative and absolute. */
private fun resolveAgainst(pageUrl: String, href: String): String = when {
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
        cache.replace(feed.id, entries.take(EntriesPerFeed).map { entry -> FeedPost(feed.id, entry.url, entry.title ?: titleFromUrl(entry.url)) })
        synced++
    }

    return synced
}

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
private const val MaxBytesScanned = 500_000
