package dev.mks.duskread.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A NEXT UP pick tapped from the reading-suggestion widget, waiting to be
 * opened and recorded.
 *
 * The same handoff shape as [HomeTabRequest]/`FocusRequest`, and for a
 * sharper reason than usual: the widget ranks its own throwaway copy of the
 * candidate pool to decide what to suggest, which is fine for a read-only
 * pick, but *opening* one has to run through the app's own live
 * `LinkLibrary`/`ReadingSignals` — the instances `HomeScreen` already
 * hoists — rather than a second copy the widget constructs for itself. Two
 * writers over the same storage key is exactly the hazard this app's own
 * invariants warn about (`docs/architecture.md`), so the widget only ever
 * hands over a URL; the save, the read toggle and the signal all happen here.
 */
object SuggestionOpenRequest {
    private val _pending = MutableStateFlow<PendingSuggestion?>(null)
    val pending: StateFlow<PendingSuggestion?> = _pending

    fun open(url: String, title: String, topic: String?) {
        _pending.value = PendingSuggestion(url, title, topic)
    }

    fun consume() {
        _pending.value = null
    }
}

data class PendingSuggestion(val url: String, val title: String, val topic: String?)

/** Read by `MainActivity`, set on the suggestion widget's tap intent. */
const val OpenSuggestionUrlExtra = "dev.mks.duskread.OPEN_SUGGESTION_URL"
const val OpenSuggestionTitleExtra = "dev.mks.duskread.OPEN_SUGGESTION_TITLE"
const val OpenSuggestionTopicExtra = "dev.mks.duskread.OPEN_SUGGESTION_TOPIC"
