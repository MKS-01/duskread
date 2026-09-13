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
 */
private fun JsonObject.property(name: String): JsonObject? = this["properties"]?.jsonObject?.get(name)?.jsonObject

private fun JsonObject.titleText(name: String): String = property(name)?.get("title")?.jsonArray.orEmpty().plainText()

private fun JsonObject.urlText(name: String): String? = property(name)?.get("url")?.stringOrNull()

// Notion writes `"select": null` for an unset column, so the cast is the emptiness check
// as well as the type check.
private fun JsonObject.selectName(name: String): String? = (property(name)?.get("select") as? JsonObject)?.get("name")?.stringOrNull()

private fun JsonObject.richText(name: String): String? = property(name)?.get("rich_text")?.jsonArray.orEmpty().plainText().trim().takeIf { it.isNotBlank() }

private fun JsonObject.checkbox(name: String, default: Boolean): Boolean = property(name)?.get("checkbox")?.let { element ->
    (element as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
} ?: default

/**
 * Reads a queried row into a [NotionSource], or null if it is not usable.
 */
fun parseSource(row: JsonObject): NotionSource? {
    val feedUrl = row.urlText(FeedUrlProperty)?.trim().orEmpty()
    if (feedUrl.isBlank()) return null

    val name = row.titleText(NameProperty).trim()

    return NotionSource(
        // Falling back to the feed address keeps a nameless row usable; the Following
        // list would otherwise show a blank line.
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
 * The `Feed.id` a row was created from. The stable half of the match when the push runs.
 */
internal const val SourceIdProperty = "Duskread ID"

/**
 * What one push did, in the two numbers a sync line reports.
 */
data class SourcePushSummary(val created: Int, val claimed: Int)

/**
 * Sends followed blogs up to the `Sources` table. **This is the one place the app writes
 * into the curation half**, and it reverses a rule the design notes held for a long time.
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

        // Only ever written once.
        if (row.duskreadId == feed.id) return@forEach
        if (row.pageId == null) return@forEach

        val result = api.updatePage(row.pageId, sourceClaim(feed))
        if (result is NotionResult.Failure) return result
        claimed++
    }

    return NotionResult.Ok(SourcePushSummary(created = created, claimed = claimed))
}

/**
 * A followed blog as Notion properties. `Active` is written true because following it is
 * what put it here.
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
