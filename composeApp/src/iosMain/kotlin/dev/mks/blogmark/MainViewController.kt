package dev.mks.blogmark

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Entry point consumed by the SwiftUI shell in iosApp/.
 *
 * PascalCase is the Compose Multiplatform convention for this factory — Swift
 * calls it as a type-like initialiser — so the naming rule is suppressed here
 * rather than relaxed project-wide.
 */
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
