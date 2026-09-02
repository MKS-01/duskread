package dev.mks.duskread.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.speech.VoiceChoice
import dev.mks.duskread.summary.SummaryLength
import dev.mks.duskread.summary.SwipeDefault

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
    var mono: Boolean by mutableStateOf(store.getBoolean(KeyMono, default = DefaultMono))
        private set

    /**
     * Whether the Readback tab is shown at all.
     *
     * Off by default, and there is no visible switch for it. The tab browses a
     * `library.db` that the separate readback project generates on a laptop
     * and a hand-rolled sync step copies onto the device — machinery nobody
     * who installs this from a store has, so for them the tab is a quarter of
     * the navigation that can only ever be empty.
     *
     * Hidden rather than deleted: this repo is the open half of a two-project
     * setup, and removing the reader would strand the other half. The way in
     * is three taps on the version line in Settings, which is a developer
     * gesture on purpose — anyone who has the sync script also knows where the
     * gesture is documented, and anyone who does not is never shown a switch
     * they have no way to use.
     */
    var readbackEnabled: Boolean by mutableStateOf(store.getBoolean(KeyReadback))
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

    /** Returns what it switched to, so the caller can say which way it went. */
    fun toggleReadback(): Boolean {
        readbackEnabled = !readbackEnabled
        store.putBoolean(KeyReadback, readbackEnabled)

        // Switching the tab off takes its voice with it. Left alone, a reader
        // who had chosen the readback library would be pointed at a source
        // they can no longer see, reach or change — the Voice list stops
        // offering the row that would let them pick something else.
        if (!readbackEnabled && voice == VoiceChoice.ReadbackLibrary) updateVoice(VoiceChoice.System)

        return readbackEnabled
    }

    /**
     * Which voice reads an article aloud.
     *
     * Stored by name rather than ordinal, for the reason [summaryLength]
     * already gives: a voice added to the middle of the enum one day must not
     * silently repoint an existing reader at a different one.
     *
     * An unknown name falls back to [VoiceChoice.System], which is also the
     * only voice guaranteed to exist on every phone.
     */
    var voice: VoiceChoice by mutableStateOf(
        store.getString(KeyVoice)?.let { name -> VoiceChoice.entries.firstOrNull { it.name == name } } ?: VoiceChoice.System,
    )
        private set

    fun updateVoice(value: VoiceChoice) {
        voice = value
        store.putString(KeyVoice, value.name)
    }

    fun updateSummaryLength(value: SummaryLength) {
        summaryLength = value
        store.putString(KeySummaryLength, value.name)
    }

    /**
     * Whether the left swipe opens the panel already speaking, or waits for
     * the play button. `Summary`, not `ReadAloud`, by default — the panel is
     * a summary that also plays, not the other way round, and a swipe that
     * starts talking before anyone asked for it is the more surprising of
     * the two ways to get this wrong.
     */
    var swipeDefault: SwipeDefault by mutableStateOf(
        store.getString(KeySwipeDefault)?.let { name -> SwipeDefault.entries.firstOrNull { it.name == name } } ?: SwipeDefault.Summary,
    )
        private set

    fun updateSwipeDefault(value: SwipeDefault) {
        swipeDefault = value
        store.putString(KeySwipeDefault, value.name)
    }

    /**
     * Every preference back to the day the app was installed, for the reset in
     * Settings.
     *
     * It used to clear the name and the intro flag alone, which was right when
     * those were all there was and quietly wrong ever since: a reader who
     * erased everything kept their theme, summary length, voice and swipe
     * default, and — worse — kept the Readback tab switched on, a developer
     * gesture surviving the one action whose whole promise is that nothing
     * does.
     *
     * [readbackEnabled] is the reason this writes `false` rather than removing
     * the key: `getBoolean` has no third answer, so absent and off are the
     * same state, and being explicit costs nothing.
     */
    fun reset() {
        updateName(null)
        updateMono(DefaultMono)
        updateVoice(VoiceChoice.System)
        updateSummaryLength(SummaryLength.Full)
        updateSwipeDefault(SwipeDefault.Summary)

        introSeen = false
        store.putBoolean(KeyIntroSeen, false)

        readbackEnabled = false
        store.putBoolean(KeyReadback, false)
    }

    private companion object {
        /** Ink, not Paper Black — the app opens with the hue already drained. */
        const val DefaultMono = true

        const val KeyName = "user.name"
        const val KeyIntroSeen = "intro.seen"
        const val KeyMono = "theme.mono"
        const val KeySummaryLength = "summary.length"
        const val KeyReadback = "readback.enabled"
        const val KeyVoice = "speech.voice"
        const val KeySwipeDefault = "swipe.default"
    }
}

@Composable
fun rememberUserPrefs(): UserPrefs {
    val store = rememberKeyValueStore()
    return remember(store) { UserPrefs(store) }
}
