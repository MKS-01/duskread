package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.SecretStore

/**
 * Everything the SwiftUI shell is allowed to touch, behind one root.
 *
 * Swift cannot enter a composition, so none of the `rememberX()` factories are
 * reachable from it; and it cannot see `HttpClient` or `JsonObject`, because
 * ktor and kotlinx-serialization are `implementation` dependencies that are not
 * exported to the framework. Both problems have the same answer — a facade that
 * owns the graph and hands out only types Obj-C can carry.
 *
 * **Storage comes from Swift.** [KeyValueStore] and [SecretStore] are ports,
 * and the iOS adapters for them are written in Swift rather than here: that is
 * what lets the token live in the Keychain instead of `NSUserDefaults` in the
 * clear, which the Kotlin-side `PlaintextSecretStore` never could. Kotlin keeps
 * the contract, Swift keeps the platform.
 *
 * Note for the Swift side: Kotlin's default method bodies on [KeyValueStore]
 * (`getBoolean`/`putBoolean`) do not survive the Obj-C export, so a conforming
 * Swift type has to implement all four members, not just the two without
 * defaults.
 *
 * Deliberately not a singleton on the Kotlin side. Swift creates one at launch
 * and holds it; making it global here would put the lifetime in the wrong
 * language and make it impossible to stand a second one up in a preview.
 */
class DuskReadBridge(store: KeyValueStore, secrets: SecretStore) {
    private val graph = AppGraph(store, secrets)

    val links = LinksBridge(graph)
    val feeds = FeedsBridge(graph)
    val prefs = PrefsBridge(graph)
    val signals = SignalsBridge(graph)
    val pomodoro = PomodoroBridge()
    val design = DesignBridge()

    fun close() = graph.close()
}
