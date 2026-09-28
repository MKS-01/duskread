package dev.mks.duskread.speech

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/** One post in a listen-through, with whatever the feed already carried of its text. */
data class ListenEntry(
    val url: String,
    val title: String,
    /** The blog's own name, said before the post so a run of them can be told apart. */
    val source: String,
    val feedContent: String? = null,
    val topic: String? = null,
)

/**
 * Several posts read aloud one after another. Shared so both platforms skip, announce and
 * mark posts the same way; each driver only says when a read ended and how.
 */
class ListenQueue(
    val entries: List<ListenEntry>,
    private val loadText: suspend (ListenEntry) -> String?,
    /** A post heard to its end — not skipped, not stopped. */
    private val onHeard: (ListenEntry) -> Unit,
) {
    private var index = -1
    private val loads = mutableMapOf<Int, Deferred<String?>>()

    /** The next post with enough text to be worth hearing, or null past the last. */
    suspend fun next(scope: CoroutineScope): SpeechSession.Request? {
        while (++index < entries.size) {
            val entry = entries[index]
            val body = textAt(index, scope).await()
            // Fetched while this one plays, so a turn does not wait on the network.
            if (index + 1 < entries.size) textAt(index + 1, scope)
            if (body == null || body.length < MinSpeakableChars) continue
            return SpeechSession.Request(
                key = entry.url,
                title = entry.title,
                text = "${entry.title}. From ${entry.source}.\n\n$body",
                position = index + 1,
                total = entries.size,
            )
        }
        return null
    }

    fun heard(url: String) {
        entries.getOrNull(index)?.takeIf { it.url == url }?.let(onHeard)
    }

    private fun textAt(at: Int, scope: CoroutineScope): Deferred<String?> = loads.getOrPut(at) {
        scope.async { runCatching { loadText(entries[at]) }.getOrNull() }
    }
}

/** Below this, a page is chrome or a teaser rather than an article worth hearing. */
const val MinSpeakableChars = 200
