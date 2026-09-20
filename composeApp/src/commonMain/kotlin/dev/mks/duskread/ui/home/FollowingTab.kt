package dev.mks.duskread.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.syncFeeds
import dev.mks.duskread.summary.rememberSummaryCache
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch

/**
 * The followed blogs, on their own screen rather than a digest at the foot of Home.
 */
@Composable
fun FollowingTab(
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    links: LinkLibrary,
    client: HttpClient,
    onOpenTopics: (Feed) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val summaries = rememberSummaryCache()
    var refreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            if (feeds.feeds.isEmpty()) return@PullToRefreshBox
            scope.launch {
                refreshing = true
                syncFeeds(client, feeds.feeds, feedPosts, links, summaries)
                refreshing = false
            }
        },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item("following") {
                FollowingDigest(
                    feedLibrary = feeds,
                    postCache = feedPosts,
                    linkLibrary = links,
                    client = client,
                    onOpenTopics = onOpenTopics,
                    modifier = Modifier.fillMaxWidth(),
                    // Only resolvable here: `fillParentMaxHeight` is a member of
                    // `LazyItemScope`, which only this lambda has.
                    emptyStateModifier = if (feeds.feeds.isEmpty()) Modifier.fillParentMaxHeight(0.65f) else Modifier,
                )
            }
        }
    }
}
