package dev.mks.duskread.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import dev.mks.duskread.db.DuskReadDatabase
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.FeedSyncer
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.ReadingSignals
import dev.mks.duskread.links.createHttpClient
import dev.mks.duskread.notion.NotionClient
import dev.mks.duskread.notion.NotionPrefs
import dev.mks.duskread.notion.NotionSyncer
import dev.mks.duskread.notion.PastedTokenAuth
import dev.mks.duskread.summary.SummaryCache
import io.ktor.client.HttpClient

/**
 * The app's one object graph. Until now every library was built by a `remember` inside
 * whichever screen happened to need it first, which had two costs.
 */
class AppGraph(
    private val store: KeyValueStore,
    secrets: SecretStore,
    database: DuskReadDatabase,
) {
    val prefs = UserPrefs(store)
    val links = LinkLibrary(store)
    val feeds = FeedLibrary(store)
    val feedPosts = FeedPostCache(store, database)
    val signals = ReadingSignals(store)
    val summaries = SummaryCache(store)
    val notionPrefs = NotionPrefs(store)
    val notionAuth = PastedTokenAuth(secrets)

    internal val http: HttpClient = createHttpClient()
    internal val notionApi = NotionClient(http, notionAuth)
    val feedSync = FeedSyncer(http, feeds, feedPosts, links, summaries)
    val notionSync = NotionSyncer(notionApi, notionAuth, notionPrefs, links, feeds, feedSync, http)

    /** The store itself, for the few callers that still key their own state off it. */
    val keyValueStore: KeyValueStore get() = store

    fun close() {
        notionSync.close()
        feedSync.close()
        http.close()
    }
}

/**
 * No default: a graph cannot be conjured without a platform store.
 */
val LocalAppGraph: ProvidableCompositionLocal<AppGraph> =
    compositionLocalOf { error("No AppGraph provided — wrap the tree in ProvideAppGraph") }

/**
 * Builds the graph once and publishes it. Composable because the platform stores are:
 * Android needs the local `Context` to reach both its preferences file and its keystore.
 */
@Composable
fun ProvideAppGraph(content: @Composable () -> Unit) {
    val store = rememberKeyValueStore()
    val secrets = rememberSecretStore()
    val database = rememberDatabase()
    val graph = remember(store, secrets, database) { AppGraph(store, secrets, database) }
    DisposableEffect(graph) { onDispose { graph.close() } }
    CompositionLocalProvider(LocalAppGraph provides graph, content = content)
}
