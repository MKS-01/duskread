package dev.mks.duskread.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.LocalAppGraph
import dev.mks.duskread.data.Observed
import dev.mks.duskread.data.rememberKeyValueStore
import dev.mks.duskread.db.DuskReadDatabase
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import dev.mks.duskread.db.SavedLink as SavedLinkRow

/**
 * The saved links, newest first, one SQLite row each — a save or a tick writes one row,
 * not the whole list.
 */
@OptIn(ExperimentalTime::class)
class LinkLibrary(
    private val store: KeyValueStore,
    private val db: DuskReadDatabase,
) {
    // Each row's sort key and what was last written, so [persist] can write only the
    // difference. Declared first: [load] fills them.
    private val seqById = HashMap<String, Long>()
    private var written: Map<String, SavedLink> = emptyMap()

    // Snapshot state and a StateFlow in one, so Compose and the iOS bridge read the same
    // value.
    private val observedLinks = Observed(load())
    private val observedRemovedUrls = Observed(loadRemoved())

    var links: List<SavedLink> by observedLinks
        private set

    /** [links] for observers outside a composition; see [Observed]. */
    val linksUpdates: StateFlow<List<SavedLink>> get() = observedLinks.updates

    /**
     * URLs deleted here, so the reading-list sync does not hand them back.
     */
    var removedUrls: Map<String, Long> by observedRemovedUrls
        private set

    /** [removedUrls] for observers outside a composition; see [Observed]. */
    val removedUrlsUpdates: StateFlow<Map<String, Long>> get() = observedRemovedUrls.updates

    /** The canonical forms of [removedUrls], derived rather than stored. */
    val removedKeys: Set<String>
        get() = removedUrls.keys.mapTo(mutableSetOf(), ::canonicalUrl)

    /**
     * Saves [rawUrl], or returns the existing entry if it is already here — re-sharing an
     * article you saved last week should not give you two of it.
     */
    fun save(rawUrl: String, title: String? = null, topic: String? = null): SavedLink? {
        if (!looksLikeUrl(rawUrl)) return null

        val url = normaliseUrl(rawUrl).clean()
        // Matched on the canonical form, not the address as typed: the same article
        // reaches this app from a feed, from a newsletter carrying `?utm_source=`.
        val key = canonicalUrl(url)
        links.firstOrNull { canonicalUrl(it.url) == key }?.let { return it }

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

    /** Whether [url] is already in the reading list — the state a save button on a feed card renders itself from. */
    fun isSaved(url: String): Boolean = links.any { sameArticle(it.url, url) }

    /**
     * The feed-card save button: tapping it once adds [url] to the reading list, tapping
     * it again on the same card takes it back out.
     */
    fun toggleSaved(url: String, title: String?, topic: String? = null) {
        val existing = links.firstOrNull { sameArticle(it.url, url) }
        if (existing != null) remove(existing.id) else save(url, title, topic)
    }

    /** Replaces the URL-derived guess once the page itself has answered. */
    fun describe(id: String, title: String?, description: String?) {
        links = links.map { link ->
            if (link.id != id) {
                link
            } else {
                val described = link.copy(
                    title = title?.clean()?.takeIf { it.isNotBlank() } ?: link.title,
                    description = description?.clean()?.takeIf { it.isNotBlank() } ?: link.description,
                    fetched = true,
                    fetchFailed = false,
                )
                // Stamped only on a real change: a pull-to-refresh that re-reads the same
                // titles must not mark every link as owed to Notion.
                val changed = described.title != link.title || described.description != link.description
                if (changed) described.copy(changedAt = Clock.System.now().toEpochMilliseconds()) else described
            }
        }
        persist()
    }

    /**
     * Marks a fetch as finished without changing anything but [SavedLink.fetchFailed] —
     * the row stops showing a spinner and starts saying it couldn't reach the page.
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
     * Pull-to-refresh: re-fetches every link, not just the ones that never finished.
     */
    fun refreshAll() {
        links = links.map { it.copy(fetched = false, fetchFailed = false) }
    }

    /**
     * Marking read stamps the time rather than flipping a flag, and nothing about it
     * removes the link.
     */
    fun toggleRead(id: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        links = links.map {
            if (it.id != id) it else it.copy(readAt = if (it.read) null else now, changedAt = now)
        }
        persist()
    }

    /**
     * The only way a record leaves. Deliberately not on a tap target on the card — a
     * mis-tap should never cost a saved article — so the UI puts it behind a long press.
     */
    fun remove(id: String) {
        links.firstOrNull { it.id == id }?.let { gone -> tombstone(gone.url) }
        links = links.filterNot { it.id == id }
        persist()
    }

    /**
     * Everything, gone — the saved links *and* the tombstones. The tombstones are the
     * half worth stating.
     */
    fun clear() {
        links = emptyList()
        removedUrls = emptyMap()
        db.savedLinkQueries.transaction {
            db.savedLinkQueries.deleteAll()
            db.savedLinkQueries.deleteAllRemoved()
        }
        seqById.clear()
        written = emptyMap()
    }

    /**
     * What the reading-list sync writes back down, all rows in one write. Returns how
     * many were new; one persist per row was a full re-encode per row.
     */
    fun upsertAllFromNotion(incoming: List<SavedLink>): Int {
        val refused = removedKeys
        // Newest-first like the list itself: each new row goes on top, as one save would.
        val merged = ArrayDeque(links)
        val byId = HashMap<String, SavedLink>()
        val byKey = HashMap<String, SavedLink>()
        merged.forEach { link ->
            byId[link.id] = link
            byKey[canonicalUrl(link.url)] = link
        }
        var added = 0

        incoming.forEach { link ->
            val key = canonicalUrl(link.url)
            if (key in refused) return@forEach

            val existing = byId[link.id] ?: byKey[key]
            val next = existing?.merge(link) ?: link
            if (existing == null) {
                merged.addFirst(next)
                added++
            } else {
                merged[merged.indexOf(existing)] = next
            }
            byId[next.id] = next
            byKey[canonicalUrl(next.url)] = next
        }

        if (merged != links) {
            links = merged.toList()
            persist()
        }
        return added
    }

    /**
     * Notion wins only where it is newer, and only on what it actually knows. Whole-row
     * last-write-wins on read state, which is the one field that realistically diverges.
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
        val current = removedUrls + (url to now)
        removedUrls = if (current.size <= MaxRemembered) {
            current
        } else {
            current.entries.sortedByDescending { it.value }.take(MaxRemembered).associate { it.key to it.value }
        }
        db.savedLinkQueries.transaction {
            db.savedLinkQueries.upsertRemoved(url, now)
            db.savedLinkQueries.trimRemoved(MaxRemembered.toLong())
        }
    }

    private fun loadRemoved(): Map<String, Long> = db.savedLinkQueries.selectRemoved().executeAsList().associate { it.url to it.removedAt }

    /**
     * Writes only the rows that differ from the last write. A new link takes the next
     * [seqById] value, handed out oldest-first so a batch keeps its on-screen order.
     */
    private fun persist() {
        val current = links
        val ids = current.mapTo(HashSet()) { it.id }
        val gone = written.keys.filterNot { it in ids }
        val changed = current.filter { written[it.id] != it }
        if (gone.isEmpty() && changed.isEmpty()) return

        var next = (seqById.values.maxOrNull() ?: 0L) + 1
        changed.asReversed().forEach { link -> if (link.id !in seqById) seqById[link.id] = next++ }
        db.savedLinkQueries.transaction {
            gone.forEach { id ->
                db.savedLinkQueries.deleteById(id)
                seqById.remove(id)
            }
            changed.forEach { link -> db.savedLinkQueries.upsert(link.asRow(seqById.getValue(link.id))) }
        }
        written = current.associateBy { it.id }
    }

    private fun load(): List<SavedLink> {
        importLegacy()
        val rows = db.savedLinkQueries.selectAll().executeAsList()
        rows.forEach { seqById[it.id] = it.seq }
        return rows.map { it.asLink() }.also { loaded -> written = loaded.associateBy { it.id } }
    }

    /**
     * Moves an older build's two strings into empty tables. Never over rows: a removal
     * lost to an early kill can bring a stale string back.
     */
    private fun importLegacy() {
        val empty = db.savedLinkQueries.count().executeAsOne() == 0L
        store.getString(Key)?.let { legacy ->
            if (empty) {
                val links = legacy.split(RecordSeparator).mapNotNull(::decode).distinctBy { it.id }
                db.savedLinkQueries.transaction {
                    links.forEachIndexed { index, link -> db.savedLinkQueries.upsert(link.asRow((links.size - index).toLong())) }
                }
            }
            store.putString(Key, null)
        }
        store.getString(RemovedKey)?.let { legacy ->
            if (empty) {
                db.savedLinkQueries.transaction {
                    legacy.split(RecordSeparator).forEach { record ->
                        val fields = record.split(FieldSeparator)
                        val url = fields.getOrNull(0)?.ifBlank { null } ?: return@forEach
                        val at = fields.getOrNull(1)?.toLongOrNull() ?: return@forEach
                        db.savedLinkQueries.upsertRemoved(url, at)
                    }
                }
            }
            store.putString(RemovedKey, null)
        }
    }

    // Anything malformed is dropped rather than throwing: a corrupt row should cost one
    // link, not the whole reading list on next launch.
    private fun decode(record: String): SavedLink? {
        val fields = record.split(FieldSeparator)
        if (fields.size < 7) return null

        return SavedLink(
            id = fields[0],
            url = fields[1].ifBlank { return null },
            title = fields[2],
            description = fields[3].takeIf { it.isNotBlank() },
            savedAt = fields[4].toLongOrNull() ?: 0L,
            // Was a "1"/"0" read flag before it became a timestamp; an old record's "1"
            // has no time attached.
            readAt = if (fields[5] == "1") 0L else fields[5].toLongOrNull(),
            fetched = fields[6] == "1",
            // A record written before this field existed has no 8th field — absent reads
            // as "not failed", the same as it did implicitly before.
            fetchFailed = fields.getOrNull(7) == "1",
            // Likewise for the two the reading-list sync added.
            changedAt = fields.getOrNull(8)?.toLongOrNull() ?: fields[4].toLongOrNull() ?: 0L,
            topic = fields.getOrNull(9)?.takeIf { it.isNotBlank() },
        )
    }

    private fun String.clean() = filterNot { it == FieldSeparator || it == RecordSeparator }.trim()

    private companion object {
        /** Where an older build kept both lists; read once by [importLegacy]. */
        const val Key = "links.saved"
        const val RemovedKey = "links.removed"

        /** Deep enough to cover any plausible clear-out, shallow enough to stay a few kilobytes. */
        const val MaxRemembered = 200
        const val FieldSeparator = ''
        const val RecordSeparator = ''
    }
}

private fun SavedLink.asRow(seq: Long) = SavedLinkRow(
    id = id,
    url = url,
    title = title,
    description = description,
    savedAt = savedAt,
    readAt = readAt,
    fetched = if (fetched) 1L else 0L,
    fetchFailed = if (fetchFailed) 1L else 0L,
    changedAt = changedAt,
    topic = topic,
    seq = seq,
)

private fun SavedLinkRow.asLink() = SavedLink(
    id = id,
    url = url,
    title = title,
    description = description,
    savedAt = savedAt,
    readAt = readAt,
    fetched = fetched == 1L,
    fetchFailed = fetchFailed == 1L,
    changedAt = changedAt,
    topic = topic,
)

@Composable
fun rememberLinkLibrary(): LinkLibrary = LocalAppGraph.current.links

/**
 * "3h ago".
 */
@OptIn(ExperimentalTime::class)
fun savedAgo(savedAt: Long, now: Long = Clock.System.now().toEpochMilliseconds()): String {
    val minutes = (now - savedAt) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)}d ago"
        else -> "${minutes / (60 * 24 * 7)}w ago"
    }
}
