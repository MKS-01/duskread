package dev.mks.duskread.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.AppVersion
import dev.mks.duskread.data.NotionTokenKey
import dev.mks.duskread.data.UserPrefs
import dev.mks.duskread.data.rememberSecretStore
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.ReadingSignals
import dev.mks.duskread.links.pool
import dev.mks.duskread.links.rank
import dev.mks.duskread.links.savedAgo
import dev.mks.duskread.links.syncFeeds
import dev.mks.duskread.notion.NotionClient
import dev.mks.duskread.notion.NotionResult
import dev.mks.duskread.notion.PastedTokenAuth
import dev.mks.duskread.notion.applySources
import dev.mks.duskread.notion.pullSources
import dev.mks.duskread.notion.rememberNotionPrefs
import dev.mks.duskread.notion.syncReadingList
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
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Space
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Everything that isn't a tab of its own, gathered behind a gear rather than
 * scattered across whichever screen happens to own a given piece of state:
 * the profile name onboarding asked for once, the Notion connection that
 * supplies the followed blogs, and a way to paste a list of links in. If more
 * settles here later, this is where it goes, not a second button bar bolted
 * onto some other tab.
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
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    feedClient: HttpClient,
    signals: ReadingSignals,
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
                    // Before verticalScroll, not after: the inset has to
                    // shrink the *viewport* for the focused field to be
                    // scrolled into view. Applied inside the scroll it would
                    // only pad the content and the keyboard would still cover
                    // the field it was opened for.
                    //
                    // union rather than navigationBarsPadding().imePadding():
                    // the two overlap — an open keyboard already covers the
                    // navigation bar — so applying both in turn pads twice and
                    // leaves a gap the height of the bar under the keyboard.
                    // union takes the larger, which is what is actually in the
                    // way.
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                EyebrowHeader(text = "PROFILE")
                Spacer(Modifier.height(14.dp))
                NameField(prefs)

                Spacer(Modifier.height(28.dp))

                EyebrowHeader(text = "APPEARANCE")
                Spacer(Modifier.height(14.dp))
                ThemeRow(mono = mono, onToggleTheme = onToggleTheme)

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

                EyebrowHeader(text = "NOTION")
                Spacer(Modifier.height(14.dp))
                NotionSettings(library = library, feeds = feeds, feedPosts = feedPosts, client = feedClient)

                Spacer(Modifier.height(28.dp))

                EyebrowHeader(text = "DISCOVERY")
                Spacer(Modifier.height(14.dp))
                Discovery(library = library, feeds = feeds, feedPosts = feedPosts, signals = signals)

                Spacer(Modifier.height(28.dp))

                EyebrowHeader(text = "IMPORT LINKS")
                Spacer(Modifier.height(14.dp))
                LinkImport(library)

                // Last, unheaded, and mono like every other fact in the app.
                // A version number is not a setting — it earns a line because
                // it is the first thing anyone is asked for when something is
                // wrong, and nowhere else in the app reports it.
                Spacer(Modifier.height(36.dp))
                Text(
                    text = "DuskRead $AppVersion",
                    fontFamily = Mono,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
private fun LinkImport(library: LinkLibrary) {
    val clipboard = LocalClipboardManager.current
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

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "${library.links.size} link${if (library.links.size == 1) "" else "s"} saved.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        TransferAction(if (importing) "Cancel" else "Import…") {
            importing = !importing
            if (!importing) pasted = ""
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

/**
 * The Notion connection: a token, a database, and the two buttons that use
 * them.
 *
 * Notion is where subscriptions are curated — a newsletter arrives by email,
 * gets filed into a `Sources` table, and this pulls the result down. The
 * direction only ever runs that way: nothing here writes to Notion, so no
 * mistake in the app can damage the table it reads.
 *
 * Both fields are paste-once. The token is shown masked after it is saved and
 * never revealed again, because the field is the way in, not a display — and
 * Notion itself only shows a personal access token at the moment it is
 * created.
 */
@OptIn(ExperimentalTime::class)
@Composable
private fun NotionSettings(library: LinkLibrary, feeds: FeedLibrary, feedPosts: FeedPostCache, client: HttpClient) {
    val secrets = rememberSecretStore()
    val notion = rememberNotionPrefs()
    val auth = remember(secrets) { PastedTokenAuth(secrets) }
    val api = remember(client, auth) { NotionClient(client, auth) }
    val scope = rememberCoroutineScope()

    // Read once into state rather than on every recomposition: reaching the
    // keystore is cheap but not free, and the answer only changes here.
    var connected by remember { mutableStateOf(secrets.get(NotionTokenKey) != null) }
    var token by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    // Same self-clearing note the export/import block uses — a result worth
    // reading once, not a status that lives on the screen forever.
    LaunchedEffect(note) {
        if (note != null) {
            delay(5_000)
            note = null
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Follow the blogs listed in a Notion database, and keep saved links in step with it.",
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        AppTextField(
            value = if (connected && token.isEmpty()) MaskedToken else token,
            onValueChange = { token = it },
            placeholder = "Personal access token",
            fontSize = 13.5.sp,
            mono = true,
            trailing = {
                AnimatedVisibility(token.isNotBlank()) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                auth.save(token)
                                connected = true
                                token = ""
                                note = "Token saved"
                            }
                            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    )
                }
            },
        )

        Spacer(Modifier.height(8.dp))

        DatabaseField(
            stored = notion.sourcesDatabaseId,
            placeholder = "Sources database ID",
            onSave = {
                notion.updateDatabaseId(it)
                note = "Sources database saved"
            },
        )

        Spacer(Modifier.height(8.dp))

        DatabaseField(
            stored = notion.readingDatabaseId,
            placeholder = "Reading List database ID (optional)",
            onSave = {
                notion.updateReadingDatabaseId(it)
                note = "Reading list saved"
            },
        )

        Spacer(Modifier.height(14.dp))

        // The state line, shaped like every other two-line settings row. The
        // title is what is true now; the detail is when it was last true.
        Text(
            text = note ?: when {
                !connected -> "Not connected"
                notion.databaseName != null -> "${notion.databaseName} · ${feeds.feeds.size} feeds"
                else -> "Token saved — test the connection"
            },
            style = MaterialTheme.typography.titleSmall,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = notion.lastSyncAt?.let { "Last synced ${savedAgo(it)}" }
                ?: "Paste a token from Notion's developer portal, then the database ID.",
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.offset(x = (-12).dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransferAction(if (busy) "Working…" else "Test connection") {
                if (busy) return@TransferAction
                busy = true
                scope.launch {
                    note = when (val result = api.databaseTitle(notion.sourcesDatabaseId.orEmpty())) {
                        is NotionResult.Ok -> {
                            notion.recordConnection(result.value)
                            "Connected to ${result.value}"
                        }

                        is NotionResult.Failure -> result.message
                    }
                    busy = false
                }
            }

            TransferAction(if (busy) "…" else "Sync now") {
                if (busy) return@TransferAction
                busy = true
                scope.launch {
                    note = runNotionSync(
                        api,
                        notion.sourcesDatabaseId.orEmpty(),
                        notion.readingDatabaseId,
                        library,
                        feeds,
                        feedPosts,
                        client,
                        notion::recordSync,
                    )
                    busy = false
                }
            }

            // Only once there is something to disconnect from, and never in
            // the accent: the one coloured thing on a screen should not be
            // the destructive one.
            AnimatedVisibility(connected) {
                TransferAction("Disconnect") {
                    auth.disconnect()
                    notion.clear()
                    connected = false
                    token = ""
                    // Followed feeds stay. They are DuskRead's own data now,
                    // and signing out of a source should not empty the app.
                    note = "Disconnected — feeds kept"
                }
            }
        }
    }
}

/**
 * Pull the sources, follow them, then run the feed sync that already exists.
 *
 * Written as a plain function rather than inline in the button so the whole
 * chain reads in one place: what fails, where it stops, and what the reader
 * is told. Any failure returns before touching [feeds] — a bad response
 * should leave the followed list exactly as it was.
 */
@OptIn(ExperimentalTime::class)
private suspend fun runNotionSync(
    api: NotionClient,
    databaseId: String,
    readingDatabaseId: String?,
    library: LinkLibrary,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    client: HttpClient,
    recordSync: (Long) -> Unit,
): String = when (val sources = pullSources(api, databaseId)) {
    is NotionResult.Failure -> sources.message

    is NotionResult.Ok -> {
        val summary = applySources(client, sources.value, feeds)
        // The second half is the part that already shipped: Home's own "Sync
        // now" and its pull-to-refresh both end here too.
        val fetched = syncFeeds(client, feeds.feeds, feedPosts)

        // The reading list is optional and reported separately. A failure
        // there is worth saying out loud rather than folding into the feed
        // count, but it must not discard the sources sync that already
        // succeeded — those feeds are followed whatever Notion says next.
        val reading = readingDatabaseId?.let { syncReadingList(api, it, library) }
        recordSync(Clock.System.now().toEpochMilliseconds())

        val head = "${summary.line} · $fetched fetched"
        when (reading) {
            null -> head
            is NotionResult.Failure -> "$head · saved links: ${reading.message}"
            is NotionResult.Ok -> reading.value.line?.let { "$head · $it" } ?: head
        }
    }
}

/** Enough to show a token is held without showing the token. */
private const val MaskedToken = "ntn_••••••••••••••••"

/**
 * Why the ranking picked what it picked.
 *
 * Home shows three rows chosen from a pool of a couple of hundred, and the
 * weights behind that choice are wrong until they are seen to be wrong. They
 * cannot be judged from the source — only from real candidates on a real
 * phone — and "why is *that* at the top" has to be answerable in the room,
 * without a debugger. So the score is broken out per term rather than shown as
 * one number, for the same reason `Summariser.android.kt` refuses to flatten
 * an error code into "something went wrong".
 *
 * A developer tool living in a shipped Settings screen, deliberately: it costs
 * one section, it is the only way to tune the thing, and a reader who opens it
 * sees a list of what the app is about to suggest, which is not a bad answer
 * to a question nobody asked.
 */
@OptIn(ExperimentalTime::class)
@Composable
private fun Discovery(
    library: LinkLibrary,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    signals: ReadingSignals,
) {
    // Re-rank is a button rather than something that happens on its own: the
    // point of this block is to compare two rankings, which is impossible if
    // it moves while being read.
    var reranks by remember { mutableStateOf(0) }

    val ranked = remember(reranks, library.links, feedPosts.postsByFeed, feeds.feeds, signals.byHost, signals.skippedPosts) {
        val candidates = pool(library, feedPosts, feeds.feeds)
        candidates to rank(
            candidates = candidates,
            signals = signals,
            now = Clock.System.now().toEpochMilliseconds(),
            seed = reranks,
            focusMinutes = null,
        )
    }

    val (candidates, scored) = ranked
    val tagged = candidates.count { it.tag != null }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "${candidates.size} candidates · $tagged tagged · ${signals.totalReads} reads · " +
                "${signals.skippedPosts.size} skipped",
            fontFamily = Mono,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        scored.take(5).forEach { item ->
            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(
                    text = item.candidate.title,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    // Only the terms that actually contributed. A row of
                    // seven values where four are 0.00 hides the three that
                    // decided it.
                    text = item.terms.entries
                        .filter { abs(it.value) >= 0.005f }
                        .sortedByDescending { abs(it.value) }
                        .joinToString("  ") { (name, value) -> "$name ${value.format()}" }
                        .ifBlank { "no signal" },
                    fontFamily = Mono,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (scored.isEmpty()) {
            Text(
                text = "Nothing to rank — follow a blog or save a link.",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(Modifier.offset(x = (-12).dp), verticalAlignment = Alignment.CenterVertically) {
            TransferAction("Re-rank") { reranks++ }
            TransferAction("Clear signals") {
                signals.clear()
                reranks++
            }
        }
    }
}

/** Two decimals, with the sign, because a negative term is the interesting one. */
private fun Float.format(): String {
    val hundredths = (abs(this) * 100).roundToInt()
    val sign = if (this < 0) "-" else ""
    return "$sign${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
}

/**
 * A Notion database ID: paste once, then masked.
 *
 * Not a credential — an ID grants nothing without the token — but it names a
 * private workspace, and this app's own repository is public. A settings
 * screen ends up in screenshots and screen-shares, and there is no reason for
 * a workspace identifier to travel in either.
 *
 * The last four characters survive, unlike the token's full mask. Two of these
 * fields sit one above the other and the only question anyone asks of a saved
 * one is "is that the right database" — four characters answer it, and answer
 * nothing else.
 */
@Composable
private fun DatabaseField(stored: String?, placeholder: String, onSave: (String) -> Unit) {
    // Keyed on `stored` so saving clears the field back to the mask rather
    // than leaving what was typed sitting in plain sight.
    var typed by remember(stored) { mutableStateOf("") }

    AppTextField(
        value = if (typed.isEmpty() && !stored.isNullOrBlank()) maskId(stored) else typed,
        onValueChange = { typed = it },
        placeholder = placeholder,
        fontSize = 13.5.sp,
        mono = true,
        trailing = {
            AnimatedVisibility(typed.isNotBlank()) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            onSave(typed)
                            typed = ""
                        }
                        .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                )
            }
        },
    )
}

/** A fixed run of dots and the last four characters — never the real length, which is itself a hint. */
private fun maskId(id: String): String = "•".repeat(12) + id.takeLast(4)
