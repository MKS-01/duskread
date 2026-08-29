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

    /**
     * URLs deleted here, so the reading-list sync does not hand them back.
     *
     * The app never deletes a Notion row — it is the archive, and destroying
     * upstream data on a mis-tap is not a trade worth making. But that leaves
     * a deleted link sitting in Notion still ticked as saved, ready to return
     * on the very next pull, so the refusal has to be remembered locally.
     *
     * Bounded and oldest-evicted for the same reason the skip list is: what
     * matters is recent intent, and an unbounded set of every link ever
     * deleted would outgrow the list it protects.
     */
    var removedUrls: Map<String, Long> by mutableStateOf(loadRemoved())
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
     *
     * [topic] is the same idea for the subject: a post saved from a followed
     * blog knows what that blog is about, and recording it here is what lets
     * it survive the trip to Notion and back to another device. Inferring it
     * from the host works only while the feed is still followed.
     */
    fun save(rawUrl: String, title: String? = null, topic: String? = null): SavedLink? {
        if (!looksLikeUrl(rawUrl)) return null

        val url = normaliseUrl(rawUrl).clean()
        links.firstOrNull { it.url.equals(url, ignoreCase = true) }?.let { return it }

        val now = Clock.System.now().toEpochMilliseconds()
        val link = SavedLink(
            id = now.toString(36) + "-" + links.size,
            url = url,
            title = title?.clean()?.takeIf { it.isNotBlank() } ?: titleFromUrl(url),
            savedAt = now,
            changedAt = now,
            topic = topic?.takeIf { it.isNotBlank() },
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
                changedAt = now,
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
    fun toggleSaved(url: String, title: String?, topic: String? = null) {
        val existing = links.firstOrNull { it.url.equals(url, ignoreCase = true) }
        if (existing != null) remove(existing.id) else save(url, title, topic)
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
                    fetchFailed = false,
                    changedAt = Clock.System.now().toEpochMilliseconds(),
                )
            }
        }
        persist()
    }

    /**
     * Marks a fetch as finished without changing anything but [SavedLink.fetchFailed] —
     * the row stops showing a spinner and starts saying it couldn't reach the page,
     * rather than quietly keeping the URL-guessed title forever with no sign anything
     * went wrong.
     */
    fun markFetchFailed(id: String) {
        links = links.map { if (it.id == id) it.copy(fetched = true, fetchFailed = true) else it }
        persist()
    }

    /** One row's retry, from the offline glyph on it — sets it back to `!fetched` without touching the rest of the list. */
    fun retryFetch(id: String) {
        links = links.map { if (it.id == id) it.copy(fetched = false) else it }
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
        links = links.map { it.copy(fetched = false, fetchFailed = false) }
    }

    /**
     * Marking read stamps the time rather than flipping a flag, and nothing
     * about it removes the link: a reading list that deletes what you finish
     * leaves you unable to answer "what was that article I read last week",
     * which is half of why a record is worth keeping at all.
     */
    fun toggleRead(id: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        links = links.map {
            if (it.id != id) it else it.copy(readAt = if (it.read) null else now, changedAt = now)
        }
        persist()
    }

    /**
     * Files this link under a subject.
     *
     * Stamps [SavedLink.changedAt] like every other mutator, which is the
     * whole mechanism by which the next sync carries it to Notion — there is
     * no separate "needs pushing" flag anywhere, and adding one would be a
     * second source of truth about the same fact.
     */
    fun setTopic(id: String, topic: String?) {
        val cleaned = topic?.clean()?.trim()?.takeIf { it.isNotBlank() }
        links = links.map {
            if (it.id != id) it else it.copy(topic = cleaned, changedAt = Clock.System.now().toEpochMilliseconds())
        }
        persist()
    }

    /**
     * The only way a record leaves. Deliberately not on a tap target on the
     * card — a mis-tap should never cost a saved article — so the UI puts it
     * behind a long press.
     */
    fun remove(id: String) {
        links.firstOrNull { it.id == id }?.let { gone -> tombstone(gone.url) }
        links = links.filterNot { it.id == id }
        persist()
    }

    /**
     * What the reading-list sync writes back down.
     *
     * Separate from [save] because this is reconciliation, not capture: it
     * carries an id chosen elsewhere, a read state that may already be set,
     * and a topic no local code could have known. [save] would discard all
     * three and stamp a fresh `savedAt`, which would make the next sync think
     * the phone had just changed the row.
     *
     * A tombstoned URL is refused outright — a link deleted here is deleted,
     * and a row still ticked in Notion is not an argument.
     */
    fun upsertFromNotion(incoming: SavedLink): Boolean {
        if (incoming.url.lowercase() in removedUrls) return false

        val existing = links.firstOrNull {
            it.id == incoming.id || it.url.equals(incoming.url, ignoreCase = true)
        }

        links = if (existing == null) {
            listOf(incoming) + links
        } else {
            links.map { if (it.id == existing.id) existing.merge(incoming) else it }
        }
        persist()
        return existing == null
    }

    /**
     * Notion wins only where it is newer, and only on what it actually knows.
     *
     * Whole-row last-write-wins on read state, which is the one field that
     * realistically diverges. Title and description are taken only to fill a
     * gap: the phone fetches the real page and Notion holds whatever was typed
     * or scraped, so overwriting a fetched title with a filed one would be a
     * downgrade even when the row is newer.
     */
    private fun SavedLink.merge(incoming: SavedLink): SavedLink = copy(
        title = if (fetched) title else incoming.title.ifBlank { title },
        description = description ?: incoming.description,
        readAt = if (incoming.changedAt > changedAt) incoming.readAt else readAt,
        topic = incoming.topic ?: topic,
        changedAt = maxOf(changedAt, incoming.changedAt),
    )

    private fun tombstone(url: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val current = removedUrls + (url.lowercase() to now)
        removedUrls = if (current.size <= MaxRemembered) {
            current
        } else {
            current.entries.sortedByDescending { it.value }.take(MaxRemembered).associate { it.key to it.value }
        }
        store.putString(RemovedKey, encodeRemoved(removedUrls).takeIf { it.isNotEmpty() })
    }

    private fun loadRemoved(): Map<String, Long> = store.getString(RemovedKey)?.split(RecordSeparator)?.mapNotNull { record ->
        val fields = record.split(FieldSeparator)
        val url = fields.getOrNull(0)?.ifBlank { null } ?: return@mapNotNull null
        val at = fields.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
        url to at
    }?.toMap().orEmpty()

    private fun encodeRemoved(urls: Map<String, Long>): String = urls.entries.joinToString(RecordSeparator.toString()) { (url, at) ->
        listOf(url.clean(), at.toString()).joinToString(FieldSeparator.toString())
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
            if (link.fetchFailed) "1" else "0",
            link.changedAt.toString(),
            link.topic.orEmpty(),
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
            // A record written before this field existed has no 8th field —
            // absent reads as "not failed", the same as it did implicitly before.
            fetchFailed = fields.getOrNull(7) == "1",
            // Likewise for the two the reading-list sync added. A record with
            // no change stamp falls back to when it was saved, which is the
            // truth for a link nothing has touched since.
            changedAt = fields.getOrNull(8)?.toLongOrNull() ?: fields[4].toLongOrNull() ?: 0L,
            topic = fields.getOrNull(9)?.takeIf { it.isNotBlank() },
        )
    }

    private fun String.clean() = filterNot { it == FieldSeparator || it == RecordSeparator }.trim()

    private companion object {
        const val Key = "links.saved"
        const val RemovedKey = "links.removed"

        /** Deep enough to cover any plausible clear-out, shallow enough to stay a few kilobytes. */
        const val MaxRemembered = 200
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
