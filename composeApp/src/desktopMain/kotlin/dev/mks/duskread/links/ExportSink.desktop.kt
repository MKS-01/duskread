package dev.mks.duskread.links

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

/**
 * A save dialog and nothing else. Desktop has no share sheet worth speaking
 * of across the three platforms this runs on, and a synced Drive folder here
 * is just a directory — which the file chooser already lists.
 */
@Composable
actual fun rememberExportSink(): ExportSink = remember {
    object : ExportSink {
        override val canSaveFile = true
        override val canSend = false

        override fun saveFile(name: String, text: String) {
            // On the Swing thread, not Compose's: JFileChooser is a Swing
            // component and shows a modal dialog.
            SwingUtilities.invokeLater {
                val chooser = JFileChooser().apply { selectedFile = File(name) }
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    runCatching { chooser.selectedFile.writeText(text) }
                }
            }
        }

        override fun send(name: String, text: String) = Unit
    }
}
