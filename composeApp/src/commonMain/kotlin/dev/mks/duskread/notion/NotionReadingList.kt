package dev.mks.duskread.notion

import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.SavedLink
import dev.mks.duskread.links.canonicalUrl
import dev.mks.duskread.links.normaliseUrl
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * One row of `Reading List`, reduced to what the sync reconciles. [saved] is the whole
 * reason this table can be shared with everything else the reader files in Notion.
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
    /** Ticked = the reader said no. Never pulled down, and nothing should re-file it. */
    val dismissed: Boolean,
    /** When Notion says it was filed, as opposed to when the row was last touched. */
    val savedAt: Long?,
    /** When it was read, where that is recorded. [read] says whether; this says when. */
    val readAt: Long?,
    val lastEditedAt: Long,
)

/** What one reading-list sync did, in the numbers Settings reports. */
data class ReadingSyncSummary(
    val pushed: Int,
    /** Rows changed, including any marked "not interested". */
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
 * Saved links, both directions, in one pass. The order matters and is not arbitrary:
 * query once, pull what Notion has, then push what the phone has.
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

    // Two indexes because a row can be known two ways: by the id the app wrote when it
    // created the row, and.
    val byId = rows.filter { it.duskreadId != null }.associateBy { it.duskreadId }
    val byUrl = rows.associateBy { canonicalUrl(it.url) }

    // Refusals go up first.
    var dismissed = 0
    links.removedKeys.forEach { key ->
        val row = byUrl[key] ?: return@forEach
        if (row.dismissed && !row.saved) return@forEach

        val result = client.updatePage(row.pageId, dismissal())
        if (result is NotionResult.Failure) return result
        dismissed++
    }

    var pulled = 0
    // A dismissed row is refused here as well as by the local tombstone list.
    rows.filter { it.saved && !it.dismissed }.forEach { row ->
        val url = normaliseUrl(row.url)
        val created = links.upsertFromNotion(
            SavedLink(
                // A row Claude filed has no id yet; minting one here means the push below
                // claims it.
                id = row.duskreadId ?: ("n-" + row.pageId.filterNot { it == '-' }.take(12)),
                url = url,
                title = row.title,
                description = row.excerpt,
                // Notion's own dates where it has them.
                savedAt = row.savedAt ?: row.lastEditedAt,
                readAt = row.readAt ?: row.lastEditedAt.takeIf { row.read },
                changedAt = row.lastEditedAt,
                topic = row.topic,
            ),
        )
        if (created) pulled++
    }

    var pushed = 0
    var updated = 0
    links.links.forEach { link ->
        val row = byId[link.id] ?: byUrl[canonicalUrl(link.url)]

        if (row == null) {
            val result = client.createPage(databaseId, properties(link, statusNames, includeUrl = true))
            if (result is NotionResult.Failure) return result
            pushed++
            return@forEach
        }

        // Two different writes, because they answer to different rules.
        val localIsNewer = link.changedAt > row.lastEditedAt

        // Compared against what a write would actually set: a null description writes
        // nothing.
        val contentDiffers = row.read != link.read ||
            row.title != link.title ||
            (link.description != null && row.excerpt != link.description) ||
            (link.topic != null && row.topic != link.topic)

        val unclaimed = row.duskreadId != link.id || !row.saved

        val properties = when {
            localIsNewer && contentDiffers -> properties(link, statusNames, includeUrl = false)
            unclaimed -> claim(link)
            else -> null
        }

        if (properties != null) {
            val result = client.updatePage(row.pageId, properties)
            if (result is NotionResult.Failure) return result
            updated++
        }
    }

    return NotionResult.Ok(ReadingSyncSummary(pushed = pushed, updated = updated + dismissed, pulled = pulled))
}

/**
 * The status option names this database actually uses.
 */
private fun statusNames(schema: JsonObject): StatusNames {
    val column = schema["properties"]?.jsonObject?.get("Status")?.jsonObject

    // A select, not a status.
    column?.get("select")?.jsonObject?.let { select ->
        val options = select["options"]?.jsonArray.orEmpty()
            .mapNotNull { (it as? JsonObject)?.get("name")?.stringOrNull() }

        fun match(vararg wanted: String): String? = options.firstOrNull { option -> wanted.any { it.equals(option, ignoreCase = true) } }

        return StatusNames(
            unread = match(UnreadOption, "Not started", "To read") ?: UnreadOption,
            read = match(ReadOption, "Done", "Complete") ?: ReadOption,
            select = true,
        )
    }

    val status = column?.get("status")?.jsonObject
    val groups = status?.get("groups")?.jsonArray.orEmpty()

    fun firstIn(group: String): String? = groups
        .mapNotNull { it as? JsonObject }
        .firstOrNull { it["name"]?.stringOrNull() == group }
        ?.get("option_ids")?.jsonArray?.firstOrNull()?.stringOrNull()
        ?.let { id ->
            status?.get("options")?.jsonArray.orEmpty()
                .mapNotNull { it as? JsonObject }
                .firstOrNull { it["id"]?.stringOrNull() == id }
                ?.get("name")?.stringOrNull()
        }

    return StatusNames(
        unread = firstIn("To-do") ?: firstIn("to_do") ?: "Not started",
        read = firstIn("Complete") ?: firstIn("complete") ?: "Done",
        select = false,
    )
}

/**
 * The two option names, and which of Notion's two column types holds them.
 */
private data class StatusNames(val unread: String, val read: String, val select: Boolean) {
    /** `{"status": {...}}` or `{"select": {...}}`, whichever this table takes. */
    fun value(name: String): JsonObject = buildJsonObject {
        put(
            if (select) "select" else "status",
            buildJsonObject { put("name", JsonPrimitive(name)) },
        )
    }
}

/**
 * A link as Notion properties.
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
    put("Status", status.value(if (link.read) status.read else status.unread))
    // Paired with Status rather than folded into it: Status answers whether, this answers
    // when.
    put(
        "Read At",
        buildJsonObject {
            put("date", link.readAt?.takeIf { link.read && it > 0L }?.let { at -> buildJsonObject { put("start", JsonPrimitive(isoDate(at))) } } ?: JsonNull)
        },
    )
    link.description?.let { put("Excerpt", richText(it.take(1_900))) }
    link.topic?.let { put("Topic", buildJsonObject { put("select", buildJsonObject { put("name", JsonPrimitive(it)) }) }) }
    if (includeUrl) {
        put(
            "Saved At",
            buildJsonObject { put("date", buildJsonObject { put("start", JsonPrimitive(isoDate(link.savedAt))) }) },
        )
    }
}

/**
 * "Not interested" — the only refusal this app can express upstream.
 */
private fun dismissal(): JsonObject = buildJsonObject {
    put("Dismissed", buildJsonObject { put("checkbox", JsonPrimitive(true)) })
    put("Saved", buildJsonObject { put("checkbox", JsonPrimitive(false)) })
}

/**
 * The two properties that say "this row is that link".
 */
private fun claim(link: SavedLink): JsonObject = buildJsonObject {
    put("Duskread ID", richText(link.id))
    put("Saved", buildJsonObject { put("checkbox", JsonPrimitive(true)) })
}

private fun richText(value: String): JsonObject = buildJsonObject {
    put(
        "rich_text",
        buildJsonArray {
            add(buildJsonObject { put("text", buildJsonObject { put("content", JsonPrimitive(value)) }) })
        },
    )
}

/** A Notion date property's start, or null for an unset one. */
private fun JsonObject.dateStart(): Long? = (this["date"] as? JsonObject)?.get("start")?.stringOrNull()?.let(::parseIso)

/** Reads one queried row. Null for anything without an address, which is not a link. */
fun parseArticle(row: JsonObject): NotionArticle? {
    val props = row["properties"]?.jsonObject ?: return null
    fun prop(name: String): JsonObject? = props[name]?.jsonObject

    val url = prop("URL")?.get("url")?.stringOrNull()?.trim().orEmpty()
    if (url.isBlank()) return null

    val title = prop("Title")?.get("title")?.jsonArray.orEmpty().plainText().trim()
    // Either column type, because both exist in the wild — see `statusNames`.
    val statusColumn = prop("Status")
    val statusName = ((statusColumn?.get("status") ?: statusColumn?.get("select")) as? JsonObject)
        ?.get("name")?.stringOrNull()

    return NotionArticle(
        pageId = row["id"]?.stringOrNull() ?: return null,
        duskreadId = prop("Duskread ID")?.get("rich_text")?.jsonArray.orEmpty().plainText().trim().takeIf { it.isNotBlank() },
        url = url,
        title = title.ifBlank { url },
        excerpt = prop("Excerpt")?.get("rich_text")?.jsonArray.orEmpty().plainText().trim().takeIf { it.isNotBlank() },
        topic = (prop("Topic")?.get("select") as? JsonObject)?.get("name")?.stringOrNull(),
        // Matched by name against both the stock spelling and the one a reading list
        // would rename it to, so the rename is safe either way.
        read = statusName == "Done" || statusName == ReadOption,
        saved = (prop("Saved")?.get("checkbox") as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false,
        dismissed = (prop("Dismissed")?.get("checkbox") as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false,
        savedAt = prop("Saved At")?.dateStart(),
        readAt = prop("Read At")?.dateStart(),
        lastEditedAt = row["last_edited_time"]?.stringOrNull()?.let(::parseIso) ?: 0L,
    )
}

@OptIn(ExperimentalTime::class)
private fun parseIso(value: String): Long? = runCatching { Instant.parse(value).toEpochMilliseconds() }.getOrNull()

@OptIn(ExperimentalTime::class)
private fun isoDate(epochMs: Long): String = Instant.fromEpochMilliseconds(epochMs).toString()
