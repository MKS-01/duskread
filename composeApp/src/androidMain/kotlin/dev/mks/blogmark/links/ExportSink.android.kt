package dev.mks.blogmark.links

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Both routes are the system's, not ours.
 *
 * `CREATE_DOCUMENT` is the interesting one: the picker it opens lists every
 * documents provider on the device, and Google Drive is one of them. Choosing
 * a Drive folder there uploads the file, with the account, the folder tree and
 * the sync all handled by Drive itself — no OAuth in this app, no API key, and
 * the same gesture also reaches Files, Dropbox and an SD card.
 *
 * The text has to survive the trip to the picker and back, so it is parked in
 * composition state while the activity is away rather than captured by the
 * launch call, which cannot carry it.
 */
@Composable
actual fun rememberExportSink(): ExportSink {
    val context = LocalContext.current
    val pending = remember { mutableStateOf<String?>(null) }

    val create = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MimeType),
    ) { uri ->
        val body = pending.value
        pending.value = null
        if (uri != null && body != null) {
            // A failed write is silent by design: the picker has already gone,
            // there is nothing to retry into, and the reading list itself is
            // untouched either way. The clipboard route is always still there.
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(body.encodeToByteArray()) }
            }
        }
    }

    return remember(context, create) {
        object : ExportSink {
            override val canSaveFile = true
            override val canSend = true

            override fun saveFile(name: String, text: String) {
                pending.value = text
                create.launch(name)
            }

            override fun send(name: String, text: String) {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, name)
                    putExtra(Intent.EXTRA_SUBJECT, name)
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                val chooser = Intent.createChooser(send, "Send reading list")
                // Same guard as the URL opener, and on the chooser rather than
                // the intent it wraps: that is the one actually started.
                if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(chooser) }
            }
        }
    }
}

// Markdown, because that is what the export is. Providers that only understand
// text/plain still accept it — the picker matches on the filename extension
// too — and the ones that do understand it show it as a document rather than
// an unknown blob.
private const val MimeType = "text/markdown"
