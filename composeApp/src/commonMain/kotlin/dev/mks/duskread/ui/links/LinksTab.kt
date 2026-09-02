package dev.mks.duskread.ui.links

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
import dev.mks.duskread.data.rememberUserPrefs
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.ReadingSignals
import dev.mks.duskread.links.SavedLink
import dev.mks.duskread.links.createHttpClient
import dev.mks.duskread.links.fetchLinkMetadata
import dev.mks.duskread.links.looksLikeUrl
import dev.mks.duskread.links.savedAgo
import dev.mks.duskread.speech.speechSupported
import dev.mks.duskread.summary.SummaryRequest
import dev.mks.duskread.summary.SummaryTarget
import dev.mks.duskread.summary.summariesSupported
import dev.mks.duskread.ui.common.AppTextField
import dev.mks.duskread.ui.common.CompactEmptyState
import dev.mks.duskread.ui.common.EmptyState
import dev.mks.duskread.ui.common.EyebrowHeader
import dev.mks.duskread.ui.common.HeaderAction
import dev.mks.duskread.ui.common.ListRowBody
import dev.mks.duskread.ui.common.ListRowDivider
import dev.mks.duskread.ui.common.MonogramBadge
import dev.mks.duskread.ui.common.Pill
import dev.mks.duskread.ui.common.RowMeta
import dev.mks.duskread.ui.common.RowTone
import dev.mks.duskread.ui.common.ToastRequest
import dev.mks.duskread.ui.rememberUrlOpener
import dev.mks.duskread.ui.summary.SummariseBackground
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.SectionLabel

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
    signals: ReadingSignals,
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

    // Three controls, all folded away until asked for: a filter, a search
    // field and the paste box itself. Saved is a list you come back to, and
    // by the time it is worth searching it is long enough that a permanently
    // parked paste field is the least useful thing on the screen — sharing
    // from the browser is how most links actually arrive. Open with it
    // showing while there is nothing saved, for the same reason Following
    // opens on Manage: a first visit is exactly when adding is the only
    // thing to do here.
    var adding by remember { mutableStateOf(library.links.isEmpty()) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LinkFilter.ALL) }

    val matching = library.links.filter { it.matches(query) }
    val (read, unread) = matching.partition { it.read }
    // The filter picks which of the two sections exist at all rather than
    // reordering anything: UNREAD and READ are already the shape of this
    // screen, so "Unread" is that heading on its own, not a third layout.
    val showUnread = filter != LinkFilter.READ && unread.isNotEmpty()
    val showRead = filter != LinkFilter.UNREAD && read.isNotEmpty()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            library.refreshAll()
        },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item("controls") {
                Column(Modifier.padding(bottom = 18.dp)) {
                    EyebrowHeader(
                        text = "SAVED · ${library.links.size}",
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HeaderAction(icon = DuskReadIcons.Search, label = "Search") {
                                    searching = !searching
                                    if (!searching) query = ""
                                }
                                HeaderAction(if (adding) "Done" else "Add") { adding = !adding }
                            }
                        },
                    )
                    Spacer(Modifier.height(12.dp))

                    if (library.links.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            LinkFilter.entries.forEach { choice ->
                                Pill(choice.label, filter == choice) { filter = choice }
                            }
                        }
                    }

                    AnimatedVisibility(searching) {
                        AppTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = "Search by title, host or topic",
                            fontSize = 14.5.sp,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }

                    AnimatedVisibility(adding) {
                        AddLinkField(
                            onSave = {
                                val saved = library.save(it) != null
                                if (saved) ToastRequest.show("Saved")
                                saved
                            },
                        )
                    }
                }
            }

            if (library.links.isEmpty()) {
                item("empty") {
                    // Fills the rest of the viewport below the paste field so the
                    // empty state sits low on the screen rather than pinned under
                    // it the way a plain list item would.
                    Box(Modifier.fillMaxWidth().fillParentMaxHeight(0.65f), contentAlignment = Alignment.BottomStart) {
                        EmptyState(
                            title = "Nothing saved yet",
                            message = "Share an article to DuskRead from any app, or paste its address above. " +
                                "The title fills itself in once the page has been read.",
                        )
                    }
                }
            } else if (!showUnread && !showRead) {
                // Narrowed to nothing — which is a fact about the query or the
                // filter, not about the library, so it says which one and stays
                // compact rather than taking over a screen that still has
                // content one tap away.
                item("no-matches") {
                    CompactEmptyState(
                        title = if (query.isNotBlank()) "Nothing matches “$query”" else "Nothing ${filter.label.lowercase()} here",
                        message = if (query.isNotBlank()) {
                            "Try a different title, host or topic."
                        } else {
                            "Switch the filter back to All to see everything saved."
                        },
                    )
                }
            }

            if (showUnread) {
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
                                signals.recordRead(link.url)
                            },
                            onToggleRead = {
                                library.toggleRead(link.id)
                                signals.recordRead(link.url)
                            },
                            onRetry = { library.retryFetch(link.id) },
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
            if (showRead) {
                item("read-head") {
                    EyebrowHeader(
                        text = "READ · ${read.size}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = if (showUnread) 20.dp else 0.dp, bottom = 12.dp),
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
                            onRetry = { library.retryFetch(link.id) },
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
 * What the pills above the list choose between. Read links are kept forever
 * and eventually outnumber the unread ones, which is the whole reason this
 * exists: "Unread" is the reading queue, "Read" is the record, "All" is the
 * screen as it always was.
 */
private enum class LinkFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    READ("Read"),
}

/**
 * What the search field looks at: the three facts a row actually shows. The
 * URL is deliberately not searched — a query typed here is remembered words,
 * and matching a slug inside an address surfaces rows whose visible text has
 * nothing to do with what was typed.
 */
private fun SavedLink.matches(query: String): Boolean = query.isBlank() ||
    title.contains(query, ignoreCase = true) ||
    host.contains(query, ignoreCase = true) ||
    topic?.contains(query, ignoreCase = true) == true

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
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val swipeDefault = rememberUserPrefs().swipeDefault

    // The two directions swapped when the summary panel learned to read aloud.
    // Listening is the thing a saved row is reached for most, and it had ended
    // up as the deep half of a metered pull; it gets the leftward swipe, which
    // is the easier one for a right thumb, and Remove takes the other side.
    //
    // Nothing about removal got easier or harder in the move: it is the same
    // single-threshold pull with the same worded warning, in the other
    // direction.
    val dismiss = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                // Same commitment as removal, nothing destroyed. The panel
                // does its own fetching, so this hands over the little the row
                // knows and lets it spring back.
                SwipeToDismissBoxValue.EndToStart ->
                    SummaryRequest.open(SummaryTarget(link.url, link.title))

                SwipeToDismissBoxValue.StartToEnd -> onRemove()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            // Never let the box settle into a dismissed state of its own: the
            // row is gone from the list the moment onRemove lands, and a box
            // holding a "dismissed" position would flash the background of a
            // row that no longer exists.
            false
        },
    )

    Column(Modifier.fillMaxWidth()) {
        // The body and the divider are placed separately, rather than using
        // `ListRow` whole, so the hairline stays put while the row slides out
        // from over it — see `ListRowBody`.
        SwipeToDismissBox(
            state = dismiss,
            // Only where there is something to run: a gesture whose whole
            // outcome is a panel explaining that it cannot work is worse than
            // no gesture at all. Either half is enough — a phone with a voice
            // and no on-device model still has a use for this panel, which is
            // then a player with an explanation where the summary would be.
            enableDismissFromEndToStart = summariesSupported() || speechSupported(),
            backgroundContent = {
                if (dismiss.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    SummariseBackground(dismiss.progress, swipeDefault)
                } else {
                    RemoveBackground(dismiss.progress)
                }
            },
        ) {
            ListRowBody(
                host = link.host,
                title = link.title,
                onClick = onOpen,
                // The box slides the row over its own background, so the row
                // needs one of its own — without it the remove background
                // shows through the gaps between the words.
                modifier = Modifier.background(scheme.background),
                tone = if (link.read) RowTone.Faded else RowTone.Normal,
                trailing = {
                    // Only a read row carries the tick — matching the unread
                    // row above it exactly, sourcechip and two facts, nothing
                    // more. Still tappable, so marking something read is
                    // reversible without having to reopen it.
                    if (link.read) {
                        Icon(
                            imageVector = DuskReadIcons.Check,
                            contentDescription = "Mark unread",
                            modifier = Modifier
                                .size(26.dp)
                                .clickable(onClick = onToggleRead)
                                .padding(6.dp),
                            tint = scheme.onSurfaceVariant,
                        )
                    } else if (link.fetchFailed) {
                        // A retry that reaches in and refetches this one link,
                        // rather than making a couldn't-load row wait for a
                        // pull-to-refresh over the whole list to try again.
                        Icon(
                            imageVector = DuskReadIcons.Offline,
                            contentDescription = "Couldn't load — retry",
                            modifier = Modifier
                                .size(26.dp)
                                .clickable(onClick = onRetry)
                                .padding(6.dp),
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                },
            ) {
                RowMeta(
                    text = when {
                        !link.fetched -> "reading the page…"
                        link.fetchFailed -> "couldn’t load this page"
                        link.readAt != null && link.readAt > 0L -> "${link.host} · ${savedAgo(link.readAt)}"
                        else -> "${link.host} · ${savedAgo(link.savedAt)}"
                    },
                )

                // The subject, when something knew it — Notion filed it, or the
                // feed it came from carries one. A fact on the line, the same
                // as it is on Home; assigning one is Notion's job.
                link.topic?.let { RowMeta(it) }
            }
        }

        ListRowDivider(last)
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
        // Start-aligned: this is the rightward pull now, so the background is
        // uncovered from the left edge and a label at the far right would stay
        // hidden under the row for most of the gesture.
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            DuskReadIcons.Close,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (progress > 0.4f) "Release to remove" else "Remove",
            style = MaterialTheme.typography.labelLarge,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
