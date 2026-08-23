package dev.mks.blogmark.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import dev.mks.blogmark.ui.theme.BlogmarkIcons

/**
 * A reference article, opened without leaving the app.
 *
 * Not Chrome Custom Tabs: those hand the page to whatever browser is set as
 * default, so the page's own light-or-dark rendering follows the *system*
 * theme rather than this app's, and a Custom Tab's colour scheme only skins
 * the browser's chrome — it cannot force a page that ignores
 * `prefers-color-scheme` into dark. An embedded [WebView] with
 * [WebSettingsCompat]'s algorithmic darkening can. The cost is real: no
 * shared cookies or logins with the reader's actual browser, no native share
 * sheet, and a site that refuses framing here has nowhere else to go —
 * [BlogmarkIcons.External] is always one tap away as the escape hatch.
 */
@Composable
fun InAppBrowserScreen(url: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var title by remember { mutableStateOf(hostOf(url)) }
    var currentUrl by remember { mutableStateOf(url) }
    var progress by remember { mutableStateOf(0f) }

    PlatformBackHandler(enabled = true) {
        webView?.takeIf { it.canGoBack() }?.goBack() ?: onClose()
    }

    // Read once, outside the WebView factory: that lambda runs a single time
    // on first composition, so a value it captures is frozen at whatever the
    // theme was then. Fine here — this screen closes and reopens across a
    // theme change, it never lives through one.
    val ground = MaterialTheme.colorScheme.background.toArgb()

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            BrowserToolbar(
                title = title,
                onClose = onClose,
                onOpenExternally = { context.openExternally(currentUrl) },
            )
            if (progress < 1f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                )
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
                                if (target.startsWith("http")) return false
                                ctx.openExternally(target)
                                return true
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
                        loadUrl(url)
                    }.also { webView = it }
                },
            )
        }
    }
}

@Composable
private fun BrowserToolbar(title: String, onClose: () -> Unit, onOpenExternally: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarButton(BlogmarkIcons.Back, "Close", onClose)
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
        )
        ToolbarButton(BlogmarkIcons.External, "Open in browser", onOpenExternally)
    }
}

@Composable
private fun ToolbarButton(icon: ImageVector, label: String, onClick: () -> Unit) {
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Algorithmic darkening (the modern replacement for `FORCE_DARK_ON`) is what
 * actually repaints a page that never declared a dark theme of its own,
 * rather than merely honouring one the page opted into. Falls back to the
 * older API on WebView builds too old to know about the new one, and does
 * nothing on a WebView too old for either — there is no third option.
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
