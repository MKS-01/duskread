package dev.mks.algoatlas

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Entry point consumed by the SwiftUI shell in iosApp/. */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
