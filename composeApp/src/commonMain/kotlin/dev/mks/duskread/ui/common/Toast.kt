package dev.mks.duskread.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.mks.duskread.ui.theme.Motion
import dev.mks.duskread.ui.theme.Stroke
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A one-line, self-dismissing confirmation for actions that would otherwise
 * happen silently — saving or removing a link updates a list the reader may
 * not even have on screen (a bookmark tapped from the Following carousel on
 * Home lands in the Saved tab, out of view), and a swipe-to-remove leaves
 * nothing else behind to confirm it actually did anything.
 *
 * A singleton request rather than state threaded through every screen that
 * can trigger one, matching [dev.mks.duskread.links.SharedLinkRequest] and
 * [dev.mks.duskread.ui.home.HomeTabRequest] — the action and the one host
 * that renders it live in different, unrelated composables.
 */
object ToastRequest {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun show(text: String) {
        _message.value = text
    }

    fun consume() {
        _message.value = null
    }
}

/** Mounted once, above everything else — see [HomeScreen][dev.mks.duskread.ui.home.HomeScreen]. */
@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val message by ToastRequest.message.collectAsState()

    LaunchedEffect(message) {
        if (message != null) {
            delay(1_800)
            ToastRequest.consume()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(Motion.Chip)) + slideInVertically(tween(Motion.Chip)) { -it },
        exit = fadeOut(tween(Motion.Fade)) + slideOutVertically(tween(Motion.Fade)) { -it },
        modifier = modifier,
    ) {
        Box(
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .padding(horizontal = 18.dp, vertical = 11.dp),
        ) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
