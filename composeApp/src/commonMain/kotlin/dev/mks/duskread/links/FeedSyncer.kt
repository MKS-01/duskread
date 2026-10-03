package dev.mks.duskread.links

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.DataEpoch
import dev.mks.duskread.data.Observed
import dev.mks.duskread.summary.SummaryCache
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * Where the one feed sync stands. Every screen reads this rather than keeping its own
 * "syncing" flag, so a sync started anywhere shows everywhere.
 */
data class FeedSyncState(
    val running: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
) {
    /** Never 0 while running, so the bar shows at once; 1 only once the sync has landed. */
    val progress: Float? get() = if (running) (done + 1f) / (total + 1) else null

    val label: String get() = "Syncing $done/$total"
}

/** What a finished sync found, worded once so both platforms' toasts agree. */
data class FeedSyncResult(
    val reached: Int,
    val total: Int,
    val newPosts: Int,
    val manual: Boolean,
    val erased: Boolean = false,
) {
    /** A sync nobody asked for speaks only when it brought something. */
    val worthSaying: Boolean get() = !erased && (manual || newPosts > 0)

    val line: String get() {
        if (reached == 0) return "Couldn't reach any feed"
        val news = when (newPosts) {
            0 -> if (reached == total) return "Up to date" else "No new posts"
            1 -> "1 new post"
            else -> "$newPosts new posts"
        }
        return if (reached < total) "$news · $reached of $total feeds answered" else news
    }
}

/**
 * The only way feeds get fetched. A caller arriving mid-sync joins the one in the air
 * instead of starting a second, and the sync outlives whichever screen started it.
 */
class FeedSyncer(
    private val client: HttpClient,
    private val feeds: FeedLibrary,
    private val cache: FeedPostCache,
    private val links: LinkLibrary,
    private val summaries: SummaryCache,
) {
    // Main, so state writes stay on one thread; every parse, body scan and encode below
    // hops to Default, which is what kept a sync from stuttering the list.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val observedState = Observed(FeedSyncState())

    var state: FeedSyncState by observedState
        private set

    /** [state] for observers outside a composition; see [Observed]. */
    val stateUpdates: StateFlow<FeedSyncState> get() = observedState.updates

    private val finished = MutableSharedFlow<FeedSyncResult>(extraBufferCapacity = 1)

    /** Each finished sync, once, for the toast. */
    val results: SharedFlow<FeedSyncResult> get() = finished

    private var inFlight: Deferred<FeedSyncResult>? = null

    // A manual caller that joins an automatic sync still wants to hear how it went.
    private var manualJoined = false

    // In memory only: offline, a failed round should hold off the next resume, but a
    // fresh launch may well have a network again.
    private var attemptedAt: Long? = null

    /** Starts a sync, or joins the one already running. Null with nothing followed. */
    suspend fun sync(manual: Boolean = true): FeedSyncResult? {
        if (feeds.feeds.isEmpty()) return null
        if (manual) manualJoined = true
        val running = inFlight?.takeIf { it.isActive } ?: scope.async { run() }.also { inFlight = it }
        return running.await()
    }

    /** [sync] only when the last one is older than [StaleAfterMs]; null when skipped. */
    suspend fun syncIfStale(now: Long): FeedSyncResult? {
        val last = maxOf(cache.syncedAt ?: 0L, attemptedAt ?: 0L)
        if (now - last in 0..StaleAfterMs) return null
        return sync(manual = false)
    }

    /** [sync] without waiting on it, for callers that only mean to start one. */
    fun syncInBackground() {
        scope.launch { sync(manual = false) }
    }

    fun close() = scope.cancel()

    private suspend fun run(): FeedSyncResult {
        val list = feeds.feeds
        // What the fetch below is about. An erase can land while a dozen feeds are in
        // the air; see [DataEpoch].
        val epoch = DataEpoch.mark()
        val cached = cache.postsByFeed
        val known = withContext(Dispatchers.Default) { cached.values.flatten().mapTo(HashSet()) { it.url } }
        state = FeedSyncState(running = true, total = list.size)
        try {
            val gate = Semaphore(ParallelFetches)
            // What each feed calls itself, so a blog followed by address gets a real name.
            val titles = mutableMapOf<String, String>()
            val fetched = coroutineScope {
                list.map { feed ->
                    async {
                        val document = gate.withPermit { runCatching { fetchFeedDocument(client, feed.url) }.getOrNull() }
                        state = state.copy(done = state.done + 1)
                        document?.title?.let { titles[feed.id] = it }
                        val entries = document?.entries
                        if (entries.isNullOrEmpty()) {
                            null
                        } else {
                            feed.id to withContext(Dispatchers.Default) { entries.take(EntriesPerFeed).map { it.asPost(feed.id) } }
                        }
                    }
                }.awaitAll().filterNotNull().toMap()
            }
            attemptedAt = Clock.System.now().toEpochMilliseconds()

            val result = if (DataEpoch.stale(epoch)) {
                // Writing now would put back a Following list that no longer exists.
                FeedSyncResult(0, list.size, 0, manualJoined, erased = true)
            } else {
                cache.replaceAll(fetched)
                feeds.nameUnnamed(titles)
                pruneSummaries(summaries, links, cache)
                val fresh = fetched.values.sumOf { posts -> posts.count { it.url !in known } }
                FeedSyncResult(fetched.size, list.size, fresh, manualJoined)
            }
            finished.tryEmit(result)
            return result
        } finally {
            state = FeedSyncState()
            manualJoined = false
        }
    }

    companion object {
        /** Often enough that Home feels live, rare enough that it is not a fetch per glance. */
        const val StaleAfterMs = 30L * 60 * 1000

        // Enough to hide one slow blog behind the rest without opening a dozen sockets.
        private const val ParallelFetches = 6
    }
}
