package dev.mks.duskread.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.links.Feed
import dev.mks.duskread.links.FeedPost
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.savedAgo
import dev.mks.duskread.ui.PlatformBackHandler
import dev.mks.duskread.ui.common.EyebrowHeader
import dev.mks.duskread.ui.common.MonogramBadge
import dev.mks.duskread.ui.common.ToastRequest
import dev.mks.duskread.ui.rememberUrlOpener
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.SectionLabel

/**
 * Everything one followed blog has posted, as a list you scroll.
 */
@Composable
fun TopicsScreen(
    feed: Feed,
    posts: List<FeedPost>,
    linkLibrary: LinkLibrary,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(enabled = true, onBack = onClose)

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // A bare glyph, no circle behind it — the same call the dashboard's settings
            // icon makes. Chrome around an icon is the boxed-card habit in miniature.
            Icon(
                imageVector = DuskReadIcons.Back,
                contentDescription = "Back",
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp)
                    .size(38.dp)
                    .clickable(onClick = onClose)
                    .padding(10.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))

            // The blog names the screen.
            Column(Modifier.padding(horizontal = 16.dp)) {
                EyebrowHeader(
                    text = feed.label.uppercase(),
                    trailing = {
                        Text(
                            text = "${posts.size} POSTS",
                            style = SectionLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            Spacer(Modifier.height(18.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            ) {
                itemsIndexed(posts, key = { _, post -> post.url }) { index, post ->
                    TopicRow(
                        post = post,
                        host = feed.host,
                        last = index == posts.lastIndex,
                        linkLibrary = linkLibrary,
                        topic = feed.topic,
                    )
                }
            }
        }
    }
}
