package dev.mks.duskread.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.links.LatestItem
import dev.mks.duskread.links.savedAgo
import dev.mks.duskread.ui.OpenRecord
import dev.mks.duskread.ui.ReadingQueue
import dev.mks.duskread.ui.ReadingQueueEntry
import dev.mks.duskread.ui.common.ArticleCard
import dev.mks.duskread.ui.common.CompactEmptyState
import dev.mks.duskread.ui.common.EyebrowHeader
import dev.mks.duskread.ui.common.RowMeta
import dev.mks.duskread.ui.theme.Mono

/**
 * What the followed blogs put out this week, one card each.
 *
 * Emitted as items of Home's own list rather than a `Column` inside one item, so ten
 * cards recycle instead of all being measured at once.
 */
fun LazyListScope.latestSection(
    items: List<LatestItem>,
    bodies: Map<String, CardBody>,
    hasFeeds: Boolean,
    onOpen: (ReadingQueue) -> Unit,
    onFollow: () -> Unit,
) {
    item("latest-head") {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            EyebrowHeader(
                text = "LATEST",
                trailing = if (items.isNotEmpty()) {
                    {
                        Text(
                            text = "${items.size} this week",
                            fontFamily = Mono,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }

    if (items.isEmpty()) {
        item("latest-empty") {
            Column(Modifier.fillMaxWidth().padding(bottom = SectionGap)) {
                if (hasFeeds) {
                    CompactEmptyState(
                        title = "Nothing new this week",
                        message = "The blogs you follow haven't published since last week. Pull down to check again.",
                    )
                } else {
                    CompactEmptyState(
                        title = "Follow a blog",
                        message = "Whatever it publishes this week lands here, with a line about what it says.",
                        onClick = onFollow,
                    )
                }
            }
        }
        return
    }

    // Built once for the whole section: opening a card hands the reader the week, so a
    // page turn walks to the next post rather than back out to Home.
    val queue = ReadingQueue(
        entries = items.map { ReadingQueueEntry(it.url, it.title, it.host, it.topic) },
        source = "Latest",
        // Same as Next up: reading something offered is how it becomes the reader's own.
        record = OpenRecord.SaveAndMarkRead,
    )

    itemsIndexed(items, key = { _, item -> item.url }) { index, item ->
        val body = bodies[item.url]

        Column(Modifier.fillMaxWidth().padding(bottom = if (index == items.lastIndex) SectionGap else CardGap)) {
            ArticleCard(
                host = item.host,
                title = item.title,
                body = body?.text ?: item.excerpt,
                timeAgo = savedAgo(item.publishedAt),
                faded = item.read,
                onClick = { onOpen(queue.at(index)) },
            ) {
                RowMeta("${item.minutes} min")
                item.topic?.let { RowMeta(it.lowercase()) }
                // Said plainly, because a summary is the model's words and an excerpt is
                // the publisher's, and the card should not blur the two.
                when {
                    body?.busy == true -> RowMeta("summarising…")
                    body?.summarised == true -> RowMeta("summary")
                }
            }
        }
    }
}

/** Between one card and the next — closer than two sections, further than two rows. */
private val CardGap = 10.dp
