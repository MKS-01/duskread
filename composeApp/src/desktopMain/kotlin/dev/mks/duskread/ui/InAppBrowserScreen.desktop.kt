package dev.mks.duskread.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFBrowser
import dev.mks.duskread.links.Article
import dev.mks.duskread.links.ReaderPalette
import dev.mks.duskread.links.articleDocument
import dev.mks.duskread.links.createHttpClient
import dev.mks.duskread.links.loadArticle
import dev.mks.duskread.ui.theme.DuskReadIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI

/** Which of the two things this screen can show is showing. */
private enum class BrowserMode { Reader, Original }

/**
 * A reference article, opened without leaving the window.
 *
 * The desktop counterpart of `androidMain`'s screen of the same name, and
 * deliberately the same two views: an extracted reader document by default,
 * the live page one click away. The extraction, the sanitiser and the
 * stylesheet are all `commonMain` already — only the surface the document is
 * painted on differs, so this file is mostly plumbing to CEF and a toolbar.
 *
 * Handing the link to Safari was what this replaces. That is a perfectly
 * good browser and entirely the wrong one: the page arrives in the system's
 * light-or-dark, in someone else's typography, in a window that is not this
 * one — and the reader who was two links into an evening's queue is now in
 * an app that knows nothing about the queue.
 *
 * [SwingPanel] is a heavyweight AWT component and therefore paints *above*
 * all Compose content regardless of z-order, which is why the toolbar is a
 * sibling in a [Column] rather than a bar floating over the page the way the
 * phone's does. Nothing else in the app may overlap it while it is open.
 */
@Composable
fun InAppBrowserScreen(url: String, mono: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val client = remember { createHttpClient() }
    DisposableEffect(client) { onDispose { client.close() } }

    val chromium by Chromium.state.collectAsState()
    LaunchedEffect(Unit) { Chromium.ensureStarted() }

    var browser by remember { mutableStateOf<KCEFBrowser?>(null) }
    var article by remember(url) { mutableStateOf<Article?>(null) }
    var extracting by remember(url) { mutableStateOf(true) }
    var mode by remember(url) { mutableStateOf(BrowserMode.Reader) }
    // What the browser currently holds, so a recomposition that touches mode
    // or article does not reload the page underneath the reader.
    var loaded by remember(url) { mutableStateOf("") }

    val palette = MaterialTheme.colorScheme.readerPalette(mono)
    val ground = MaterialTheme.colorScheme.background

    LaunchedEffect(url) {
        article = loadArticle(client, url)
        if (article == null) mode = BrowserMode.Original
        extracting = false
    }

    // Creating a browser blocks until CEF has one, so it cannot happen on the
    // frame thread. It also cannot happen before the install has finished,
    // which is what the `Ready` gate is.
    LaunchedEffect(chromium) {
        if (chromium !is Chromium.State.Ready || browser != null) return@LaunchedEffect
        browser = withContext(Dispatchers.IO) {
            runCatching {
                KCEF.newClientOrNullBlocking()?.createBrowser(KCEFBrowser.BLANK_URI)
            }.getOrNull()
        }
    }

    DisposableEffect(browser) {
        onDispose { browser?.dispose() }
    }

    LaunchedEffect(browser, mode, article, extracting) {
        val view = browser ?: return@LaunchedEffect
        // Nothing loads until extraction has answered: showing the live page
        // meanwhile means fetching it twice and watching it get replaced.
        if (extracting) return@LaunchedEffect

        val readable = article.takeIf { mode == BrowserMode.Reader }
        val key = readable?.let { "reader:${it.url}" } ?: "live:$url"
        if (key == loaded) return@LaunchedEffect
        loaded = key

        // Base URL is the article's own, so the body's relative links resolve
        // and the document stays same-origin with the images it loads.
        if (readable != null) view.loadHtml(articleDocument(readable, palette), readable.url) else view.loadURL(url)
    }

    Surface(modifier.fillMaxSize(), color = ground) {
        Column(Modifier.fillMaxSize()) {
            BrowserToolbar(
                title = article?.title ?: hostOf(url),
                readerAvailable = article != null,
                readerActive = mode == BrowserMode.Reader && article != null,
                onToggleReader = { mode = if (mode == BrowserMode.Reader) BrowserMode.Original else BrowserMode.Reader },
                onClose = onClose,
                onOpenExternally = { openExternally(url) },
            )

            if (extracting || chromium !is Chromium.State.Ready) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            }

            Box(Modifier.fillMaxSize()) {
                val ready = browser
                if (ready != null) {
                    SwingPanel(
                        background = ground,
                        factory = { ready.uiComponent },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // The one screen in the app that has to explain a wait
                    // measured in minutes. Saying what is being downloaded and
                    // why beats a spinner over a blank rectangle, which is
                    // indistinguishable from the app having hung.
                    ChromiumNotice(chromium, onOpenExternally = { openExternally(url) })
                }
            }
        }
    }
}

/**
 * What stands in for the page while Chromium is being fetched, or after it
 * has failed. Always offers the system browser: a reader who wanted to read
 * something should not be held up by an install they did not ask for.
 */
@Composable
private fun ChromiumNotice(state: Chromium.State, onOpenExternally: () -> Unit) {
    val scheme = MaterialTheme.colorScheme

    val (title, detail) = when (state) {
        is Chromium.State.Failed -> "The built-in browser is unavailable" to state.message
        is Chromium.State.Preparing -> {
            val pct = state.fraction?.let { " · ${(it * 100).toInt()}%" } ?: ""
            "Setting up the reader" to "${state.label}$pct. This happens once."
        }

        else -> "Setting up the reader" to "DuskRead brings its own browser so pages open here rather than in Safari."
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 40.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Column(Modifier.widthIn(max = 420.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onBackground)
            Spacer(Modifier.height(6.dp))
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.clickable(onClick = onOpenExternally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(DuskReadIcons.External, null, Modifier.size(15.dp), tint = scheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Open in the system browser instead", style = MaterialTheme.typography.bodyMedium, color = scheme.primary)
            }
        }
    }
}

@Composable
private fun BrowserToolbar(
    title: String,
    readerAvailable: Boolean,
    readerActive: Boolean,
    onToggleReader: () -> Unit,
    onClose: () -> Unit,
    onOpenExternally: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
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
        // can never do anything on this page is one to learn to ignore.
        if (readerAvailable) {
            ToolbarButton(
                icon = DuskReadIcons.Reader,
                label = if (readerActive) "Show the original page" else "Show the reader view",
                onClick = onToggleReader,
                tint = if (readerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ToolbarButton(DuskReadIcons.External, "Open in browser", onOpenExternally)
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, Modifier.size(18.dp), tint = tint)
    }
}

/** The escape hatch, and what [rememberUrlOpener] used to do unconditionally. */
internal fun openExternally(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url))
    }
}

private fun hostOf(url: String): String = runCatching { URI(url).host }.getOrNull() ?: url

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
