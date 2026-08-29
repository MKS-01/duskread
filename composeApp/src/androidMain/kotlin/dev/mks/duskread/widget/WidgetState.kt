package dev.mks.duskread.widget

import android.content.Context
import android.content.Intent
import dev.mks.duskread.data.keyValueStore

/**
 * What the home-screen widget draws, and the one channel for telling it to
 * redraw.
 *
 * The widget itself lives in the `androidApp` host module, which depends on
 * this one and not the other way round, so [PomodoroService] cannot call it.
 * Rather than invert the modules for a countdown, the service writes the state
 * here and broadcasts [ActionRefresh]; the provider registers for that action
 * and re-renders. The coupling is one string.
 *
 * State is written as wall-clock milliseconds, not `elapsedRealtime`, because
 * `elapsedRealtime` resets on reboot and a stale value would render as a
 * session that is somehow still running. Wall clock survives a reboot and a
 * deadline in the past is unambiguously over. The chronometer's own base is
 * converted back to elapsed time at render, which is the only place the two
 * clocks need to meet.
 */
object WidgetState {
    const val ActionRefresh = "dev.mks.duskread.widget.REFRESH"

    /** Sent by the one-shot alarm that retires a capture confirmation. */
    const val ActionClearFlash = "dev.mks.duskread.widget.CLEAR_FLASH"

    /** When the running focus session ends, or null if there isn't one. */
    fun focusEndsAt(context: Context): Long? = keyValueStore(context).getString(KeyFocusEndsAt)?.toLongOrNull()?.takeIf { it > System.currentTimeMillis() }

    /** False while a session is paused — the widget freezes the clock and says so. */
    fun focusRunning(context: Context): Boolean = keyValueStore(context).getBoolean(KeyFocusRunning)

    /**
     * Records a session with [remainingSeconds] left, or clears it when the
     * session is over. Called from every [PomodoroService] transition, so the
     * widget is only ever written to at the two or three moments a session
     * actually changes — never on the per-second tick that drives the
     * notification.
     */
    fun setFocus(context: Context, remainingSeconds: Int, running: Boolean) {
        val store = keyValueStore(context)
        if (remainingSeconds <= 0) {
            store.putString(KeyFocusEndsAt, null)
            store.putString(KeyFocusRunning, null)
        } else {
            store.putString(KeyFocusEndsAt, (System.currentTimeMillis() + remainingSeconds * 1000L).toString())
            store.putBoolean(KeyFocusRunning, running)
        }
        refresh(context)
    }

    /**
     * The transient capture confirmation, or null once it has aged out.
     *
     * Two parts rather than one sentence: the host is the answer to "what did
     * I just save", the label is the answer to "did it work", and the widget
     * sets them at different sizes. Joining them into a string here would
     * mean splitting it again at render.
     */
    fun flash(context: Context): Flash? {
        val store = keyValueStore(context)
        val at = store.getString(KeyFlashAt)?.toLongOrNull() ?: return null
        if (System.currentTimeMillis() - at > FlashMillis) return null
        val host = store.getString(KeyFlashHost) ?: return null
        return Flash(host, store.getString(KeyFlashLabel).orEmpty())
    }

    fun setFlash(context: Context, host: String?, label: String? = null) {
        val store = keyValueStore(context)
        store.putString(KeyFlashHost, host)
        store.putString(KeyFlashLabel, label)
        store.putString(KeyFlashAt, host?.let { System.currentTimeMillis().toString() })
    }

    /** [host] is the page, [label] the eyebrow above it — "SAVED" or "ALREADY SAVED". */
    data class Flash(val host: String, val label: String)

    /** Ink or Paper Black, read from the same key the app's theme toggle writes. */
    fun mono(context: Context): Boolean = keyValueStore(context).getBoolean("theme.mono", default = true)

    /** Package-scoped so it reaches our provider and nothing else's. */
    fun refresh(context: Context) {
        context.sendBroadcast(Intent(ActionRefresh).setPackage(context.packageName))
    }

    /** How long the capture confirmation stays up before the widget returns to idle. */
    const val FlashMillis = 6_000L

    private const val KeyFocusEndsAt = "widget.focusEndsAt"
    private const val KeyFocusRunning = "widget.focusRunning"
    private const val KeyFlashHost = "widget.flashHost"
    private const val KeyFlashLabel = "widget.flashLabel"
    private const val KeyFlashAt = "widget.flashAt"
}
