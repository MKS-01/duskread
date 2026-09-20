package dev.mks.duskread.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LatestItem
import dev.mks.duskread.links.articleTextOf
import dev.mks.duskread.links.postFor
import dev.mks.duskread.summary.SummariserState
import dev.mks.duskread.summary.parseSummary
import dev.mks.duskread.summary.rememberSummariser
import dev.mks.duskread.summary.rememberSummaryCache
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * What a Latest card is showing and where it came from — the reader should know whose
 * words those are.
 */
data class CardBody(val text: String, val summarised: Boolean, val busy: Boolean)

/**
 * Summaries for the cards on Home, cheaply.
 *
 * Every card opens on its own excerpt, and is upgraded in place if and when the model
 * answers. Nothing here ever downloads a model or reaches the network.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun rememberCardBodies(items: List<LatestItem>, feedPosts: FeedPostCache): Map<String, CardBody> {
    val cache = rememberSummaryCache()
    val summariser = rememberSummariser()
    val engineState = summariser.state

    // The one in flight, so a card can say it is working rather than looking stuck.
    var working by remember { mutableStateOf<String?>(null) }

    // A model that could not finish this article will not finish it on the next sync
    // either; retrying each time would spend the battery to be told so again.
    val failed = remember { mutableSetOf<String>() }

    LaunchedEffect(items, engineState) {
        working = null
        // Only ever *uses* a model already on the device. Home is not where a
        // several-hundred-megabyte download gets started without being asked.
        if (engineState !is SummariserState.Ready) return@LaunchedEffect

        // One at a time and in order, so the cards the reader is looking at fill first
        // and the phone is never running two of these at once.
        for (item in items.take(SummariesPerVisit)) {
            if (cache.summaryFor(item.url) != null || item.url in failed) continue

            val markup = feedPosts.postsByFeed.postFor(item.url)?.content ?: continue
            val text = articleTextOf(markup)
            if (text.length < MinSummarisableChars) {
                failed += item.url
                continue
            }

            working = item.url
            // Not `runCatching`: it swallows cancellation too, and this effect is
            // cancelled routinely — Home is one swipe from being left.
            val answer = try {
                var latest = ""
                summariser.summarise(item.title, text).collect { latest = it }
                latest
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                failed += item.url
                working = null
                continue
            }

            val summary = parseSummary(answer, item.url, item.title, engineState.model, Clock.System.now().toEpochMilliseconds())
            if (summary == null) failed += item.url else cache.put(summary)
            working = null
        }
    }

    // Read outside the effect so a summary landing recomposes the card that wanted it.
    val summaries = cache.summaries
    val inFlight = working

    return items.associate { item ->
        val summary = summaries[item.url]
        item.url to CardBody(
            text = summary?.text ?: item.excerpt,
            summarised = summary != null,
            busy = item.url == inFlight,
        )
    }
}

/**
 * How many of the ten cards get a summary per visit to Home. The rest keep their
 * excerpts, which is not a failure state — an excerpt is the publisher's own opening.
 */
private const val SummariesPerVisit = 6

/** The same floor the summary panel uses: below it, a model summarises nothing confidently. */
private const val MinSummarisableChars = 400
