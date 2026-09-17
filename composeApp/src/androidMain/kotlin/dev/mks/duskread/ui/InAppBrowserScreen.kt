package dev.mks.duskread.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import dev.mks.duskread.links.Article
import dev.mks.duskread.links.ReaderPalette
import dev.mks.duskread.links.articleDocument
import dev.mks.duskread.links.createHttpClient
import dev.mks.duskread.links.loadArticle
import dev.mks.duskread.links.postFor
import dev.mks.duskread.links.rememberFeedPostCache
import dev.mks.duskread.links.rememberLinkLibrary
import dev.mks.duskread.links.rememberReadingSignals
import dev.mks.duskread.speech.speechSupported
import dev.mks.duskread.summary.SummaryTarget
import dev.mks.duskread.summary.summariesSupported
import dev.mks.duskread.ui.common.EmptyState
import dev.mks.duskread.ui.common.ToastHost
import dev.mks.duskread.ui.common.ToastRequest
import dev.mks.duskread.ui.summary.SummaryPanel
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Layout
import dev.mks.duskread.ui.theme.Motion
import dev.mks.duskread.ui.theme.Radius
import dev.mks.duskread.ui.theme.SectionLabel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Which of the two things this screen can show is showing. */
private enum class BrowserMode { Reader, Original }

/** Which button opened the summary-and-listen panel, and so whether it should already be talking. */
private enum class PanelIntent { Summary, ReadAloud }

/**
 * A reference article, opened without leaving the app — as the article, not as the page
 * it arrived in.
 */
@Composable
fun InAppBrowserScreen(queue: ReadingQueue, mono: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val client = remember { createHttpClient() }
    val feedPosts = rememberFeedPostCache().postsByFeed

    // Where in the queue the reader has got to. Every piece of per-article state below
    // keys off the URL it resolves to, so turning a page resets all of it at once.
    var at by remember(queue) { mutableStateOf(queue.index) }
    val entry = queue.entryAt(at) ?: queue.current
    val url = entry.url

    // Opening an article is also the moment it counts as read — and the same moment for
    // the fifth page turned to as for the row that was tapped.
    val links = rememberLinkLibrary()
    val signals = rememberReadingSignals()
    LaunchedEffect(url) { recordOpened(entry, queue.record, links, signals) }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var title by remember(url) { mutableStateOf(entry.host ?: hostOf(url)) }
    var currentUrl by remember(url) { mutableStateOf(url) }
    var progress by remember(url) { mutableStateOf(0f) }

    // Set only when the *main frame* fails. A page whose analytics script cannot load has
    // not failed; a page that cannot load has.
    var loadFailed by remember(url) { mutableStateOf(false) }

    var article by remember(url) { mutableStateOf<Article?>(null) }
    var extracting by remember(url) { mutableStateOf(true) }
    var mode by remember(url) { mutableStateOf(BrowserMode.Reader) }
    // Closed by default, and per article: following a link out of one piece into another
    // should not carry the first one's panel with it.
    var panelIntent by remember(url) { mutableStateOf<PanelIntent?>(null) }
    var summaryBusy by remember(url) { mutableStateOf(false) }
    // What the WebView currently holds. Without it, every recomposition that touches mode
    // or article would reload the page underneath the reader.
    var loaded by remember(url) { mutableStateOf("") }

    // How many links have been followed out of *this* article. The WebView's own history
    // spans every article turned through, so asking it would walk back through them
    // invisibly while the toolbar went on naming the one this screen thinks it shows.
    var depth by remember(url) { mutableIntStateOf(0) }

    PlatformBackHandler(enabled = true) {
        if (depth > 0) {
            depth--
            webView?.goBack()
        } else {
            onClose()
        }
    }

    // A post opened from a followed feed often needs no request at all: the feed itself
    // carried the publisher's own markup for it, already clean.
    val cached = feedPosts.postFor(url)
    LaunchedEffect(url) {
        article = loadArticle(client, url, cached?.title, cached?.content)
        if (article == null) mode = BrowserMode.Original
        article?.let { title = it.title }
        extracting = false
    }

    // Read once, outside the WebView factory: that lambda runs a single time on first
    // composition, so a value it captures is frozen at whatever the theme was then.
    val ground = MaterialTheme.colorScheme.background.toArgb()
    val palette = MaterialTheme.colorScheme.readerPalette(mono)

    LaunchedEffect(webView, mode, article, extracting) {
        val view = webView ?: return@LaunchedEffect
        // Nothing loads until extraction has answered. Showing the live page in the
        // meantime would mean fetching it twice and watching it get replaced.
        if (extracting) return@LaunchedEffect

        val readable = article.takeIf { mode == BrowserMode.Reader }
        val key = readable?.let { "reader:${it.url}" } ?: "live:$currentUrl"
        if (key == loaded) return@LaunchedEffect
        loaded = key
        loadFailed = false

        if (readable != null) {
            progress = 1f
            // Base URL is the article's own: it makes the body's relative links resolve
            // and keeps the document same-origin with the images it loads.
            view.loadDataWithBaseURL(readable.url, articleDocument(readable, palette), "text/html", "utf-8", readable.url)
        } else {
            view.loadUrl(currentUrl)
        }
    }

    // The page turn, and only in the reader: on the live page a horizontal drag is the
    // page's own, and the WebView is welcome to it.
    val canTurn = queue.entries.size > 1 && mode == BrowserMode.Reader && panelIntent == null

    // Read through a ref, and deliberately not a `pointerInput` key: keying the gesture
    // on the position restarts the handler mid-drag, and the rest of one flick then
    // turns two more pages.
    val position = rememberUpdatedState(at)

    // A turn in flight owns the gesture until it lands. Without this a second flick
    // arriving mid-push starts another one from a page that is already leaving.
    var turning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val shift = remember { Animatable(0f) }
    var stageWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val edgeSlack = with(density) { EdgeSlack.toPx() }
    // Far enough to be a push at the end rather than a twitch on the way to a tap.
    val edgeVoice = with(density) { EdgeVoice.toPx() }
    val flingAway = with(density) { FlingAway.toPx() }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            BrowserToolbar(
                title = title,
                readerAvailable = article != null,
                readerActive = mode == BrowserMode.Reader && article != null,
                onToggleReader = { mode = if (mode == BrowserMode.Reader) BrowserMode.Original else BrowserMode.Reader },
                // Hidden until there is an article, for the same reason the reader toggle
                // is: the summary is made from the extracted text.
                summaryAvailable = article != null && summariesSupported(),
                summaryActive = panelIntent == PanelIntent.Summary,
                summaryBusy = summaryBusy,
                onToggleSummary = {
                    panelIntent = if (panelIntent == PanelIntent.Summary) null else PanelIntent.Summary
                },
                // Same gate, on speech rather than the summariser: without extracted text
                // there is nothing to read aloud either.
                readAloudAvailable = article != null && speechSupported(),
                readAloudActive = panelIntent == PanelIntent.ReadAloud,
                onToggleReadAloud = {
                    panelIntent = if (panelIntent == PanelIntent.ReadAloud) null else PanelIntent.ReadAloud
                },
                onClose = onClose,
                onOpenExternally = { context.openExternally(currentUrl) },
            )
            if (extracting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            } else if (progress < 1f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            }
            Box(Modifier.fillMaxSize()) {
                // The article on the other side, revealed by the drag rather than drawn
                // over it — the page behind this one.
                ArticlePeek(queue = queue, at = at, shift = shift.value, width = stageWidth, modifier = Modifier.matchParentSize())

                Box(
                    Modifier
                        .fillMaxSize()
                        .onSizeChanged { stageWidth = it.width }
                        .offset { IntOffset(shift.value.roundToInt(), 0) }
                        .background(MaterialTheme.colorScheme.background)
                        .pointerInput(canTurn, queue) {
                            if (!canTurn) return@pointerInput
                            awaitEachGesture {
                                // Watched on the Initial pass because the WebView consumes
                                // every move it is handed and never gives one back.
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                if (turning) return@awaitEachGesture
                                val velocity = VelocityTracker()
                                var claimed = false
                                var travelX = 0f
                                var travelY = 0f

                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break

                                    travelX += change.positionChange().x
                                    travelY += change.positionChange().y
                                    velocity.addPointerInputChange(change)

                                    if (!claimed) {
                                        // Vertical first means the page is being scrolled,
                                        // and the WebView keeps the gesture for good.
                                        if (abs(travelY) > viewConfiguration.touchSlop && abs(travelY) >= abs(travelX)) break
                                        claimed = abs(travelX) > viewConfiguration.touchSlop &&
                                            abs(travelX) > abs(travelY) * HorizontalBias
                                    }
                                    if (claimed) {
                                        change.consume()
                                        val neighbour = queue.entryAt(position.value + travelX.pageStep()) != null
                                        scope.launch { shift.snapTo(resisted(travelX, neighbour, edgeSlack)) }
                                    }
                                }

                                if (!claimed) return@awaitEachGesture

                                // Pushed against an end rather than towards an article.
                                // The strip is 64dp of rubber band and cannot hold a
                                // sentence, so the sentence goes where the app puts every
                                // other one.
                                val step = shift.value.pageStep()
                                if (abs(shift.value) > edgeVoice && queue.entryAt(position.value + step) == null) {
                                    ToastRequest.show(queue.endMessage(step))
                                }

                                val flung = velocity.calculateVelocity().x
                                scope.launch {
                                    turning = true
                                    settle(shift, queue, position.value, stageWidth, flung, flingAway) { at = it }
                                    turning = false
                                }
                            }
                        },
                ) {
                    // The article could not be built from cache and could not be fetched.
                    if (loadFailed && article == null) {
                        Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                            EmptyState(
                                title = "Not saved for offline",
                                message = "This blog's feed carries only a summary, so the article itself " +
                                    "was never cached. Open it again when you have signal.",
                            )
                        }
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                darken(settings)
                                // A light flash while the WebView inflates and before the
                                // page paints would undo the whole point of forcing dark.
                                setBackgroundColor(ground)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                // Otherwise the WebView focuses the first focusable element
                                // when a document loads and scrolls it into view.
                                settings.setNeedInitialFocus(false)
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                                        progress = newProgress / 100f
                                    }

                                    override fun onReceivedTitle(view: WebView, pageTitle: String?) {
                                        title = pageTitle?.takeIf { it.isNotBlank() } ?: hostOf(currentUrl)
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    // Without this the WebView renders Chrome's own "Webpage
                                    // not available" inside a reading app.
                                    override fun onReceivedError(
                                        view: WebView,
                                        request: WebResourceRequest,
                                        error: WebResourceError,
                                    ) {
                                        if (request.isForMainFrame) loadFailed = true
                                    }

                                    // Anything that isn't itself a page — a mailto: , an
                                    // intent: link, an app deep link.
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        val target = request.url.toString()
                                        if (!target.startsWith("http")) {
                                            ctx.openExternally(target)
                                            return true
                                        }

                                        // A link followed out of the reader leaves the
                                        // extracted article behind.
                                        loaded = "live:$target"
                                        mode = BrowserMode.Original
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                        progress = 0f
                                        currentUrl = url ?: currentUrl
                                        title = hostOf(currentUrl)
                                    }

                                    override fun onPageFinished(view: WebView, url: String?) {
                                        progress = 1f
                                    }
                                }
                            }.also { webView = it }
                        },
                    )

                    // The WebView holds nothing yet — extraction is still an HTTP fetch away.
                    if (extracting) ArticleSkeleton(Modifier.fillMaxSize())
                }

                // Over the article rather than beside it: the summary is a second look at
                // what is already on screen.
                if (panelIntent != null) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { panelIntent = null },
                            ),
                    )
                }

                // Its own, because the one in HomeScreen is underneath this screen and a
                // message fired from here would never be seen.
                ToastHost(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                )

                SummaryOverArticle(
                    article = article,
                    visible = panelIntent != null,
                    autoPlay = panelIntent == PanelIntent.ReadAloud,
                    onClose = { panelIntent = null },
                    onBusyChange = { summaryBusy = it },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/**
 * The article a drag is reaching for, or the fact that there isn't one.
 */
@Composable
private fun ArticlePeek(queue: ReadingQueue, at: Int, shift: Float, width: Int, modifier: Modifier = Modifier) {
    if (shift == 0f) return

    val forward = shift < 0f
    val neighbour = queue.entryAt(at + shift.pageStep())
    // Full strength at the point where releasing would commit, so what it says is true by
    // the time it is legible.
    val reveal = if (width > 0) (abs(shift) / (width * TurnFraction)).coerceIn(0f, 1f) else 0f

    // The column is exactly the strip the drag has opened, so the words set inside what
    // can be seen rather than starting behind the article and emerging tail-first.
    val band = with(LocalDensity.current) { abs(shift).toDp() }.coerceAtMost(PeekWidth)

    Box(
        modifier.background(MaterialTheme.colorScheme.background),
        contentAlignment = if (forward) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            Modifier
                .width(band)
                .padding(horizontal = PeekGutter)
                .alpha(reveal),
        ) {
            if (neighbour == null) {
                // A tick, not a barrier: there is nothing further because the whole list
                // has been got through, which is the good version of this news. The band
                // here is 64dp of rubber and would set a label one letter to the line, so
                // the words are a toast instead.
                Icon(
                    imageVector = DuskReadIcons.Check,
                    contentDescription = queue.endLabel(shift.pageStep()),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            Icon(
                imageVector = DuskReadIcons.Chevron,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    // The set has one chevron and it points right; the other direction is
                    // the same glyph, not a second one to draw.
                    .graphicsLayer { scaleX = if (forward) 1f else -1f },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${at + if (forward) 2 else 0} OF ${queue.entries.size}",
                style = SectionLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Only once there is room to set it. Three characters to a line is not a
            // title, it is an accident.
            if (band >= TitleFloor) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = neighbour.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Where a released drag ends up: the next article, or back where it started.
 */
private suspend fun settle(
    shift: Animatable<Float, AnimationVector1D>,
    queue: ReadingQueue,
    at: Int,
    width: Int,
    velocity: Float,
    flingAway: Float,
    onArrive: (Int) -> Unit,
) {
    val step = shift.value.pageStep()
    val far = width > 0 && abs(shift.value) > width * TurnFraction
    val thrown = abs(velocity) > flingAway && velocity.pageStep() == step

    if (queue.entryAt(at + step) == null || !(far || thrown)) {
        // A tween, not a spring: the app has four durations and no springs, and one here
        // for the sake of a nicer rubber band would be a change to the motion design.
        shift.animateTo(0f, tween(Motion.Chip))
        return
    }

    // Out the way it was going, then in from the other side — the same push the app uses
    // for any other navigation.
    shift.animateTo(-step * width.toFloat(), tween(Motion.PushIn))
    onArrive(at + step)
    shift.snapTo(step * width.toFloat())
    shift.animateTo(0f, tween(Motion.PushIn))
}

/** Which way a drag or a fling of this sign is heading: forward through the queue, or back. */
private fun Float.pageStep(): Int = if (this < 0f) 1 else -1

/** Past the end the drag still moves, but only enough to say there is nothing there. */
private fun resisted(travel: Float, hasNeighbour: Boolean, slack: Float): Float = if (hasNeighbour) travel else (travel * EdgeResistance).coerceIn(-slack, slack)

/** How far across the screen a drag has to get before releasing turns the page. */
private const val TurnFraction = 0.25f

/** A turn is nearly horizontal — this is how much more horizontal than vertical. */
private const val HorizontalBias = 1.5f

/** What is left of a drag with no article on that side. */
private const val EdgeResistance = 0.22f

/** And the furthest that resisted drag can travel. */
private val EdgeSlack = 64.dp

/** How far into that band counts as asking for an article that is not there. */
private val EdgeVoice = 24.dp

/** A flick this fast per second turns the page from wherever it got to. */
private val FlingAway = 520.dp

/** The peek column at its widest: enough for a title to set in, still an edge not a pane. */
private val PeekWidth = 210.dp

/** Its own gutter, narrower than a reading one — this is a margin, not a measure. */
private val PeekGutter = 14.dp

/** Below this the strip holds the chevron and the count, and the title waits. */
private val TitleFloor = 130.dp

/**
 * The summary panel, sliding up from the bottom edge.
 */
@Composable
private fun SummaryOverArticle(
    article: Article?,
    visible: Boolean,
    autoPlay: Boolean,
    onClose: () -> Unit,
    onBusyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && article != null,
        enter = fadeIn(tween(Motion.Chip)) + slideInVertically(tween(Motion.Chip)) { it / 3 },
        exit = fadeOut(tween(Motion.Fade)) + slideOutVertically(tween(Motion.Fade)) { it / 3 },
        modifier = modifier,
    ) {
        article?.let { found ->
            SummaryPanel(
                target = SummaryTarget(found.url, found.title, text = found.text),
                onClose = onClose,
                autoPlay = autoPlay,
                hostShowsBusy = true,
                onBusyChange = onBusyChange,
                modifier = Modifier
                    .navigationBarsPadding()
                    // 12dp either side and clear of the gesture handle, as the design
                    // system's card draws it — the panel is bottom-anchored.
                    .padding(horizontal = 12.dp)
                    .padding(top = 14.dp, bottom = 16.dp),
            )
        }
    }
}

/**
 * Stands in for [dev.mks.duskread.links.articleDocument]'s own shape — source line,
 * title, lead image, body copy.
 */
@Composable
private fun ArticleSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "extracting")

    // Linear and restarting, not eased and reversing: the easing lives in the triangle
    // wave below, and a reversing phase would run the highlight back up the page.
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SkeletonPulseMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "extractingPhase",
    )

    Column(
        modifier
            .padding(horizontal = Layout.ReadingGutter)
            .padding(top = 20.dp),
    ) {
        Text(
            // Uppercase and letter-spaced to match `.source`, whose slot this is standing
            // in.
            text = "FETCHING THE ARTICLE…",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.08.em,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // `.source`'s own margin-bottom, so the title starts where it will.
        Spacer(Modifier.height(22.dp))

        SkeletonBar(0.92f, 22.dp, phase, index = 0)
        Spacer(Modifier.height(10.dp))
        SkeletonBar(0.65f, 22.dp, phase, index = 1)

        Spacer(Modifier.height(24.dp))
        SkeletonBar(1f, 180.dp, phase, index = 2, shape = RoundedCornerShape(Radius.Inline))
        Spacer(Modifier.height(24.dp))

        // As many lines as there is room for, rather than a fixed eight that ran out half
        // way down and left the rest of the screen blank.
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val rows = (maxHeight / BodyLineSlot).toInt().coerceAtLeast(1)
            Column {
                repeat(rows) { line ->
                    SkeletonBar(BodyLineWidths[line % BodyLineWidths.size], BodyLineHeight, phase, index = 3 + line)
                    Spacer(Modifier.height(BodyLineGap))
                }
            }
        }
    }
}

/**
 * [Radius.Chip] by default, the app's softened corner — a text placeholder is standing in
 * for a line of prose, and a fully rounded pill would make it read as a control instead.
 */
@Composable
private fun SkeletonBar(
    widthFraction: Float,
    height: Dp,
    phase: Float,
    index: Int,
    shape: Shape = RoundedCornerShape(Radius.Chip),
) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .alpha(pulseAlpha(phase, index))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    )
}

/**
 * Where one row sits in the travelling pulse.
 */
private fun pulseAlpha(phase: Float, index: Int): Float {
    val shifted = (phase - index * SkeletonStagger).mod(1f)
    val triangle = if (shifted < 0.5f) shifted * 2f else (1f - shifted) * 2f
    return SkeletonDim + (SkeletonBright - SkeletonDim) * triangle
}

/** Ragged like set prose, with a short line where a paragraph ends. */
private val BodyLineWidths = listOf(0.97f, 0.9f, 0.98f, 0.4f, 0.95f, 0.88f, 0.93f, 0.6f)

private val BodyLineHeight = 12.dp
private val BodyLineGap = 14.dp

/** One line and the space under it — what a row of body copy costs vertically. */
private val BodyLineSlot = BodyLineHeight + BodyLineGap

/**
 * Slow for UI — the sub-300ms rule in `Motion` is for a control answering a touch, and
 * this is ambient.
 */
private const val SkeletonPulseMs = 1_400

/** How far behind the row above each row runs. Small: the page fills, it does not chase. */
private const val SkeletonStagger = 0.05f

private const val SkeletonDim = 0.30f
private const val SkeletonBright = 0.85f

@Composable
private fun BrowserToolbar(
    title: String,
    readerAvailable: Boolean,
    readerActive: Boolean,
    onToggleReader: () -> Unit,
    summaryAvailable: Boolean,
    summaryActive: Boolean,
    summaryBusy: Boolean,
    onToggleSummary: () -> Unit,
    readAloudAvailable: Boolean,
    readAloudActive: Boolean,
    onToggleReadAloud: () -> Unit,
    onClose: () -> Unit,
    onOpenExternally: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarButton(DuskReadIcons.Back, "Close", onClose)
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
        )
        // Hidden rather than disabled when there is no article: a control that can never
        // do anything on this page is one the reader has to learn to ignore.
        if (readerAvailable) {
            ToolbarButton(
                icon = DuskReadIcons.Reader,
                label = if (readerActive) "Show the original page" else "Show the reader view",
                onClick = onToggleReader,
                tint = if (readerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (summaryAvailable) {
            // The glyph becomes the spinner rather than sitting beside one: the article
            // stays readable while the model runs.
            if (summaryBusy) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 1.5.dp,
                    )
                }
            } else {
                ToolbarButton(
                    icon = DuskReadIcons.Summary,
                    label = if (summaryActive) "Hide the summary" else "Summarise this article",
                    onClick = onToggleSummary,
                    tint = if (summaryActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Its own button rather than something found by first opening the summary.
        if (readAloudAvailable) {
            ToolbarButton(
                icon = DuskReadIcons.Waveform,
                label = if (readAloudActive) "Stop reading aloud" else "Read this aloud",
                onClick = onToggleReadAloud,
                tint = if (readAloudActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ToolbarButton(DuskReadIcons.External, "Open in browser", onOpenExternally)
    }
}

@Composable
private fun ToolbarButton(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = tint,
        )
    }
}

/**
 * The current scheme as CSS, so a rendered article is the same page as the app around it
 * rather than a white rectangle wearing its toolbar.
 */
@Composable
private fun androidx.compose.material3.ColorScheme.readerPalette(mono: Boolean): ReaderPalette = ReaderPalette(
    background = background.css(),
    ink = onBackground.css(),
    muted = onSurfaceVariant.css(),
    accent = primary.css(),
    rule = outlineVariant.css(),
    panel = surfaceContainer.css(),
    mono = mono,
)

private fun Color.css(): String = "#" + (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0')

/**
 * Algorithmic darkening (the modern replacement for `FORCE_DARK_ON`) is what actually
 * repaints a page that never declared a dark theme of its own.
 */
private fun darken(settings: android.webkit.WebSettings) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
    } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        @Suppress("DEPRECATION")
        WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
    }
}

private fun hostOf(url: String): String = runCatching { Uri.parse(url).host }.getOrNull() ?: url

internal fun Context.openExternally(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
