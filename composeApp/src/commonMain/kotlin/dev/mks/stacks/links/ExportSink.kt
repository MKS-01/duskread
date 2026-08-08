package dev.mks.stacks.links

import androidx.compose.runtime.Composable

/**
 * Where an export can go once it exists, beyond the clipboard.
 *
 * Deliberately not a Drive client. Uploading to Google Drive properly means an
 * OAuth flow, a Cloud project and API keys shipped in the binary — a lot of
 * moving parts, all of them Android-and-web-only, to reach one destination.
 * The system's own document picker already reaches Drive, because Drive
 * registers as a documents provider, along with Files, Dropbox and everything
 * else the reader has installed. So [saveFile] hands the text to the picker
 * and lets the reader say where, which is both less code and more places.
 *
 * Capability, not assumption: each platform says what it can actually do and
 * the screen offers only that. Nothing here throws for being unsupported.
 */
interface ExportSink {
    /** Whether the platform can write the export somewhere the reader chooses. */
    val canSaveFile: Boolean

    /** Whether the platform has a share sheet — mail, chat, notes, Drive again. */
    val canSend: Boolean

    /** [name] is a suggested filename; the reader may well change it. */
    fun saveFile(name: String, text: String)

    fun send(name: String, text: String)
}

/**
 * Composable for the same reason as `rememberUrlOpener`: Android's document
 * picker is an activity result launcher, which only exists in composition.
 */
@Composable
expect fun rememberExportSink(): ExportSink

/**
 * The suggested filename. No date in it on purpose: the app has no date
 * formatter (see `savedAgo`), and a wrong or half-formatted date in a filename
 * is worse than none — the picker shows the real modified time anyway.
 */
const val ExportFileName = "stacks-reading-list.md"

/** Shared by the platforms that can do neither, so the screen falls back to the clipboard. */
internal object ClipboardOnlySink : ExportSink {
    override val canSaveFile = false
    override val canSend = false

    override fun saveFile(name: String, text: String) = Unit

    override fun send(name: String, text: String) = Unit
}
