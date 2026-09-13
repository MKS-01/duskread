package dev.mks.duskread.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.data.KeyValueStore
import dev.mks.duskread.data.LocalAppGraph
import dev.mks.duskread.data.Observed
import dev.mks.duskread.data.rememberKeyValueStore
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The saved links, newest first, persisted through [KeyValueStore].
 */
@OptIn(ExperimentalTime::class)
class LinkLibrary(private val store: KeyValueStore) {
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
        store.putString(Key, null)
        store.putString(RemovedKey, null)
    }

    /**
     * What the reading-list sync writes back down.
     */
    fun upsertFromNotion(incoming: SavedLink): Boolean {
        if (canonicalUrl(incoming.url) in removedKeys) return false

        val existing = links.firstOrNull {
            it.id == incoming.id || sameArticle(it.url, incoming.url)
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
        const val Key = "links.saved"
        const val RemovedKey = "links.removed"

        /** Deep enough to cover any plausible clear-out, shallow enough to stay a few kilobytes. */
        const val MaxRemembered = 200
        const val FieldSeparator = ''
        const val RecordSeparator = ''
    }
}

@Composable
fun rememberLinkLibrary(): LinkLibrary = LocalAppGraph.current.links

/**
 * "3h ago".
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
