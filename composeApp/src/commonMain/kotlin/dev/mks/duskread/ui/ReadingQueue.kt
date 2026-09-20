package dev.mks.duskread.ui

import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.ReadingSignals
import dev.mks.duskread.links.sameArticle

/**
 * One article in a list, reduced to what the reader needs to announce it and what
 * opening it has to record.
 */
data class ReadingQueueEntry(
    val url: String,
    val title: String,
    val host: String? = null,
    /** The subject, where the list that built this had one to give. */
    val topic: String? = null,
)

/**
 * What opening an article leaves behind. Each list already answers this differently —
 * Saved marks the row read, a feed row records nothing, Next Up saves the post first —
 * and a page turned to has to leave the same record as a row tapped.
 */
enum class OpenRecord { None, MarkRead, SaveAndMarkRead }

/**
 * The list an article was opened from, so the reader can turn to the next one without
 * being sent back for it.
 */
data class ReadingQueue(
    val entries: List<ReadingQueueEntry>,
    val index: Int = 0,
    /** What the ends of the queue are called: "Saved", or a blog's own name. */
    val source: String = "",
    val record: OpenRecord = OpenRecord.None,
) {
    val current: ReadingQueueEntry get() = entries[index]

    fun entryAt(at: Int): ReadingQueueEntry? = entries.getOrNull(at)

    /** Where an article sits in this queue; the start, for one that somehow is not in it. */
    fun positionOf(url: String): Int = entries.indexOfFirst { sameArticle(it.url, url) }.coerceAtLeast(0)

    fun at(at: Int): ReadingQueue = copy(index = at)

    /** Said out loud when a drag runs out of queue: the list ends, and it says which list. */
    fun endMessage(step: Int): String = when {
        source.isBlank() -> if (step > 0) "That's the last one" else "That's the first one"
        step > 0 -> "That's the last one in $source"
        else -> "That's the first one in $source"
    }

    /** The same fact set as a label, for the strip the drag opens. */
    fun endLabel(step: Int): String = when {
        source.isBlank() -> if (step > 0) "THE LAST ONE" else "THE FIRST ONE"
        step > 0 -> "END OF ${source.uppercase()}"
        else -> "START OF ${source.uppercase()}"
    }
}

/**
 * A URL with no list behind it — a shared link, a widget pick, a row in a screen that is
 * not a reading queue. It opens the same way and simply has nowhere to turn.
 */
fun singleArticle(url: String): ReadingQueue = ReadingQueue(listOf(ReadingQueueEntry(url, url)))

/**
 * The bookkeeping half of opening an article, split out so the tap path and the turn path
 * cannot drift apart.
 */
fun recordOpened(entry: ReadingQueueEntry, record: OpenRecord, links: LinkLibrary, signals: ReadingSignals) {
    if (record == OpenRecord.None) return

    val existing = links.links.firstOrNull { sameArticle(it.url, entry.url) }
    val id = existing?.id
        ?: if (record == OpenRecord.SaveAndMarkRead) links.save(entry.url, entry.title, entry.topic)?.id else null

    // `toggleRead` is a toggle: re-opening something already read must not flip it back.
    if (existing?.readAt == null) id?.let { links.toggleRead(it) }

    signals.recordRead(entry.url)
    entry.topic?.let { signals.recordTopicRead(it) }
}
