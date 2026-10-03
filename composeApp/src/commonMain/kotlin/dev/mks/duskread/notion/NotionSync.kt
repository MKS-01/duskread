package dev.mks.duskread.notion

import dev.mks.duskread.data.DataEpoch
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedSyncer
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.discoverFeedUrl
import dev.mks.duskread.links.sameArticle
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * What one pull did, in the three numbers Settings reports.
 */
data class SourceSyncSummary(
    val found: Int,
    val added: Int,
    val skipped: Int,
    /** Rows this sync created in Notion for blogs followed on the phone. */
    val pushed: Int = 0,
) {
    /** "17 feeds · 4 new · 2 up", trimmed of whichever halves had nothing to say. */
    val line: String
        get() = listOfNotNull(
            "$found feeds",
            if (added == 0) "nothing new" else "$added new",
            "$pushed up".takeIf { pushed > 0 },
        ).joinToString(" · ")
}

/** Reads the `Sources` database and turns its rows into something the app can follow. */
suspend fun pullSources(client: NotionClient, databaseId: String): NotionResult<List<NotionSource>> = client.queryAll(databaseId).then { rows -> NotionResult.Ok(rows.mapNotNull(::parseSource)) }

/** What [applySources] did, and which unclaimed rows it matched to a feed by discovery. */
data class AppliedSources(
    val summary: SourceSyncSummary,
    /** Feed id to the row it came from, so the push claims that row instead of filing a twin. */
    val resolved: Map<String, NotionSource> = emptyMap(),
)

/**
 * Follows every active source, and never unfollows anything. **Additive only**, the same
 * contract as `LinkLibrary.import`.
 */
suspend fun applySources(
    http: HttpClient,
    sources: List<NotionSource>,
    feeds: FeedLibrary,
    /** The sync's own [DataEpoch.mark]; the loop stops following once it is stale. */
    epoch: Int = DataEpoch.mark(),
): AppliedSources {
    val active = sources.filter { it.active }
    var added = 0
    var skipped = 0
    val resolved = mutableMapOf<String, NotionSource>()

    // A row already followed needs no network at all — only a source the phone has not
    // seen is worth resolving, so an unchanged table syncs in one query.
    val unknown = active.filter { source ->
        val known = feeds.feeds.firstOrNull { it.id == source.duskreadId || sameArticle(it.url, source.feedUrl) }
        if (known != null) {
            feeds.add(known.url, source.name, source.topic)
            skipped++
        }
        known == null
    }

    // Resolution is a network call and can fail; the raw address is still worth
    // following, since fetchFeed may well accept what discovery could not confirm.
    val gate = Semaphore(ParallelDiscoveries)
    val addresses = coroutineScope {
        unknown.map { source ->
            async { gate.withPermit { runCatching { discoverFeedUrl(http, source.feedUrl) }.getOrDefault(source.feedUrl) } }
        }.awaitAll()
    }
    if (DataEpoch.stale(epoch)) return AppliedSources(SourceSyncSummary(found = active.size, added = 0, skipped = skipped))

    unknown.zip(addresses).forEach { (source, address) ->
        val before = feeds.feeds.size
        val feed = feeds.add(address, source.name, source.topic) ?: return@forEach
        if (feeds.feeds.size > before) added++ else skipped++
        if (source.duskreadId == null) resolved[feed.id] = source
    }

    return AppliedSources(SourceSyncSummary(found = active.size, added = added, skipped = skipped), resolved)
}

/** Everything one sync did, and whether it got far enough to be worth recording. */
data class SyncOutcome(val line: String, val ok: Boolean)

/**
 * The whole sync, in the order the halves depend on each other. Each half runs end to end
 * only when something moved on its side — the phone's or Notion's — unless [force]d.
 */
@OptIn(ExperimentalTime::class)
suspend fun runFullSync(
    api: NotionClient,
    prefs: NotionPrefs,
    library: LinkLibrary,
    feeds: FeedLibrary,
    feedSync: FeedSyncer,
    http: HttpClient,
    recordSync: (Long) -> Unit,
    force: Boolean = false,
): SyncOutcome {
    // Resolve first, every time — free once the ids are stored.
    val epoch = DataEpoch.mark()
    // Taken before any read, so an edit landing mid-sync is still "since" next time.
    val startedAt = Clock.System.now().toEpochMilliseconds()

    val ready = when (val result = provision(api, prefs)) {
        is NotionResult.Failure -> return SyncOutcome(result.message, ok = false)
        is NotionResult.Ok -> when (val state = result.value) {
            is Provisioning.Ready -> state
            // Both mean the same thing to a sync: there is nothing to sync against yet,
            // and the fix is a screen, not a retry.
            Provisioning.NoPagesShared -> return SyncOutcome(NoPagesLine, ok = false)
            is Provisioning.NeedsParent -> return SyncOutcome(NeedsParentLine, ok = false)
        }
    }

    // Null means "assume everything moved": first sync, or asked for by hand.
    val since = prefs.lastSyncAt?.takeIf { !force }
    val sinceIso = since?.let { Instant.fromEpochMilliseconds(it - EditedSlackMs).toString() }

    // A failed probe counts as changed, so the full path runs and reports the failure.
    suspend fun notionMoved(databaseId: String): Boolean = sinceIso == null ||
        (api.editedSince(databaseId, sinceIso) as? NotionResult.Ok)?.value != false

    val fingerprint = feedsFingerprint(feeds.feeds)
    val sourcesDue = fingerprint != prefs.syncedFeeds || notionMoved(ready.sourcesId)
    val linksDue = since == null ||
        library.links.any { it.changedAt > since } ||
        library.removedUrls.values.any { it > since } ||
        notionMoved(ready.readingId)

    if (!sourcesDue && !linksDue) {
        recordSync(startedAt)
        return SyncOutcome(NothingChangedLine, ok = true)
    }

    val parts = mutableListOf<String>()

    if (sourcesDue) {
        val sources = when (val result = pullSources(api, ready.sourcesId)) {
            is NotionResult.Failure -> return SyncOutcome(result.message, ok = false)
            is NotionResult.Ok -> result.value
        }

        if (DataEpoch.stale(epoch)) return SyncOutcome(ErasedLine, ok = false)

        val applied = applySources(http, sources, feeds, epoch)

        // After the pull, so a blog that arrived from Notion a moment ago is already
        // followed and gets matched rather than created a second time.
        val push = pushSources(api, ready.sourcesId, feeds.feeds, sources, applied.resolved)
        val summary = when (push) {
            is NotionResult.Failure -> applied.summary
            is NotionResult.Ok -> applied.summary.copy(pushed = push.value.created)
        }
        // Only once both directions landed; a failed push must be retried next time.
        if (push is NotionResult.Ok) prefs.recordSyncedFeeds(feedsFingerprint(feeds.feeds))
        parts += summary.line

        // Not awaited: feeds are fetched straight from each blog on their own clock, so a
        // Notion sync only nudges one when it brought a blog the cache has never seen.
        if (summary.added > 0) feedSync.syncInBackground()
    }

    // Last gate, and the one that also keeps `notion.sync.last` off a store the erase has
    // just emptied.
    if (DataEpoch.stale(epoch)) return SyncOutcome(ErasedLine, ok = false)

    if (linksDue) {
        when (val reading = syncReadingList(api, ready.readingId, library)) {
            is NotionResult.Failure -> parts += "saved links: ${reading.message}"
            is NotionResult.Ok -> reading.value.line?.let { parts += it }
        }
    }
    recordSync(startedAt)

    return SyncOutcome(line = parts.joinToString(" · ").ifEmpty { NothingChangedLine }, ok = true)
}

/** The followed list as one comparable string: its length, then every id in order. */
private fun feedsFingerprint(feeds: List<Feed>): String = "${feeds.size}:" + feeds.map { it.id }.sorted().joinToString(",")

/**
 * Notion stamps `last_edited_time` to the minute, rounded down, so the probe looks a
 * little further back than the last sync rather than miss an edit in that minute.
 */
private const val EditedSlackMs = 2L * 60 * 1000

internal const val NothingChangedLine = "Nothing changed"

/**
 * Reported by a sync abandoned mid-flight because the data underneath it was erased.
 */
internal const val ErasedLine = "Erased — sync stopped"

/** Said by a sync as well as by the setup sheet, so it is written once. */
internal const val NoPagesLine = "Share a Notion page with the token first"

// Discovery can be four fetches a source; a handful at once keeps a new table quick
// without a burst of sockets.
private const val ParallelDiscoveries = 4

internal const val NeedsParentLine = "Choose where to put the DuskRead databases"
