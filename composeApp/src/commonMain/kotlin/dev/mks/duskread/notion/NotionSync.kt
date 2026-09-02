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
 * links a moment later. The push is treated the same way — a refused write
 * costs the count it would have reported and nothing else, because a feed that
 * failed to reach Notion is still followed on this phone.
 *
 * Takes [prefs] whole rather than two database ids, because it now resolves
 * them itself. That is also what removed the "reading list is optional" branch:
 * both tables exist by the time a sync runs, since `provision` builds whichever
 * one is missing.
 */
@OptIn(ExperimentalTime::class)
suspend fun runFullSync(
    api: NotionClient,
    prefs: NotionPrefs,
    library: LinkLibrary,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    http: HttpClient,
    recordSync: (Long) -> Unit,
): SyncOutcome {
    // Resolve first, every time. A sync that assumed the ids were already
    // stored would be one deleted database away from failing forever, and the
    // call is free once they are: `provision` returns on the stored id without
    // reaching the network at all.
    val ready = when (val result = provision(api, prefs)) {
        is NotionResult.Failure -> return SyncOutcome(result.message, ok = false)
        is NotionResult.Ok -> when (val state = result.value) {
            is Provisioning.Ready -> state
            // Both mean the same thing to a sync: there is nothing to sync
            // against yet, and the fix is a screen, not a retry.
            Provisioning.NoPagesShared -> return SyncOutcome(NoPagesLine, ok = false)
            is Provisioning.NeedsParent -> return SyncOutcome(NeedsParentLine, ok = false)
        }
    }

    val sources = when (val result = pullSources(api, ready.sourcesId)) {
        is NotionResult.Failure -> return SyncOutcome(result.message, ok = false)
        is NotionResult.Ok -> result.value
    }

    val applied = applySources(http, sources, feeds)

    // After the pull, so a blog that arrived from Notion a moment ago is
    // already followed and gets matched rather than created a second time —
    // the same ordering, for the same reason, as `syncReadingList`.
    val push = pushSources(api, ready.sourcesId, feeds.feeds, sources)
    val summary = when (push) {
        is NotionResult.Failure -> applied
        is NotionResult.Ok -> applied.copy(pushed = push.value.created)
    }

    val fetched = syncFeeds(http, feeds.feeds, feedPosts)
    val reading = syncReadingList(api, ready.readingId, library)
    recordSync(Clock.System.now().toEpochMilliseconds())

    val head = "${summary.line} · $fetched fetched"
    return SyncOutcome(
        line = when (reading) {
            is NotionResult.Failure -> "$head · saved links: ${reading.message}"
            is NotionResult.Ok -> reading.value.line?.let { "$head · $it" } ?: head
        },
        ok = true,
    )
}

/** Said by a sync as well as by the setup sheet, so it is written once. */
internal const val NoPagesLine = "Share a Notion page with the token first"

internal const val NeedsParentLine = "Choose where to put the DuskRead databases"
