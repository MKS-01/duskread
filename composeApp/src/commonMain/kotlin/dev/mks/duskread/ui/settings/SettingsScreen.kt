package dev.mks.duskread.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.AppVersion
import dev.mks.duskread.data.DataEpoch
import dev.mks.duskread.data.NotionTokenKey
import dev.mks.duskread.data.UserPrefs
import dev.mks.duskread.data.rememberKeyValueStore
import dev.mks.duskread.data.rememberSecretStore
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LinkInbox
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.ReadingSignals
import dev.mks.duskread.links.savedAgo
import dev.mks.duskread.notion.NotionClient
import dev.mks.duskread.notion.NotionPrefs
import dev.mks.duskread.notion.NotionResult
import dev.mks.duskread.notion.PastedTokenAuth
import dev.mks.duskread.notion.runFullSync
import dev.mks.duskread.speech.SpeakerState
import dev.mks.duskread.speech.VoiceChoice
import dev.mks.duskread.speech.rememberSpeaker
import dev.mks.duskread.speech.speechSupported
import dev.mks.duskread.summary.SummariserState
import dev.mks.duskread.summary.SwipeDefault
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Everything that isn't a tab of its own, gathered behind a gear rather than scattered
 * across whichever screen happens to own a given piece of state.
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
    notion: NotionPrefs,
    auth: PastedTokenAuth,
    api: NotionClient,
    modifier: Modifier = Modifier,
) {
    val secrets = rememberSecretStore()
    var setupOpen by remember { mutableStateOf(false) }

    // Bumped when the setup sheet changes the connection, so `NotionSettings` re-reads
    // the keystore instead of showing what was true when it was first composed.
    var connectionEpoch by remember { mutableStateOf(0) }

    // Mounted here rather than inside `NotionSettings`, and the reason is a crash rather
    // than tidiness: the section is rendered inside a `verticalScroll`.
    if (setupOpen) {
        NotionSetupSheet(
            prefs = notion,
            auth = auth,
            api = api,
            hasToken = secrets.get(NotionTokenKey) != null,
            onTokenSaved = { connectionEpoch++ },
            onConnected = { connectionEpoch++ },
            onClose = { setupOpen = false },
            modifier = modifier,
        )
        return
    }

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
                    // Before verticalScroll, not after: the inset has to shrink the
                    // *viewport* for the focused field to be scrolled into view.
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // Hidden, not disabled, off Android.
                if (summariesSupported()) {
                    EyebrowHeader(text = "SUMMARIES")
                    Spacer(Modifier.height(14.dp))
                    SummarySettings()

                    Spacer(Modifier.height(28.dp))
                }

                // Same rule as SUMMARIES above: hidden where the platform has no engine,
                // rather than shown as a choice between two voices that cannot speak.
                if (speechSupported()) {
                    EyebrowHeader(text = "VOICE")
                    Spacer(Modifier.height(14.dp))
                    VoiceSettings(prefs)

                    Spacer(Modifier.height(28.dp))
                }

                // Only when the swipe genuinely has two things to choose between.
                if (summariesSupported() && speechSupported()) {
                    EyebrowHeader(text = "SWIPE")
                    Spacer(Modifier.height(14.dp))
                    SwipeSettings(prefs)

                    Spacer(Modifier.height(28.dp))
                }

                EyebrowHeader(text = "NOTION")
                Spacer(Modifier.height(14.dp))
                NotionSettings(
                    library = library,
                    feeds = feeds,
                    feedPosts = feedPosts,
                    client = feedClient,
                    notion = notion,
                    auth = auth,
                    api = api,
                    epoch = connectionEpoch,
                    onOpenSetup = { setupOpen = true },
                )

                Spacer(Modifier.height(28.dp))

                // Below the reading settings and the connection, because both are things
                // a reader came here to change and these two are things they set once.
                EyebrowHeader(text = "APPEARANCE")
                Spacer(Modifier.height(14.dp))
                ThemeRow(mono = mono, onToggleTheme = onToggleTheme)

                Spacer(Modifier.height(28.dp))

                EyebrowHeader(text = "PROFILE")
                Spacer(Modifier.height(14.dp))
                NameField(prefs)

                Spacer(Modifier.height(28.dp))

                EyebrowHeader(text = "RESET", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                ResetSettings(
                    library = library,
                    feeds = feeds,
                    feedPosts = feedPosts,
                    signals = signals,
                    prefs = prefs,
                    notion = notion,
                    auth = auth,
                    onErased = {
                        connectionEpoch++
                        onClose()
                    },
                )

                // Last, unheaded, and mono like every other fact in the app.
                Spacer(Modifier.height(36.dp))
                VersionLine(prefs)
            }
        }
    }
}

/**
 * The same name onboarding asks for, editable afterwards — it only feeds the dashboard
 * greeting.
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
 * The same toggle the tab bar's contrast button reaches.
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
 * Whether the model is there, and a way to throw away what it has written. There is no
 * length control any more.
 */
@Composable
private fun SummarySettings() {
    val summariser = rememberSummariser()
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

        // Only when there is something to clear: an action that does nothing is worse
        // than no action, and the count is the only reason to show it.
        if (cache.summaries.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            // Offset back by the action's own inset so its text starts on the section's
            // left edge rather than 12dp inside it.
            Box(Modifier.offset(x = (-12).dp)) {
                TransferAction("Clear ${cache.summaries.size} saved summar${if (cache.summaries.size == 1) "y" else "ies"}") {
                    cache.clear()
                    ToastRequest.show("Summaries cleared")
                }
            }
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
 * The Notion connection, reduced to a state line and three actions.
 */
@Composable
private fun NotionSettings(
    library: LinkLibrary,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    client: HttpClient,
    notion: NotionPrefs,
    auth: PastedTokenAuth,
    api: NotionClient,
    /** Changes whenever the setup sheet touched the connection; re-reads the keystore. */
    epoch: Int,
    onOpenSetup: () -> Unit,
) {
    val secrets = rememberSecretStore()
    val scope = rememberCoroutineScope()

    // Read into state rather than on every recomposition: reaching the keystore is cheap
    // but not free, and the answer only changes here or in the setup sheet.
    var connected by remember(epoch) { mutableStateOf(secrets.get(NotionTokenKey) != null) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    // Same self-clearing note the export/import block uses — a result worth reading once,
    // not a status that lives on the screen forever.
    LaunchedEffect(note) {
        if (note != null) {
            delay(5_000)
            note = null
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Optional. Connect Notion and the blogs you follow and the links you " +
                "save are each kept in step with a database there — both built for you — " +
                "so you can read and sort them on a laptop too.",
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(14.dp))

        // The state line, shaped like every other two-line settings row. The title is
        // what is true now; the detail is when it was last true.
        Text(
            text = note ?: when {
                !connected -> "Not connected"
                notion.sourcesDatabaseId == null || notion.readingDatabaseId == null ->
                    "Token saved — finish setting up"

                else -> {
                    val followed = feeds.feeds.size
                    val saved = library.links.size
                    "Connected · $followed feed${if (followed == 1) "" else "s"} · $saved saved"
                }
            },
            style = MaterialTheme.typography.titleSmall,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = notion.lastSyncAt?.let { "Last synced ${savedAgo(it)}" }
                ?: "DuskRead creates the databases itself — it only needs a token.",
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
            TransferAction(if (connected) "Set up again" else "Set up", onClick = onOpenSetup)

            // Only once there is something to sync. Before that the button could only
            // ever report the setup that has not happened.
            AnimatedVisibility(connected) {
                TransferAction(if (busy) "Syncing…" else "Sync now") {
                    if (busy) return@TransferAction
                    busy = true
                    scope.launch {
                        note = runFullSync(
                            api = api,
                            prefs = notion,
                            library = library,
                            feeds = feeds,
                            feedPosts = feedPosts,
                            http = client,
                            recordSync = notion::recordSync,
                        ).line
                        busy = false
                    }
                }
            }

            // Never in the accent: the one coloured thing on a screen should not be the
            // destructive one.
            AnimatedVisibility(connected) {
                TransferAction("Disconnect") {
                    auth.disconnect()
                    notion.clear()
                    connected = false
                    // Followed feeds stay. They are DuskRead's own data now, and signing
                    // out of a source should not empty the app.
                    note = "Disconnected — feeds kept"
                }
            }
        }
    }
}

/**
 * Erase everything, behind a confirmation that happens in place. **Not a dialog.** This
 * screen has no boxed card anywhere in it and the app has no dialog pattern at all.
 */
@Composable
private fun ResetSettings(
    library: LinkLibrary,
    feeds: FeedLibrary,
    feedPosts: FeedPostCache,
    signals: ReadingSignals,
    prefs: UserPrefs,
    notion: NotionPrefs,
    auth: PastedTokenAuth,
    onErased: () -> Unit,
) {
    val summaries = rememberSummaryCache()
    val store = rememberKeyValueStore()
    var confirming by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Saved links, followed blogs, cached posts, your name and the Notion " +
                "connection — all removed from this phone, and the app starts over. Your " +
                "Notion pages are left exactly as they are.",
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(14.dp))

        if (confirming) {
            Text(
                text = "Erase everything? This cannot be undone.",
                style = MaterialTheme.typography.titleSmall,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
        }

        Row(
            Modifier.offset(x = (-12).dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (confirming) {
                TransferAction("Erase") {
                    // Both of these before a single thing is cleared, and in this order —
                    // see the note on ordering above.
                    DataEpoch.bump()
                    // The token and the ids it resolved, together — one without the other
                    // is a connection that cannot be used or repaired.
                    auth.disconnect()
                    notion.clear()

                    library.clear()
                    feeds.clear()
                    feedPosts.clear()
                    signals.clear()
                    summaries.clear()
                    // Anything the widget captured since the last resume, too: draining
                    // is the only way to empty it.
                    LinkInbox.drain(store)

                    onErased()
                    prefs.reset()
                }
                TransferAction("Cancel") { confirming = false }
            } else {
                TransferAction("Erase everything") { confirming = true }
            }
        }
    }
}

/**
 * Which voice reads an article aloud.
 */
@Composable
private fun VoiceSettings(prefs: UserPrefs) {
    val speaker = rememberSpeaker(prefs.voice)
    val state = speaker.state

    val choices = VoiceChoice.entries.filter {
        it != VoiceChoice.ReadbackLibrary || prefs.readbackEnabled
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Articles are read aloud on this phone. Nothing is sent anywhere to be spoken.",
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        // A row that wraps rather than a plain `Row`: "Readback library" is long enough,
        // next to "System voice".
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.ChipGap),
            verticalArrangement = Arrangement.spacedBy(Space.ChipGap),
        ) {
            choices.forEach { choice ->
                SummaryChip(
                    label = choice.label,
                    tone = if (prefs.voice == choice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { prefs.updateVoice(choice) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        Text(
            text = prefs.voice.detail,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // The readback library's readiness is a folder grant, which the Readback tab
        // already asks about in its own words.
        if (prefs.voice != VoiceChoice.ReadbackLibrary) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = when (state) {
                    is SpeakerState.Ready -> "Ready"
                    is SpeakerState.NeedsVoice -> state.detail
                    is SpeakerState.Unavailable -> state.reason
                },
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = if (state is SpeakerState.Ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Whether a left swipe opens speaking, or opens the summary and waits.
 */
@Composable
private fun SwipeSettings(prefs: UserPrefs) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Swiping a saved link or a post opens a panel that always both " +
                "summarises and reads aloud. This picks which one it starts doing.",
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(Space.ChipGap)) {
            SwipeDefault.entries.forEach { choice ->
                SummaryChip(
                    label = choice.label,
                    tone = if (prefs.swipeDefault == choice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { prefs.updateSwipeDefault(choice) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        Text(
            text = when (prefs.swipeDefault) {
                SwipeDefault.Summary -> "Opens showing the summary. Press play to also hear it."
                SwipeDefault.ReadAloud -> "Starts reading the moment it opens. The summary is still there to read."
            },
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The version, and the way in to the Readback tab. Three taps inside [UnlockWindowMs]
 * switches `readbackEnabled`.
 */
@OptIn(ExperimentalTime::class)
@Composable
private fun VersionLine(prefs: UserPrefs) {
    var taps by remember { mutableStateOf(0) }
    var firstTapAt by remember { mutableStateOf(0L) }

    Text(
        text = "DuskRead $AppVersion",
        fontFamily = Mono,
        fontSize = 10.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clickable(
                // No ripple and no pointer affordance: the whole point is that the line
                // does not advertise itself.
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                val now = Clock.System.now().toEpochMilliseconds()
                // A late tap restarts the run rather than failing it, so the gesture is
                // never in a state where it has to be waited out.
                taps = if (now - firstTapAt > UnlockWindowMs) 1 else taps + 1
                if (taps == 1) firstTapAt = now

                if (taps >= UnlockTaps) {
                    taps = 0
                    ToastRequest.show(if (prefs.toggleReadback()) "Readback on" else "Readback off")
                }
            }
            .padding(vertical = 6.dp),
    )
}

private const val UnlockTaps = 3

/** Two seconds — long enough for three deliberate taps, short enough that idle prodding never adds up. */
private const val UnlockWindowMs = 2_000L
