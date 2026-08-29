package dev.mks.duskread.notion

import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.SavedLink
import dev.mks.duskread.links.normaliseUrl
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * One row of `Reading List`, reduced to what the sync reconciles.
 *
 * [saved] is the whole reason this table can be shared with everything else
 * the reader files in Notion. Roughly fifty rows there are articles pulled
 * from feeds and mail that nobody chose — a saved link is the deliberate
 * subset, and the checkbox is what separates the two. Only ticked rows ever
 * reach the phone.
 */
data class NotionArticle(
    val pageId: String,
    val duskreadId: String?,
    val url: String,
    val title: String,
    val excerpt: String?,
    val topic: String?,
    val read: Boolean,
    val saved: Boolean,
    val lastEditedAt: Long,
)

/** What one reading-list sync did, in the numbers Settings reports. */
data class ReadingSyncSummary(
    val pushed: Int,
    val updated: Int,
    val pulled: Int,
) {
    /** "3 up · 1 down", or null when there was nothing to say. */
    val line: String?
        get() {
            val up = pushed + updated
            if (up == 0 && pulled == 0) return null
            return listOfNotNull(
                (up > 0).takeIf { it }?.let { "$up up" },
                (pulled > 0).takeIf { it }?.let { "$pulled down" },
            ).joinToString(" · ")
        }
}

/**
 * Saved links, both directions, in one pass.
 *
 * The order matters and is not arbitrary: query once, pull what Notion has,
 * then push what the phone has. Pulling first means a link that arrived from
 * another device is already present when the push runs and gets matched rather
 * than created a second time.
 *
 * Nothing here deletes or archives a Notion row. The app is a working set over
 * an archive it does not own — the same rule the source pull follows, and the
 * reason [LinkLibrary.removedUrls] has to exist.
 */
@OptIn(ExperimentalTime::class)
suspend fun syncReadingList(
    client: NotionClient,
    databaseId: String,
    links: LinkLibrary,
): NotionResult<ReadingSyncSummary> {
    val statusNames = client.schema(databaseId).let { result ->
        when (result) {
            is NotionResult.Failure -> return result
            is NotionResult.Ok -> statusNames(result.value)
        }
    }

    val rows = when (val result = client.queryAll(databaseId)) {
        is NotionResult.Failure -> return result
        is NotionResult.Ok -> result.value.mapNotNull(::parseArticle)
    }

    // Two indexes because a row can be known two ways: by the id the app wrote
    // when it created the row, and — for a row Claude filed from mail before
    // the app ever saw it — by its address alone.
    val byId = rows.filter { it.duskreadId != null }.associateBy { it.duskreadId }
    val byUrl = rows.associateBy { normaliseUrl(it.url).lowercase() }

    var pulled = 0
    rows.filter { it.saved }.forEach { row ->
        val url = normaliseUrl(row.url)
        val created = links.upsertFromNotion(
            SavedLink(
                // A row Claude filed has no id yet; minting one here means the
                // push below claims it, and every sync after this matches on
                // the id rather than re-deriving it from the address.
                id = row.duskreadId ?: ("n-" + row.pageId.filterNot { it == '-' }.take(12)),
                url = url,
                title = row.title,
                description = row.excerpt,
                savedAt = row.lastEditedAt,
                readAt = if (row.read) row.lastEditedAt else null,
                changedAt = row.lastEditedAt,
                topic = row.topic,
            ),
        )
        if (created) pulled++
    }

    var pushed = 0
    var updated = 0
    links.links.forEach { link ->
        val row = byId[link.id] ?: byUrl[normaliseUrl(link.url).lowercase()]

        if (row == null) {
            val result = client.createPage(databaseId, properties(link, statusNames, includeUrl = true))
            if (result is NotionResult.Failure) return result
            pushed++
            return@forEach
        }

        // Only when the phone is the newer of the two, and only when something
        // would actually change. A sync that rewrites every row every time
        // would make `last_edited_time` meaningless, which is the one thing
        // this whole reconciliation rests on.
        val stale = link.changedAt > row.lastEditedAt
        val differs = row.read != link.read || !row.saved || row.duskreadId != link.id
        if (stale && differs) {
            val result = client.updatePage(row.pageId, properties(link, statusNames, includeUrl = false))
            if (result is NotionResult.Failure) return result
            updated++
        }
    }

    return NotionResult.Ok(ReadingSyncSummary(pushed = pushed, updated = updated, pulled = pulled))
}

/**
 * The status option names this database actually uses.
 *
 * Notion ships `Not started` / `In progress` / `Done` and its DDL will not
 * rename them, so a reading list that says `Unread` / `Read` is renamed by
 * hand. Reading the names out of the schema's own groups — `to_do` for unread,
 * `complete` for read — means either spelling works and neither is written
 * down here.
 */
private fun statusNames(schema: JsonObject): StatusNames {
    val groups = schema["properties"]?.jsonObject
        ?.get("Status")?.jsonObject
        ?.get("status")?.jsonObject
        ?.get("groups")?.jsonArray
        .orEmpty()

    fun firstIn(group: String): String? = groups
        .mapNotNull { it as? JsonObject }
        .firstOrNull { it["name"]?.stringOrNull() == group }
        ?.get("option_ids")?.jsonArray?.firstOrNull()?.stringOrNull()
        ?.let { id ->
            schema["properties"]?.jsonObject?.get("Status")?.jsonObject
                ?.get("status")?.jsonObject?.get("options")?.jsonArray.orEmpty()
                .mapNotNull { it as? JsonObject }
                .firstOrNull { it["id"]?.stringOrNull() == id }
                ?.get("name")?.stringOrNull()
        }

    return StatusNames(
        unread = firstIn("To-do") ?: firstIn("to_do") ?: "Not started",
        read = firstIn("Complete") ?: firstIn("complete") ?: "Done",
    )
}

private data class StatusNames(val unread: String, val read: String)

/**
 * A link as Notion properties.
 *
 * Built explicitly rather than parsed generically, unlike the read side: the
 * shapes a write uses are few and fixed, and getting one wrong should be a
 * compile-time-ish mistake in one place rather than a silent no-op.
 *
 * The URL is written only on create. It is the natural key the whole match
 * falls back to, and rewriting it on every update would let a normalisation
 * change quietly orphan a row from the link that owns it.
 */
private fun properties(link: SavedLink, status: StatusNames, includeUrl: Boolean): JsonObject = buildJsonObject {
    put(
        "Title",
        buildJsonObject {
            put(
                "title",
                buildJsonArray {
                    add(buildJsonObject { put("text", buildJsonObject { put("content", JsonPrimitive(link.title.take(1_900))) }) })
                },
            )
        },
    )
    if (includeUrl) put("URL", buildJsonObject { put("url", JsonPrimitive(link.url)) })
    put("Duskread ID", richText(link.id))
    put("Saved", buildJsonObject { put("checkbox", JsonPrimitive(true)) })
    put(
        "Status",
        buildJsonObject { put("status", buildJsonObject { put("name", JsonPrimitive(if (link.read) status.read else status.unread)) }) },
    )
    link.description?.let { put("Excerpt", richText(it.take(1_900))) }
    if (includeUrl) {
        put(
            "Saved At",
            buildJsonObject { put("date", buildJsonObject { put("start", JsonPrimitive(isoDate(link.savedAt))) }) },
        )
    }
}

private fun richText(value: String): JsonObject = buildJsonObject {
    put(
        "rich_text",
        buildJsonArray {
            add(buildJsonObject { put("text", buildJsonObject { put("content", JsonPrimitive(value)) }) })
        },
    )
}

/** Reads one queried row. Null for anything without an address, which is not a link. */
fun parseArticle(row: JsonObject): NotionArticle? {
    val props = row["properties"]?.jsonObject ?: return null
    fun prop(name: String): JsonObject? = props[name]?.jsonObject

    val url = prop("URL")?.get("url")?.stringOrNull()?.trim().orEmpty()
    if (url.isBlank()) return null

    val title = prop("Title")?.get("title")?.jsonArray.orEmpty().plainText().trim()
    val statusName = (prop("Status")?.get("status") as? JsonObject)?.get("name")?.stringOrNull()

    return NotionArticle(
        pageId = row["id"]?.stringOrNull() ?: return null,
        duskreadId = prop("Duskread ID")?.get("rich_text")?.jsonArray.orEmpty().plainText().trim().takeIf { it.isNotBlank() },
        url = url,
        title = title.ifBlank { url },
        excerpt = prop("Excerpt")?.get("rich_text")?.jsonArray.orEmpty().plainText().trim().takeIf { it.isNotBlank() },
        topic = (prop("Topic")?.get("select") as? JsonObject)?.get("name")?.stringOrNull(),
        // Matched by name against both the stock spelling and the one a
        // reading list would rename it to, so the rename is safe either way.
        read = statusName == "Done" || statusName == "Read",
        saved = (prop("Saved")?.get("checkbox") as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false,
        lastEditedAt = row["last_edited_time"]?.stringOrNull()?.let(::parseIso) ?: 0L,
    )
}

@OptIn(ExperimentalTime::class)
private fun parseIso(value: String): Long? = runCatching { Instant.parse(value).toEpochMilliseconds() }.getOrNull()

@OptIn(ExperimentalTime::class)
private fun isoDate(epochMs: Long): String = Instant.fromEpochMilliseconds(epochMs).toString()
