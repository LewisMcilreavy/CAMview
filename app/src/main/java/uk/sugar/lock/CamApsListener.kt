package uk.sugar.lock

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView

/**
 * Watches for CamAPS FX's notification and copies the glucose value out of it.
 * It only reads; it never dismisses or changes CamAPS's notifications.
 */
class CamApsListener : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            // Refresh "x min ago" / stale warning once a minute.
            LockScreenNotifier.show(this@CamApsListener, Store.load(this@CamApsListener))
            handler.postDelayed(this, 60_000)
        }
    }

    override fun onListenerConnected() {
        handler.removeCallbacks(tick)
        handler.post(tick)
        // Pick up a reading that is already showing.
        try { activeNotifications?.forEach { onNotificationPosted(it) } } catch (_: Exception) {}
    }

    override fun onListenerDisconnected() {
        handler.removeCallbacks(tick)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!sbn.packageName.startsWith(CAMAPS_PREFIX)) return
        val texts = collectTexts(sbn.notification)
        val raw = texts.joinToString(" | ")
        var reading = GlucoseParser.parse(texts, System.currentTimeMillis())
        if (reading != null && !plausible(reading)) reading = null
        if (reading != null) {
            val calc = Trend.update(this, reading)
            if (reading.trend.isEmpty()) reading = reading.copy(trend = calc)
            Range.record(this, reading)
            History.add(this, reading)
            Store.save(this, reading, raw)
            LockScreenNotifier.show(this, reading)
        } else {
            Store.saveRaw(this, raw) // shown in the app, to help fix parsing
        }
    }

    /**
     * Glucose can't change faster than a few tenths of a mmol/L per minute, so a value
     * that jumps further than that from the last reading (within 15 minutes) is almost
     * certainly a different number from a CamAPS notification, such as an insulin dose.
     * Allows 0.5 mmol/L per minute, and at least 1.0 mmol/L, which is far more than any
     * real change. After 15 minutes without an accepted reading, anything is accepted.
     */
    private fun plausible(r: Reading): Boolean {
        val prev = Store.load(this) ?: return true
        val gapMin = (r.timeMs - prev.timeMs) / 60_000.0
        if (gapMin < 0 || gapMin > 15) return true
        val now = GlucoseParser.mmol(r)
        val before = GlucoseParser.mmol(prev)
        if (now == null || before == null) return true // LOW/HIGH
        val allowed = maxOf(1.0, 0.5 * gapMin)
        return kotlin.math.abs(now - before) <= allowed
    }

    private fun collectTexts(n: Notification): List<String> {
        val e = n.extras
        val out = mutableListOf<String>()
        listOf(
            Notification.EXTRA_TITLE, Notification.EXTRA_TITLE_BIG, Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT, Notification.EXTRA_SUB_TEXT, Notification.EXTRA_INFO_TEXT,
            Notification.EXTRA_SUMMARY_TEXT,
        ).forEach { k -> e.getCharSequence(k)?.toString()?.let(out::add) }
        n.tickerText?.toString()?.let(out::add)

        // CamAPS may draw its notification with a custom layout; read its TextViews too.
        if (out.none { GlucoseParser.parse(listOf(it), 0) != null }) {
            @Suppress("DEPRECATION")
            listOfNotNull(n.contentView, n.bigContentView).forEach { rv -> out += textsFrom(rv) }
        }
        return out
    }

    private fun textsFrom(rv: RemoteViews): List<String> = try {
        val v = rv.apply(applicationContext, FrameLayout(applicationContext))
        val found = mutableListOf<String>()
        fun walk(view: View) {
            if (view is TextView && view.visibility == View.VISIBLE) found += view.text.toString()
            if (view is ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i))
        }
        walk(v)
        found
    } catch (_: Exception) {
        emptyList()
    }

    companion object {
        /** Covers the mmol/L, mg/dL and HX variants (com.camdiab.fx_alert.*). */
        const val CAMAPS_PREFIX = "com.camdiab.fx_alert"
    }
}
