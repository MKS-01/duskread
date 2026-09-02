package dev.mks.duskread.notion

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * What a Notion call can come back as.
 *
 * Separate cases rather than an exception or a nullable, because Settings has
 * to tell the reader *which* thing is wrong and each one has a different fix:
 * a rejected token is retyped, a missing database is re-copied, a rate limit
 * is waited out, and a network failure is nobody's fault. Collapsing them into
 * "sync failed" would be the difference between a screen that helps and one
 * that shrugs.
 */
sealed class NotionResult<out T> {
    data class Ok<out T>(val value: T) : NotionResult<T>()

    /** Every failure carries the line Settings shows, so no caller writes copy. */
    sealed class Failure(val message: String) : NotionResult<Nothing>()

    data object NotConnected : Failure("Not connected")

    data object Unauthorized : Failure("Token rejected — check it was copied whole")

    data object NotFound : Failure("Database not found — check the ID")

    data object RateLimited : Failure("Notion is rate limiting — try again shortly")

    data class Network(val detail: String) : Failure("Could not reach Notion")

    /**
     * A 400: the request was reached, understood and refused.
     *
     * Split out from [Network] because the two have nothing in common except
     * that neither worked. This one carries Notion's own body, which is the
     * only way to tell *why* something was refused — the status-property
     * fallback in `NotionProvision.kt` turns on reading it.
     */
    data class Rejected(val detail: String) : Failure("Notion refused that — $detail")

    data class Malformed(val detail: String) : Failure("Notion sent something unexpected")
}

/** Runs [block] only if this succeeded, so a caller can chain without unwrapping twice. */
inline fun <T, R> NotionResult<T>.then(block: (T) -> NotionResult<R>): NotionResult<R> = when (this) {
    is NotionResult.Ok -> block(value)
    is NotionResult.Failure -> this
}

/**
 * The Notion REST API, reduced to the calls this app makes.
 *
 * Wraps the shared [HttpClient] rather than configuring one, so the four
 * platform `createHttpClient()` actuals stay the one-liners they are. Reading
 * is `bodyAsText()` into a [JsonObject]: Notion's property values are
 * variant-typed — a `url`, a `select` and a `multi_select` share no shape —
 * so walking the tree is genuinely smaller than modelling every case, and it
 * degrades to null instead of throwing when a column is missing.
 */
class NotionClient(
    private val client: HttpClient,
    private val auth: NotionAuth,
) {
    /**
     * Every row of a database, following `next_cursor` to the end.
     *
     * Paging is not optional even at eighteen rows: Notion caps a page at 100
     * and the caller has no way to know it was truncated, so a table that
     * quietly grows past the cap would start silently dropping feeds.
     */
    suspend fun queryAll(databaseId: String): NotionResult<List<JsonObject>> {
        val rows = mutableListOf<JsonObject>()
        var cursor: String? = null

        while (true) {
            val body = """{"page_size":$PageSize${cursor?.let { ""","start_cursor":"$it"""" } ?: ""}}"""

            val page = request { token ->
                client.post("$ApiBase/databases/${databaseId.trim()}/query") {
                    notionHeaders(token)
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }

            when (page) {
                is NotionResult.Failure -> return page
                is NotionResult.Ok -> {
                    page.value["results"]?.jsonArray.orEmpty().forEach { row ->
                        (row as? JsonObject)?.let(rows::add)
                    }

                    val more = page.value["has_more"]?.jsonPrimitive?.booleanOrNull ?: false
                    cursor = page.value["next_cursor"]?.stringOrNull()
                    if (!more || cursor == null) return NotionResult.Ok(rows)
                }
            }
        }
    }

    /**
     * The database's own schema.
     *
     * Wanted for one thing: the names of the `Status` options. Notion's DDL
     * refuses to rename them from the stock `Not started` / `Done`, so anyone
     * wanting a reading list to say `Unread` / `Read` does it by hand — and
     * hard-coding either spelling would break the moment they did. Reading the
     * schema costs one request per sync and makes the rename a non-event.
     */
    suspend fun schema(databaseId: String): NotionResult<JsonObject> = request { token ->
        client.get("$ApiBase/databases/${databaseId.trim()}") { notionHeaders(token) }
    }

    /**
     * Titles matching [query], of one `object` type — `"database"` or `"page"`.
     *
     * Only ever the first page of results, unlike [queryAll], and deliberately:
     * Notion filters by title server-side when [query] is set, so a hundred
     * matches for a name as specific as "DuskRead Sources" would already mean
     * something has gone very wrong. The unfiltered call — used to list pages
     * that could host the databases — is capped for the same reason in
     * reverse: nobody is going to scroll past a hundred to pick one.
     *
     * Search only sees what the credential has been given, which is the whole
     * reason setup can end at "share one page": an empty result here is not an
     * error, it is the instruction.
     */
    suspend fun search(objectType: String, query: String? = null): NotionResult<List<JsonObject>> {
        val body = buildJsonObject {
            query?.let { put("query", JsonPrimitive(it)) }
            put(
                "filter",
                buildJsonObject {
                    put("property", JsonPrimitive("object"))
                    put("value", JsonPrimitive(objectType))
                },
            )
            // Most-recently-touched first, so the page someone just shared with
            // the integration is the one at the top of the picker.
            put(
                "sort",
                buildJsonObject {
                    put("direction", JsonPrimitive("descending"))
                    put("timestamp", JsonPrimitive("last_edited_time"))
                },
            )
            put("page_size", JsonPrimitive(PageSize))
        }

        return request { token ->
            client.post("$ApiBase/search") {
                notionHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        }.then { page ->
            NotionResult.Ok(page["results"]?.jsonArray.orEmpty().mapNotNull { it as? JsonObject })
        }
    }

    /**
     * A plain page under [parentPageId], returning its id.
     *
     * Exists because a database cannot be created at the workspace root — the
     * API requires a page parent — so the databases need something of their
     * own to live in rather than being scattered into whichever page the
     * reader happened to share.
     */
    suspend fun createSubPage(parentPageId: String, title: String): NotionResult<String> {
        val body = buildJsonObject {
            put("parent", buildJsonObject { put("page_id", JsonPrimitive(parentPageId.trim())) })
            put(
                "properties",
                buildJsonObject {
                    put(
                        "title",
                        buildJsonObject {
                            put(
                                "title",
                                buildJsonArray {
                                    add(buildJsonObject { put("text", buildJsonObject { put("content", JsonPrimitive(title)) }) })
                                },
                            )
                        },
                    )
                },
            )
        }

        return write { token ->
            client.post("$ApiBase/pages") {
                notionHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        }.then { page ->
            page["id"]?.stringOrNull()?.let { NotionResult.Ok(it) } ?: NotionResult.Malformed("no page id")
        }
    }

    /**
     * A database under [parentPageId], returning its id.
     *
     * [properties] is the schema, in Notion's own DDL shape — see
     * `NotionProvision.kt`, which is the only caller and holds the two schemas
     * this app knows how to read back.
     */
    suspend fun createDatabase(
        parentPageId: String,
        title: String,
        properties: JsonObject,
    ): NotionResult<String> {
        val body = buildJsonObject {
            put("parent", buildJsonObject { put("page_id", JsonPrimitive(parentPageId.trim())) })
            put(
                "title",
                buildJsonArray {
                    add(buildJsonObject { put("text", buildJsonObject { put("content", JsonPrimitive(title)) }) })
                },
            )
            put("properties", properties)
        }

        return write { token ->
            client.post("$ApiBase/databases") {
                notionHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        }.then { database ->
            database["id"]?.stringOrNull()?.let { NotionResult.Ok(it) } ?: NotionResult.Malformed("no database id")
        }
    }

    /** Creates a row, returning its page id. */
    suspend fun createPage(databaseId: String, properties: JsonObject): NotionResult<String> {
        val body = buildJsonObject {
            put("parent", buildJsonObject { put("database_id", JsonPrimitive(databaseId.trim())) })
            put("properties", properties)
        }

        return write { token ->
            client.post("$ApiBase/pages") {
                notionHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        }.then { page ->
            page["id"]?.stringOrNull()?.let { NotionResult.Ok(it) } ?: NotionResult.Malformed("no page id")
        }
    }

    /** Updates a row in place. Only the properties named are touched; everything else is left alone. */
    suspend fun updatePage(pageId: String, properties: JsonObject): NotionResult<Unit> = write { token ->
        client.patch("$ApiBase/pages/$pageId") {
            notionHeaders(token)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("properties", properties) }.toString())
        }
    }.then { NotionResult.Ok(Unit) }

    /**
     * A write, paced.
     *
     * Notion allows roughly three requests a second, and a first push is one
     * request per saved link — enough to walk straight into the limiter. The
     * 429 handling in [request] is a recovery; this is the policy that means
     * it rarely has to fire. Reads are not paced because there is only ever
     * one of them per sync.
     */
    private suspend fun write(call: suspend (String) -> HttpResponse): NotionResult<JsonObject> {
        delay(WriteSpacingMs)
        return request(call)
    }

    /**
     * One call, with the token attached and the two failure shapes Notion
     * imposes handled: a status code that means something specific, and a 429
     * that wants waiting out.
     *
     * The retry is bounded at [MaxAttempts] and honours `Retry-After` when
     * Notion sends one. Notion allows roughly three requests a second, which
     * this app will never approach — the backoff is here so a shared-workspace
     * burst degrades into a slower sync rather than a failed one.
     */
    private suspend fun request(call: suspend (String) -> HttpResponse): NotionResult<JsonObject> {
        val token = auth.bearer() ?: return NotionResult.NotConnected
        var wait = InitialBackoffMs

        repeat(MaxAttempts) { attempt ->
            val response = runCatching { call(token) }
                .getOrElse { return NotionResult.Network(it.message ?: it::class.simpleName.orEmpty()) }

            when (response.status.value) {
                in 200..299 -> return runCatching {
                    NotionResult.Ok(Json.parseToJsonElement(response.bodyAsText()).jsonObject)
                }.getOrElse { NotionResult.Malformed(it.message.orEmpty()) }

                401 -> return NotionResult.Unauthorized
                // Notion answers 404 both for a database that does not exist
                // and for one this credential cannot see. Indistinguishable
                // from here, and the same fix either way.
                403, 404 -> return NotionResult.NotFound

                // The body, not just the code: a refused schema names the
                // property it objected to and nothing else can.
                400 -> return NotionResult.Rejected(
                    runCatching { response.bodyAsText() }.getOrDefault("").take(300),
                )

                429 -> {
                    if (attempt == MaxAttempts - 1) return NotionResult.RateLimited
                    val after = response.headers["Retry-After"]?.toLongOrNull()?.times(1000)
                    delay(after ?: wait)
                    wait *= 2
                }

                else -> return NotionResult.Network("HTTP ${response.status.value}")
            }
        }

        return NotionResult.RateLimited
    }

    private fun HttpRequestBuilder.notionHeaders(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
        header(NotionVersionHeader, NotionVersion)
    }

    private companion object {
        const val ApiBase = "https://api.notion.com/v1"

        /**
         * Pinned, not "latest". Notion versions its API by date and a newer
         * one reshapes databases into data sources; this app asks for the
         * shape it was written against and will keep getting it.
         */
        const val NotionVersionHeader = "Notion-Version"
        const val NotionVersion = "2022-06-28"

        const val PageSize = 100
        const val MaxAttempts = 3
        const val InitialBackoffMs = 1_000L

        /** Just under Notion's ~3 requests a second, so a long push never reaches the limiter. */
        const val WriteSpacingMs = 350L
    }
}

/** Notion writes every piece of text as an array of runs; this is the whole string. */
internal fun List<JsonElement>.plainText(): String = joinToString("") { run ->
    (run as? JsonObject)?.get("plain_text")?.stringOrNull().orEmpty()
}

/** The string value, or null for JSON null — which Notion uses freely for empty columns. */
internal fun JsonElement.stringOrNull(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
