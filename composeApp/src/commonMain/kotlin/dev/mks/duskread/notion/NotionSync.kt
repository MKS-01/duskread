package dev.mks.duskread.notion

import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.discoverFeedUrl
import io.ktor.client.HttpClient

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
