package dev.mks.duskread.speech

import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LatestItem
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.ReadingSignals
import dev.mks.duskread.links.loadArticle
import dev.mks.duskread.ui.OpenRecord
import dev.mks.duskread.ui.ReadingQueueEntry
import dev.mks.duskread.ui.recordOpened
import io.ktor.client.HttpClient

/**
 * Latest's unread posts as one listen-through, in the cards' own order. Shared so both
 * homes queue, announce and mark the same posts; null when the week is all read.
 */
fun weekListenQueue(
    items: List<LatestItem>,
    feeds: FeedLibrary,
    cache: FeedPostCache,
    links: LinkLibrary,
    signals: ReadingSignals,
    http: HttpClient,
): ListenQueue? {
    val unread = items.filterNot { it.read }
    if (unread.isEmpty()) return null
    val entries = unread.map { item ->
        ListenEntry(
            url = item.url,
            title = item.title,
            source = feeds.feeds.firstOrNull { it.id == item.feedId }?.label ?: item.host,
            feedContent = cache.postsByFeed[item.feedId]?.firstOrNull { it.url == item.url }?.content,
            topic = item.topic,
        )
    }
    return ListenQueue(
        entries = entries,
        loadText = { loadArticle(http, it.url, it.title, it.feedContent)?.text },
        // The same record opening the card leaves: heard to the end is read.
        onHeard = { recordOpened(ReadingQueueEntry(url = it.url, title = it.title, topic = it.topic), OpenRecord.SaveAndMarkRead, links, signals) },
    )
}
