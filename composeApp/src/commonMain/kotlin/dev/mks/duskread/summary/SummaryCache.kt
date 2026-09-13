package dev.mks.duskread.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.LocalAppGraph
import dev.mks.duskread.data.Observed
import dev.mks.duskread.data.rememberKeyValueStore
import kotlinx.coroutines.flow.StateFlow

/**
 * Summaries already generated, kept so a second look costs nothing.
 */
class SummaryCache(private val store: KeyValueStore) {
    // Snapshot state and a StateFlow in one, so Compose and the iOS bridge read the same
    // value.
    private val observedSummaries = Observed(load())

    var summaries: Map<String, ArticleSummary> by observedSummaries
        private set

    /** [summaries] for observers outside a composition; see [Observed]. */
    val summariesUpdates: StateFlow<Map<String, ArticleSummary>> get() = observedSummaries.updates

    fun summaryFor(url: String): ArticleSummary? = summaries[url]

    /** Newest first, oldest dropped: a convenience, not a record. */
    fun put(summary: ArticleSummary) {
        val kept = (listOf(summary) + summaries.values.filterNot { it.url == summary.url })
            .sortedByDescending { it.createdAt }
            .take(MaxSummaries)

        summaries = kept.associateBy { it.url }
        persist()
    }

    fun clear() {
        summaries = emptyMap()
        persist()
    }

    private fun persist() = store.putString(Key, encode(summaries.values).takeIf { it.isNotEmpty() })

    private fun load(): Map<String, ArticleSummary> = store.getString(Key)
        ?.split(RecordSeparator)
        ?.mapNotNull(::decode)
        ?.associateBy { it.url }
        .orEmpty()

    private fun encode(summaries: Collection<ArticleSummary>): String = summaries.joinToString(RecordSeparator.toString()) { summary ->
        listOf(
            summary.url,
            summary.text.clean(),
            summary.model.clean(),
            summary.createdAt.toString(),
        ).joinToString(FieldSeparator.toString())
    }

    private fun decode(record: String): ArticleSummary? {
        val fields = record.split(FieldSeparator)
        // Four, not five: records written before length stopped being a field still carry
        // it, and are read by ignoring the tail rather than being dropped.
        if (fields.size < 4) return null

        return ArticleSummary(
            url = fields[0].ifBlank { return null },
            text = fields[1],
            model = fields[2],
            createdAt = fields[3].toLongOrNull() ?: 0L,
        )
    }

    private fun String.clean() = filterNot { it == FieldSeparator || it == RecordSeparator }.trim()

    private companion object {
        const val Key = "summaries"
        const val FieldSeparator = ''
        const val RecordSeparator = ''

        // Roughly a month of reading. Far below what the store can hold, and the point is
        // the article opened twice this week, not an archive.
        const val MaxSummaries = 60
    }
}

@Composable
fun rememberSummaryCache(): SummaryCache = LocalAppGraph.current.summaries
