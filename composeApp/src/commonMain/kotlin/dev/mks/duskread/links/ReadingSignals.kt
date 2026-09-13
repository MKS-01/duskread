package dev.mks.duskread.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.LocalAppGraph
import dev.mks.duskread.data.Observed
import dev.mks.duskread.data.rememberKeyValueStore
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock

/**
 * What this reader has actually done with a host's articles.
 */
data class HostSignal(
    val host: String,
    val opens: Int = 0,
    val reads: Int = 0,
    val skips: Int = 0,
    /** Epoch millis of the last read from this host, or null if never read. */
    val lastReadAt: Long? = null,
    /** Epoch millis of the last skip, so the penalty can wear off rather than accumulate forever. */
    val lastSkipAt: Long? = null,
)

/**
 * The record of what gets read, written from the places the app already knows something
 * happened and read only by [Recommender].
 */
class ReadingSignals(private val store: KeyValueStore) {
    // Snapshot state and a StateFlow in one, so Compose and the iOS bridge read the same
    // value.
    private val observedByHost = Observed(loadHosts())
    private val observedTopicReads = Observed(loadTopics())
    private val observedSkippedPosts = Observed(loadSkips())

    var byHost: Map<String, HostSignal> by observedByHost
        private set

    /** [byHost] for observers outside a composition; see [Observed]. */
    val byHostUpdates: StateFlow<Map<String, HostSignal>> get() = observedByHost.updates

    /** tag -> reads. Empty until something tags the candidates; every term that reads it is then zero. */
    var topicReads: Map<String, Int> by observedTopicReads
        private set

    /** [topicReads] for observers outside a composition; see [Observed]. */
    val topicReadsUpdates: StateFlow<Map<String, Int>> get() = observedTopicReads.updates

    /**
     * url -> when the shuffle stepped past it.
     */
    var skippedPosts: Map<String, Long> by observedSkippedPosts
        private set

    /** [skippedPosts] for observers outside a composition; see [Observed]. */
    val skippedPostsUpdates: StateFlow<Map<String, Long>> get() = observedSkippedPosts.updates

    /** Total reads across every host — the denominator source affinity is smoothed against. */
    val totalReads: Int
        get() = byHost.values.sumOf { it.reads }

    /** A link marked read. The strongest signal there is, and the only one that sets [HostSignal.lastReadAt]. */
    fun recordRead(url: String) = update(url) {
        it.copy(reads = it.reads + 1, lastReadAt = Clock.System.now().toEpochMilliseconds())
    }

    /**
     * Interest short of a read: a summary was asked for.
     */
    fun recordOpen(url: String) = update(url) { it.copy(opens = it.opens + 1) }

    /**
     * Shuffle stepped past this one.
     */
    fun recordSkip(url: String) {
        val now = Clock.System.now().toEpochMilliseconds()

        // Oldest-first eviction on a bounded map.
        val current = loadSkips() + (url to now)
        skippedPosts = if (current.size <= MaxSkippedPosts) {
            current
        } else {
            current.entries.sortedByDescending { it.value }.take(MaxSkippedPosts).associate { it.key to it.value }
        }
        store.putString(SkipKey, encodeSkips(skippedPosts).takeIf { it.isNotEmpty() })

        update(url) { it.copy(skips = it.skips + 1, lastSkipAt = now) }
    }

    fun recordTopicRead(tag: String) {
        val current = loadTopics()
        topicReads = current + (tag to (current[tag] ?: 0) + 1)
        store.putString(TopicKey, encodeTopics(topicReads).takeIf { it.isNotEmpty() })
    }

    /** For the Discovery block in Settings: start the ranking over from nothing. */
    fun clear() {
        byHost = emptyMap()
        topicReads = emptyMap()
        skippedPosts = emptyMap()
        store.putString(HostKey, null)
        store.putString(TopicKey, null)
        store.putString(SkipKey, null)
    }

    /**
     * Re-reads the store before writing rather than trusting the in-memory copy.
     */
    private fun update(url: String, change: (HostSignal) -> HostSignal) {
        val host = hostOf(url).ifBlank { return }
        val current = loadHosts()
        byHost = current + (host to change(current[host] ?: HostSignal(host)))
        store.putString(HostKey, encodeHosts(byHost.values).takeIf { it.isNotEmpty() })
    }

    private fun loadHosts(): Map<String, HostSignal> = store.getString(HostKey)?.split(RecordSeparator)?.mapNotNull(::decodeHost)?.associateBy { it.host }.orEmpty()

    private fun loadTopics(): Map<String, Int> = store.getString(TopicKey)?.split(RecordSeparator)?.mapNotNull { record ->
        val fields = record.split(FieldSeparator)
        val tag = fields.getOrNull(0)?.ifBlank { null } ?: return@mapNotNull null
        tag to (fields.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null)
    }?.toMap().orEmpty()

    private fun encodeHosts(signals: Collection<HostSignal>): String = signals.joinToString(RecordSeparator.toString()) { signal ->
        listOf(
            signal.host.clean(),
            signal.opens.toString(),
            signal.reads.toString(),
            signal.skips.toString(),
            signal.lastReadAt?.toString().orEmpty(),
            signal.lastSkipAt?.toString().orEmpty(),
        ).joinToString(FieldSeparator.toString())
    }

    private fun encodeTopics(reads: Map<String, Int>): String = reads.entries.joinToString(RecordSeparator.toString()) { (tag, count) ->
        listOf(tag.clean(), count.toString()).joinToString(FieldSeparator.toString())
    }

    private fun decodeHost(record: String): HostSignal? {
        // Four, not six: a record written before skips carried a timestamp decodes as it
        // always did rather than being dropped.
        val fields = record.split(FieldSeparator)
        if (fields.size < 4) return null

        return HostSignal(
            host = fields[0].ifBlank { return null },
            opens = fields[1].toIntOrNull() ?: 0,
            reads = fields[2].toIntOrNull() ?: 0,
            skips = fields[3].toIntOrNull() ?: 0,
            lastReadAt = fields.getOrNull(4)?.toLongOrNull(),
            lastSkipAt = fields.getOrNull(5)?.toLongOrNull(),
        )
    }

    private fun loadSkips(): Map<String, Long> = store.getString(SkipKey)?.split(RecordSeparator)?.mapNotNull { record ->
        val fields = record.split(FieldSeparator)
        val url = fields.getOrNull(0)?.ifBlank { null } ?: return@mapNotNull null
        val at = fields.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
        url to at
    }?.toMap().orEmpty()

    private fun encodeSkips(skips: Map<String, Long>): String = skips.entries.joinToString(RecordSeparator.toString()) { (url, at) ->
        listOf(url.clean(), at.toString()).joinToString(FieldSeparator.toString())
    }

    private fun String.clean() = filterNot { it == FieldSeparator || it == RecordSeparator }.trim()

    private companion object {
        const val HostKey = "signals.hosts"
        const val TopicKey = "signals.topics"
        const val SkipKey = "signals.skipped"

        /** Enough to cover a long shuffle session over a pool of a few hundred, and no more. */
        const val MaxSkippedPosts = 60
        const val FieldSeparator = ''
        const val RecordSeparator = ''
    }
}

@Composable
fun rememberReadingSignals(): ReadingSignals = LocalAppGraph.current.signals
