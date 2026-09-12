package dev.mks.duskread.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.ReadingSignals
import dev.mks.duskread.links.createHttpClient
import dev.mks.duskread.notion.NotionClient
import dev.mks.duskread.notion.NotionPrefs
import dev.mks.duskread.notion.PastedTokenAuth
import dev.mks.duskread.summary.SummaryCache
import io.ktor.client.HttpClient

/**
 * The app's one object graph.
 *
 * Until now every library was built by a `remember` inside whichever screen
 * happened to need it first, which had two costs. Settings and the summary
 * panel each constructed a *second* `LinkLibrary` and `SummaryCache` over the
 * same store, and three different screens each opened their own `HttpClient`
 * — three connection pools for one app. More importantly a composition-scoped
 * object cannot be reached from outside Compose, and the SwiftUI iOS shell has
 * to construct all of this without entering one.
 *
 * Still no DI container and no ViewModel: this is a plain class holding plain
 * constructors, created once and read through [LocalAppGraph] on the Compose
 * side and directly on the Swift side.
 *
 * The HTTP client and the Notion API are deliberately `internal`. ktor is an
 * `implementation` dependency and nothing is exported to the iOS framework, so
 * an `HttpClient` in a public signature would be uncallable from Swift anyway;
 * keeping them internal means the iOS bridge is forced to wrap them rather
 * than leak them.
 */
class AppGraph(
    private val store: KeyValueStore,
    secrets: SecretStore,
) {
    val prefs = UserPrefs(store)
    val links = LinkLibrary(store)
    val feeds = FeedLibrary(store)
    val feedPosts = FeedPostCache(store)
    val signals = ReadingSignals(store)
    val summaries = SummaryCache(store)
    val notionPrefs = NotionPrefs(store)
    val notionAuth = PastedTokenAuth(secrets)

    internal val http: HttpClient = createHttpClient()
    internal val notionApi = NotionClient(http, notionAuth)

    /** The store itself, for the few callers that still key their own state off it. */
    val keyValueStore: KeyValueStore get() = store

    fun close() = http.close()
}

/**
 * No default: a graph cannot be conjured without a platform store, so reading
 * this outside the provider is a wiring mistake and should say so loudly
 * rather than hand back a second, empty app.
 */
val LocalAppGraph: ProvidableCompositionLocal<AppGraph> =
    compositionLocalOf { error("No AppGraph provided — wrap the tree in ProvideAppGraph") }

/**
 * Builds the graph once and publishes it.
 *
 * Composable because the platform stores are: Android needs the local
 * `Context` to reach both its preferences file and its keystore. The client is
 * closed with the composition rather than never, which matters on desktop
 * where the window can be reopened.
 */
@Composable
fun ProvideAppGraph(content: @Composable () -> Unit) {
    val store = rememberKeyValueStore()
    val secrets = rememberSecretStore()
    val graph = remember(store, secrets) { AppGraph(store, secrets) }
    DisposableEffect(graph) { onDispose { graph.close() } }
    CompositionLocalProvider(LocalAppGraph provides graph, content = content)
}
