package dev.mks.duskread.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.rememberKeyValueStore

/**
 * Summaries already generated, kept so a second look costs nothing.
 *
 * Generation is seconds of the phone's own silicon and shows up as heat, and
 * AICore meters inference per app — so this is what keeps an afternoon of
 * reading inside the quota.
 *
 * Same flat-store encoding as [dev.mks.duskread.links.FeedPostCache], for the
 * same reason: this is a few dozen short records and a database would be
 * ceremony.
 */
class SummaryCache(private val store: KeyValueStore) {
    var summaries: Map<String, ArticleSummary> by mutableStateOf(load())
        private set

    /** Short and full are different answers, so the other length is a miss. */
    fun summaryFor(url: String, length: SummaryLength): ArticleSummary? = summaries[url]?.takeIf { it.length == length }

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
            summary.length.name,
        ).joinToString(FieldSeparator.toString())
    }

    private fun decode(record: String): ArticleSummary? {
        val fields = record.split(FieldSeparator)
        if (fields.size < 5) return null

        return ArticleSummary(
            url = fields[0].ifBlank { return null },
            text = fields[1],
            model = fields[2],
            createdAt = fields[3].toLongOrNull() ?: 0L,
            length = SummaryLength.entries.firstOrNull { it.name == fields[4] } ?: return null,
        )
    }

    private fun String.clean() = filterNot { it == FieldSeparator || it == RecordSeparator }.trim()

    private companion object {
        const val Key = "summaries"
        const val FieldSeparator = ''
        const val RecordSeparator = ''

        // Roughly a month of reading. Far below what the store can hold, and
        // the point is the article opened twice this week, not an archive.
        const val MaxSummaries = 60
    }
}

@Composable
fun rememberSummaryCache(): SummaryCache {
    val store = rememberKeyValueStore()
    return remember(store) { SummaryCache(store) }
}
