package dev.mks.duskread.ui.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import dev.mks.duskread.ui.theme.Layout

/**
 * Which of the two layouts the window is wide enough for.
 *
 * Two values, not a size-class ladder: there are exactly two plans in this
 * app — the phone one, and the rail-plus-two-panes one — so a Medium that
 * nothing switches on would be a name for a decision nobody made.
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
 * Read rather than passed, because almost everything that cares is a leaf —
 * a row deciding whether it can hover, a gutter deciding how wide to be —
 * and threading a parameter through every layer between here and there would
 * be most of the diff for none of the meaning.
 *
 * `compositionLocalOf`, not `staticCompositionLocalOf`: on web the value
 * genuinely changes mid-session as the tab is resized, and the static
 * variant recomposes the entire subtree under the provider when it does
 * rather than only the handful of places that read it.
 */
val LocalWindowClass = compositionLocalOf { WindowClass.Compact }

/**
 * Measures the window and publishes [LocalWindowClass] beneath it.
 *
 * [BoxWithConstraints] rather than a platform window-size class, because a
 * browser tab can be resized at any moment and desktop windows are dragged
 * about — a value read once at start-up would be wrong the first time
 * somebody grabs an edge. The measurement is the composition's own, so it
 * cannot disagree with what is actually being laid out.
 */
@Composable
fun WindowClassProvider(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier) {
        val windowClass = if (maxWidth >= Layout.TwoPaneBreakpoint) WindowClass.Wide else WindowClass.Compact
        CompositionLocalProvider(LocalWindowClass provides windowClass) { content() }
    }
}
