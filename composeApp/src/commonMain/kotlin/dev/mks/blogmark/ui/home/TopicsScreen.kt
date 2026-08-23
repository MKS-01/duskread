package dev.mks.blogmark.ui.home

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
import dev.mks.blogmark.links.Feed
import dev.mks.blogmark.links.FeedPost
import dev.mks.blogmark.links.LinkLibrary
import dev.mks.blogmark.links.savedAgo
import dev.mks.blogmark.ui.PlatformBackHandler
import dev.mks.blogmark.ui.common.EyebrowHeader
import dev.mks.blogmark.ui.common.MonogramBadge
import dev.mks.blogmark.ui.common.ToastRequest
import dev.mks.blogmark.ui.rememberUrlOpener
import dev.mks.blogmark.ui.theme.BlogmarkIcons
import dev.mks.blogmark.ui.theme.Mono
import dev.mks.blogmark.ui.theme.SectionLabel

/**
 * Everything one followed blog has posted, as a list you scroll.
 *
 * The digest's carousel on Home deliberately shows a few posts at a time and
 * no more — it is a strip inside a section inside a dashboard, and making it
 * tall enough to browse properly would cost Home the quiet shape the digest
 * exists to keep. This screen is where that constraint is lifted: same posts,
 * same actions, one per row and the full width of the phone, with nothing
 * else on screen competing for the space.
 *
 * Built in the flat-row language the rest of the app moved to, not in the
 * carousel's boxed cards: an eyebrow with its inline rule opens the list, and
 * a row is a sourcechip, a headline, a mono meta line and its own bottom
 * hairline — no container around it. The strip on Home keeps its boxes
 * because a horizontal row of cards has to agree on a size; a list does not,
 * and a box per row here would be the one screen in the app still building
 * them.
 *
 * A full-screen destination rather than a fourth stop on the floating bar,
 * the same as Focus and Settings: it belongs to a feed you picked, so there
 * is nothing for it to show until you have picked one.
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
            // A bare glyph, no circle behind it — the same call the dashboard's
            // settings icon makes. Chrome around an icon is the boxed-card
            // habit in miniature.
            Icon(
                imageVector = BlogmarkIcons.Back,
                contentDescription = "Back",
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp)
                    .size(38.dp)
                    .clickable(onClick = onClose)
                    .padding(10.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))

            // The blog names the screen. There is no fixed title here on
            // purpose: this screen is only ever reached from one feed's line
            // in the digest, so a constant word at the top would say less
            // than the eyebrow already has to say anyway.
            Column(Modifier.padding(horizontal = 16.dp)) {
                EyebrowHeader(
                    text = feed.host.uppercase(),
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
                    )
                }
            }
        }
    }
}
