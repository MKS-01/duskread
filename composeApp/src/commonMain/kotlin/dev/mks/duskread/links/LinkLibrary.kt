package dev.mks.duskread.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.rememberKeyValueStore
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The saved links, newest first, persisted through [KeyValueStore].
 *
 * Records are packed into one string with ASCII's own separators — unit
 * separator between fields, record separator between links — rather than
 * bringing in a JSON serialiser for six fields. They are control characters
 * no URL or page title can contain, and any that somehow arrive are stripped
 * on the way in, so the format needs no escaping and no parser.
 *
 * The whole list lives in memory and is rewritten on every change. At the size
 * this can plausibly reach — a reading list, not an archive — that is cheaper
 * than any incremental scheme would be to maintain.
 */
@OptIn(ExperimentalTime::class)
class LinkLibrary(private val store: KeyValueStore) {
    var links: List<SavedLink> by mutableStateOf(load())
        private set

    val unreadCount: Int
        get() = links.count { !it.read }

    /**
     * Saves [rawUrl], or returns the existing entry if it is already here —
     * re-sharing an article you saved last week should not give you two of it.
     * Null when the text is not a link at all.
     *
     * [title] lets a caller that already knows the headline — a feed entry
     * carries its own — skip the URL-slug guess. It still improves once the
     * page itself is fetched, same as any other saved link.
     */
    fun save(rawUrl: String, title: String? = null): SavedLink? {
        if (!looksLikeUrl(rawUrl)) return null

        val url = normaliseUrl(rawUrl).clean()
        links.firstOrNull { it.url.equals(url, ignoreCase = true) }?.let { return it }

        val link = SavedLink(
            id = Clock.System.now().toEpochMilliseconds().toString(36) + "-" + links.size,
            url = url,
            title = title?.clean()?.takeIf { it.isNotBlank() } ?: titleFromUrl(url),
            savedAt = Clock.System.now().toEpochMilliseconds(),
        )
        links = listOf(link) + links
        persist()
        return link
    }

    /**
     * Adds everything in [text] that looks like a link — see [parseImport].
     *
     * Links already here are counted and left exactly as they are, read state
     * included. An import is additive by definition: pasting a year-old backup
     * over a live list must not resurrect articles you have since read, and
     * nothing in a paste box is worth the risk of overwriting the record.
     *
     * One state write and one persist for the whole paste, rather than a
     * hundred, which also keeps the list from animating itself apart row by
     * row while it lands.
     */
    fun import(text: String): ImportSummary {
        val found = parseImport(text)
        val known = links.mapTo(mutableSetOf()) { it.url.lowercase() }
        val now = Clock.System.now().toEpochMilliseconds()

        val fresh = found.filterNot { it.url.lowercase() in known }.mapIndexed { index, imported ->
            SavedLink(
                id = now.toString(36) + "-i" + (links.size + index),
                url = imported.url.clean(),
                title = imported.title?.clean()?.takeIf { it.isNotBlank() } ?: titleFromUrl(imported.url),
                savedAt = now,
                // Restored, not re-read: the import knows it was read but not
                // when, and 0L is already how the decoder says exactly that.
                readAt = if (imported.read) 0L else null,
                // A line that carried its own title needs no network. Only the
                // bare URLs are left for the fetcher, so importing a backup of
                // two hundred articles does not become two hundred requests.
                fetched = imported.title != null,
            )
        }

        if (fresh.isNotEmpty()) {
            links = fresh + links
            persist()
        }
        return ImportSummary(found = found.size, added = fresh.size)
    }

    /** Whether [url] is already in the reading list — the state a save button on a feed card renders itself from. */
    fun isSaved(url: String): Boolean = links.any { it.url.equals(url, ignoreCase = true) }

    /**
     * The feed-card save button: tapping it once adds [url] to the reading
     * list, tapping it again on the same card takes it back out. Unlike
     * [remove], there is no swipe-to-confirm here — a card the reader is
     * looking at right now is not the same "did I mean that" risk a row
     * already filed away is.
     */
    fun toggleSaved(url: String, title: String?) {
        val existing = links.firstOrNull { it.url.equals(url, ignoreCase = true) }
        if (existing != null) remove(existing.id) else save(url, title)
    }

    /** Replaces the URL-derived guess once the page itself has answered. */
    fun describe(id: String, title: String?, description: String?) {
        links = links.map { link ->
            if (link.id != id) {
                link
            } else {
                link.copy(
                    title = title?.clean()?.takeIf { it.isNotBlank() } ?: link.title,
                    description = description?.clean()?.takeIf { it.isNotBlank() } ?: link.description,
                    fetched = true,
                )
            }
        }
        persist()
    }

    /** Marks a fetch as finished without changing anything, so the row stops showing a spinner. */
    fun markFetchFailed(id: String) {
        links = links.map { if (it.id == id) it.copy(fetched = true) else it }
        persist()
    }

    /**
     * Pull-to-refresh: re-fetches every link, not just the ones that never
     * finished. A title or description can change after the fact — this is
     * the one deliberate way to notice, rather than waiting for a re-save.
     *
     * Flipping [SavedLink.fetched] back to false is enough on its own: the
     * existing title/description stay put as the fallback shown while the
     * refetch is in flight, and the same fetch loop that handles new links
     * picks these back up because they're `!fetched` again.
     *
     * No [persist] here, unlike every other mutator — `fetched` flipping back
     * to true (or not) as each fetch actually resolves is what's worth
     * writing down; a refresh interrupted mid-flight should just resume as
     * ordinary unfetched links next launch, not persist as a stalled one.
     */
    fun refreshAll() {
        links = links.map { it.copy(fetched = false) }
    }

    /**
     * Marking read stamps the time rather than flipping a flag, and nothing
     * about it removes the link: a reading list that deletes what you finish
     * leaves you unable to answer "what was that article I read last week",
     * which is half of why a record is worth keeping at all.
     */
    fun toggleRead(id: String) {
        links = links.map {
            if (it.id != id) it else it.copy(readAt = if (it.read) null else Clock.System.now().toEpochMilliseconds())
        }
        persist()
    }

    /**
     * The only way a record leaves. Deliberately not on a tap target on the
     * card — a mis-tap should never cost a saved article — so the UI puts it
     * behind a long press.
     */
    fun remove(id: String) {
        links = links.filterNot { it.id == id }
        persist()
    }

    private fun persist() = store.putString(Key, encode(links).takeIf { it.isNotEmpty() })

    private fun load(): List<SavedLink> = store.getString(Key)?.split(RecordSeparator)?.mapNotNull(::decode).orEmpty()

    private fun encode(links: List<SavedLink>): String = links.joinToString(RecordSeparator.toString()) { link ->
        listOf(
            link.id,
            link.url,
            link.title,
            link.description.orEmpty(),
            link.savedAt.toString(),
            link.readAt?.toString().orEmpty(),
            if (link.fetched) "1" else "0",
        ).joinToString(FieldSeparator.toString())
    }

    // Anything malformed is dropped rather than throwing: a corrupt row should
    // cost one link, not the whole reading list on next launch.
    private fun decode(record: String): SavedLink? {
        val fields = record.split(FieldSeparator)
        if (fields.size < 7) return null

        return SavedLink(
            id = fields[0],
            url = fields[1].ifBlank { return null },
            title = fields[2],
            description = fields[3].takeIf { it.isNotBlank() },
            savedAt = fields[4].toLongOrNull() ?: 0L,
            // Was a "1"/"0" read flag before it became a timestamp; an old
            // record's "1" has no time attached, so it reads as "read, when
            // unknown" rather than being thrown away.
            readAt = if (fields[5] == "1") 0L else fields[5].toLongOrNull(),
            fetched = fields[6] == "1",
        )
    }

    private fun String.clean() = filterNot { it == FieldSeparator || it == RecordSeparator }.trim()

    private companion object {
        const val Key = "links.saved"
        const val FieldSeparator = ''
        const val RecordSeparator = ''
    }
}

@Composable
fun rememberLinkLibrary(): LinkLibrary {
    val store = rememberKeyValueStore()
    return remember(store) { LinkLibrary(store) }
}

/**
 * "3h ago". Relative only, and deliberately so: an absolute date needs a
 * calendar, which needs a date-time library this project does not have, and
 * for a reading list "when did I save this" is the question anyway.
 */
@OptIn(ExperimentalTime::class)
fun savedAgo(savedAt: Long): String {
    val minutes = (Clock.System.now().toEpochMilliseconds() - savedAt) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d ago"
        else -> "${minutes / (60 * 24 * 7)}w ago"
    }
}
