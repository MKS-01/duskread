package dev.mks.duskread.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.data.UserPrefs
import dev.mks.duskread.links.ExportFileName
import dev.mks.duskread.links.ExportSink
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.exportLinks
import dev.mks.duskread.links.rememberExportSink
import dev.mks.duskread.summary.SummariserState
import dev.mks.duskread.summary.SummaryLength
import dev.mks.duskread.summary.rememberSummariser
import dev.mks.duskread.summary.rememberSummaryCache
import dev.mks.duskread.summary.summariesSupported
import dev.mks.duskread.ui.PlatformBackHandler
import dev.mks.duskread.ui.common.AppTextField
import dev.mks.duskread.ui.common.EyebrowHeader
import dev.mks.duskread.ui.common.ToastRequest
import dev.mks.duskread.ui.summary.SummaryActionChip
import dev.mks.duskread.ui.summary.SummaryChip
import dev.mks.duskread.ui.theme.AccentColor
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.Space
import dev.mks.duskread.ui.theme.Stroke
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Everything that isn't a tab of its own, gathered behind a gear rather than
 * scattered across whichever screen happens to own a given piece of state:
 * the profile name onboarding asked for once, and the saved-links backup —
 * the only thing in DuskRead with no copy anywhere else, since the readback
 * library is readback's own backup and a followed feed is trivially re-added
 * by URL. If more settles here later, this is where it goes, not a second
 * button bar bolted onto some other tab.
 *
 * Flat, same as every other screen in the Amplitude direction: an eyebrow
 * with its inline rule opens each section, and nothing here sits in a boxed
 * card — this used to be the one screen still built that way.
 */
@Composable
fun SettingsScreen(
    library: LinkLibrary,
    prefs: UserPrefs,
    mono: Boolean,
    onToggleTheme: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        imageVector = DuskReadIcons.Close,
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

                EyebrowHeader(text = "APPEARANCE")
                Spacer(Modifier.height(14.dp))
                ColourModeRow(
                    enabled = prefs.colourMode,
                    onToggle = { prefs.updateColourMode(!prefs.colourMode) },
                )
                // Both of these only mean anything once colour is on offer,
                // and both vanish rather than grey out — a greyed accent
                // swatch reads as "unavailable here", which is wrong; it is
                // available, it simply has not been asked for.
                if (prefs.colourMode) {
                    Spacer(Modifier.height(18.dp))
                    ThemeRow(mono = mono, onToggleTheme = onToggleTheme)
                    Spacer(Modifier.height(18.dp))
                    AccentPicker(accent = prefs.accent, mono = mono, onAccentChange = prefs::updateAccent)
                }

                Spacer(Modifier.height(28.dp))

                // Hidden, not disabled, off Android. The length chips choose
                // between two shapes of a summary that this platform cannot
                // produce at all, and `SummarySettings` binds the engine as
                // its first act — on a target where that engine is a stub,
                // the section is a control to learn to ignore and a needless
                // allocation behind it.
                if (summariesSupported()) {
                    EyebrowHeader(text = "SUMMARIES")
                    Spacer(Modifier.height(14.dp))
                    SummarySettings(prefs)

                    Spacer(Modifier.height(28.dp))
                }

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
 * The one control that decides whether the other two exist.
 *
 * Off by default: DuskRead is a monochrome app that can be lit, not a
 * colour app with the lights off, and a reader who never wants the accent
 * should never have to see the switch for it in the bar. Turning it off
 * again also drops back to Ink — see [UserPrefs.updateColourMode], which is
 * where that has to live so the two cannot disagree.
 *
 * A bordered state chip rather than a Material `Switch`: there is no switch
 * anywhere else in this app, and adding one here would be the only piece of
 * stock Material chrome on a screen built out of hairlines and small caps.
 */
@Composable
private fun ColourModeRow(enabled: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Colour mode",
                style = MaterialTheme.typography.titleSmall,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (enabled) {
                    "Paper Black and the accent are on offer, and the bar carries the switch."
                } else {
                    "Ink only. Nothing to choose, and one fewer glyph in the bar."
                },
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        SummaryChip(
            label = if (enabled) "Shown" else "Hidden",
            tone = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onToggle,
        )
    }
}

/**
 * The same toggle the tab bar's contrast button reaches, surfaced here too
 * so the current scheme is somewhere a reader would think to check it rather
 * than only discoverable by noticing the bar icon changed state. The detail
 * line doubles as the current-state readout the row itself is titled after.
 */
@Composable
private fun ThemeRow(mono: Boolean, onToggleTheme: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleTheme)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = if (mono) "Ink" else "Paper Black",
            style = MaterialTheme.typography.titleSmall,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = if (mono) {
                "Colour drained out — tap to bring the accent back."
            } else {
                "One accent, chosen below — tap to drop colour entirely."
            },
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The accent "Paper Black" lights up with, as swatches rather than a word
 * list — the whole point of picking one is seeing it, not reading its name.
 * Shown regardless of [mono] up above: it's a choice for the *next* time
 * colour is on, so picking one while Ink is active still has to work — but
 * while Ink is active the swatches themselves drop their hue too
 * ([greyed]), the same "nothing but lightness left to tell things apart"
 * rule [MonoScheme] applies everywhere else. Showing three saturated
 * squares in a screen that has otherwise gone entirely grey would read as a
 * bug, not a preview.
 *
 * Squared off with [Radius.Chip], same as the sort chips and the sourcechip
 * cell — a circular swatch would be the one rounded shape in a screen built
 * entirely from hairline squares. The border alone carries selection: a
 * hairline in [outline][androidx.compose.material3.ColorScheme.outline] at
 * rest, doubled and switched to onSurface when chosen — no ring, no check.
 */
@Composable
private fun AccentPicker(accent: AccentColor, mono: Boolean, onAccentChange: (AccentColor) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Accent",
            style = MaterialTheme.typography.titleSmall,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.ChipGap)) {
            AccentColor.entries.forEach { option ->
                AccentSwatch(
                    color = if (mono) greyed(option.primary) else option.primary,
                    selected = option == accent,
                    contentDescription = option.label,
                    onClick = { onAccentChange(option) },
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(color: Color, selected: Boolean, contentDescription: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.Chip)
    val borderColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(shape)
            .background(color)
            .border(if (selected) Stroke.Hairline * 2 else Stroke.Hairline, borderColor, shape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
    ) {}
}

/** Drains the hue, keeps the value — the same trade [MonoScheme] makes for the whole app. */
private fun greyed(color: Color): Color {
    val value = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return Color(value, value, value, color.alpha)
}

/**
 * How long a summary should be, whether the model is there, and a way to
 * throw away what it has written.
 *
 * Length is asked of the engine rather than trimmed out of its answer, so
 * the two settings are genuinely different summaries — which is why an
 * article already summarised at one is regenerated when read at the other.
 *
 * This is the one screen that binds the engine deliberately: everywhere else
 * the summariser is built only when a summary is actually asked for.
 */
@Composable
private fun SummarySettings(prefs: UserPrefs) {
    val summariser = rememberSummariser(prefs.summaryLength)
    val cache = rememberSummaryCache()
    val scope = rememberCoroutineScope()
    val state = summariser.state

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Summaries are generated on this phone. Nothing about an article is sent anywhere.",
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(Space.ChipGap)) {
            SummaryLength.entries.forEach { length ->
                SummaryChip(
                    label = length.label,
                    tone = if (prefs.summaryLength == length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { prefs.updateSummaryLength(length) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        Text(
            text = lengthNote(prefs.summaryLength),
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))

        Text(
            text = when (state) {
                is SummariserState.Checking -> "Checking…"
                is SummariserState.Ready -> "Ready · ${state.model}"
                is SummariserState.Downloadable -> "Not on this phone yet. It downloads once, then every summary runs offline."
                is SummariserState.Downloading -> state.fraction?.let { "Downloading · ${(it * 100).toInt()}%" } ?: "Downloading…"
                is SummariserState.Unavailable -> state.reason
            },
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = if (state is SummariserState.Ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state is SummariserState.Downloadable) {
            Spacer(Modifier.height(12.dp))
            SummaryActionChip("Download the model") { scope.launch { summariser.prepare() } }
        }

        // Only when there is something to clear: an action that does nothing
        // is worse than no action, and the count is the only reason to show it.
        if (cache.summaries.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            // Offset back by the action's own inset so its text starts on the
            // section's left edge rather than 12dp inside it.
            Box(Modifier.offset(x = (-12).dp)) {
                TransferAction("Clear ${cache.summaries.size} saved summar${if (cache.summaries.size == 1) "y" else "ies"}") {
                    cache.clear()
                    ToastRequest.show("Summaries cleared")
                }
            }
        }
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

/**
 * Described by what you get to read, not by the number of points the engine
 * is configured with — that number is an implementation detail of AICore and
 * means nothing to someone deciding whether to open an article.
 */
private fun lengthNote(length: SummaryLength): String = when (length) {
    SummaryLength.Short -> "A sentence or two — just enough to decide."
    SummaryLength.Full -> "A short paragraph. The most this phone's model will give."
}
