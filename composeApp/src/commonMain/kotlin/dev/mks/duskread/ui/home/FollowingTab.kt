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
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch

/**
 * The followed blogs, on their own screen rather than a digest at the foot of
 * Home.
 *
 * [FollowingDigest] used to be the last section of the dashboard's own
 * `LazyColumn`, which meant a reader with more than a handful of feeds never
 * got a scroll of its own — the digest was one item among four, and grew or
 * shrank with however much of it Home was willing to give up. This tab is
 * that same digest with a `LazyColumn` and pull-to-refresh of its own, sized
 * to however many feeds are actually followed rather than to what fits under
 * NEXT UP and the two other sections above it.
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
    var refreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            if (feeds.feeds.isEmpty()) return@PullToRefreshBox
            scope.launch {
                refreshing = true
                syncFeeds(client, feeds.feeds, feedPosts)
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
                )
            }
        }
    }
}
