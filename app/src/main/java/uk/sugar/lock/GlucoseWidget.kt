package uk.sugar.lock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Widget for the home screen, or the lock screen via Good Lock > LockStar. */
class GlucoseWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = refresh(c)

    companion object {
        fun refresh(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            val ids = m.getAppWidgetIds(ComponentName(c, GlucoseWidget::class.java))
            if (ids.isEmpty()) return
            val r = Store.load(c)
            val v = RemoteViews(c.packageName, R.layout.widget)
            if (r == null) {
                v.setTextViewText(R.id.value, "--")
                v.setTextViewText(R.id.age, "Waiting for CamAPS FX")
            } else {
                val age = System.currentTimeMillis() - r.timeMs
                val mins = (age / 60000).coerceAtLeast(0)
                v.setTextViewText(R.id.value, listOf(r.value, r.trend).filter(String::isNotEmpty).joinToString(" "))
                v.setTextViewText(
                    R.id.age,
                    if (age > LockScreenNotifier.STALE_MS) "⚠ OLD – $mins min ago"
                    else "${r.unit} · " + (if (mins == 0L) "just now" else "$mins min ago")
                )
            }
            // Background colour by range; neutral when there's no reading or it's old.
            val stale = r == null || System.currentTimeMillis() - r.timeMs > LockScreenNotifier.STALE_MS
            val bg = when {
                stale -> R.drawable.widget_bg
                Range.classify(c, r!!) == Range.Cls.LOW -> R.drawable.widget_low
                Range.classify(c, r) == Range.Cls.HIGH -> R.drawable.widget_high
                else -> R.drawable.widget_bg
            }
            v.setInt(R.id.root, "setBackgroundResource", bg)

            val tir = Range.percentToday(c)
            if (tir != null) {
                v.setTextViewText(R.id.tir, "$tir% in range today")
                v.setViewVisibility(R.id.tir, android.view.View.VISIBLE)
            } else {
                v.setViewVisibility(R.id.tir, android.view.View.GONE)
            }

            v.setImageViewBitmap(R.id.graph, Graph.render(c, 480, 160, Graph.WIDGET))

            v.setTextViewText(R.id.joke, Jokes.today(c))

            v.setOnClickPendingIntent(
                R.id.root,
                PendingIntent.getActivity(c, 1, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            )
            m.updateAppWidget(ids, v)
        }
    }
}
