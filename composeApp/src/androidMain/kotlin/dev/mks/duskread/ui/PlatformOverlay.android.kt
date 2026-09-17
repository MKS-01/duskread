package dev.mks.duskread.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.mks.duskread.ui.theme.Motion
import kotlinx.coroutines.delay

@Composable
actual fun PlatformOverlay(mono: Boolean) {
    val requested by InAppBrowserRequest.queue.collectAsState()

    // Held past the request going null so the close animation fades out the page it was
    // actually showing, rather than a blank screen — and dropped once it has, so the
    // reader is disposed with its WebView and reopens where the list says rather than
    // where the last reading left off.
    val shown = remember { mutableStateOf<ReadingQueue?>(null) }
    requested?.let { shown.value = it }
    LaunchedEffect(requested) {
        if (requested == null) {
            delay(Motion.PopFade.toLong())
            shown.value = null
        }
    }

    AnimatedVisibility(
        visible = requested != null,
        enter = fadeIn(tween(Motion.PushIn)) + slideInVertically(tween(Motion.PushIn)) { it / 8 },
        exit = fadeOut(tween(Motion.PopFade)),
    ) {
        shown.value?.let { queue -> InAppBrowserScreen(queue = queue, mono = mono, onClose = InAppBrowserRequest::consume) }
    }
}
