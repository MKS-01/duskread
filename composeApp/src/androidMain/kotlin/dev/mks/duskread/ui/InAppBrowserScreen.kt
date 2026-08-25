package dev.mks.duskread.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import dev.mks.duskread.summary.SummaryTarget
import dev.mks.duskread.summary.summariesSupported
import dev.mks.duskread.ui.summary.SummaryPanel
import dev.mks.duskread.ui.theme.DuskReadIcons
import dev.mks.duskread.ui.theme.Motion

/** Which of the two things this screen can show is showing. */
private enum class BrowserMode { Reader, Original }

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

    var article by remember(url) { mutableStateOf<Article?>(null) }
    var extracting by remember(url) { mutableStateOf(true) }
    var mode by remember(url) { mutableStateOf(BrowserMode.Reader) }
    // Closed by default, and per article: following a link out of one piece
    // into another should not carry the first one's summary with it.
    var summarising by remember(url) { mutableStateOf(false) }
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
                summaryActive = summarising,
                summaryBusy = summaryBusy,
                onToggleSummary = { summarising = !summarising },
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
                if (summarising) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { summarising = false },
                            ),
                    )
                }

                SummaryOverArticle(
                    article = article,
                    visible = summarising,
                    onClose = { summarising = false },
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
 * Stands in for [dev.mks.duskread.links.articleDocument]'s own shape — title,
 * byline, lead image, body copy — so the screen looks like a page arriving
 * rather than a blank one waiting to be told what to become. The fetching
 * icon and message live inside the lead-image block: that's the one shape
 * big enough to hold them without a line of body text running behind, and an
 * image is the thing a reader most expects to still be loading.
 */
@Composable
private fun ArticleSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "extracting")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "extractingAlpha",
    )
    val block = MaterialTheme.colorScheme.surfaceContainer

    Column(modifier.padding(20.dp)) {
        SkeletonBar(0.5f, 16.dp, block, alpha)
        Spacer(Modifier.height(22.dp))
        SkeletonBar(0.92f, 22.dp, block, alpha)
        Spacer(Modifier.height(10.dp))
        SkeletonBar(0.65f, 22.dp, block, alpha)
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(block.copy(alpha = alpha)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = DuskReadIcons.Reader,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Fetching the article…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        val lineWidths = listOf(0.97f, 0.9f, 0.98f, 0.4f, 0.95f, 0.88f, 0.93f, 0.6f)
        lineWidths.forEach { width ->
            SkeletonBar(width, 12.dp, block, alpha)
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SkeletonBar(widthFraction: Float, height: Dp, color: Color, alpha: Float) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .alpha(alpha)
            .clip(RoundedCornerShape(4.dp))
            .background(color),
    )
}

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

private fun Context.openExternally(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
