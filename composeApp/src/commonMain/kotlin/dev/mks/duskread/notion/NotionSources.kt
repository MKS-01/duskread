package dev.mks.duskread.notion

import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.canonicalUrl
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * One row of the `Sources` database, reduced to what the app can act on.
 *
 * Everything else the table carries — tags, notes, the delivery address a
 * newsletter arrives at — is for reading in Notion, not for the phone. This
 * holds the four fields a sync actually uses and nothing more, so a column
 * added upstream never breaks a build down here.
 */
data class NotionSource(
    val name: String,
    val feedUrl: String,
    val topic: String?,
    val active: Boolean,
    /** The row's own page id, needed to claim it on the way back up. */
    val pageId: String? = null,
    /** The `Feed.id` this row was created from, when the app created it. */
    val duskreadId: String? = null,
)

/**
 * Pulls the property out of Notion's row shape.
 *
 * Every value arrives wrapped in an object naming its own type — a title is
 * `{"title":[{"plain_text":…}]}`, a URL is `{"url":…}`, a checkbox is
 * `{"checkbox":true}` — so each accessor has to know which key to open. All of
 * them return null rather than throwing on a missing or renamed column: a
 * table edited in Notion should degrade to one unusable row, never to a failed
 * sync for every other row beside it.
 */
private fun JsonObject.property(name: String): JsonObject? = this["properties"]?.jsonObject?.get(name)?.jsonObject

private fun JsonObject.titleText(name: String): String = property(name)?.get("title")?.jsonArray.orEmpty().plainText()

private fun JsonObject.urlText(name: String): String? = property(name)?.get("url")?.stringOrNull()

// Notion writes `"select": null` for an unset column, so the cast is the
// emptiness check as well as the type check.
private fun JsonObject.selectName(name: String): String? = (property(name)?.get("select") as? JsonObject)?.get("name")?.stringOrNull()

private fun JsonObject.richText(name: String): String? = property(name)?.get("rich_text")?.jsonArray.orEmpty().plainText().trim().takeIf { it.isNotBlank() }

private fun JsonObject.checkbox(name: String, default: Boolean): Boolean = property(name)?.get("checkbox")?.let { element ->
    (element as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
} ?: default

/**
 * Reads a queried row into a [NotionSource], or null if it is not usable.
 *
 * A row with no feed address is dropped rather than reported: the table is
 * edited by hand and a half-typed row is a normal intermediate state, not an
 * error worth interrupting a sync for.
 *
 * `Active` defaults to **true** when the column is missing entirely, so a
 * table created without it still syncs. It is only false when Notion actually
 * says false — the difference between "not configured" and "paused".
 */
fun parseSource(row: JsonObject): NotionSource? {
    val feedUrl = row.urlText(FeedUrlProperty)?.trim().orEmpty()
    if (feedUrl.isBlank()) return null

    val name = row.titleText(NameProperty).trim()

    return NotionSource(
        // Falling back to the feed address keeps a nameless row usable; the
        // Following list would otherwise show a blank line.
        name = name.ifBlank { feedUrl },
        feedUrl = feedUrl,
        topic = row.selectName(TopicProperty),
        active = row.checkbox(ActiveProperty, default = true),
        pageId = row["id"]?.stringOrNull(),
        duskreadId = row.richText(SourceIdProperty),
    )
}

internal const val NameProperty = "Name"
internal const val FeedUrlProperty = "Feed URL"
internal const val TopicProperty = "Topic"
internal const val ActiveProperty = "Active"

/**
 * The `Feed.id` a row was created from.
 *
 * The stable half of the match when the push runs. A feed's address can be
 * rewritten — `discoverFeedUrl` resolves a site URL to the feed behind it, so
 * the address stored on the phone is not always the one that was typed — and
 * matching on an address alone would then create a second row for a blog that
 * already had one.
 */
internal const val SourceIdProperty = "Duskread ID"

/**
 * What one push did, in the two numbers a sync line reports.
 *
 * [claimed] counts rows that already existed and were stamped with the id the
 * app knows them by — a blog followed on the phone that Notion happened to
 * already list. It is not a failure and not a no-op: it is the two halves
 * agreeing on which row is which, and it happens exactly once per blog.
 */
data class SourcePushSummary(val created: Int, val claimed: Int)

/**
 * Sends followed blogs up to the `Sources` table.
 *
 * **This is the one place the app writes into the curation half**, and it
 * reverses a rule the design notes held for a long time: Notion was upstream,
 * the phone downstream, and the arrow never turned round. It turns round here
 * because the table is now created empty by [provision] rather than curated
 * into existence beforehand. A reader who installs the app, follows four blogs
 * and opens Notion to find nothing there has been handed a feature that looks
 * broken, and is right to think so.
 *
 * What has *not* changed is the destructive half. Nothing here deletes or
 * archives a row, exactly like `applySources` never unfollows and
 * `syncReadingList` never removes: unfollowing on the phone leaves the row in
 * Notion, because a table someone curates is not this app's to prune.
 *
 * Matching runs id-first and address-second, the same two indexes and the same
 * reasoning as `syncReadingList` — the id is what the app wrote, the address
 * is how a row someone typed by hand can still be recognised.
 */
suspend fun pushSources(
    api: NotionClient,
    databaseId: String,
    feeds: List<Feed>,
    rows: List<NotionSource>,
): NotionResult<SourcePushSummary> {
    val byId = rows.filter { it.duskreadId != null }.associateBy { it.duskreadId }
    val byUrl = rows.associateBy { canonicalUrl(it.feedUrl) }

    var created = 0
    var claimed = 0

    feeds.forEach { feed ->
        val row = byId[feed.id] ?: byUrl[canonicalUrl(feed.url)]

        if (row == null) {
            val result = api.createPage(databaseId, sourceProperties(feed))
            if (result is NotionResult.Failure) return result
            created++
            return@forEach
        }

        // Only ever written once. Rewriting a row that already carries the
        // right id would touch `last_edited_time` on every sync forever, for
        // no change — the same trap `syncReadingList` documents.
        if (row.duskreadId == feed.id) return@forEach
        if (row.pageId == null) return@forEach

        val result = api.updatePage(row.pageId, sourceClaim(feed))
        if (result is NotionResult.Failure) return result
        claimed++
    }

    return NotionResult.Ok(SourcePushSummary(created = created, claimed = claimed))
}

/**
 * A followed blog as Notion properties.
 *
 * `Active` is written true because following it is what put it here. `Topic`
 * is written only when the phone knows one: the app's silence about a subject
 * is not the same as knowing the blog has none, and a null select would clear
 * a topic curated by hand in Notion.
 */
private fun sourceProperties(feed: Feed): JsonObject = buildJsonObject {
    put(
        NameProperty,
        buildJsonObject {
            put(
                "title",
                buildJsonArray {
                    add(buildJsonObject { put("text", buildJsonObject { put("content", JsonPrimitive(feed.label.take(1_900))) }) })
                },
            )
        },
    )
    put(FeedUrlProperty, buildJsonObject { put("url", JsonPrimitive(feed.url)) })
    put(ActiveProperty, buildJsonObject { put("checkbox", JsonPrimitive(true)) })
    put(SourceIdProperty, sourceRichText(feed.id))
    feed.topic?.let { put(TopicProperty, buildJsonObject { put("select", buildJsonObject { put("name", JsonPrimitive(it)) }) }) }
}

/** The one property that says "this row is that feed", written on its own. */
private fun sourceClaim(feed: Feed): JsonObject = buildJsonObject {
    put(SourceIdProperty, sourceRichText(feed.id))
}

private fun sourceRichText(value: String): JsonObject = buildJsonObject {
    put(
        "rich_text",
        buildJsonArray {
            add(buildJsonObject { put("text", buildJsonObject { put("content", JsonPrimitive(value)) }) })
        },
    )
}
