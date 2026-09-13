package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.SecretStore

/**
 * Everything the SwiftUI shell is allowed to touch, behind one root.
 */
class DuskReadBridge(store: KeyValueStore, secrets: SecretStore) {
    private val graph = AppGraph(store, secrets)

    val links = LinksBridge(graph)
    val feeds = FeedsBridge(graph)
    val prefs = PrefsBridge(graph)
    val signals = SignalsBridge(graph)
    val pomodoro = PomodoroBridge()
    val design = DesignBridge()
    val notion = NotionBridge(graph)
    val speaker = SpeakerBridge(graph)

    fun close() {
        speaker.stop()
        graph.close()
    }
}
