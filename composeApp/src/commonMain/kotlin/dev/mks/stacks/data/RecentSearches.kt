package dev.mks.stacks.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * The last few things searched for, kept across launches.
 *
 * Worth persisting because of how this app is actually used: a topic is looked
 * up, read on the train, and looked up again a day later — retyping "dijkstra"
 * on a phone keyboard every time is the friction. It rides [KeyValueStore] for
 * the same reason [UserPrefs] does; a handful of short strings is not a
 * database.
 *
 * Only *committed* queries land here — a submitted search or one that opened a
 * topic — never every keystroke, or the list would fill with the prefixes of
 * one word.
 */
class RecentSearches(private val store: KeyValueStore) {
    var entries: List<String> by mutableStateOf(load())
        private set

    /** Newest first, case-insensitively de-duplicated so re-running a search promotes it. */
    fun record(query: String) {
        val cleaned = query.trim()
        if (cleaned.length < MinLength) return

        entries = (listOf(cleaned) + entries.filterNot { it.equals(cleaned, ignoreCase = true) }).take(Keep)
        save()
    }

    fun forget(query: String) {
        entries = entries.filterNot { it.equals(query, ignoreCase = true) }
        save()
    }

    fun clear() {
        entries = emptyList()
        save()
    }

    // Newline-separated rather than JSON: a query can hold anything a keyboard
    // types except a newline, which the field cannot produce (it is single
    // line), so the delimiter is safe and needs no escaping or parser.
    private fun load(): List<String> = store.getString(Key)?.split('\n')?.filter { it.isNotBlank() }?.take(Keep).orEmpty()

    private fun save() = store.putString(Key, entries.joinToString("\n").takeIf { it.isNotEmpty() })

    private companion object {
        const val Key = "search.recent"

        /** Enough to cover a study session; more turns the empty state into a second list. */
        const val Keep = 6

        /** A one-character query is a typo, not a search worth remembering. */
        const val MinLength = 2
    }
}

@Composable
fun rememberRecentSearches(): RecentSearches {
    val store = rememberKeyValueStore()
    return remember(store) { RecentSearches(store) }
}
