package dev.mks.blogmark.ui.links

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.blogmark.links.LinkLibrary
import dev.mks.blogmark.links.SavedLink
import dev.mks.blogmark.links.createHttpClient
import dev.mks.blogmark.links.fetchLinkMetadata
import dev.mks.blogmark.links.looksLikeUrl
import dev.mks.blogmark.links.savedAgo
import dev.mks.blogmark.ui.common.AppTextField
import dev.mks.blogmark.ui.common.EmptyState
import dev.mks.blogmark.ui.common.EyebrowHeader
import dev.mks.blogmark.ui.common.MonogramBadge
import dev.mks.blogmark.ui.common.ToastRequest
import dev.mks.blogmark.ui.rememberUrlOpener
import dev.mks.blogmark.ui.theme.BlogmarkIcons
import dev.mks.blogmark.ui.theme.Mono
import dev.mks.blogmark.ui.theme.Radius
import dev.mks.blogmark.ui.theme.SectionLabel

/**
 * Saved links: the blogs and articles worth reading, one URL at a time.
 *
 * Readback is a synced library of audio someone else prepared; a link here is
 * whatever the reader found themselves, still as text. This is where those
 * go, and it is the only screen in the app whose contents the reader writes.
 *
 * Told apart from Readback's rows by what they leave out rather than a
 * different shape: unread and read share the same flat row, recession alone
 * — reduced opacity and a trailing tick — marks one as done.
 *
 * A link is saved immediately with a title guessed from its URL, and the page
 * is fetched afterwards to replace that guess. The alternative — blocking the
 * save on a network round trip — means a share from the browser can fail
 * because a tunnel ate the request, which is exactly when you are saving
 * things to read later.
 *
 * Fetching lives here rather than beside the library so it only runs while
 * this screen is open. Anything still unfetched is retried the next time you
 * visit, which doubles as the retry path for links saved offline.
 */
@Composable
fun LinksTab(
    library: LinkLibrary,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val open = rememberUrlOpener()
    val client = remember { createHttpClient() }
    DisposableEffect(client) { onDispose { client.close() } }

    val pending = library.links.filterNot { it.fetched }
    LaunchedEffect(pending.map { it.id }) {
        pending.forEach { link ->
            // One at a time on purpose: this is a handful of links, and a
            // sequential walk keeps the list settling top-down rather than
            // rearranging itself in bursts.
            val meta = runCatching { fetchLinkMetadata(client, link.url) }.getOrNull()
            if (meta == null) library.markFetchFailed(link.id) else library.describe(link.id, meta.title, meta.description)
        }
    }

    // The spinner tracks the real fetch loop above rather than a fixed
    // delay — refreshAll() only flips `fetched` back to false, so "done" is
    // whenever `pending` drains again, the same signal that loop already runs on.
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(pending.isEmpty()) {
        if (pending.isEmpty()) refreshing = false
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            library.refreshAll()
        },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item("add") {
                AddLinkField(
                    onSave = {
                        val saved = library.save(it) != null
                        if (saved) ToastRequest.show("Saved")
                        saved
                    },
                    modifier = Modifier.padding(bottom = 22.dp),
                )
            }

            if (library.links.isEmpty()) {
                item("empty") {
                    // Fills the rest of the viewport below the paste field so the
                    // empty state sits low on the screen rather than pinned under
                    // it the way a plain list item would.
                    Box(Modifier.fillMaxWidth().fillParentMaxHeight(0.65f), contentAlignment = Alignment.BottomStart) {
                        EmptyState(
                            title = "Nothing saved yet",
                            message = "Share an article to Blogmark from any app, or paste its address above. " +
                                "The title fills itself in once the page has been read.",
                        )
                    }
                }
            }

            val (read, unread) = library.links.partition { it.read }

            if (unread.isNotEmpty()) {
                item("unread-head") {
                    EyebrowHeader(text = "UNREAD · ${unread.size}", modifier = Modifier.padding(bottom = 12.dp))
                }
                unread.forEachIndexed { index, link ->
                    item(link.id) {
                        LinkRow(
                            link = link,
                            last = index == unread.lastIndex,
                            onOpen = {
                                open(link.url)
                                library.toggleRead(link.id)
                            },
                            onToggleRead = { library.toggleRead(link.id) },
                            onRemove = {
                                library.remove(link.id)
                                ToastRequest.show("Removed")
                            },
                        )
                    }
                }
            }

            // Read links stay, under their own heading. They are the record: what
            // was read and when, which is the question a reading list gets asked
            // long after the reading is done. Sorted by when they were read rather
            // than saved, so the section reads as a history.
            if (read.isNotEmpty()) {
                item("read-head") {
                    EyebrowHeader(
                        text = "READ · ${read.size}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = if (unread.isEmpty()) 0.dp else 20.dp, bottom = 12.dp),
                    )
                }

                val sorted = read.sortedByDescending { it.readAt ?: it.savedAt }
                sorted.forEachIndexed { index, link ->
                    item(link.id) {
                        LinkRow(
                            link = link,
                            last = index == sorted.lastIndex,
                            onOpen = { open(link.url) },
                            onToggleRead = { library.toggleRead(link.id) },
                            onRemove = {
                                library.remove(link.id)
                                ToastRequest.show("Removed")
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The paste field: a flat, full-width pill rather than a bordered text field
 * with its own chrome — the same shape as a `.pill` control everywhere else
 * in the app, just wide. It offers the clipboard rather than reading it
 * silently — a screen that quietly knows what you copied elsewhere is
 * unsettling, and one tap is a small price for the reader staying in charge
 * of that.
 */
@Composable
private fun AddLinkField(onSave: (String) -> Boolean, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }
    var rejected by remember { mutableStateOf(false) }

    fun submit() {
        rejected = !onSave(text)
        if (!rejected) text = ""
    }

    val clipped = clipboard.getText()?.text?.trim().orEmpty()
    val offer = clipped.takeIf { it.isNotEmpty() && looksLikeUrl(it) && it != text }

    Column(modifier) {
        AppTextField(
            value = text,
            onValueChange = {
                text = it
                rejected = false
            },
            placeholder = "Paste a link",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            trailing = {
                AnimatedVisibility(text.isNotBlank()) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = ::submit)
                            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    )
                }
            },
        )

        if (rejected) {
            Text(
                text = "That doesn’t look like a link.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp, top = 6.dp),
            )
        }

        if (offer != null) {
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(Radius.Inline))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { text = offer }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "From clipboard",
                    style = SectionLabel,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = offer,
                    fontSize = 11.5.sp,
                    fontFamily = Mono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * One saved link: a monogram, title, host and a relative timestamp — nothing
 * more. A read row is the exact same shape at half opacity with a trailing
 * tick, never a strikethrough or a second layout; recession alone is what
 * tells them apart. Removal is a swipe, not a button — no tap target on this
 * row destroys anything, and a swipe carries its own undo (let go halfway and
 * nothing happens) which no icon can offer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkRow(
    link: SavedLink,
    last: Boolean,
    onOpen: () -> Unit,
    onToggleRead: () -> Unit,
    onRemove: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dismiss = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onRemove()
            // Never let the box settle into a dismissed state of its own: the
            // row is gone from the list the moment onRemove lands, and a box
            // holding a "dismissed" position would flash the background of a
            // row that no longer exists.
            false
        },
    )

    Column(Modifier.fillMaxWidth()) {
        SwipeToDismissBox(
            state = dismiss,
            enableDismissFromStartToEnd = false,
            backgroundContent = { RemoveBackground(dismiss.progress) },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(scheme.background)
                    .alpha(if (link.read) 0.5f else 1f)
                    .clickable(onClick = onOpen),
                verticalAlignment = Alignment.Top,
            ) {
                MonogramBadge(host = link.host, size = 22.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = link.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = scheme.onSurface,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = when {
                            !link.fetched -> "reading the page…"
                            link.readAt != null && link.readAt > 0L -> "${link.host} · ${savedAgo(link.readAt)}"
                            else -> "${link.host} · ${savedAgo(link.savedAt)}"
                        },
                        fontFamily = Mono,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = scheme.onSurfaceVariant,
                    )
                }
                // Only a read row carries the tick — matching the unread row
                // above it exactly, sourcechip and two facts, nothing more.
                // Still tappable, so marking something read is reversible
                // without having to reopen it.
                if (link.read) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = BlogmarkIcons.Check,
                        contentDescription = "Mark unread",
                        modifier = Modifier
                            .size(26.dp)
                            .clickable(onClick = onToggleRead)
                            .padding(6.dp),
                        tint = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(15.dp))
        if (!last) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant))
            Spacer(Modifier.height(15.dp))
        }
    }
}

/** What the swipe reveals: the intent stated in words, so nothing is destroyed by a mystery gesture. */
@Composable
private fun RemoveBackground(progress: Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (progress > 0.4f) "Release to remove" else "Remove",
            style = MaterialTheme.typography.labelLarge,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            BlogmarkIcons.Close,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
