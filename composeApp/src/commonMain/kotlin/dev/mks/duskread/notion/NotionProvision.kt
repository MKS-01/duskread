package dev.mks.duskread.notion

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/*
 * Finding or building the two databases, so nobody has to build them by hand.
 *
 * This file exists because the old setup was unshippable. It asked a reader to
 * create two Notion databases with fourteen exactly-spelled properties between
 * them and then paste two database IDs into two masked fields — a ritual with
 * its own documentation because it could not be held in a head. Everything
 * here replaces that with: paste a token.
 *
 * **The one step that could not be removed.** Notion's API will not create a
 * database, or even a page, at the workspace root: `parent` must be a page
 * that already exists. So a credential that can reach nothing can build
 * nothing, and the floor is "share one page with the token". `NotionAuth`
 * explains why OAuth — which would move that step into an authorisation screen
 * — needs a server this project does not have.
 */

/** A page the credential can reach, offered as a home for the two databases. */
data class NotionPage(val id: String, val title: String)

/**
 * Which of the two databases a resolution is for, and the names it answers to.
 *
 * Two titles each, and the second is the whole migration story: this app's
 * author already has tables called `Sources` and `Reading List` that predate
 * every line in this file. Matching the bare name adopts them instead of
 * building a second pair beside them, which would silently split one library
 * in two.
 */
enum class NotionDatabaseKind(val title: String, val legacyTitle: String) {
    Sources("DuskRead Sources", "Sources"),
    ReadingList("DuskRead Reading List", "Reading List"),
}

/** Where a connection got to, and what it needs next. */
sealed class Provisioning {
    /** Both databases exist and their ids are stored. */
    data class Ready(val sourcesId: String, val readingId: String) : Provisioning()

    /**
     * More than one page could host them, so the reader picks.
     *
     * Only ever raised when the choice is real: a credential with exactly one
     * accessible page is not asked a question it has one answer to.
     */
    data class NeedsParent(val pages: List<NotionPage>) : Provisioning()

    /** The token works but reaches nothing. Not an error — the next instruction. */
    data object NoPagesShared : Provisioning()
}

/**
 * Resolves both databases, creating whatever is missing.
 *
 * The order is stored id, then search, then create, and it matters:
 *
 * 1. **A stored id wins outright.** This is what makes the change invisible to
 *    an install that was already working — it never searches, never creates,
 *    and never notices any of this happened.
 * 2. **Search by title**, preferred name then bare name. One request per
 *    missing database, once per install, since the answer is then stored.
 * 3. **Create.** Needs a parent, which is where [Provisioning.NeedsParent] and
 *    [Provisioning.NoPagesShared] come from.
 *
 * [parentPageId] is the reader's answer to a previous `NeedsParent`. Passing
 * it skips the page search entirely.
 */
suspend fun provision(
    api: NotionClient,
    prefs: NotionPrefs,
    parentPageId: String? = null,
): NotionResult<Provisioning> {
    val sources = resolve(api, prefs.sourcesDatabaseId, NotionDatabaseKind.Sources)
    val reading = resolve(api, prefs.readingDatabaseId, NotionDatabaseKind.ReadingList)

    val sourcesId = when (sources) {
        is NotionResult.Failure -> return sources
        is NotionResult.Ok -> sources.value
    }
    val readingId = when (reading) {
        is NotionResult.Failure -> return reading
        is NotionResult.Ok -> reading.value
    }

    if (sourcesId != null && readingId != null) {
        prefs.updateDatabaseId(sourcesId)
        prefs.updateReadingDatabaseId(readingId)
        return NotionResult.Ok(Provisioning.Ready(sourcesId, readingId))
    }

    // Something has to be built, so a home for it has to be settled first.
    val parent = parentPageId ?: prefs.parentPageId
    if (parent == null) {
        return when (val pages = accessiblePages(api)) {
            is NotionResult.Failure -> pages
            is NotionResult.Ok -> when (pages.value.size) {
                0 -> NotionResult.Ok(Provisioning.NoPagesShared)
                // One page is not a choice. Asking anyway would be a screen
                // whose only button is the only option.
                1 -> provision(api, prefs, pages.value.first().id)
                else -> NotionResult.Ok(Provisioning.NeedsParent(pages.value))
            }
        }
    }

    // A container of its own rather than dropping two databases into whatever
    // page was shared: that page is the reader's, and this is the app's.
    val home = when (val existing = prefs.homePageId) {
        null -> when (val created = api.createSubPage(parent, HomePageTitle)) {
            is NotionResult.Failure -> return created
            is NotionResult.Ok -> created.value.also {
                prefs.updateParentPageId(parent)
                prefs.updateHomePageId(it)
            }
        }

        else -> existing
    }

    val builtSources = sourcesId ?: when (val created = createSources(api, home)) {
        is NotionResult.Failure -> return created
        is NotionResult.Ok -> created.value
    }
    val builtReading = readingId ?: when (val created = createReadingList(api, home)) {
        is NotionResult.Failure -> return created
        is NotionResult.Ok -> created.value
    }

    prefs.updateDatabaseId(builtSources)
    prefs.updateReadingDatabaseId(builtReading)
    return NotionResult.Ok(Provisioning.Ready(builtSources, builtReading))
}

/** A stored id, or a search for one, or null meaning "does not exist yet". */
private suspend fun resolve(
    api: NotionClient,
    stored: String?,
    kind: NotionDatabaseKind,
): NotionResult<String?> {
    stored?.takeIf { it.isNotBlank() }?.let { return NotionResult.Ok(it) }

    return findDatabase(api, kind.title).then { byPreferred ->
        if (byPreferred != null) NotionResult.Ok(byPreferred) else findDatabase(api, kind.legacyTitle)
    }
}

/**
 * The id of the database titled exactly [title], or null.
 *
 * Exact, case-insensitive, and after trimming — Notion's search matches
 * substrings, so a query for `Sources` also returns `Sources (old)` and
 * `Newsletter Sources`. Adopting either of those would be worse than building
 * a fresh table, because the damage would be quiet and in someone else's data.
 */
internal suspend fun findDatabase(api: NotionClient, title: String): NotionResult<String?> = api.search(objectType = "database", query = title).then { results ->
    val match = results.firstOrNull { database ->
        database["title"]?.jsonArray.orEmpty().plainText().trim().equals(title, ignoreCase = true)
    }
    NotionResult.Ok(match?.get("id")?.stringOrNull())
}

/**
 * Pages that could host the databases.
 *
 * Rows are excluded — a page whose parent is a database is a record inside
 * someone's table, and creating the app's home page inside one would put two
 * databases in a cell. Only pages parented by the workspace or by another page
 * are real places.
 */
internal suspend fun accessiblePages(api: NotionClient): NotionResult<List<NotionPage>> = api.search(objectType = "page").then { results ->
    NotionResult.Ok(
        results.mapNotNull { page ->
            val parentType = (page["parent"] as? JsonObject)?.get("type")?.stringOrNull()
            if (parentType != "workspace" && parentType != "page_id") return@mapNotNull null

            val id = page["id"]?.stringOrNull() ?: return@mapNotNull null
            NotionPage(id = id, title = pageTitle(page).ifBlank { "Untitled" })
        },
    )
}

/**
 * A page's title, whatever its title column is called.
 *
 * The key is not fixed: a workspace-level page uses `title`, a page inside a
 * database uses whatever that database named its title property. Finding the
 * one property of `"type": "title"` is the only way that holds for both.
 */
private fun pageTitle(page: JsonObject): String {
    val properties = page["properties"]?.jsonObject ?: return ""
    val title = properties.values
        .mapNotNull { it as? JsonObject }
        .firstOrNull { it["type"]?.stringOrNull() == "title" }
        ?: return ""

    return title["title"]?.jsonArray.orEmpty().plainText().trim()
}

/* ----------------------------------------------------------------------------
 * The two schemas. Every property name here is read back by `parseSource` or
 * `parseArticle`, so this is one half of a contract and those are the other —
 * a rename in either place has to happen in both.
 * ------------------------------------------------------------------------- */

private suspend fun createSources(api: NotionClient, homePageId: String): NotionResult<String> = api.createDatabase(
    parentPageId = homePageId,
    title = NotionDatabaseKind.Sources.title,
    properties = buildJsonObject {
        put(NameProperty, type("title"))
        put(FeedUrlProperty, type("url"))
        put(TopicProperty, select())
        put(ActiveProperty, type("checkbox"))
        put(SourceIdProperty, type("rich_text"))
    },
)

/**
 * The reading list, with the status column's two spellings tried in turn.
 *
 * `status` is the shape the table wants: it groups its options into `To-do`
 * and `Complete`, which is what lets `statusNames` survive someone renaming
 * `Not started` to `Unread` by hand. But creating one has never been reliable
 * across Notion's API versions, and a refusal here would otherwise cost the
 * reader the whole setup rather than one column.
 *
 * So a 400 falls back to a `select` of the same two names. It is the weaker
 * shape — a select carries no groups, so the rename that `status` survives
 * would break it — but a working table with a plainer column beats a failed
 * connection, and everything downstream reads both (see `statusNames`).
 */
private suspend fun createReadingList(api: NotionClient, homePageId: String): NotionResult<String> {
    fun schema(status: JsonObject) = buildJsonObject {
        put("Title", type("title"))
        put("URL", type("url"))
        put("Duskread ID", type("rich_text"))
        put("Excerpt", type("rich_text"))
        put("Saved", type("checkbox"))
        put("Dismissed", type("checkbox"))
        put("Saved At", type("date"))
        put("Read At", type("date"))
        put(TopicProperty, select())
        put("Status", status)
    }

    val withStatus = api.createDatabase(
        parentPageId = homePageId,
        title = NotionDatabaseKind.ReadingList.title,
        properties = schema(
            buildJsonObject {
                put(
                    "status",
                    buildJsonObject {
                        put(
                            "options",
                            buildJsonArray {
                                add(option(UnreadOption, "To-do"))
                                add(option(ReadOption, "Complete"))
                            },
                        )
                    },
                )
            },
        ),
    )

    // Only a refusal falls back. A network failure or a rate limit would come
    // back the same way from the retry, and turning either into a downgraded
    // schema would be a silent loss on a fault that had nothing to do with it.
    if (withStatus !is NotionResult.Rejected) return withStatus

    return api.createDatabase(
        parentPageId = homePageId,
        title = NotionDatabaseKind.ReadingList.title,
        properties = schema(
            buildJsonObject {
                put(
                    "select",
                    buildJsonObject {
                        put(
                            "options",
                            buildJsonArray {
                                add(buildJsonObject { put("name", JsonPrimitive(UnreadOption)) })
                                add(buildJsonObject { put("name", JsonPrimitive(ReadOption)) })
                            },
                        )
                    },
                )
            },
        ),
    )
}

/** Notion's DDL for a property with no configuration: `{"url": {}}`. */
private fun type(name: String): JsonObject = buildJsonObject { put(name, buildJsonObject { }) }

/**
 * A select with no options declared.
 *
 * Deliberately empty: writing a select value Notion has not seen adds the
 * option, so the topics a reader ends up with are the ones their feeds
 * actually carry rather than a list guessed here.
 */
private fun select(): JsonObject = buildJsonObject {
    put("select", buildJsonObject { put("options", buildJsonArray { }) })
}

private fun option(name: String, group: String): JsonObject = buildJsonObject {
    put("name", JsonPrimitive(name))
    put("group", JsonPrimitive(group))
}

/** The page the two databases are created inside, named for the app that owns them. */
private const val HomePageTitle = "DuskRead"

internal const val UnreadOption = "Unread"
internal const val ReadOption = "Read"
