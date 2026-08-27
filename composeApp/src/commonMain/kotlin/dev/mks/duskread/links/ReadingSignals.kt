package dev.mks.duskread.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.rememberKeyValueStore
import kotlin.time.Clock

/**
 * What this reader has actually done with a host's articles.
 *
 * One record per **host**, not per link, because a link is read once and then
 * gone — a per-link record could never inform the next pick, which is the
 * only thing these numbers exist for.
 *
 * [skips] is deliberately the weakest of the three. A skip is "not right
 * now", not a dislike: the failure mode of any recommender over a list of
 * forty items is that it prunes itself to five and then repeats them, and the
 * only defence is a skip term too weak to do that.
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
 * The record of what gets read, written from the places the app already knows
 * something happened and read only by [Recommender].
 *
 * Same flat separator-packed encoding as [FeedPostCache] and for the same
 * reason: this is a few dozen short records, and a database would be
 * ceremony. Two keys rather than one because the topic half is written by a
 * different layer on a different schedule, and a device with no on-device
 * model never writes it at all — an absent topic map is the normal case, not
 * a degraded one.
 */
class ReadingSignals(private val store: KeyValueStore) {
    var byHost: Map<String, HostSignal> by mutableStateOf(loadHosts())
        private set

    /** tag -> reads. Empty until the tagging layer exists; every term that reads it is then zero. */
    var topicReads: Map<String, Int> by mutableStateOf(loadTopics())
        private set

    /** Total reads across every host — the denominator source affinity is smoothed against. */
    val totalReads: Int
        get() = byHost.values.sumOf { it.reads }

    /** A link marked read. The strongest signal there is, and the only one that sets [HostSignal.lastReadAt]. */
    fun recordRead(url: String) = update(url) {
        it.copy(reads = it.reads + 1, lastReadAt = Clock.System.now().toEpochMilliseconds())
    }

    /**
     * Interest short of a read: a summary was asked for. Someone who asks
     * what is in an article is telling us something even if they never open
     * it, and that is worth more than nothing and less than reading it.
     */
    fun recordOpen(url: String) = update(url) { it.copy(opens = it.opens + 1) }

    /** Shuffle stepped past this one. See [HostSignal.skips] for why this barely counts. */
    fun recordSkip(url: String) = update(url) {
        it.copy(skips = it.skips + 1, lastSkipAt = Clock.System.now().toEpochMilliseconds())
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
        store.putString(HostKey, null)
        store.putString(TopicKey, null)
    }

    /**
     * Re-reads the store before writing rather than trusting the in-memory
     * copy. Signals are written from three different screens, and composition
     * may well hold more than one instance of this class backed by the same
     * store — without this, whichever wrote last would silently drop what the
     * others had recorded. A parse per write is nothing at this size, and
     * writes are rare: a read, a skip, a summary.
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
        // Four, not six: a record written before skips carried a timestamp
        // decodes as it always did rather than being dropped.
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

    private fun String.clean() = filterNot { it == FieldSeparator || it == RecordSeparator }.trim()

    private companion object {
        const val HostKey = "signals.hosts"
        const val TopicKey = "signals.topics"
        const val FieldSeparator = ''
        const val RecordSeparator = ''
    }
}

@Composable
fun rememberReadingSignals(): ReadingSignals {
    val store = rememberKeyValueStore()
    return remember(store) { ReadingSignals(store) }
}
