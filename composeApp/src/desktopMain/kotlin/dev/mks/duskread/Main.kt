package dev.mks.duskread

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.mks.duskread.ui.Chromium

/**
 * Both themes are dark, so the host window has to be told as much before AWT
 * makes its first window — macOS otherwise hands a light title bar to a
 * near-black app, and the seam across the top is the first thing you see.
 * A system property rather than a client property on the root pane because
 * the appearance is fixed at window creation; setting it afterwards leaves
 * the bar already painted.
 *
 * Deliberately pinned to dark rather than `NSAppearanceNameAqua`-following:
 * DuskRead has no light scheme to follow a light system into, and Ink is
 * still a dark ground with the hue taken out.
 */
private fun useDarkWindowChrome() {
    System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")
}

fun main() {
    useDarkWindowChrome()

    // CEF runs helper processes of its own; without this they outlive the
    // window that closed. A shutdown hook rather than a call after
    // `application {}` returns, so a kill from the dock is covered too.
    Runtime.getRuntime().addShutdownHook(Thread { Chromium.dispose() })

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "DuskRead",
            state = rememberWindowState(size = DpSize(1180.dp, 820.dp)),
        ) {
            App()
        }
    }
}
