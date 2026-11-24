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
        if (reading != null) {
            val calc = Trend.update(this, reading)
            if (reading.trend.isEmpty()) reading = reading.copy(trend = calc)
            Store.save(this, reading, raw)
            LockScreenNotifier.show(this, reading)
        } else {
            Store.saveRaw(this, raw) // shown in the app, to help fix parsing
        }
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
