package dev.mks.duskread.bridge

import dev.mks.duskread.data.AppGraph
import dev.mks.duskread.links.SavedLink
import dev.mks.duskread.links.fetchLinkMetadata
import dev.mks.duskread.links.savedAgo

/**
 * The saved-links library, minus everything Obj-C cannot carry.
 *
 * Every default argument is spelled out: Kotlin defaults do not survive the
 * export, so `save(url)` would simply not exist on the Swift side.
 */
class LinksBridge internal constructor(private val graph: AppGraph) {
    private val library get() = graph.links

    fun observe(onEach: (List<SavedLink>) -> Unit): Cancellable = library.linksUpdates.watch(onEach)

    fun current(): List<SavedLink> = library.links

    fun save(url: String, title: String?, topic: String?): SavedLink? = library.save(url, title, topic)

    fun isSaved(url: String): Boolean = library.isSaved(url)

    fun toggleSaved(url: String, title: String?, topic: String?) = library.toggleSaved(url, title, topic)

    fun toggleRead(id: String) = library.toggleRead(id)

    fun remove(id: String) = library.remove(id)

    fun retryFetch(id: String) = library.retryFetch(id)

    fun refreshAll() = library.refreshAll()

    fun clear() = library.clear()

    /**
     * Fetches titles for anything saved without one — the same backfill the
     * Compose Saved tab runs on open, kept here so the HTTP client stays behind
     * the facade. Suspending, so Swift gets `await`.
     */
    suspend fun backfillTitles() {
        library.links.filterNot { it.fetched }.forEach { link ->
            val meta = runCatching { fetchLinkMetadata(graph.http, link.url) }.getOrNull()
            if (meta == null) library.markFetchFailed(link.id) else library.describe(link.id, meta.title, meta.description)
        }
    }

    /** "3h ago", so Swift does not grow a second copy of the same rounding. */
    fun savedAgoLabel(savedAt: Long): String = savedAgo(savedAt)
}
