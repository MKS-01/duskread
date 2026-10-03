package dev.mks.duskread.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.mks.duskread.data.LocalAppGraph
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.followingGroups
import dev.mks.duskread.ui.common.rememberOffMain
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import kotlin.time.Clock

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
    val feedSync = LocalAppGraph.current.feedSync
    val state = rememberFollowingState(feeds)
    val now = remember(feedPosts.postsByFeed) { Clock.System.now().toEpochMilliseconds() }
    val groups = rememberOffMain(feeds.feeds, feedPosts.postsByFeed, links.links, state.query, now) {
        followingGroups(feeds.feeds, feedPosts, links, state.query, now)
    }

    PullToRefreshBox(
        // Handed off to FOLLOWING's header, the same as Home's pull.
        isRefreshing = false,
        onRefresh = { scope.launch { feedSync.sync() } },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item("following-head") {
                FollowingHead(
                    state = state,
                    groups = groups,
                    feedLibrary = feeds,
                    postCache = feedPosts,
                    linkLibrary = links,
                    client = client,
                    modifier = Modifier.fillMaxWidth(),
                    // Only resolvable here: `fillParentMaxHeight` is a member of
                    // `LazyItemScope`, which only this lambda has.
                    emptyStateModifier = if (feeds.feeds.isEmpty()) Modifier.fillParentMaxHeight(0.65f) else Modifier,
                )
            }
            followingRows(state, groups, links, now, onOpenTopics)
        }
    }
}
