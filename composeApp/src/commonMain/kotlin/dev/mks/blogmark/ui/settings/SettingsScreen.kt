package dev.mks.blogmark.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.blogmark.data.UserPrefs
import dev.mks.blogmark.links.ExportFileName
import dev.mks.blogmark.links.ExportSink
import dev.mks.blogmark.links.LinkLibrary
import dev.mks.blogmark.links.exportLinks
import dev.mks.blogmark.links.rememberExportSink
import dev.mks.blogmark.ui.PlatformBackHandler
import dev.mks.blogmark.ui.common.AppTextField
import dev.mks.blogmark.ui.common.EyebrowHeader
import dev.mks.blogmark.ui.theme.BlogmarkIcons
import kotlinx.coroutines.delay

/**
 * Everything that isn't a tab of its own, gathered behind a gear rather than
 * scattered across whichever screen happens to own a given piece of state:
 * the profile name onboarding asked for once, and the saved-links backup —
 * the only thing in Blogmark with no copy anywhere else, since the readback
 * library is readback's own backup and a followed feed is trivially re-added
 * by URL. If more settles here later, this is where it goes, not a second
 * button bar bolted onto some other tab.
 *
 * Flat, same as every other screen in the Amplitude direction: an eyebrow
 * with its inline rule opens each section, and nothing here sits in a boxed
 * card — this used to be the one screen still built that way.
 */
@Composable
fun SettingsScreen(library: LinkLibrary, prefs: UserPrefs, onClose: () -> Unit, modifier: Modifier = Modifier) {
    PlatformBackHandler(enabled = true, onBack = onClose)

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = BlogmarkIcons.Close,
                        contentDescription = "Close settings",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                EyebrowHeader(text = "PROFILE")
                Spacer(Modifier.height(14.dp))
                NameField(prefs)

                Spacer(Modifier.height(28.dp))

                EyebrowHeader(text = "SAVED LINKS")
                Spacer(Modifier.height(14.dp))
                DataTransfer(library)
            }
        }
    }
}

/**
 * The same name onboarding asks for, editable afterwards — it only feeds the
 * dashboard greeting, so there is nowhere else in the app a reader would
 * think to look for a way to change it once they've skipped past the intro.
 */
@Composable
private fun NameField(prefs: UserPrefs) {
    var name by remember(prefs.name) { mutableStateOf(prefs.name.orEmpty()) }
    val dirty = name.trim() != prefs.name.orEmpty()

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "What the dashboard greeting calls you.",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Your name",
            fontSize = 14.5.sp,
            trailing = {
                AnimatedVisibility(dirty) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { prefs.updateName(name) }
                            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    )
                }
            },
        )
    }
}

/**
 * Backup, in both directions.
 *
 * Both halves go through the clipboard as Markdown: it needs no file picker
 * on five platforms, and what comes out is readable text the reader can keep
 * in whatever they already keep things in.
 */
@Composable
private fun DataTransfer(library: LinkLibrary) {
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

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "${library.links.size} link${if (library.links.size == 1) "" else "s"} saved.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

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
                modifier = Modifier.padding(top = 8.dp),
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
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
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
            .padding(vertical = 8.dp),
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
        style = MaterialTheme.typography.labelMedium,
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
 * the same bargain the save field on the Saved tab makes.
 */
@Composable
private fun ImportPanel(
    text: String,
    onTextChange: (String) -> Unit,
    onPasteClipboard: () -> Unit,
    onImport: () -> Unit,
) {
    Column(Modifier.padding(top = 10.dp)) {
        AppTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = "Paste an export, a bookmarks list, or any text with links in it. " +
                "Anything already saved is skipped.",
            singleLine = false,
            minHeight = 84.dp,
            mono = true,
            fontSize = 11.5.sp,
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
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
