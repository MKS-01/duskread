package dev.mks.duskread.summary

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A read of an article, as one block of prose.
 */
data class ArticleSummary(
    val url: String,
    val text: String,
    val model: String,
    val createdAt: Long,
)

/**
 * Which of the two a left swipe opens the panel already doing, chosen once in Settings.
 */
enum class SwipeDefault(val label: String) {
    Summary("Summary"),
    ReadAloud("Read aloud"),
}

/**
 * What the engine can do right now, which is not a constant: a model can be absent,
 * downloadable, mid-download or ready.
 */
sealed interface SummariserState {
    data object Checking : SummariserState

    data class Unavailable(val reason: String) : SummariserState

    data object Downloadable : SummariserState

    /** [fraction] is null until the system says how big the download is. */
    data class Downloading(val fraction: Float?) : SummariserState

    /** [model] is what the engine calls itself, which is not always what we asked for. */
    data class Ready(val model: String) : SummariserState
}

/**
 * The on-device summariser, as the app sees it.
 */
interface Summariser {
    val state: SummariserState

    /** Re-asks the system what it can do. Cheap, and the answer changes without warning. */
    suspend fun refresh()

    /** Pulls the model down, reporting progress through [state]. */
    suspend fun prepare()

    fun summarise(title: String, text: String): Flow<String>
}

/** Every target except Android: no local model, so the control disappears rather than failing. */
object UnavailableSummariser : Summariser {
    override val state: SummariserState =
        SummariserState.Unavailable("On-device summaries need an Android phone with AICore.")

    override suspend fun refresh() = Unit

    override suspend fun prepare() = Unit

    override fun summarise(title: String, text: String): Flow<String> = emptyFlow()
}

@Composable
expect fun rememberSummariser(): Summariser

/**
 * Whether this platform has a summariser at all — not whether the model is downloaded,
 * which only [Summariser.state] can answer. The two questions have different costs.
 */
expect fun summariesSupported(): Boolean

/**
 * What to summarise, and whatever of it we already have. [text] is set when the caller
 * has already reduced the page — the reader has it the moment its view opens.
 */
data class SummaryTarget(
    val url: String,
    val title: String,
    val text: String? = null,
    val feedContent: String? = null,
)

/**
 * A summary asked for from a list row, waiting for the panel that can show it.
 */
object SummaryRequest {
    private val _target = MutableStateFlow<SummaryTarget?>(null)
    val target: StateFlow<SummaryTarget?> = _target

    fun open(target: SummaryTarget) {
        _target.value = target
    }

    fun consume() {
        _target.value = null
    }
}
