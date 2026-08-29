package dev.mks.duskread.android.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import dev.mks.duskread.android.R
import dev.mks.duskread.data.keyValueStore
import dev.mks.duskread.links.LinkInbox
import dev.mks.duskread.links.extractUrl
import dev.mks.duskread.links.hostOf
import dev.mks.duskread.links.normaliseUrl
import dev.mks.duskread.widget.WidgetState

/**
 * An activity that exists only so the clipboard can be read, and closes again
 * before it can be seen.
 *
 * Since Android 10 the clipboard is readable only by the app that currently
 * has input focus, which a widget's `AppWidgetProvider` never does — it is a
 * broadcast receiver with no window at all. The only way to honour "tap to
 * paste" from a home screen is to briefly *be* the focused app, so this
 * launches with a fully transparent theme, no history and no entry in
 * Recents, takes what it came for and finishes. The reader sees a toast and
 * their home screen; there is nothing to dismiss.
 *
 * The read happens in [onWindowFocusChanged] rather than [onCreate] for the
 * same reason the activity exists: focus is granted after creation, and
 * reading too early returns nothing on exactly the devices this is meant to
 * work on.
 */
class ClipboardSaveActivity : ComponentActivity() {
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || handled) return
        handled = true

        capture()
        finish()
        overridePendingTransition(0, 0)
    }

    private fun capture() {
        val url = clipboardUrl()
        if (url == null) {
            // Nothing on the widget changes here. A missing link is the
            // reader's mistake to correct, not a state the home screen should
            // hold on to for six seconds.
            Toast.makeText(this, getString(R.string.widget_no_link), Toast.LENGTH_SHORT).show()
            return
        }

        // Into the inbox, never into the library: see LinkInbox for why a
        // second writer of the saved-links blob would lose links.
        val added = LinkInbox.offer(keyValueStore(this), url)
        val host = hostOf(normaliseUrl(url))
        val label = getString(if (added) R.string.widget_saved else R.string.widget_already_saved)
        val spoken = getString(if (added) R.string.widget_toast_saved else R.string.widget_toast_already_saved)

        Toast.makeText(this, "$spoken · $host", Toast.LENGTH_SHORT).show()
        WidgetState.setFlash(this, host = host, label = label)
        WidgetState.refresh(this)
        scheduleFlashClear()
    }

    /**
     * Reuses the share sheet's extractor rather than taking the clip whole —
     * a copied selection is as likely to be "Some headline https://x.com/y"
     * as a bare URL, and the two paths should not disagree about what counts
     * as a link.
     */
    private fun clipboardUrl(): String? {
        val clipboard = getSystemService(ClipboardManager::class.java) ?: return null
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return null
        return extractUrl(text)
    }

    /**
     * One non-repeating alarm, a few seconds out, to put the widget back to
     * idle. A RemoteViews tree is inert once handed to the launcher, so the
     * confirmation cannot retire itself and something has to come back for
     * it. Inexact on purpose — this is cosmetic, and an exact alarm would be
     * asking the system for a guarantee in order to hide a label.
     */
    private fun scheduleFlashClear() {
        val alarms = getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(this, DuskReadWidget::class.java).setAction(WidgetState.ActionClearFlash)
        val pending = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarms.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + WidgetState.FlashMillis,
            pending,
        )
    }
}
