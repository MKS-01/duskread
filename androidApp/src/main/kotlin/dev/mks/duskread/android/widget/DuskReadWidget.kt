package dev.mks.duskread.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import dev.mks.duskread.android.MainActivity
import dev.mks.duskread.android.R
import dev.mks.duskread.pomodoro.PomodoroService
import dev.mks.duskread.ui.pomodoro.OpenFocusExtra
import dev.mks.duskread.widget.WidgetState

/**
 * The home-screen widget: capture a link, or start a focus session.
 *
 * Two things the app is for, one tap each, without opening it. Both are
 * icon-first, and only one of them is ever expanded — a capture takes the
 * width for a few seconds to confirm itself, a running session takes it for
 * as long as it runs. The reader never has to read two things at once.
 *
 * **RemoteViews rather than Glance.** Glance is the modern answer and would
 * have been the more interesting one to learn, but it has no `Chronometer`,
 * and a Chronometer is the whole battery story here: it is a real view in the
 * launcher's process, so the countdown is ticked by the launcher and this app
 * is never woken to redraw it. Reaching one from Glance means embedding a
 * RemoteViews subtree anyway — the same layout XML, plus Glance's state
 * machinery, plus no way to reach the bundled Jost. For three states and no
 * lists, the plain provider is simply the smaller thing.
 *
 * **Nothing here polls.** `updatePeriodMillis` is 0, there is no alarm except
 * the single one-shot that retires a capture confirmation, and the only other
 * redraws are the ones [PomodoroService] pushes at the two ends of a session.
 * Idle, this widget costs nothing at all.
 */
class DuskReadWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, build(context)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // The confirmation has no way to retire itself — a RemoteViews tree is
        // inert once handed over — so the capture schedules one alarm to come
        // back and clear it. Non-repeating, one per capture.
        if (intent.action == WidgetState.ActionClearFlash) WidgetState.setFlash(context, host = null)

        if (intent.action == WidgetState.ActionClearFlash || intent.action == WidgetState.ActionRefresh) refresh(context)
    }

    companion object {
        /**
         * Redraws every placed copy.
         *
         * Looks the ids up rather than taking them from an intent so that
         * anything — the capture activity, the Pomodoro service, the app on
         * its way to the background — can ask for a repaint without knowing
         * anything about widget instances.
         */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DuskReadWidget::class.java))
            if (ids.isEmpty()) return
            val views = build(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        /**
         * The whole render, as one function.
         *
         * Colours are applied here rather than baked into the layout because
         * the widget follows the app's Ink / Paper Black toggle, and a
         * RemoteViews tree cannot carry a theme. The layout holds Ink's
         * values so the picker's preview is right before any of this runs.
         */
        private fun build(context: Context): RemoteViews {
            val palette = if (WidgetState.mono(context)) Palette.Ink else Palette.PaperBlack
            val views = RemoteViews(context.packageName, R.layout.widget_dusk)

            val endsAt = WidgetState.focusEndsAt(context)
            val running = endsAt != null && WidgetState.focusRunning(context)
            val flash = WidgetState.flash(context)

            views.setInt(R.id.card, "setBackgroundResource", palette.card)
            tint(views, R.id.brand_mark, palette.primary)
            for (glyph in Glyphs) tint(views, glyph, palette.onSurface)
            for (label in Labels) views.setTextColor(label, palette.onSurfaceVariant)
            views.setTextColor(R.id.clock_idle, palette.onSurface)
            views.setTextColor(R.id.clock_focus, palette.onSurface)
            views.setTextColor(R.id.label_flash_host, palette.onSurface)

            // A capture confirmation borrows the number's place for a few
            // seconds; the controls stay where they are, because they are
            // still exactly as valid to press.
            views.setViewVisibility(R.id.state_flash, visible(flash != null))
            views.setViewVisibility(R.id.state_main, visible(flash == null))
            if (flash != null) {
                views.setTextViewText(R.id.label_flash_host, flash.host)
                views.setTextViewText(R.id.label_flash, flash.label)
            }

            // Idle draws its number as a plain TextView. Only a live session
            // gets the Chronometer, which is the thing the launcher ticks for
            // us — see the class comment.
            views.setViewVisibility(R.id.clock_focus, visible(endsAt != null))
            views.setViewVisibility(R.id.clock_idle, visible(endsAt == null))

            if (endsAt != null) {
                // The deadline is stored as wall-clock time because that
                // survives a reboot; the Chronometer counts in elapsed time
                // because that is immune to the clock being adjusted. This
                // line is the only place the two need to agree.
                val base = SystemClock.elapsedRealtime() + (endsAt - System.currentTimeMillis())
                views.setChronometerCountDown(R.id.clock_focus, true)
                views.setChronometer(R.id.clock_focus, base, null, running)
            }

            views.setTextViewText(
                R.id.label_caption,
                context.getString(
                    when {
                        endsAt == null -> R.string.widget_ready
                        running -> R.string.widget_focusing
                        else -> R.string.widget_paused
                    },
                ),
            )

            // Slot A: capture when nothing is running, otherwise the session's
            // own pause/resume toggle — the same one its notification shows,
            // so the two can never disagree about what the button does.
            views.setImageViewResource(
                R.id.icon_a,
                when {
                    endsAt == null -> R.drawable.ic_widget_paste
                    running -> R.drawable.ic_widget_pause
                    else -> R.drawable.ic_widget_play
                },
            )
            views.setOnClickPendingIntent(
                R.id.cell_a,
                when {
                    endsAt == null -> capture(context)
                    running -> session(context, PomodoroService.ActionPause)
                    else -> session(context, PomodoroService.ActionResume)
                },
            )
            views.setContentDescription(
                R.id.icon_a,
                context.getString(
                    when {
                        endsAt == null -> R.string.widget_a11y_paste
                        running -> R.string.widget_a11y_pause
                        else -> R.string.widget_a11y_resume
                    },
                ),
            )

            // Slot B: start, then stop.
            views.setImageViewResource(
                R.id.icon_b,
                if (endsAt == null) R.drawable.ic_widget_play else R.drawable.ic_widget_close,
            )
            views.setOnClickPendingIntent(
                R.id.cell_b,
                if (endsAt == null) startFocus(context) else session(context, PomodoroService.ActionReset),
            )
            views.setContentDescription(
                R.id.icon_b,
                context.getString(if (endsAt == null) R.string.widget_a11y_start else R.string.widget_a11y_stop),
            )
            tint(views, R.id.icon_a, palette.onSurface)
            tint(views, R.id.icon_b, palette.onSurface)

            // The number opens the app: the timer while a session runs, Home
            // otherwise.
            views.setOnClickPendingIntent(
                R.id.state_main,
                if (endsAt != null) openFocus(context) else openApp(context),
            )

            return views
        }

        private val Glyphs = intArrayOf(R.id.icon_a, R.id.icon_b)
        private val Labels = intArrayOf(R.id.label_caption, R.id.label_flash)

        private fun tint(views: RemoteViews, id: Int, color: Int) {
            views.setInt(id, "setColorFilter", color)
        }

        private fun visible(shown: Boolean) = if (shown) View.VISIBLE else View.GONE

        /** Tapping capture goes through an activity, not this receiver — see [ClipboardSaveActivity]. */
        private fun capture(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, ClipboardSaveActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /**
         * Starts the session without opening the app, by talking to the same
         * foreground service the in-app timer uses. There is no second timer
         * here and no state of its own — the service remains the only thing
         * that knows a session is running.
         */
        private fun startFocus(context: Context): PendingIntent = PendingIntent.getForegroundService(
            context,
            1,
            Intent(context, PomodoroService::class.java).putExtra(PomodoroService.ExtraMinutes, WidgetMinutes),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /**
         * A running session opens the app on the timer. Pause and reset stay
         * on the service's notification: a home screen is somewhere you tap
         * by accident, and losing a focus session to a mis-tap is a much
         * worse outcome than having to reach one control from the shade.
         */
        private fun openFocus(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java)
                .putExtra(OpenFocusExtra, true)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /**
         * Tells a running session what to do. `getService` rather than
         * `getForegroundService`: the service is already in the foreground by
         * the time any of these can be tapped, and asking to start it as one
         * again would be a background start with nothing to start.
         */
        private fun session(context: Context, action: String): PendingIntent = PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, PomodoroService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /** Tapping the number when nothing is running opens Home. */
        private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            3,
            Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /** The widget's one session length. Longer ones are a decision, and decisions belong in the app. */
        private const val WidgetMinutes = 15
    }

    /**
     * The two schemes, reduced to the five roles a widget actually shows.
     *
     * Hard-coded rather than read from `MaterialTheme.colorScheme`, which is
     * the rule everywhere else in this app and is broken here on purpose:
     * a colour scheme is Compose state and a RemoteViews tree is built
     * outside composition, with no theme to read. These values mirror
     * `ui/theme/Theme.kt` and have to be changed with it.
     */
    private enum class Palette(
        val card: Int,
        val onSurface: Int,
        val onSurfaceVariant: Int,
        val outline: Int,
        val primary: Int,
    ) {
        Ink(
            card = R.drawable.widget_card_ink,
            onSurface = 0xFFDCDCDC.toInt(),
            onSurfaceVariant = 0xFF9C9C9C.toInt(),
            outline = 0xFF464646.toInt(),
            primary = 0xFFDCDCDC.toInt(),
        ),
        PaperBlack(
            card = R.drawable.widget_card_paper,
            onSurface = 0xFFE8E6E2.toInt(),
            onSurfaceVariant = 0xFFA3A19D.toInt(),
            outline = 0xFF3E3E3D.toInt(),
            primary = 0xFFC6684A.toInt(),
        ),
    }
}
