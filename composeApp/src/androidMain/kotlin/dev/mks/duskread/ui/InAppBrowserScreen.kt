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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import dev.mks.duskread.speech.speechSupported
import dev.mks.duskread.summary.SummaryTarget
import dev.mks.duskread.summary.summariesSupported
import dev.mks.duskread.ui.common.EmptyState
import dev.mks.duskread.ui.summary.SummaryPanel
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Layout
import dev.mks.duskread.ui.theme.Motion
import dev.mks.duskread.ui.theme.Radius

/** Which of the two things this screen can show is showing. */
private enum class BrowserMode { Reader, Original }

/** Which button opened the summary-and-listen panel, and so whether it should already be talking. */
private enum class PanelIntent { Summary, ReadAloud }

/**
 * A reference article, opened without leaving the app — as the article, not
 * as the page it arrived in.
 *
 * The default is a reader view: the page is fetched, reduced to headline,
 * lead image and body by [dev.mks.duskread.links.extractArticle], and
 * rendered into a document this app styles. Injecting CSS into the live page
 * to hide its header and footer was the other option and is the worse one —
 * it only hides the elements you can name, every site names them
 * differently, and the cookie bar and newsletter interstitial are not among
 * them.
 *
 * The live page is always one tap away, and is what shows when extraction
 * finds nothing — a site that renders its body in JavaScript hands a plain
 * HTTP GET an empty shell, and no heuristic fixes that.
 *
 * Not Chrome Custom Tabs, for the original view either: those hand the page
 * to whatever browser is default, so its light-or-dark rendering follows the
 * *system* theme rather than this app's, and a Custom Tab's colour scheme
 * only skins the browser's own chrome. An embedded [WebView] with
 * [WebSettingsCompat]'s algorithmic darkening can repaint the page itself.
 * The cost is real: no shared cookies or logins with the reader's actual
 * browser, and a site that refuses framing here has nowhere else to go —
 * [DuskReadIcons.External] is always one tap away as the escape hatch.
 */
@Composable
fun InAppBrowserScreen(url: String, mono: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val client = remember { createHttpClient() }
    val feedPosts = rememberFeedPostCache().postsByFeed

    var webView by remember { mutableStateOf<WebView?>(null) }
    var title by remember { mutableStateOf(hostOf(url)) }
    var currentUrl by remember { mutableStateOf(url) }
    var progress by remember { mutableStateOf(0f) }

    // Set only when the *main frame* fails. A page whose analytics script
    // cannot load has not failed; a page that cannot load has.
    var loadFailed by remember { mutableStateOf(false) }

    var article by remember(url) { mutableStateOf<Article?>(null) }
    var extracting by remember(url) { mutableStateOf(true) }
    var mode by remember(url) { mutableStateOf(BrowserMode.Reader) }
    // Closed by default, and per article: following a link out of one piece
    // into another should not carry the first one's panel with it.
    //
    // A nullable intent rather than a plain boolean, because the panel now
    // has two doors onto the same card — the existing summary button and the
    // read-aloud button beside it — and the only thing that differs between
    // them is whether the panel starts speaking the instant it opens. Which
    // door was used is the one thing a boolean cannot carry.
    var panelIntent by remember(url) { mutableStateOf<PanelIntent?>(null) }
    var summaryBusy by remember(url) { mutableStateOf(false) }
    // What the WebView currently holds. Without it, every recomposition that
    // touches mode or article would reload the page underneath the reader.
    var loaded by remember(url) { mutableStateOf("") }

    PlatformBackHandler(enabled = true) {
        webView?.takeIf { it.canGoBack() }?.goBack() ?: onClose()
    }

    // A post opened from a followed feed often needs no request at all: the
    // feed itself carried the publisher's own markup for it, already clean.
    val cached = feedPosts.postFor(url)
    LaunchedEffect(url) {
        article = loadArticle(client, url, cached?.title, cached?.content)
        if (article == null) mode = BrowserMode.Original
        article?.let { title = it.title }
        extracting = false
    }

    // Read once, outside the WebView factory: that lambda runs a single time
    // on first composition, so a value it captures is frozen at whatever the
    // theme was then. Fine here — this screen closes and reopens across a
    // theme change, it never lives through one.
    val ground = MaterialTheme.colorScheme.background.toArgb()
    val palette = MaterialTheme.colorScheme.readerPalette(mono)

    LaunchedEffect(webView, mode, article, extracting) {
        val view = webView ?: return@LaunchedEffect
        // Nothing loads until extraction has answered. Showing the live page
        // in the meantime would mean fetching it twice and watching it get
        // replaced.
        if (extracting) return@LaunchedEffect

        val readable = article.takeIf { mode == BrowserMode.Reader }
        val key = readable?.let { "reader:${it.url}" } ?: "live:$currentUrl"
        if (key == loaded) return@LaunchedEffect
        loaded = key
        loadFailed = false

        if (readable != null) {
            progress = 1f
            // Base URL is the article's own: it makes the body's relative
            // links resolve and keeps the document same-origin with the
            // images it loads.
            view.loadDataWithBaseURL(readable.url, articleDocument(readable, palette), "text/html", "utf-8", readable.url)
        } else {
            view.loadUrl(currentUrl)
        }
    }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            BrowserToolbar(
                title = title,
                readerAvailable = article != null,
                readerActive = mode == BrowserMode.Reader && article != null,
                onToggleReader = { mode = if (mode == BrowserMode.Reader) BrowserMode.Original else BrowserMode.Reader },
                // Hidden until there is an article, for the same reason the
                // reader toggle is: the summary is made from the extracted
                // text, so on a page that yielded none there is nothing to
                // summarise and the control could only disappoint.
                summaryAvailable = article != null && summariesSupported(),
                summaryActive = panelIntent == PanelIntent.Summary,
                summaryBusy = summaryBusy,
                onToggleSummary = {
                    panelIntent = if (panelIntent == PanelIntent.Summary) null else PanelIntent.Summary
                },
                // Same gate, on speech rather than the summariser: without
                // extracted text there is nothing to read aloud either.
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
                // The article could not be built from cache and could not be
                // fetched. Saying so in the app's own voice beats handing the
                // reader a browser error page they cannot act on.
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
                            // The app's own ground, not a hardcoded black — this used to
                            // be pure black while the app itself sits on #101010, which
                            // is the kind of seam that makes an embedded browser feel
                            // like a different app wearing this one's toolbar.
                            setBackgroundColor(ground)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // Otherwise the WebView focuses the first focusable
                            // element when a document loads and scrolls it into
                            // view — which on a reader document, where the first
                            // link can be several screens down, means the article
                            // opens somewhere in its own middle.
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
                                // Without this the WebView renders Chrome's own
                                // "Webpage not available" inside a reading app,
                                // which is both ugly and unhelpful — it names a
                                // net:: error code at someone who wanted to read
                                // an article on a train.
                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: WebResourceError,
                                ) {
                                    if (request.isForMainFrame) loadFailed = true
                                }

                                // Anything that isn't itself a page — a mailto:,
                                // an intent: link, an app deep link — has no
                                // business loading inside this WebView.
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val target = request.url.toString()
                                    if (!target.startsWith("http")) {
                                        ctx.openExternally(target)
                                        return true
                                    }

                                    // A link followed out of the reader leaves the
                                    // extracted article behind: what it points at
                                    // has not been extracted, so it can only be
                                    // the live page. Claiming the load here stops
                                    // the effect above from fetching it a second
                                    // time when the mode flips.
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

                // The WebView holds nothing yet — extraction is still an HTTP
                // fetch away — so without this the screen is a bare rectangle
                // of `ground` for however long that takes.
                if (extracting) ArticleSkeleton(Modifier.fillMaxSize())

                // Over the article rather than beside it: the summary is a
                // second look at what is already on screen, and pushing the
                // page aside to show four lines would lose the thing being
                // summarised.
                // Tapping the article dismisses the panel. No ripple and no
                // scrim: the page underneath stays legible, which is the
                // point of floating over it, so the only sign this layer is
                // there is that the first tap closes the summary.
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
 * The summary panel, sliding up from the bottom edge.
 *
 * Its own composable rather than an `AnimatedVisibility` written inline: at
 * the call site both the column's scoped overload and the plain one are in
 * scope, and the column's wins — which is not the one that can be aligned
 * inside the box the WebView lives in.
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
                    // 12dp either side and clear of the gesture handle, as
                    // the design system's card draws it — the panel is
                    // bottom-anchored, so its own inset is all that separates
                    // it from the edge of the screen.
                    .padding(horizontal = 12.dp)
                    .padding(top = 14.dp, bottom = 16.dp),
            )
        }
    }
}

/**
 * Stands in for [dev.mks.duskread.links.articleDocument]'s own shape — source
 * line, title, lead image, body copy — so the screen looks like a page
 * arriving rather than a blank one waiting to be told what to become.
 *
 * **Drawn from the document's own measurements.** [Layout.ReadingGutter] and
 * the 20dp top inset are `articleDocument`'s body padding, and the lead block
 * carries [Radius.Inline] because its `.lead` rule does. A skeleton only works
 * if nothing moves when the text lands, and that holds only while both sides
 * read from the same numbers: this sat at a hand-written 20dp and shifted the
 * whole page 2dp on arrival.
 *
 * **The status line replaces the source line.** It used to be an icon and a
 * sentence centred in the lead block — which made a placeholder the one
 * boxed, filled card left anywhere in the app, and put the only words on
 * screen halfway down a page that had not arrived. In the slot where
 * `.source` prints the hostname, uppercase and muted, it is where the eye
 * already is and it is replaced by real content rather than vanishing.
 *
 * **The pulse travels rather than breathes.** One phase, offset per row, so
 * the page reads as filling in from the top; a single alpha driving every bar
 * at once made the whole screen throb in unison, which looks like a fault
 * rather than work in progress.
 */
@Composable
private fun ArticleSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "extracting")

    // Linear and restarting, not eased and reversing: the easing lives in the
    // triangle wave below, and a reversing phase would run the highlight back
    // up the page, which reads as undoing rather than loading.
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
            // Uppercase and letter-spaced to match `.source`, whose slot this
            // is standing in.
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

        // As many lines as there is room for, rather than a fixed eight that
        // ran out half way down and left the rest of the screen blank — which
        // read as an article that had finished loading and was mostly empty.
        // An article is longer than a screen; its placeholder should be too.
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val rows = (maxHeight / BodyLineSlot).toInt().coerceAtLeast(1)
            Column {
                repeat(rows) { line ->
                    // Cycled, so the short line that stands in for the end of
                    // a paragraph keeps recurring instead of the page turning
                    // into one unbroken block.
                    SkeletonBar(BodyLineWidths[line % BodyLineWidths.size], BodyLineHeight, phase, index = 3 + line)
                    Spacer(Modifier.height(BodyLineGap))
                }
            }
        }
    }
}

/**
 * [Radius.Chip] by default, the app's softened corner — a text placeholder is
 * standing in for a line of prose, and a fully rounded pill would make it read
 * as a control instead.
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
 *
 * A triangle wave rather than a sine or a raw phase: it has no seam where it
 * wraps, so the highlight leaves the bottom of the page and re-enters the top
 * without a visible jump.
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
 * Slow for UI — the sub-300ms rule in `Motion` is for a control answering a
 * touch, and this is ambient. Fast enough to look alive, slow enough that it
 * is not competing with the article for attention when it arrives.
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
        // Hidden rather than disabled when there is no article: a control that
        // can never do anything on this page is one the reader has to learn to
        // ignore.
        if (readerAvailable) {
            ToolbarButton(
                icon = DuskReadIcons.Reader,
                label = if (readerActive) "Show the original page" else "Show the reader view",
                onClick = onToggleReader,
                tint = if (readerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (summaryAvailable) {
            // The glyph becomes the spinner rather than sitting beside one:
            // the article stays readable while the model runs, and this is
            // the only thing on screen that should move.
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
        // Its own button rather than something found by first opening the
        // summary: the swipe already taught the app to draw a line between
        // "open this and see" and "start talking immediately", and the
        // reader deserves the same direct route, not two taps to get there.
        // It opens the same card either way — see `PanelIntent`.
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
 * The current scheme as CSS, so a rendered article is the same page as the app
 * around it rather than a white rectangle wearing its toolbar. Alpha is
 * dropped: every one of these is opaque, and `#RRGGBBAA` is not understood by
 * every WebView still in the field.
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
 * Algorithmic darkening (the modern replacement for `FORCE_DARK_ON`) is what
 * actually repaints a page that never declared a dark theme of its own,
 * rather than merely honouring one the page opted into. Falls back to the
 * older API on WebView builds too old to know about the new one, and does
 * nothing on a WebView too old for either — there is no third option.
 *
 * The reader view is unaffected either way: its document declares
 * `color-scheme: dark`, which is exactly the "the page handles this itself"
 * signal that turns algorithmic darkening off for it.
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
