package dev.mks.duskread.notion

import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.discoverFeedUrl
import dev.mks.duskread.links.syncFeeds
import io.ktor.client.HttpClient
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * What one pull did, in the three numbers Settings reports.
 *
 * [skipped] counts rows that were already followed, not rows that failed —
 * on a healthy second sync everything is skipped, and that is the success
 * case, not a warning.
 */
data class SourceSyncSummary(
    val found: Int,
    val added: Int,
    val skipped: Int,
) {
    /** "17 feeds · 4 new", or "17 feeds · nothing new". */
    val line: String
        get() = "$found feeds · " + if (added == 0) "nothing new" else "$added new"
}

/** Reads the `Sources` database and turns its rows into something the app can follow. */
suspend fun pullSources(client: NotionClient, databaseId: String): NotionResult<List<NotionSource>> = client.queryAll(databaseId).then { rows -> NotionResult.Ok(rows.mapNotNull(::parseSource)) }

/**
 * Follows every active source, and never unfollows anything.
 *
 * **Additive only**, the same contract as `LinkLibrary.import`. A row deleted
 * in Notion leaves the feed followed on the phone: silently emptying someone's
 * Following list because a network response came back short is the kind of
 * damage that is impossible to notice and impossible to undo. Unfollowing
 * stays a deliberate act in the app.
 *
 * Each address goes through [discoverFeedUrl] first — the same resolution the
 * manual "add a feed" path uses. The `Sources` table is edited by hand and
 * half of its original rows recorded a site address where a feed was meant, so
 * accepting a page URL and finding the feed behind it is the difference
 * between a table that works and one that has to be perfect.
 */
suspend fun applySources(
    http: HttpClient,
    sources: List<NotionSource>,
    feeds: FeedLibrary,
): SourceSyncSummary {
    val active = sources.filter { it.active }
    var added = 0
    var skipped = 0

    active.forEach { source ->
        val before = feeds.feeds.size
        // Resolution is a network call and can fail; the raw address is still
        // worth following, since fetchFeed may well accept what discovery
        // could not confirm.
        val resolved = runCatching { discoverFeedUrl(http, source.feedUrl) }.getOrDefault(source.feedUrl)

        feeds.add(resolved, source.name, source.topic) ?: return@forEach
        if (feeds.feeds.size > before) added++ else skipped++
    }

    return SourceSyncSummary(found = active.size, added = added, skipped = skipped)
}

/** Everything one sync did, and whether it got far enough to be worth recording. */
data class SyncOutcome(val line: String, val ok: Boolean)

/**
 * The whole sync, in the order the halves depend on each other.
 *
 * Lives here rather than in Settings because it now has two callers: the
 * button, and the automatic run when the app opens. A copy in each would be
 * two chances to fix a bug once.
 *
 * The reading list is reported separately and never allowed to discard the
 * sources half: those feeds are followed whatever Notion says about saved
 * links a moment later.
 */
@OptIn(ExperimentalTime::class)
suspend fun runFullSync(
    api: NotionClient,
    sourcesDatabaseId: String,
    readingDatabaseId: String?,
    library: LinkLibrary,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    http: HttpClient,
    recordSync: (Long) -> Unit,
): SyncOutcome = when (val sources = pullSources(api, sourcesDatabaseId)) {
    is NotionResult.Failure -> SyncOutcome(sources.message, ok = false)

    is NotionResult.Ok -> {
        val summary = applySources(http, sources.value, feeds)
        val fetched = syncFeeds(http, feeds.feeds, feedPosts)
        val reading = readingDatabaseId?.let { syncReadingList(api, it, library) }
        recordSync(Clock.System.now().toEpochMilliseconds())

        val head = "${summary.line} · $fetched fetched"
        SyncOutcome(
            line = when (reading) {
                null -> head
                is NotionResult.Failure -> "$head · saved links: ${reading.message}"
                is NotionResult.Ok -> reading.value.line?.let { "$head · $it" } ?: head
            },
            ok = true,
        )
    }
}
