package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.data.NotionTokenKey
import dev.mks.duskread.notion.NotionPage
import dev.mks.duskread.notion.NotionResult
import dev.mks.duskread.notion.Provisioning
import dev.mks.duskread.notion.provision
import dev.mks.duskread.notion.runFullSync

/**
 * Notion setup and sync, with every type Obj-C cannot carry left behind.
 */
class NotionBridge internal constructor(private val graph: AppGraph) {
    fun hasToken(): Boolean = graph.notionAuth.let { graph.keyValueStore.getString(NotionTokenKey) != null }

    fun saveToken(token: String) = graph.notionAuth.save(token)

    fun disconnect() {
        graph.notionAuth.disconnect()
        graph.notionPrefs.clear()
    }

    fun isConnected(): Boolean = graph.notionPrefs.sourcesDatabaseId != null

    fun lastSyncAt(): Long? = graph.notionPrefs.lastSyncAt

    fun observeLastSync(onEach: (Long?) -> Unit): Cancellable = graph.notionPrefs.lastSyncAtUpdates.watch(onEach)

    /**
     * Finds or creates the two databases.
     */
    suspend fun provisionDatabases(parentPageId: String?): NotionOutcome = when (val result = provision(graph.notionApi, graph.notionPrefs, parentPageId)) {
        is NotionResult.Ok -> when (val state = result.value) {
            is Provisioning.Ready -> NotionOutcome(status = NotionStatus.READY)
            is Provisioning.NeedsParent -> NotionOutcome(status = NotionStatus.NEEDS_PARENT, pages = state.pages)
            is Provisioning.NoPagesShared -> NotionOutcome(status = NotionStatus.NO_PAGES_SHARED)
        }
        // Every failure carries its own sentence already — the whole point of the sealed
        // hierarchy is that the wording lives with the case.
        is NotionResult.Failure -> NotionOutcome(status = result.status(), message = result.message)
    }

    /** Returns a one-line summary of what moved, or the failure message. */
    suspend fun sync(): NotionOutcome {
        val outcome = runFullSync(
            api = graph.notionApi,
            prefs = graph.notionPrefs,
            library = graph.links,
            feeds = graph.feeds,
            feedPosts = graph.feedPosts,
            http = graph.http,
            recordSync = { at -> graph.notionPrefs.recordSync(at) },
        )
        return NotionOutcome(status = NotionStatus.READY, message = outcome.toString())
    }
}

/** [Provisioning] and [NotionResult] flattened into something Swift can switch on. */
enum class NotionStatus {
    READY,
    NEEDS_PARENT,
    NO_PAGES_SHARED,
    NOT_CONNECTED,
    UNAUTHORIZED,
    NOT_FOUND,
    RATE_LIMITED,
    NETWORK,
    REJECTED,
    MALFORMED,
}

private fun NotionResult.Failure.status(): NotionStatus = when (this) {
    is NotionResult.NotConnected -> NotionStatus.NOT_CONNECTED
    is NotionResult.Unauthorized -> NotionStatus.UNAUTHORIZED
    is NotionResult.NotFound -> NotionStatus.NOT_FOUND
    is NotionResult.RateLimited -> NotionStatus.RATE_LIMITED
    is NotionResult.Network -> NotionStatus.NETWORK
    is NotionResult.Rejected -> NotionStatus.REJECTED
    is NotionResult.Malformed -> NotionStatus.MALFORMED
}

data class NotionOutcome(
    val status: NotionStatus,
    val message: String? = null,
    val pages: List<NotionPage> = emptyList(),
)
