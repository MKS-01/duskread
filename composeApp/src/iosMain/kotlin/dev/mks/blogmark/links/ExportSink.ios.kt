package dev.mks.blogmark.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/**
 * One sheet, both jobs.
 *
 * iOS has no equivalent of `CREATE_DOCUMENT` for text you are holding in
 * memory, but its share sheet carries "Save to Files" and any installed
 * provider — Drive included — beside Mail and Notes. So [saveFile] and [send]
 * are the same gesture here, and the screen offering both would be offering
 * the same thing twice.
 */
@Composable
actual fun rememberExportSink(): ExportSink = remember {
    object : ExportSink {
        override val canSaveFile = false
        override val canSend = true

        override fun saveFile(name: String, text: String) = send(name, text)

        override fun send(name: String, text: String) {
            val sheet = UIActivityViewController(
                activityItems = listOf(text),
                applicationActivities = null,
            )
            // The popover anchor matters on iPad, where a sheet with no source
            // view is a crash rather than a layout problem.
            val host = topViewController() ?: return
            sheet.popoverPresentationController?.sourceView = host.view
            host.presentViewController(sheet, animated = true, completion = null)
        }
    }
}

/** Whatever is frontmost, so the sheet is not presented on an already-covered controller. */
private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
