package dev.mks.duskread.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.mks.duskread.ui.theme.Motion

/**
 * Desktop's embedded browser, laid over the whole window — the same hook
 * Android uses, now that desktop has a browser of its own to put in it
 * rather than handing the link to Safari.
 */
@Composable
actual fun PlatformOverlay(mono: Boolean) {
    val requested by InAppBrowserRequest.url.collectAsState()

    // Held past the request going null so the close animation fades out the
    // page it was actually showing rather than a blank window.
    val shown = remember { mutableStateOf<String?>(null) }
    requested?.let { shown.value = it }

    AnimatedVisibility(
        visible = requested != null,
        enter = fadeIn(tween(Motion.PushIn)) + slideInVertically(tween(Motion.PushIn)) { it / 8 },
        exit = fadeOut(tween(Motion.PopFade)),
    ) {
        shown.value?.let { url -> InAppBrowserScreen(url = url, mono = mono, onClose = InAppBrowserRequest::consume) }
    }
}
