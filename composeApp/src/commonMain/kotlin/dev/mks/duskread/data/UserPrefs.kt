package dev.mks.duskread.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.speech.VoiceChoice
import dev.mks.duskread.summary.SwipeDefault
import kotlinx.coroutines.flow.StateFlow

/**
 * The first state in this app that is genuinely mutable and outlives a composition, so it
 * is also the first that needs an owner.
 */
class UserPrefs(private val store: KeyValueStore) {
    // Snapshot state and a StateFlow in one, so Compose and the iOS bridge read the same
    // value.
    private val observedName = Observed(store.getString(KeyName)?.takeIf { it.isNotBlank() })
    private val observedIntroSeen = Observed(store.getBoolean(KeyIntroSeen))
    private val observedMono = Observed(store.getBoolean(KeyMono, fallback = DefaultMono))
    private val observedReadbackEnabled = Observed(store.getBoolean(KeyReadback))
    private val observedVoice = Observed(
        store.getString(KeyVoice)?.let { name -> VoiceChoice.entries.firstOrNull { it.name == name } } ?: VoiceChoice.System,
    )
    private val observedSwipeDefault = Observed(
        store.getString(KeySwipeDefault)?.let { name -> SwipeDefault.entries.firstOrNull { it.name == name } } ?: SwipeDefault.Summary,
    )

    var name: String? by observedName
        private set

    /** [name] for observers outside a composition; see [Observed]. */
    val nameUpdates: StateFlow<String?> get() = observedName.updates

    var introSeen: Boolean by observedIntroSeen
        private set

    /** [introSeen] for observers outside a composition; see [Observed]. */
    val introSeenUpdates: StateFlow<Boolean> get() = observedIntroSeen.updates

    /**
     * The monochrome ("Ink") scheme, kept across restarts until changed by hand.
     */
    var mono: Boolean by observedMono
        private set

    /** [mono] for observers outside a composition; see [Observed]. */
    val monoUpdates: StateFlow<Boolean> get() = observedMono.updates

    /**
     * Whether the Readback tab is shown at all. Off by default, and there is no visible
     * switch for it.
     */
    var readbackEnabled: Boolean by observedReadbackEnabled
        private set

    /** [readbackEnabled] for observers outside a composition; see [Observed]. */
    val readbackEnabledUpdates: StateFlow<Boolean> get() = observedReadbackEnabled.updates

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

        // Switching the tab off takes its voice with it.
        if (!readbackEnabled && voice == VoiceChoice.ReadbackLibrary) updateVoice(VoiceChoice.System)

        return readbackEnabled
    }

    /**
     * Which voice reads an article aloud.
     */
    var voice: VoiceChoice by observedVoice
        private set

    /** [voice] for observers outside a composition; see [Observed]. */
    val voiceUpdates: StateFlow<VoiceChoice> get() = observedVoice.updates

    fun updateVoice(value: VoiceChoice) {
        voice = value
        store.putString(KeyVoice, value.name)
    }

    /**
     * Whether the left swipe opens the panel already speaking, or waits for the play
     * button.
     */
    var swipeDefault: SwipeDefault by observedSwipeDefault
        private set

    /** [swipeDefault] for observers outside a composition; see [Observed]. */
    val swipeDefaultUpdates: StateFlow<SwipeDefault> get() = observedSwipeDefault.updates

    fun updateSwipeDefault(value: SwipeDefault) {
        swipeDefault = value
        store.putString(KeySwipeDefault, value.name)
    }

    /**
     * Every preference back to the day the app was installed, for the reset in Settings.
     */
    fun reset() {
        updateName(null)
        updateMono(DefaultMono)
        updateVoice(VoiceChoice.System)
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
        const val KeyReadback = "readback.enabled"
        const val KeyVoice = "speech.voice"
        const val KeySwipeDefault = "swipe.default"
    }
}

@Composable
fun rememberUserPrefs(): UserPrefs = LocalAppGraph.current.prefs
