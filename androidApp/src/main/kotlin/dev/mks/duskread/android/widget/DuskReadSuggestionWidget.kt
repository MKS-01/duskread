package dev.mks.duskread.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.mks.duskread.android.MainActivity
import dev.mks.duskread.android.R
import dev.mks.duskread.data.keyValueStore
import dev.mks.duskread.links.FeedLibrary
import dev.mks.duskread.links.FeedPostCache
import dev.mks.duskread.links.LinkLibrary
import dev.mks.duskread.links.ReadingSignals
import dev.mks.duskread.links.pool
import dev.mks.duskread.links.rank
import dev.mks.duskread.links.topPicks
import dev.mks.duskread.ui.home.OpenSuggestionTitleExtra
import dev.mks.duskread.ui.home.OpenSuggestionTopicExtra
import dev.mks.duskread.ui.home.OpenSuggestionUrlExtra
import dev.mks.duskread.widget.WidgetState
import kotlin.random.Random

/**
 * The other home-screen widget: one NEXT UP pick, not a control.
 *
 * A separate provider from [DuskReadWidget] rather than a third state
 * squeezed into its 64dp bar — that bar is built around exactly two states on
 * purpose (see its own class comment), and an article title has nowhere to
 * go in a 9sp caption slot without displacing the "start a session"
 * affordance the bar exists to keep always in reach.
 *
 * **Ranks its own read-only copy of the pool.** [dev.mks.duskread.links.pool],
 * [dev.mks.duskread.links.rank] and [dev.mks.duskread.links.topPicks] are
 * pure functions over plain data, and `LinkLibrary`/`FeedLibrary`/
 * `FeedPostCache`/`ReadingSignals` are plain classes over [keyValueStore] —
 * none of it needs a Compose tree, so this can construct a throwaway copy of
 * each, rank once, and discard them. What it must *not* do is write through
 * that copy: opening a pick has to go through the app's own live instances,
 * which is what [dev.mks.duskread.ui.home.SuggestionOpenRequest] is for.
 *
 * **Re-picks on the same broadcast [DuskReadWidget] already answers to.**
 * Registered for the same `APPWIDGET_UPDATE` and [WidgetState.ActionRefresh]
 * actions, so a capture (`ClipboardSaveActivity`) or a focus-session
 * transition (`PomodoroService.setFocus`) repaints both widgets from the one
 * broadcast already firing — no new trigger added anywhere in `composeApp`.
 * `MainActivity.onStop` calls [refresh] directly for the same reason it
 * already calls `DuskReadWidget.refresh` there. Nothing here polls: the seed
 * is fresh on every one of those redraws, which is the whole mechanism behind
 * the pick changing over a day without an alarm anywhere in it.
 */
class DuskReadSuggestionWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, build(context)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetState.ActionRefresh) refresh(context)
    }

    companion object {
        /** Redraws every placed copy — see [DuskReadWidget.refresh], which this mirrors. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DuskReadSuggestionWidget::class.java))
            if (ids.isEmpty()) return
            val views = build(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun build(context: Context): RemoteViews {
            val palette = if (WidgetState.mono(context)) Palette.Ink else Palette.PaperBlack
            val views = RemoteViews(context.packageName, R.layout.widget_suggestion)

            views.setInt(R.id.suggestion_card, "setBackgroundResource", palette.card)
            views.setInt(R.id.suggestion_mark, "setColorFilter", palette.primary)
            views.setTextColor(R.id.suggestion_eyebrow, palette.primary)
            views.setTextColor(R.id.suggestion_title, palette.onSurface)
            views.setTextColor(R.id.suggestion_meta, palette.onSurfaceVariant)

            val store = keyValueStore(context)
            val links = LinkLibrary(store)
            val feeds = FeedLibrary(store)
            val feedPosts = FeedPostCache(store)
            val signals = ReadingSignals(store)

            // A fresh seed on every redraw, not the day-stable one NEXT UP
            // ranks with on Home — that section is deliberately stable across
            // a morning's worth of openings, but this widget only ever
            // redraws for a real reason in the first place, so there is
            // nothing to protect against re-rolling every time it does.
            val ranked = rank(
                candidates = pool(links, feedPosts, feeds.feeds),
                signals = signals,
                now = System.currentTimeMillis(),
                seed = Random.nextInt(),
                focusMinutes = null,
            )
            val pick = topPicks(ranked, count = 1).firstOrNull()

            if (pick == null) {
                views.setTextViewText(R.id.suggestion_title, context.getString(R.string.widget_suggestion_empty_title))
                views.setTextViewText(R.id.suggestion_meta, context.getString(R.string.widget_suggestion_empty_meta))
                views.setOnClickPendingIntent(R.id.suggestion_card, openApp(context))
            } else {
                val candidate = pick.candidate
                views.setTextViewText(R.id.suggestion_title, candidate.title)
                views.setTextViewText(
                    R.id.suggestion_meta,
                    listOfNotNull(candidate.host, candidate.tag, "${pick.minutes} min").joinToString(" · "),
                )
                views.setContentDescription(R.id.suggestion_card, context.getString(R.string.widget_suggestion_a11y_open))
                views.setOnClickPendingIntent(
                    R.id.suggestion_card,
                    openArticle(context, candidate.url, candidate.title, candidate.tag),
                )
            }

            return views
        }

        /** Tapping a pick opens straight to it — see `MainActivity.handleIntent` and `SuggestionOpenRequest`. */
        private fun openArticle(context: Context, url: String, title: String, topic: String?): PendingIntent = PendingIntent.getActivity(
            context,
            url.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(OpenSuggestionUrlExtra, url)
                .putExtra(OpenSuggestionTitleExtra, title)
                .putExtra(OpenSuggestionTopicExtra, topic)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /** Nothing to suggest yet — tapping the empty state opens Home instead of doing nothing. */
        private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** The same five roles [DuskReadWidget.Palette] reads, mirrored here rather than shared — see that enum for why. */
    private enum class Palette(
        val card: Int,
        val onSurface: Int,
        val onSurfaceVariant: Int,
        val primary: Int,
    ) {
        Ink(
            card = R.drawable.widget_card_ink,
            onSurface = 0xFFDCDCDC.toInt(),
            onSurfaceVariant = 0xFF9C9C9C.toInt(),
            primary = 0xFFDCDCDC.toInt(),
        ),
        PaperBlack(
            card = R.drawable.widget_card_paper,
            onSurface = 0xFFE8E6E2.toInt(),
            onSurfaceVariant = 0xFFA3A19D.toInt(),
            primary = 0xFFC6684A.toInt(),
        ),
    }
}
