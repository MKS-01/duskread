package dev.mks.stacks.trending

import dev.mks.stacks.data.KeyValueStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The dashboard's Trending card: what people in tech are actually publishing
 * about right now, not a curated list that goes stale between app updates.
 * This is the app's one network call — everything else ships in the bundle.
 *
 * dev.to's public articles API is the source: no API key, a `cover_image` per
 * article (most tech feeds without one make for a card with nothing to
 * show), and — unlike Reddit's JSON endpoints, which now 403 unauthenticated
 * requests as bot traffic — it is built to be read by third-party clients. No
 * `tag` filter is set: dev.to's own top-articles ranking is already tech and
 * programming broadly, not just AI, which is the point.
 */
@Serializable
private data class DevToArticle(
    val title: String,
    val description: String? = null,
    val url: String,
    @SerialName("cover_image") val coverImage: String? = null,
    @SerialName("public_reactions_count") val reactions: Int = 0,
    val user: DevToUser? = null,
)

@Serializable
private data class DevToUser(val name: String? = null)

@Serializable
data class TrendingItem(
    val id: String,
    val title: String,
    val description: String?,
    val meta: String,
    val imageUrl: String?,
    val url: String,
)

private val JsonCodec = Json { ignoreUnknownKeys = true }

/**
 * Throws on any network or parsing failure — the dashboard card decides how
 * to show that, since "say nothing" and "show an error" are both reasonable
 * depending on how prominent the card is.
 */
suspend fun fetchTrendingTech(limit: Int = 1): List<TrendingItem> {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(JsonCodec)
        }
    }
    try {
        val articles: List<DevToArticle> = client.get("https://dev.to/api/articles") {
            parameter("top", 7) // top articles from the last 7 days, i.e. "trending"
            parameter("per_page", limit)
        }.body()

        return articles.map { article ->
            TrendingItem(
                id = article.url,
                title = article.title,
                description = article.description,
                meta = article.user?.name?.let { "$it · ${article.reactions} reactions" }
                    ?: "${article.reactions} reactions",
                imageUrl = article.coverImage,
                url = article.url,
            )
        }
    } finally {
        client.close()
    }
}

private const val CacheItemKey = "trending_item_json"
private const val CacheTimeKey = "trending_cached_at_ms"
private const val CacheTtlMs = 24 * 60 * 60 * 1000L

/**
 * The card should feel current without hitting the network every time the
 * dashboard recomposes — once a day is often enough for something described
 * as "trending" to still be true. A failed refetch falls back to the stale
 * cached item rather than an error, since a day-old article beats none.
 */
@OptIn(ExperimentalTime::class)
suspend fun loadTrendingTech(store: KeyValueStore): TrendingItem {
    val cachedJson = store.getString(CacheItemKey)
    val cachedAt = store.getString(CacheTimeKey)?.toLongOrNull()
    val now = Clock.System.now().toEpochMilliseconds()

    if (cachedJson != null && cachedAt != null && now - cachedAt < CacheTtlMs) {
        return JsonCodec.decodeFromString(cachedJson)
    }

    return try {
        val fresh = fetchTrendingTech(limit = 1).first()
        store.putString(CacheItemKey, JsonCodec.encodeToString(fresh))
        store.putString(CacheTimeKey, now.toString())
        fresh
    } catch (e: Exception) {
        cachedJson?.let { JsonCodec.decodeFromString(it) } ?: throw e
    }
}
