package dev.mks.duskread.links

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
    /** When the publisher says it went out, or null for a feed that does not date its entries. */
    val publishedAt: Long? = null,
)

/**
 * Reads [url] as RSS or Atom and pulls out its entries.
 *
 * Regex over tags, the same call [fetchLinkMetadata] makes: a feed is
 * machine-generated XML, well-formed often enough that a real parser buys
 * nothing a five-target KMP project doesn't already pay for as a dependency,
 * and the two formats only need two tag shapes told apart.
 *
 * Strictly XML: a page that is not a feed comes back empty rather than being
 * read some other way, because that answer is what [discoverFeedSource] uses
 * to reject a candidate address.
 */
suspend fun fetchFeed(client: HttpClient, url: String): List<FeedEntry> = parseFeed(fetchSource(client, url))

/**
 * The posts behind a followed address, whichever shape that address turned out
 * to be — an XML feed, or a listing page read by [parseIndexPage].
 *
 * One fetch decides, rather than the caller having to remember which kind of
 * source it stored: what came back either parses as a feed or it does not, and
 * a publisher that adds a feed later starts being read as one on the next sync
 * with nothing to migrate.
 */
suspend fun fetchEntries(client: HttpClient, url: String): List<FeedEntry> {
    val body = fetchSource(client, url)
    return parseFeed(body).ifEmpty { parseIndexPage(body, url) }
}

/** The body of [url], capped — both a feed and a listing page are read as one string. */
private suspend fun fetchSource(client: HttpClient, url: String): String = client.get(url) {
    header(HttpHeaders.UserAgent, UserAgent)
    header(
        HttpHeaders.Accept,
        "application/rss+xml, application/atom+xml, application/xml, text/xml, text/html;q=0.9, */*;q=0.8",
    )
}.bodyAsText().take(MaxBytesScanned)

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
            publishedAt = DatePatterns.firstNotNullOfOrNull { pattern ->
                pattern.find(block)?.groupValues?.get(1)?.let(::parsePostDate)
            },
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
 * A publication date out of any of the shapes a post arrives dated in, without
 * a calendar library.
 *
 * Atom dates are ISO-8601 and [Instant] parses those outright. Everything else
 * — RSS's RFC-822 "Wed, 13 Aug 2026 09:00:00 +0000", a bare "2026-08-13", the
 * "August 13, 2026" a listing page prints for people — is rewritten into ISO
 * and handed to the same parser rather than turned into an epoch by hand: the
 * arithmetic that converts a civil date to a count of days is exactly the part
 * worth not writing twice.
 *
 * A named zone other than UTC ("EST", "PDT") is read as UTC, and a date with
 * no time at all is read as midnight UTC. They are rare and small errors
 * respectively, against a stamp shown as "3d ago".
 */
@OptIn(ExperimentalTime::class)
internal fun parsePostDate(raw: String): Long? {
    val text = raw.trim().removeSurrounding("<![CDATA[", "]]>").trim().ifEmpty { return null }
    runCatching { return Instant.parse(text).toEpochMilliseconds() }

    IsoDayPattern.find(text)?.let { day -> return isoMillis(day.value + "T00:00:00Z") }

    return text.writtenDateAsIso()?.let(::isoMillis)
}

/**
 * "Wed, 13 Aug 2026 09:00:00 +0000" or "August 13, 2026" as an ISO timestamp.
 *
 * Day-first with a clock, and month-first without one, are the same fields in
 * a different order — matching both here rather than in two functions is what
 * keeps the month lookup and the zero-padding written once.
 */
private fun String.writtenDateAsIso(): String? {
    val dayFirst = Rfc822Pattern.find(this)
    val parts = (dayFirst ?: MonthFirstPattern.find(this) ?: return null).groupValues

    val day = if (dayFirst != null) parts[1] else parts[2]
    val month = MonthNames.indexOf((if (dayFirst != null) parts[2] else parts[1]).take(3).lowercase()) + 1
    if (month == 0) return null

    // Only the RFC-822 shape carries a clock and a zone; a printed date is
    // midnight UTC, which is as precise as the page was.
    val zone = if (dayFirst != null) parts[7] else ""
    val offset = if (zone.length == 5 && (zone[0] == '+' || zone[0] == '-')) "${zone.take(3)}:${zone.drop(3)}" else "Z"
    val clock = if (dayFirst != null) listOf(parts[4], parts[5], parts[6]) else emptyList()

    return buildString {
        append(parts[3].padStart(4, '0')).append('-')
        append(month.toString().padStart(2, '0')).append('-')
        append(day.padStart(2, '0')).append('T')
        append(clock.getOrNull(0).orEmpty().ifEmpty { "00" }.padStart(2, '0')).append(':')
        append(clock.getOrNull(1).orEmpty().ifEmpty { "00" }.padStart(2, '0')).append(':')
        append(clock.getOrNull(2).orEmpty().ifEmpty { "00" }.padStart(2, '0'))
        append(offset)
    }
}

@OptIn(ExperimentalTime::class)
private fun isoMillis(iso: String): Long? = runCatching { Instant.parse(iso).toEpochMilliseconds() }.getOrNull()

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

/** Where a followed address's posts come from, once [discoverFeedSource] has looked. */
enum class FeedSourceKind {
    /** An RSS or Atom document — the publisher's own machine-readable answer. */
    Feed,

    /** No feed anywhere, but the page lists its posts and [parseIndexPage] can read them. */
    Page,

    /** Neither. Followed anyway, so the reader can see what they typed and fix it. */
    Unknown,
}

/** The address a follow resolved to, and what kind of thing it turned out to be. */
data class FeedSource(val url: String, val kind: FeedSourceKind)

/**
 * Resolves whatever the reader typed — a blog's homepage, a section of it, or
 * the feed address itself — to something that can actually be synced.
 *
 * Asking for the exact feed address up front is how a follow ends up pointed
 * at an HTML page with nothing to parse: a blog's page is what people have on
 * hand, not its `/rss.xml`. So four things are tried, cheapest and most
 * authoritative first:
 *
 * 1. the address itself, so pasting a real feed URL costs one request;
 * 2. the `<link rel="alternate">` tags a publisher points feed readers at;
 * 3. addresses built from the typed path by [feedCandidates], for the many
 *    sites that serve a feed without ever linking to it;
 * 4. the page read as a listing, for publishers that have no feed at all.
 *
 * The first fetch is what steps 2 and 4 both read, rather than each pulling
 * the page again — on a page-builder site that body is most of a megabyte and
 * downloading it twice is the slowest thing a follow does.
 *
 * Whatever is tried last, successful or not, is what gets followed — a result
 * the reader can inspect and fix beats silently failing.
 */
suspend fun discoverFeedSource(client: HttpClient, rawUrl: String): FeedSource {
    val body = runCatching { fetchSource(client, rawUrl) }.getOrNull()
    if (body != null && parseFeed(body).isNotEmpty()) return FeedSource(rawUrl, FeedSourceKind.Feed)

    // Every declared feed, not just the first: a blog that publishes both
    // Atom and RSS lists both, and a WordPress page also advertises the
    // comment feed for the post it is showing.
    val declared = body?.let { page ->
        FeedLinkPattern.flatMap { pattern -> pattern.findAll(page).map { it.groupValues[1] }.toList() }
            .mapNotNull { href -> href.tidy()?.let { resolveAgainst(rawUrl, it) } }
            .distinct()
            // A comment feed parses perfectly and is the wrong thing to
            // follow, so it goes last rather than being dropped: it is still
            // better than nothing on a blog that declares only that.
            .sortedBy { it.contains("comment", ignoreCase = true) }
    }.orEmpty()

    firstFeedAmong(client, declared)?.let { return FeedSource(it, FeedSourceKind.Feed) }
    firstFeedAmong(client, feedCandidates(rawUrl))?.let { return FeedSource(it, FeedSourceKind.Feed) }

    if (body != null && parseIndexPage(body, rawUrl).isNotEmpty()) return FeedSource(rawUrl, FeedSourceKind.Page)

    return FeedSource(rawUrl, FeedSourceKind.Unknown)
}

/**
 * Addresses a feed might be at, derived from the typed URL rather than looked
 * up in a table of publishers.
 *
 * Two conventions cover nearly everything. Most sites hang the feed off the
 * path being read — `/blog` → `/blog/rss.xml` — and some put the feed segment
 * in front of it instead, which is how Medium addresses a publication
 * (`medium.com/basecs` → `medium.com/feed/basecs`). Both are generated for
 * every prefix of the path, deepest first: a section's own feed is the better
 * answer than the site-wide one, and trying the whole path before its parent
 * is what prefers it without either being named anywhere.
 */
internal fun feedCandidates(url: String): List<String> {
    val origin = originOf(url)
    val segments = pathSegmentsOf(url)
    val prefixes = (segments.size downTo 0).map { depth -> segments.take(depth) }

    return prefixes.flatMap { prefix ->
        val path = prefix.joinToString("") { "/$it" }
        FeedPaths.map { suffix -> origin + path + suffix } +
            // The prefix form only says something when there is a path to put
            // in front of; at the root it would repeat "/feed" from above.
            listOfNotNull(prefix.takeIf { it.isNotEmpty() }?.let { "$origin/feed$path" })
    }.distinct().take(MaxCandidates)
}

/**
 * The first of [candidates] that answers with a parseable feed, or null.
 *
 * Probed a few at a time rather than one after another: a miss costs a round
 * trip, and a dozen of them in sequence is the difference between a follow
 * that feels instant and one that looks stuck. Priority still holds — a batch
 * is only accepted at its best hit, so a deeper path always wins over a
 * shallower one that also happens to work.
 */
private suspend fun firstFeedAmong(client: HttpClient, candidates: List<String>): String? {
    for (batch in candidates.chunked(ProbeBatch)) {
        val hits = coroutineScope {
            batch.map { candidate -> async { candidate to isFeed(client, candidate) } }.awaitAll()
        }
        hits.firstOrNull { (_, isFeed) -> isFeed }?.let { (url, _) -> return url }
    }

    return null
}

/**
 * Whether [url] is a feed, without downloading a quarter-megabyte of XML to
 * find out it was a 404 page.
 *
 * A HEAD says what a body would say for a fraction of the bytes, and a
 * publisher's 404 is `text/html` where a feed is some flavour of XML — enough
 * to throw out most candidates for free. Only what survives that is fetched
 * and parsed, which is the answer that actually counts. A server that refuses
 * HEAD is not held against the candidate: it falls through to the fetch, the
 * same way it would have without this at all.
 */
private suspend fun isFeed(client: HttpClient, url: String): Boolean {
    val head = runCatching { client.head(url) { header(HttpHeaders.UserAgent, UserAgent) } }.getOrNull()
    if (head != null && head.status != HttpStatusCode.MethodNotAllowed && head.status != HttpStatusCode.NotImplemented) {
        if (!head.status.isSuccess()) return false
        val type = head.contentType()?.let { "${it.contentType}/${it.contentSubtype}" }?.lowercase()
        if (type != null && !FeedContentType.containsMatchIn(type)) return false
    }

    return !runCatching { fetchFeed(client, url) }.getOrNull().isNullOrEmpty()
}

/**
 * A `href` from a `<link>` tag, which publishers write root-relative,
 * scheme-relative and absolute. Internal rather than private: [extractArticle]
 * resolves an article's own images and links against its page URL by the same
 * rules, and so does [parseIndexPage] for every link on a listing.
 */
internal fun resolveAgainst(pageUrl: String, href: String): String = when {
    href.startsWith("http://") || href.startsWith("https://") -> href
    // "//cdn.example.com/x" — the scheme is inherited, and treating it as a
    // path would produce "https://example.com//cdn.example.com/x".
    href.startsWith("//") -> pageUrl.substringBefore("://") + ":" + href
    href.startsWith("/") -> originOf(pageUrl) + href
    else -> pageUrl.substringBeforeLast("/") + "/" + href
}

/** "https://example.com" — scheme and host, what every root-relative path hangs off. */
internal fun originOf(url: String): String = url.substringBefore("://") + "://" + url.substringAfter("://").substringBefore("/")

/** The path as segments: "https://h/blog/page/2?x=1" → ["blog", "page", "2"]. */
internal fun pathSegmentsOf(url: String): List<String> = url
    .substringAfter("://", url)
    .substringBefore('?')
    .substringBefore('#')
    .substringAfter('/', "")
    .split('/')
    .filter { it.isNotBlank() }

/**
 * Fetches every followed feed and, for each one that actually yields posts,
 * replaces its slot in [cache] — the source Home's topic rows read from. A
 * feed that fails to load — down, moved, not actually a feed — keeps
 * whatever it last had cached rather than going blank, and does not block
 * the rest from syncing.
 *
 * An empty parse is treated the same as a failed fetch, not a real "no
 * posts" answer: a 200 response that yields nothing is far more often a rate
 * limit or an interstitial page served instead of the feed than an actually
 * empty blog, and overwriting a good cache with that would throw away real
 * posts over a transient hiccup. Returns how many feeds actually yielded
 * posts, for the caller to report.
 *
 * "Feed" is loose here — [fetchEntries] reads a followed address as XML or as
 * a listing page, whichever it turns out to be, so a blog that publishes no
 * feed syncs through this the same as one that does.
 */
suspend fun syncFeeds(client: HttpClient, feeds: List<Feed>, cache: FeedPostCache): Int {
    var synced = 0

    for (feed in feeds) {
        val entries = runCatching { fetchEntries(client, feed.url) }.getOrNull()
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
    publishedAt = publishedAt,
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

// `dc:date` is what a feed generated from a CMS often carries instead of
// either standard tag. Atom's `updated` is last: a post edited after
// publication would otherwise report the edit as its date.
private val DatePatterns = listOf(
    Regex("""<pubDate[^>]*>(.*?)</pubDate>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
    Regex("""<published[^>]*>(.*?)</published>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
    Regex("""<dc:date[^>]*>(.*?)</dc:date>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
    Regex("""<updated[^>]*>(.*?)</updated>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
)

private val Rfc822Pattern = Regex(
    """(\d{1,2})\s+([A-Za-z]{3})[a-z]*\s+(\d{2,4})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?\s*([+-]\d{4}|[A-Za-z]{1,4})?""",
)

// "August 13, 2026" and "Aug 13 2026" — how a page dates a card for a reader
// rather than for a parser, which is all a listing page ever offers.
private val MonthFirstPattern = Regex("""([A-Za-z]{3,9})\.?\s+(\d{1,2}),?\s+(\d{4})""")

// A date with no time, which Instant.parse rejects outright.
private val IsoDayPattern = Regex("""\d{4}-\d{2}-\d{2}""")

private val MonthNames = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

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
// worked — the endings enough publishing platforms default to that they are
// worth a probe before giving up. "/feeds/posts/default" is Blogger's, which
// is a large enough share of blogs to earn its place at the end.
private val FeedPaths = listOf("/feed", "/rss.xml", "/feed.xml", "/index.xml", "/atom.xml", "/rss", "/feeds/posts/default")

// What a server calls a feed. A publisher's 404 page is text/html, which is
// the whole point of looking — see [isFeed].
private val FeedContentType = Regex("""xml|rss|atom""")

// A path three deep generates two dozen candidates, and past the first few the
// odds of a hit are thin. This is where trying stops being worth the requests.
private const val MaxCandidates = 18

// Probed concurrently, a few at a time. A publisher that rate-limits the
// burst answers 403 to some of them, which reads here as "not a feed" — the
// cost of that is landing on a later candidate that serves the same feed at a
// different address, not a failed follow.
private const val ProbeBatch = 4

// A feed with a thousand-item archive should not flood the list on first
// sync — the point of following a blog is what's new, not its backlog.
private const val EntriesPerFeed = 15

// See [asPost]: what the cache is allowed to hold.
private const val EntriesWithContent = 6
private const val MaxCachedContentChars = 24_000

// Generous, because this is also the cap a listing page is read under: a
// page-builder site is mostly markup, and the posts can sit past the point a
// feed-sized budget would have stopped at.
private const val MaxBytesScanned = 1_000_000
