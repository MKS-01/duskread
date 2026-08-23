package dev.mks.blogmark.ui

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
import dev.mks.blogmark.ui.theme.Motion

@Composable
actual fun PlatformOverlay() {
    val requested by InAppBrowserRequest.url.collectAsState()

    // Held past the request going null so the close animation fades out the
    // page it was actually showing, rather than a blank screen — same reason
    // `FloatingBar` holds onto `nowPlaying` through its own exit transition.
    val shown = remember { mutableStateOf<String?>(null) }
    requested?.let { shown.value = it }

    AnimatedVisibility(
        visible = requested != null,
        enter = fadeIn(tween(Motion.PushIn)) + slideInVertically(tween(Motion.PushIn)) { it / 8 },
        exit = fadeOut(tween(Motion.PopFade)),
    ) {
        shown.value?.let { url -> InAppBrowserScreen(url = url, onClose = InAppBrowserRequest::consume) }
    }
}
