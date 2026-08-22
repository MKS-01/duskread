package dev.mks.blogmark.ui.links

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.blogmark.links.ExportFileName
import dev.mks.blogmark.links.ExportSink
import dev.mks.blogmark.links.LinkLibrary
import dev.mks.blogmark.links.SavedLink
import dev.mks.blogmark.links.createHttpClient
import dev.mks.blogmark.links.exportLinks
import dev.mks.blogmark.links.fetchLinkMetadata
import dev.mks.blogmark.links.looksLikeUrl
import dev.mks.blogmark.links.rememberExportSink
import dev.mks.blogmark.links.savedAgo
import dev.mks.blogmark.links.topicIcon
import dev.mks.blogmark.ui.rememberUrlOpener
import dev.mks.blogmark.ui.theme.BlogmarkIcons
import dev.mks.blogmark.ui.theme.Mono
import dev.mks.blogmark.ui.theme.Radius
import dev.mks.blogmark.ui.theme.SectionLabel
import dev.mks.blogmark.ui.theme.Space
import kotlinx.coroutines.delay

/**
 * Saved links: the blogs and articles worth reading, one URL at a time.
 *
 * Readback is a synced library of audio someone else prepared; a link here is
 * whatever the reader found themselves, still as text. This is where those
 * go, and it is the only screen in the app whose contents the reader writes.
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

    LazyColumn(
        modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Space.CardGap),
    ) {
        item("head") {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "Saved",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = when {
                        library.links.isEmpty() -> "Articles you keep for later."
                        library.unreadCount == 0 -> "${library.links.size} saved · all read."
                        else -> "${library.unreadCount} to read · ${library.links.size} saved."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item("add") { AddLinkField(onSave = { library.save(it) != null }) }

        item("transfer") { TransferControls(library) }

        if (library.links.isEmpty()) {
            item("empty") { EmptyNote() }
        }

        val (read, unread) = library.links.partition { it.read }

        unread.forEach { link ->
            item(link.id) {
                LinkCard(
                    link = link,
                    onOpen = {
                        open(link.url)
                        library.toggleRead(link.id)
                    },
                    onToggleRead = { library.toggleRead(link.id) },
                    onRemove = { library.remove(link.id) },
                )
            }
        }

        // Read links stay, under their own heading. They are the record: what
        // was read and when, which is the question a reading list gets asked
        // long after the reading is done. Sorted by when they were read rather
        // than saved, so the section reads as a history.
        if (read.isNotEmpty()) {
            item("read-head") {
                Text(
                    text = "READ · ${read.size}",
                    style = SectionLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 2.dp),
                )
            }

            read.sortedByDescending { it.readAt ?: it.savedAt }.forEach { link ->
                item(link.id) {
                    LinkCard(
                        link = link,
                        onOpen = { open(link.url) },
                        onToggleRead = { library.toggleRead(link.id) },
                        onRemove = { library.remove(link.id) },
                    )
                }
            }
        }
    }
}

/**
 * The paste field. It offers the clipboard rather than reading it silently —
 * a screen that quietly knows what you copied elsewhere is unsettling, and one
 * tap is a small price for the reader staying in charge of that.
 */
@Composable
private fun AddLinkField(onSave: (String) -> Boolean) {
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }
    var rejected by remember { mutableStateOf(false) }

    fun submit() {
        rejected = !onSave(text)
        if (!rejected) text = ""
    }

    val clipped = clipboard.getText()?.text?.trim().orEmpty()
    val offer = clipped.takeIf { it.isNotEmpty() && looksLikeUrl(it) && it != text }

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text(
                        text = "Paste a link",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.5.sp,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        rejected = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.5.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
            }

            AnimatedVisibility(text.isNotBlank()) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = ::submit)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        if (rejected) {
            Text(
                text = "That doesn’t look like a link.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
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
 * Backup, in both directions.
 *
 * The saved links are the only thing in the app with no copy anywhere else, so
 * a way out of the device matters more here than anywhere else in Blogmark. Both
 * halves go through the clipboard as Markdown: it needs no file picker on five
 * platforms, and what comes out is readable text the reader can keep in
 * whatever they already keep things in.
 *
 * The pair sits under the paste field rather than in a menu — a backup nobody
 * can find is not a backup — but as quiet text buttons, because it is a thing
 * done twice a year and should not compete with the list itself.
 */
@Composable
private fun TransferControls(library: LinkLibrary) {
    val clipboard = LocalClipboardManager.current
    val sink = rememberExportSink()
    var exporting by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var pasted by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }

    // The confirmation clears itself. It is the receipt for an action already
    // taken, and a receipt that needs dismissing is worse than none.
    LaunchedEffect(note) {
        if (note != null) {
            delay(5_000)
            note = null
        }
    }

    fun runImport() {
        val summary = library.import(pasted)
        note = when {
            summary.found == 0 -> "No links in that text."
            summary.added == 0 -> "All ${summary.found} were already saved."
            summary.duplicates > 0 -> "Added ${summary.added} · ${summary.duplicates} already saved."
            else -> "Added ${summary.added} link${if (summary.added == 1) "" else "s"}."
        }
        if (summary.added > 0) {
            pasted = ""
            importing = false
        }
    }

    // One destination and no menu is better than a menu of one, so a platform
    // that can only reach the clipboard exports straight to it.
    val hasChoice = sink.canSaveFile || sink.canSend

    fun copyExport() {
        clipboard.setText(AnnotatedString(exportLinks(library.links)))
        note = "Copied ${library.links.size} links to the clipboard."
        exporting = false
    }

    Column(Modifier.padding(top = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (library.links.isNotEmpty()) {
                TransferAction(if (exporting) "Export ✕" else "Export…") {
                    if (hasChoice) exporting = !exporting else copyExport()
                }
            }
            TransferAction(if (importing) "Cancel" else "Import…") {
                importing = !importing
                if (!importing) pasted = ""
            }
        }

        AnimatedVisibility(exporting) {
            ExportDestinations(
                sink = sink,
                onCopy = ::copyExport,
                onSaveFile = {
                    sink.saveFile(ExportFileName, exportLinks(library.links))
                    exporting = false
                },
                onSend = {
                    sink.send(ExportFileName, exportLinks(library.links))
                    exporting = false
                },
            )
        }

        AnimatedVisibility(importing) {
            ImportPanel(
                text = pasted,
                onTextChange = { pasted = it },
                onPasteClipboard = { clipboard.getText()?.text?.let { pasted = it } },
                onImport = ::runImport,
            )
        }

        note?.let {
            Text(
                text = it,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp),
            )
        }
    }
}

/**
 * Where the export goes.
 *
 * Each row says what the destination *is* rather than naming an app: "Save to
 * Drive" would be a lie on a phone without Drive installed, while the picker
 * behind "Save as file" lists Drive, Files and everything else the reader
 * actually has. The second line is there because the difference between these
 * two is not obvious from four words.
 */
@Composable
private fun ExportDestinations(
    sink: ExportSink,
    onCopy: () -> Unit,
    onSaveFile: () -> Unit,
    onSend: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(Radius.Card))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 4.dp),
    ) {
        DestinationRow(
            title = "Copy to clipboard",
            detail = "Paste it anywhere — notes, mail, a document.",
            onClick = onCopy,
        )
        if (sink.canSaveFile) {
            DestinationRow(
                title = "Save as file",
                detail = "Choose where: Drive, Files, or anywhere else on the device.",
                onClick = onSaveFile,
            )
        }
        if (sink.canSend) {
            DestinationRow(
                title = "Send…",
                detail = "Hand it to another app through the share sheet.",
                onClick = onSend,
            )
        }
    }
}

@Composable
private fun DestinationRow(title: String, detail: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = detail,
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransferAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = SectionLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

/**
 * The paste box.
 *
 * Deliberately a big empty field rather than a one-shot "import from
 * clipboard" button: what lands here is usually pasted by hand from a notes
 * app, and seeing it before committing is the whole difference between an
 * import and a surprise. The clipboard is offered, never read on its own —
 * the same bargain the save field makes.
 */
@Composable
private fun ImportPanel(
    text: String,
    onTextChange: (String) -> Unit,
    onPasteClipboard: () -> Unit,
    onImport: () -> Unit,
) {
    Column(Modifier.padding(top = 8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.Card))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Card))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (text.isEmpty()) {
                Text(
                    text = "Paste an export, a bookmarks list, or any text with links in it. " +
                        "Anything already saved is skipped.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                // The field, not the box around it, carries the height: a tap
                // anywhere in that empty rectangle has to land on the field,
                // and a one-line-tall field inside a tall box means most of
                // the box does nothing when tapped.
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 84.dp),
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Mono,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransferAction("From clipboard", onPasteClipboard)
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Add links",
                style = MaterialTheme.typography.labelLarge,
                color = if (text.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(enabled = text.isNotBlank(), onClick = onImport)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun EmptyNote() {
    Text(
        text = "Share an article to Blogmark from any app, or paste its address above. " +
            "The title fills itself in once the page has been read.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp),
    )
}

/**
 * One saved link.
 *
 * Removal is a swipe, not a button. No tap target on this card destroys
 * anything: losing an article you meant to read because a thumb landed 6dp off
 * the tick is a far worse outcome than the swipe costing a deliberate gesture.
 * A swipe also carries its own undo — let go halfway and nothing happens —
 * which no icon can offer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkCard(
    link: SavedLink,
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
            // card that no longer exists.
            false
        },
    )

    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        backgroundContent = { RemoveBackground(dismiss.progress) },
        modifier = Modifier.clip(RoundedCornerShape(Radius.Card)),
    ) {
        LinkCardFace(link, scheme, onOpen, onToggleRead)
    }
}

/** What the swipe reveals: the intent stated in words, so nothing is destroyed by a mystery gesture. */
@Composable
private fun RemoveBackground(progress: Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(Radius.Card))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 20.dp),
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

@Composable
private fun LinkCardFace(
    link: SavedLink,
    scheme: androidx.compose.material3.ColorScheme,
    onOpen: () -> Unit,
    onToggleRead: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Card))
            // Read cards sit on a quieter surface rather than losing their
            // title to a strikethrough — the whole card reads as "done" at a
            // glance, not just the one line.
            .background(if (link.read) scheme.surfaceContainer else scheme.surface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(Radius.Card))
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
    ) {
        Box(
            Modifier
                .padding(top = 1.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(if (link.read) scheme.surfaceContainerHigh else scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = topicIcon(link.host),
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (link.read) scheme.onSurfaceVariant else scheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = link.title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (link.read) scheme.onSurfaceVariant else scheme.onSurface,
            )

            link.description?.takeIf { !link.read }?.let { description ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        !link.fetched -> "${link.host} · reading the page…"
                        link.readAt != null && link.readAt > 0L -> "${link.host} · read ${savedAgo(link.readAt)}"
                        link.read -> "${link.host} · read"
                        else -> "${link.host} · saved ${savedAgo(link.savedAt)}"
                    },
                    fontFamily = Mono,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                CardAction(
                    icon = BlogmarkIcons.Check,
                    label = if (link.read) "Mark unread" else "Mark read",
                    tint = if (link.read) scheme.primary else scheme.onSurfaceVariant,
                    onClick = onToggleRead,
                )
            }
        }
    }
}

@Composable
private fun CardAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, Modifier.size(15.dp), tint = tint)
    }
}
