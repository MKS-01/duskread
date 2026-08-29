package dev.mks.duskread.notion

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
    )
}

internal const val NameProperty = "Name"
internal const val FeedUrlProperty = "Feed URL"
internal const val TopicProperty = "Topic"
internal const val ActiveProperty = "Active"
