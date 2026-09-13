package dev.mks.duskread.links

import dev.mks.duskread.data.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Links captured while the app was not on screen, waiting to be filed. The home-screen
 * widget cannot go through [LinkLibrary].
 */
object LinkInbox {
    /**
     * Bumped whenever the app comes back to the foreground, so the drain runs again on a
     * process that never died.
     */
    private val _pokes = MutableStateFlow(0)
    val pokes: StateFlow<Int> = _pokes

    fun poke() {
        _pokes.value += 1
    }

    /**
     * Adds [rawUrl] to the inbox, or returns false if it is not a link or is already
     * waiting.
     */
    fun offer(store: KeyValueStore, rawUrl: String): Boolean {
        if (!looksLikeUrl(rawUrl)) return false

        val url = normaliseUrl(rawUrl).filterNot { it == RecordSeparator }.trim()
        val pending = read(store)
        if (pending.any { it.equals(url, ignoreCase = true) }) return false

        store.putString(Key, (pending + url).joinToString(RecordSeparator.toString()))
        return true
    }

    /** Everything waiting, in capture order, cleared in the same step. */
    fun drain(store: KeyValueStore): List<String> {
        val pending = read(store)
        if (pending.isNotEmpty()) store.putString(Key, null)
        return pending
    }

    private fun read(store: KeyValueStore): List<String> = store.getString(Key)?.split(RecordSeparator)?.filter { it.isNotBlank() }.orEmpty()

    // The same record separator LinkLibrary packs with, and for the same reason: a
    // control character no URL can contain.
    private const val Key = "links.inbox"
    private const val RecordSeparator = '\u001E'
}
