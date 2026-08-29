package dev.mks.duskread.links

import dev.mks.duskread.data.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Links captured while the app was not on screen, waiting to be filed.
 *
 * The home-screen widget cannot go through [LinkLibrary]. Not because of
 * module boundaries — it could build one — but because doing so would lose
 * links. The library holds the whole list in memory and rewrites *all* of it
 * on every change, so a second instance saving into the same key would be
 * silently overwritten the next time the live one persisted anything. Two
 * writers of one blob is a race nobody wins.
 *
 * So the widget writes here instead, under its own key, and only ever
 * appends. The app drains it into the library on resume, which is also the
 * behaviour asked for: a link captured from the home screen shows up in Saved
 * the next time the app is opened. The race isn't fought, it's removed — the
 * widget never touches "links.saved", and the app never leaves anything
 * behind in the inbox.
 *
 * No lock. Both sides run on the Android main thread — the widget's capture
 * activity from `onWindowFocusChanged`, the drain from a `LaunchedEffect` on
 * the main dispatcher — so they are already serialised, and a lock would only
 * document a hazard that isn't reachable. (`kotlin.jvm.Synchronized` is not
 * available in common code in any case.)
 */
object LinkInbox {
    /**
     * Bumped whenever the app comes back to the foreground, so the drain runs
     * again on a process that never died.
     *
     * `HomeTabRequest` and `SharedLinkRequest` are separate objects because
     * the thing being handed over had nowhere else to live. Here it already
     * does — it is in the store — so this flow carries only "look again", and
     * keeping it beside the inbox it refers to says more than a third
     * near-identical Request object would.
     */
    private val _pokes = MutableStateFlow(0)
    val pokes: StateFlow<Int> = _pokes

    fun poke() {
        _pokes.value += 1
    }

    /**
     * Adds [rawUrl] to the inbox, or returns false if it is not a link or is
     * already waiting. Deduping here rather than only at drain time stops a
     * repeated tap on the widget from stacking the same URL up.
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

    // The same record separator LinkLibrary packs with, and for the same
    // reason: a control character no URL can contain. Written as an escape
    // rather than the literal character so the value is legible when reading
    // the file. Bare URLs, though, not encoded records — the widget must not
    // need the library's decoder in order to write here.
    private const val Key = "links.inbox"
    private const val RecordSeparator = '\u001E'
}
