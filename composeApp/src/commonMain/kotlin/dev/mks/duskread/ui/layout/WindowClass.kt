package dev.mks.duskread.ui.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import dev.mks.duskread.ui.theme.Layout

/**
 * Which of the two layouts the window is wide enough for.
 */
enum class WindowClass {
    /** A phone, a landscape phone, or a deliberately narrow desktop window. */
    Compact,

    /** Desktop, a tablet in landscape, a browser tab someone widened. */
    Wide,
    ;

    val isWide: Boolean get() = this == Wide
}

/**
 * Read rather than passed, because almost everything that cares is a leaf — a row
 * deciding whether it can hover, a gutter deciding how wide to be.
 */
val LocalWindowClass = compositionLocalOf { WindowClass.Compact }

/**
 * Measures the window and publishes [LocalWindowClass] beneath it.
 */
@Composable
fun WindowClassProvider(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier) {
        val windowClass = if (maxWidth >= Layout.TwoPaneBreakpoint) WindowClass.Wide else WindowClass.Compact
        CompositionLocalProvider(LocalWindowClass provides windowClass) { content() }
    }
}
