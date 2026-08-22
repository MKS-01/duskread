package dev.mks.blogmark.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.mks.blogmark.R

/**
 * Opens links in a Chrome Custom Tab rather than an embedded WebView: the
 * reader keeps the browser's cookies, logins, password manager and share
 * sheet, and sites that refuse framing (Medium, most docs) still render.
 *
 * The point of the styling below is that a reference should feel like the next
 * screen in Blogmark, not a hand-off to a different app — same push/pop
 * animation as [dev.mks.blogmark.App], same surface colour, and a back arrow
 * where Chrome would otherwise put an ✕.
 *
 * Falls back to a plain `ACTION_VIEW` when no browser supports Custom Tabs.
 */
@Composable
actual fun rememberUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val toolbar = scheme.surface.toArgb()
    val onToolbar = scheme.onSurface.toArgb()
    val iconPx = with(LocalDensity.current) { 24.dp.roundToPx() }

    val backArrow = remember(onToolbar, iconPx) { backArrowBitmap(onToolbar, iconPx) }

    return remember(context, toolbar, onToolbar, backArrow) {
        { url ->
            val uri = Uri.parse(url)
            val colors = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(toolbar)
                .setSecondaryToolbarColor(toolbar)
                .setNavigationBarColor(toolbar)
                .setNavigationBarDividerColor(toolbar)
                .build()

            val customTab = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colors)
                .setShowTitle(true)
                // A toolbar that stays put reads as a screen header; one that
                // hides on scroll reads as a browser.
                .setUrlBarHidingEnabled(false)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .setCloseButtonIcon(backArrow)
                .setStartAnimations(context, R.anim.atlas_push_enter, R.anim.atlas_push_exit)
                .setExitAnimations(context, R.anim.atlas_pop_enter, R.anim.atlas_pop_exit)
                .build()

            try {
                if (context !is Activity) customTab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                customTab.launchUrl(context, uri)
            } catch (_: ActivityNotFoundException) {
                context.openInAnyBrowser(uri)
            }
        }
    }
}

private fun Context.openInAnyBrowser(uri: Uri) {
    val view = Intent(Intent.ACTION_VIEW, uri)
    if (this !is Activity) view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(view) }
}

/**
 * Custom Tabs takes a [Bitmap], not a vector, so the Material back arrow is
 * drawn by hand at the density it will be shown at.
 */
private fun backArrowBitmap(color: Int, sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val unit = sizePx / 24f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = 2f * unit
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val path = Path().apply {
        moveTo(20f * unit, 12f * unit)
        lineTo(4f * unit, 12f * unit)
        moveTo(11f * unit, 5f * unit)
        lineTo(4f * unit, 12f * unit)
        lineTo(11f * unit, 19f * unit)
    }
    Canvas(bitmap).drawPath(path, paint)
    return bitmap
}
