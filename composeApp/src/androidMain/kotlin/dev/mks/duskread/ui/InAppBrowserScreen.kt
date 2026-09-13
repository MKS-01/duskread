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
 * A reference article, opened without leaving the app — as the article, not as the page
 * it arrived in.
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

    // Set only when the *main frame* fails. A page whose analytics script cannot load has
    // not failed; a page that cannot load has.
    var loadFailed by remember { mutableStateOf(false) }

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

    PlatformBackHandler(enabled = true) {
        webView?.takeIf { it.canGoBack() }?.goBack() ?: onClose()
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
