package dev.mks.duskread.ui.summary

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mks.duskread.data.rememberUserPrefs
import dev.mks.duskread.links.createHttpClient
import dev.mks.duskread.links.loadArticle
import dev.mks.duskread.links.rememberReadingSignals
import dev.mks.duskread.speech.SpeechSession
import dev.mks.duskread.speech.speechSupported
import dev.mks.duskread.summary.ArticleSummary
import dev.mks.duskread.summary.SummariserState
import dev.mks.duskread.summary.SummaryTarget
import dev.mks.duskread.summary.parseSummary
import dev.mks.duskread.summary.rememberSummariser
import dev.mks.duskread.summary.rememberSummaryCache
import dev.mks.duskread.ui.common.ToastRequest
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Mono
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.Stroke
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Where the panel is between a URL and something worth reading. */
private sealed interface Stage {
    data object Waiting : Stage

    /** On the device? Not yet — and it is a large download, so the reader decides. */
    data object NeedsDownload : Stage

    data class Downloading(val fraction: Float?) : Stage

    /** Fetching the page, for a summary asked for from a list rather than the reader. */
    data object Reading : Stage

    /** [text] is the answer so far, shown as it arrives. */
    data class Generating(val text: String) : Stage

    data class Done(val summary: ArticleSummary) : Stage

    data class Failed(val reason: String) : Stage
}

/**
 * The summary itself: one panel, floating, wherever it was asked for.
 *
 * Everything the summary side of the feature does lives here rather than in
 * its two hosts, which are both "put this panel on screen and hand it a
 * target"; duplicating a fetch-then-generate pipeline across them is how they
 * would drift apart.
 *
 * The listening side does not live here any more. It used to — a `Speaker`,
 * a flow collected inline, a meter drawn in the card — but that meant reading
 * aloud stopped the moment this panel left composition, which is not what
 * Readback playback does and there is no reason listening to an article
 * should behave differently from listening to one. `SpeechSession` is what
 * moved: this panel now only starts and stops it, and the actual transport —
 * play state, progress, the stop button — lives in the same floating bar
 * Readback already uses, which is also why it survives this panel closing.
 *
 * Order matters: a cached summary short-circuits the lot, then the caller's
 * own text if it had any (the reader always does), otherwise a fetch — then
 * generation, streamed, so the panel fills in rather than sits. Reading aloud
 * shares that same fetched text rather than asking for the page twice.
 *
 * A downloadable model is *not* downloaded automatically. Hundreds of
 * megabytes over whatever connection the phone is on is the reader's call.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun SummaryPanel(
    target: SummaryTarget,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    hostShowsBusy: Boolean = false,
    onBusyChange: (Boolean) -> Unit = {},
    /**
     * Speaks the instant the panel opens rather than waiting for the play
     * button. The caller's call, not a preference the panel reads for
     * itself — a swipe honours `UserPrefs.swipeDefault`, the reader's own
     * dedicated read-aloud button always passes true, and its existing
     * summary button always passes false; three different answers to the
     * same question the panel has no way to guess on its own.
     */
    autoPlay: Boolean = false,
) {
    val prefs = rememberUserPrefs()
    val summariser = rememberSummariser(prefs.summaryLength)
    val cache = rememberSummaryCache()
    val signals = rememberReadingSignals()
    val client = remember { createHttpClient() }
    val scope = rememberCoroutineScope()

    var stage by remember(target.url, prefs.summaryLength) { mutableStateOf<Stage>(Stage.Waiting) }
    val engineState = summariser.state

    // Held so pressing play does not refetch a page the summariser has already
    // been out and got. It is also why the two halves belong on one panel: they
    // want the same article, and asking for it twice was the cost of keeping
    // them apart.
    var articleText by remember(target.url) { mutableStateOf(target.text) }

    // The listening half now runs in `HomeScreen`, not here — see
    // `SpeechSession`. What used to be this panel's own `speaking` flag is
    // "is *my* article the one playing", which the session's key answers
    // without this panel having to own a `Speaker` of its own.
    val speechNowPlaying by SpeechSession.state.collectAsState()
    val isThisPlaying = speechNowPlaying?.let { it.key == target.url && it.playing } == true

    fun togglePlay() {
        if (isThisPlaying) {
            SpeechSession.stop()
            return
        }
        scope.launch {
            val text = articleText ?: loadArticle(client, target.url, target.title, target.feedContent)?.text
            // Both refusals are stated. A play button that does nothing at
            // all is indistinguishable from a broken one, and this is the
            // half of "nothing happened" the speaker itself never sees — it
            // is never asked in the first place.
            if (text == null) {
                ToastRequest.show("Couldn't reach this page to read it.")
                return@launch
            }
            if (text.length < MinSpeakableChars) {
                ToastRequest.show("There isn't enough text on this page to read aloud.")
                return@launch
            }
            articleText = text
            SpeechSession.start(target.url, target.title, text)
        }
    }

    // Two floating things at the bottom of the screen, one of them saying
    // only that it has nothing to say. Where the summary failed outright, the
    // read *is* the panel's whole content, and the transport already carries
    // it — with its own title, progress and stop — so the card gets out of
    // the way rather than stacking an explanation on top of a working player.
    //
    // Only on [Stage.Failed]. A finished summary is still worth reading while
    // it plays, which is why pressing play does not close this panel in
    // general, and a model waiting to be downloaded still has its own button
    // to offer.
    // Read off [engineState] as well as [stage], not just the stage: on a
    // swipe that starts speaking immediately, the read is playing before the
    // effect below has had a chance to turn "no engine" into a
    // [Stage.Failed], and a close that depends on the two landing in the
    // right order is a close that sometimes does not happen.
    val nothingToSay = engineState is SummariserState.Unavailable || stage is Stage.Failed
    LaunchedEffect(isThisPlaying, nothingToSay) {
        if (isThisPlaying && nothingToSay) onClose()
    }

    // The one thing autoPlay does — see the parameter's own KDoc for who
    // decides it and why. `isThisPlaying` is deliberately not in the guard:
    // this must fire once per fresh target regardless of what some earlier
    // article happened to leave `SpeechSession` doing.
    LaunchedEffect(target.url) { if (autoPlay) togglePlay() }

    LaunchedEffect(target.url, engineState, prefs.summaryLength) {
        cache.summaryFor(target.url, prefs.summaryLength)?.let {
            stage = Stage.Done(it)
            return@LaunchedEffect
        }

        when (engineState) {
            is SummariserState.Checking -> stage = Stage.Waiting
            is SummariserState.Downloadable -> stage = Stage.NeedsDownload
            is SummariserState.Downloading -> stage = Stage.Downloading(engineState.fraction)
            is SummariserState.Unavailable -> stage = Stage.Failed(engineState.reason)
            is SummariserState.Ready -> {
                // A state change that isn't about readiness must not start
                // a second run for the same article.
                if (stage is Stage.Generating || stage is Stage.Done) return@LaunchedEffect

                stage = if (target.text == null) Stage.Reading else Stage.Generating("")
                val text = articleText
                    ?: loadArticle(client, target.url, target.title, target.feedContent)?.text
                // Kept whatever happens next, so the play button never repeats
                // a fetch this one already paid for.
                text?.let { articleText = it }

                if (text == null || text.length < MinSummarisableChars) {
                    stage = Stage.Failed("There isn't enough text on this page to summarise.")
                    return@LaunchedEffect
                }

                stage = Stage.Generating("")
                val answer = runCatching {
                    var latest = ""
                    summariser.summarise(target.title, text).collect { chunk ->
                        latest = chunk
                        stage = Stage.Generating(chunk)
                    }
                    latest
                }.getOrElse { failure ->
                    stage = Stage.Failed(failure.message ?: "The model could not finish this one.")
                    return@LaunchedEffect
                }

                val summary = parseSummary(answer, target.url, target.title, engineState.model, Clock.System.now().toEpochMilliseconds(), prefs.summaryLength)
                if (summary == null) {
                    stage = Stage.Failed("The model answered with nothing usable. Try again?")
                    return@LaunchedEffect
                }

                cache.put(summary)
                // Asking what is in an article is interest, even if it is
                // never opened — worth more than nothing, less than a read.
                signals.recordOpen(target.url)
                stage = Stage.Done(summary)
            }
        }
    }

    val busy = stage is Stage.Generating || stage is Stage.Reading || stage is Stage.Downloading
    // Reported upward so a host with somewhere better to put it can — the
    // reader swaps its toolbar glyph for a spinner, which is the whole of
    // what moves on screen while the model runs.
    LaunchedEffect(busy) { onBusyChange(busy) }
    DisposableEffect(Unit) { onDispose { onBusyChange(false) } }

    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(Radius.Card))
            .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Card))
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
    ) {
        PanelHeader(
            note = when (val current = stage) {
                is Stage.Done -> current.summary.model
                is Stage.Downloading -> current.fraction?.let { "${(it * 100).toInt()}%" } ?: "downloading"
                else -> (engineState as? SummariserState.Ready)?.model.orEmpty()
            },
            busy = busy && !hostShowsBusy,
            // Gated on the platform alone, the same question the reader's own
            // read-aloud button asks — see `InAppBrowserScreen`. The finer
            // question, whether a voice is actually installed, is one
            // `SpeechSession` finds out when a read is actually attempted
            // rather than one this panel checks in advance for itself.
            canPlay = speechSupported(),
            playing = isThisPlaying,
            onTogglePlay = ::togglePlay,
            onClose = onClose,
        )
        Spacer(Modifier.height(8.dp))

        when (val current = stage) {
            is Stage.Waiting -> PanelNote("Looking for the model…")
            is Stage.Reading -> PanelNote("Reading the article…")
            is Stage.NeedsDownload -> DownloadPrompt { scope.launch { summariser.prepare() } }
            is Stage.Downloading -> PanelNote("Downloading the model. This happens once.")
            is Stage.Generating -> PanelNote(current.text.ifBlank { "Summarising, on this phone…" })
            is Stage.Failed -> PanelNote(current.reason)
            is Stage.Done -> SummaryBody(current.summary)
        }
    }
}

/**
 * The design system's card puts the spinner in the toolbar slot the summary
 * icon vacates, so nothing on the page moves but that one glyph. The reader
 * does exactly that and passes `hostShowsBusy`; the overlay a swiped row
 * opens has no toolbar to put it in, so there it stays here beside the
 * model's name.
 */
@Composable
private fun PanelHeader(
    note: String,
    busy: Boolean,
    canPlay: Boolean,
    playing: Boolean,
    onTogglePlay: () -> Unit,
    onClose: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "SUMMARY",
            fontFamily = Mono,
            fontSize = 10.sp,
            letterSpacing = 1.6.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.weight(1f))
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(11.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 1.5.dp,
            )
            Spacer(Modifier.width(8.dp))
        }
        // Which model wrote this: a summary is only as good as its source.
        if (note.isNotBlank()) {
            Text(
                text = note,
                fontFamily = Mono,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
        }
        // Before the close, and bordered, because it is the one thing on this
        // header anyone is meant to press. Close is furniture.
        if (canPlay) {
            Icon(
                imageVector = if (playing) DuskReadIcons.Pause else DuskReadIcons.Play,
                contentDescription = if (playing) "Stop reading aloud" else "Read this aloud",
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(Radius.Chip))
                    .border(
                        Stroke.Hairline,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(Radius.Chip),
                    )
                    .clickable(onClick = onTogglePlay)
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            imageVector = DuskReadIcons.Close,
            contentDescription = "Close the summary",
            modifier = Modifier.size(26.dp).clickable(onClick = onClose).padding(6.5.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The summary, as one paragraph in one tone.
 *
 * No lead sentence set apart from a body, and no list. A summary of an
 * article is writing about writing: a bulleted one asks the reader to
 * reassemble the argument from fragments, which is most of the work they
 * opened the panel to avoid, and a two-tone split makes the panel change
 * shape depending on how the model happened to punctuate.
 */
@Composable
private fun SummaryBody(summary: ArticleSummary) {
    Text(
        text = summary.text,
        fontSize = 12.5.sp,
        lineHeight = 19.5.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun PanelNote(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Stated plainly, with the cost, because it is the reader's data being spent. */
@Composable
private fun DownloadPrompt(onDownload: () -> Unit) {
    Column {
        PanelNote("The model isn't on this phone yet. It downloads once, then every summary runs offline.")
        Spacer(Modifier.height(12.dp))
        SummaryActionChip("Download the model", onDownload)
    }
}

// Below this a page is a stub, a paywall or a cookie wall — and asking a model
// to summarise two sentences produces a confident summary of nothing.
private const val MinSummarisableChars = 400

/**
 * Below this, a page is chrome rather than an article.
 *
 * Lower than [MinSummarisableChars]: a paragraph is not worth summarising but
 * is perfectly worth hearing, and refusing to read a short post would refuse
 * the case listening is quickest for.
 */
private const val MinSpeakableChars = 200
