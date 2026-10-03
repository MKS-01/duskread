package dev.mks.duskread.notion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.Observed
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedSyncer
import dev.mks.duskread.links.LinkLibrary
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The one Notion sync, owned by the graph like [FeedSyncer] so it outlives the screen that
 * asked and a second caller joins the first instead of racing it.
 */
@OptIn(ExperimentalTime::class)
class NotionSyncer(
    private val api: NotionClient,
    private val auth: NotionAuth,
    private val prefs: NotionPrefs,
    private val links: LinkLibrary,
    private val feeds: FeedLibrary,
    private val feedSync: FeedSyncer,
    private val http: HttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val observedRunning = Observed(false)

    var running: Boolean by observedRunning
        private set

    /** [running] for observers outside a composition; see [Observed]. */
    val runningUpdates: StateFlow<Boolean> get() = observedRunning.updates

    private var inFlight: Deferred<SyncOutcome>? = null

    /**
     * Starts a sync, or joins the one already running. [force] skips the change checks —
     * the Settings button, the escape hatch if a check ever misses something.
     */
    suspend fun sync(force: Boolean = false): SyncOutcome {
        val current = inFlight?.takeIf { it.isActive } ?: scope.async { run(force) }.also { inFlight = it }
        return current.await()
    }

    /**
     * The launch trigger: fire and forget, silent, and only when due — anything saved, read
     * or retitled since the last sync overrides the clock.
     */
    fun syncIfDue() {
        scope.launch {
            if (auth.bearer() == null) return@launch
            val since = prefs.lastSyncAt ?: 0L
            val unpushed = links.links.any { it.changedAt > since } || links.removedUrls.values.any { it > since }
            if (!prefs.dueForSync(Clock.System.now().toEpochMilliseconds(), unpushed)) return@launch
            runCatching { sync() }
        }
    }

    fun close() = scope.cancel()

    private suspend fun run(force: Boolean): SyncOutcome {
        running = true
        try {
            return runFullSync(api, prefs, links, feeds, feedSync, http, recordSync = prefs::recordSync, force = force)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A joined caller should hear a sentence, not inherit a crash from a sync it
            // did not start.
            return SyncOutcome(e.message ?: "Sync failed", ok = false)
        } finally {
            running = false
        }
    }
}
