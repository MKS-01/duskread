package dev.mks.duskread.summary

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A read of an article, as one block of prose.
 *
 * [model] rides along because a summary is only as good as what wrote it, and
 * [text] is deliberately one field: the engine's output shape varies with its
 * mood, and a panel split into gist and body changes shape with it.
 *
 * [length] is stored so the cache can tell a short summary from a full one —
 * they are different answers to the same article, not one trimmed.
 */
data class ArticleSummary(
    val url: String,
    val text: String,
    val model: String,
    val createdAt: Long,
    val length: SummaryLength,
)

/**
 * Which of the two a left swipe opens the panel already doing, chosen once in
 * Settings.
 *
 * The panel always does both — it is one card that summarises and reads
 * aloud, never two — so this is not "which feature" the way it would have
 * been before they merged. It is only ever "does it start speaking the
 * moment it opens", the one thing about the panel a single pull cannot show
 * on its own and has to be decided ahead of time instead.
 *
 * Lives beside [SummaryLength] rather than in `ui/summary/`, the package that
 * actually reads it: `UserPrefs` needs it too, and `data/` importing from
 * `ui/` would be the layering running backwards.
 */
enum class SwipeDefault(val label: String) {
    Summary("Summary"),
    ReadAloud("Read aloud"),
}

/**
 * How much summary the reader wants.
 *
 * Two options, and they are the engine's own: AICore's summarisation feature
 * is configured with a number of points, so [Short] and [Full] ask it for
 * genuinely different answers rather than trimming one answer down.
 *
 * A word limit was tried first and thrown away. A limit can only cut, never
 * lengthen, and three points from this model often run under any ceiling
 * worth naming — so every setting produced identical text and the numbers
 * described a limit nothing reached. Two settings that always differ beat
 * three that usually do not.
 *
 * The choice is baked into the client at construction, so changing it builds
 * a new one; see `Summarisers`.
 */
enum class SummaryLength(val label: String) {
    Short("Short"),
    Full("Full"),
}

/**
 * What the engine can do right now, which is not a constant: a model can be
 * absent, downloadable, mid-download or ready, and the same device moves
 * between all four in an afternoon.
 *
 * [Checking] is the initial state rather than an optimistic guess — asking
 * the system takes a round trip, and a panel that opens claiming "download
 * the model" before it knows is worse than one that says nothing.
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
 *
 * An interface in common with one real implementation, the shape `Reader` and
 * `AudioPlayer` already use: the engine is Android-only, but Saved, the
 * reader and Settings are common code and must compile — and quietly hide the
 * control — on the other four targets.
 *
 * No choice of model, because the feature that would take one answers
 * `FEATURE_NOT_FOUND` on real hardware; see `MlKitSummariser`.
 *
 * [summarise] emits the answer *so far*, cumulative, so a caller renders the
 * latest value and never concatenates. Generation takes seconds on a phone,
 * and text arriving a few words at a time is the difference between a feature
 * that feels alive and a spinner that feels broken.
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
expect fun rememberSummariser(length: SummaryLength): Summariser

/**
 * Whether this platform has a summariser at all — not whether the model is
 * downloaded, which only [Summariser.state] can answer.
 *
 * The two questions have different costs. This one is a constant per platform
 * and decides whether a row offers the gesture; the real question means
 * binding to a system service, which a list that may never summarise anything
 * has no business doing.
 */
expect fun summariesSupported(): Boolean

/**
 * What to summarise, and whatever of it we already have.
 *
 * [text] is set when the caller has already reduced the page — the reader has
 * it the moment its view opens. [feedContent] is the middle case: a feed that
 * carried the publisher's own markup, which `loadArticle` can use instead of
 * going out at all. A row has neither, so the panel fetches.
 */
data class SummaryTarget(
    val url: String,
    val title: String,
    val text: String? = null,
    val feedContent: String? = null,
)

/**
 * A summary asked for from a list row, waiting for the panel that can show it.
 *
 * The same handoff as `InAppBrowserRequest` and `ToastRequest`, for the same
 * reason: the swipe happens in a row several composables deep in a lazy list,
 * and the only sensible place to float a panel is the top of the app. The
 * reader does not use this — it hosts its own panel over its own article.
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
