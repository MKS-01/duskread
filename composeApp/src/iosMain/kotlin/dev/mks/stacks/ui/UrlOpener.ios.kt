package dev.mks.stacks.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.SafariServices.SFSafariViewControllerDismissButtonStyle.SFSafariViewControllerDismissButtonStyleDone
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIViewController

/**
 * The iOS counterpart to Custom Tabs: `SFSafariViewController`, presented over
 * the app. Same reasoning — Safari's session, reader mode and share sheet, but
 * a Done button back to where the reader was.
 *
 * Non-web schemes (`mailto:`, App Store links) go to the system, which Safari
 * View Controller cannot show anyway.
 */
@Composable
actual fun rememberUrlOpener(): (String) -> Unit {
    val barTint = MaterialTheme.colorScheme.surface.toUIColor()
    val controlTint = MaterialTheme.colorScheme.primary.toUIColor()

    return remember(barTint, controlTint) {
        { url ->
            NSURL.URLWithString(url)?.let { nsUrl ->
                val host = topViewController()
                if (host != null && nsUrl.scheme?.lowercase() in webSchemes) {
                    val safari = SFSafariViewController(nsUrl).apply {
                        preferredBarTintColor = barTint
                        preferredControlTintColor = controlTint
                        dismissButtonStyle = SFSafariViewControllerDismissButtonStyleDone
                    }
                    host.presentViewController(safari, animated = true, completion = null)
                } else {
                    UIApplication.sharedApplication.openURL(nsUrl)
                }
            }
        }
    }
}

private val webSchemes = setOf("http", "https")

/** Whatever is frontmost, so the sheet is not presented on an already-covered controller. */
private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}

private fun Color.toUIColor(): UIColor = UIColor(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble(),
)
