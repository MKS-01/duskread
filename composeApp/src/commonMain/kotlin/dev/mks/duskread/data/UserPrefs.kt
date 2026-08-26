package dev.mks.duskread.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.summary.SummaryLength
import dev.mks.duskread.ui.theme.AccentColor

/**
 * The first state in this app that is genuinely mutable and outlives a
 * composition, so it is also the first that needs an owner.
 *
 * It is a plain class rather than a ViewModel: there is no async work, no
 * lifecycle to survive beyond the process, and nothing to inject. Reads come
 * from an in-memory snapshot taken at construction; writes go straight through
 * to the store, so nothing can be lost by a process dying between the two.
 */
class UserPrefs(private val store: KeyValueStore) {
    var name: String? by mutableStateOf(store.getString(KeyName)?.takeIf { it.isNotBlank() })
        private set

    var introSeen: Boolean by mutableStateOf(store.getBoolean(KeyIntroSeen))
        private set

    /**
     * The monochrome ("Ink") scheme, kept across restarts until changed by
     * hand. Ink by default — the app opens colourless and a reader opts into
     * an accent, not the other way round.
     */
    var mono: Boolean by mutableStateOf(store.getBoolean(KeyMono, default = true))
        private set

    /**
     * Whether colour is on offer at all.
     *
     * Off by default, and the reason is the same one that makes Ink the
     * default scheme: this app is monochrome, and the accent is a thing you
     * opt into rather than a thing you turn off. With this false there is no
     * contrast glyph in the bar and no accent row in settings — not a
     * disabled one, none — so the single screen a reader actually looks at
     * carries one control fewer.
     *
     * Kept separate from [mono] rather than folded into it because they
     * answer different questions: [mono] is which scheme is painting, this
     * is whether the reader is ever asked.
     */
    var colourMode: Boolean by mutableStateOf(store.getBoolean(KeyColourMode, default = false))
        private set

    /**
     * Which accent "Paper Black" lights up with. Stored by name, same
     * reasoning as [summaryLength] below; falls back to the scheme's own
     * terracotta if nothing was ever chosen, or the stored name no longer
     * matches an entry.
     */
    var accent: AccentColor by mutableStateOf(
        store.getString(KeyAccent)?.let { name -> AccentColor.entries.firstOrNull { it.name == name } } ?: AccentColor.Orange,
    )
        private set

    /**
     * How long a summary the reader wants. Stored by name rather than
     * ordinal, so reordering the enum one day cannot silently repoint an
     * existing reader at a different length; an unknown name falls back to
     * the default, which is the engine's own maximum.
     */
    var summaryLength: SummaryLength by mutableStateOf(
        store.getString(KeySummaryLength)?.let { name -> SummaryLength.entries.firstOrNull { it.name == name } } ?: SummaryLength.Full,
    )
        private set

    /** A blank name is stored as absent, so "skip" and "cleared" mean the same thing. */
    fun updateName(value: String?) {
        val cleaned = value?.trim()?.takeIf { it.isNotEmpty() }
        name = cleaned
        store.putString(KeyName, cleaned)
    }

    fun markIntroSeen() {
        introSeen = true
        store.putBoolean(KeyIntroSeen, true)
    }

    fun updateMono(value: Boolean) {
        mono = value
        store.putBoolean(KeyMono, value)
    }

    /**
     * Hiding colour also drops back to Ink. Without that, a reader who
     * switched to Paper Black and then hid the option would be left in it
     * with the only way out no longer drawn.
     */
    fun updateColourMode(value: Boolean) {
        colourMode = value
        store.putBoolean(KeyColourMode, value)
        if (!value) updateMono(true)
    }

    fun updateAccent(value: AccentColor) {
        accent = value
        store.putString(KeyAccent, value.name)
    }

    fun updateSummaryLength(value: SummaryLength) {
        summaryLength = value
        store.putString(KeySummaryLength, value.name)
    }

    /** Used by the "start over" affordance in settings, and by manual testing. */
    fun reset() {
        updateName(null)
        introSeen = false
        store.putBoolean(KeyIntroSeen, false)
    }

    private companion object {
        const val KeyName = "user.name"
        const val KeyIntroSeen = "intro.seen"
        const val KeyMono = "theme.mono"
        const val KeyAccent = "theme.accent"
        const val KeyColourMode = "theme.colourMode"
        const val KeySummaryLength = "summary.length"
    }
}

@Composable
fun rememberUserPrefs(): UserPrefs {
    val store = rememberKeyValueStore()
    return remember(store) { UserPrefs(store) }
}
