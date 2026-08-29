package dev.mks.duskread.links

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
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
 * A publication date out of either format, without a calendar library.
 *
 * Atom dates are ISO-8601 and [Instant] parses those outright. RSS dates are
 * RFC-822 — "Wed, 13 Aug 2026 09:00:00 +0000" — which it will not touch, so
 * they are rewritten into ISO and handed to the same parser rather than
 * turned into an epoch by hand: the arithmetic that converts a civil date to
 * a count of days is exactly the part worth not writing twice.
 *
 * A named zone other than UTC ("EST", "PDT") is read as UTC. They are rare in
 * modern feeds, and the error is hours on a stamp shown as "3d ago".
 */
@OptIn(ExperimentalTime::class)
internal fun parsePostDate(raw: String): Long? {
    val text = raw.trim().removeSurrounding("<![CDATA[", "]]>").trim().ifEmpty { return null }
    runCatching { return Instant.parse(text).toEpochMilliseconds() }

    val match = Rfc822Pattern.find(text) ?: return null
    val month = MonthNames.indexOf(match.groupValues[2].lowercase()) + 1
    if (month == 0) return null

    val zone = match.groupValues[7]
    val offset = if (zone.length == 5 && (zone[0] == '+' || zone[0] == '-')) "${zone.take(3)}:${zone.drop(3)}" else "Z"
    // RFC-822 allows a two-digit year and real feeds use one — Google Bug
    // Hunters dates every post "24 Aug 26". Padding that to "0026" put a
    // whole feed in the first century, where the freshness term decays it to
    // zero and none of it can ever surface. RFC 2822 §4.3 is the rule: 00–49
    // is the 2000s, 50–99 the 1900s, and a three-digit year is 1900 + n.
    val rawYear = match.groupValues[3]
    val year = when (rawYear.length) {
        1, 2 -> rawYear.toInt().let { if (it < 50) 2000 + it else 1900 + it }
        3 -> 1900 + rawYear.toInt()
        else -> rawYear.toInt()
    }

    val iso = buildString {
        append(year.toString().padStart(4, '0')).append('-')
        append(month.toString().padStart(2, '0')).append('-')
        append(match.groupValues[1].padStart(2, '0')).append('T')
        append(match.groupValues[4].padStart(2, '0')).append(':')
        append(match.groupValues[5].padStart(2, '0')).append(':')
        append(match.groupValues[6].ifEmpty { "00" }.padStart(2, '0'))
        append(offset)
    }

    return runCatching { Instant.parse(iso).toEpochMilliseconds() }.getOrNull()?.takeIf { it >= EarliestPlausiblePost }
}

/**
 * 1995 — before the web had feeds at all.
 *
 * A regex over dates written by hundreds of different generators will
 * eventually produce something absurd, and an absurd date is worse than none:
 * a post dated in the first century sorts last and decays to zero freshness
 * for ever, silently, where a null simply means "undated" and is handled. So
 * anything implausible is treated as unparsed rather than believed.
 */
private const val EarliestPlausiblePost = 788_918_400_000L

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
    // Gathered, then written once. The cache re-encodes its whole catalogue on
    // every write, so committing per feed made a fourteen-feed sync serialise
    // the lot fourteen times — the cost grew with the square of the catalogue,
    // which is the wrong shape for something that only ever grows.
    val fetched = mutableMapOf<String, List<FeedPost>>()

    for (feed in feeds) {
        val entries = runCatching { fetchFeed(client, feed.url) }.getOrNull()
        if (entries.isNullOrEmpty()) continue
        fetched[feed.id] = entries.take(EntriesPerFeed).map { it.asPost(feed.id) }
    }

    cache.replaceAll(fetched)
    return fetched.size
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
private fun FeedEntry.asPost(feedId: String): FeedPost {
    // Every entry keeps its body now, not just the newest few. The cap is
    // per-post and generous; what used to make this expensive was writing the
    // whole catalogue once per feed, which syncFeeds no longer does.
    val cached = content?.take(MaxCachedContentChars)

    return FeedPost(
        feedId = feedId,
        url = url,
        title = title ?: titleFromUrl(url),
        imageUrl = imageUrl,
        publishedAt = publishedAt,
        content = cached,
        // Counted from the whole body, before the line above truncates it: the
        // cache keeps a prefix, but the length estimate should describe the
        // article.
        words = content?.let(::countWords),
        // The reader's own predicate, asked at sync time with exactly the body
        // the reader will be handed. Anything cheaper would drift out of step
        // with it, and a badge that disagrees with what opens is worse than
        // none. One sanitise per post per sync, nowhere near the draw path.
        offline = articleFromFeed(url, title, cached) != null,
    )
}

/**
 * Roughly how many words a body holds.
 *
 * Tags are stripped first because a feed body is markup and counting `<p>` as
 * a word inflates a short post into a long one. Rough on purpose — this feeds
 * a minutes estimate shown as "6 min", where being out by one is invisible and
 * being out by three is not.
 */
private fun countWords(markup: String): Int = markup.replace(TagPattern, " ").split(' ', '\n', '\t', '\r').count { it.isNotBlank() }

private val TagPattern = Regex("<[^>]*>")

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
// worked — the paths enough blogging platforms default to that it is worth
// trying before giving up.
private val CommonFeedPaths = listOf("/feed", "/feed/", "/rss.xml", "/rss", "/atom.xml", "/index.xml")

// A feed with a thousand-item archive should not flood the list on first
// sync — the point of following a blog is what's new, not its backlog.
private const val EntriesPerFeed = 15

// See [asPost]: the most one post's markup may take up.
private const val MaxCachedContentChars = 24_000
private const val MaxBytesScanned = 500_000
