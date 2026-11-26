package uk.sugar.lock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon

/**
 * Posts one silent, ongoing notification with VISIBILITY_PUBLIC so the full
 * reading shows on the lock screen (and the number itself is drawn as the
 * status-bar icon).
 */
object LockScreenNotifier {
    private const val CHANNEL = "glucose"
    private const val ID = 42
    const val STALE_MS = 15 * 60 * 1000L

    fun show(c: Context, r: Reading?) {
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Glucose on lock screen", NotificationManager.IMPORTANCE_LOW).apply {
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }
            )
        }

        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val text: String
        val iconText: String
        if (r == null) {
            title = "Waiting for CamAPS FX…"
            text = "No reading yet"
            iconText = "--"
        } else {
            val ageMs = System.currentTimeMillis() - r.timeMs
            val mins = (ageMs / 60000).coerceAtLeast(0)
            val stale = ageMs > STALE_MS
            title = listOf(r.value, r.unit, r.trend).filter { it.isNotEmpty() }.joinToString(" ")
            text = when {
                stale -> "⚠ OLD READING – $mins min ago. Check CamAPS FX."
                mins == 0L -> "Just now"
                else -> "$mins min ago"
            }
            iconText = if (stale) "?" else r.value
        }

        val n = Notification.Builder(c, CHANNEL)
            .setSmallIcon(numberIcon(iconText, if (r != null && iconText == r.value) r.trend else ""))
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_STATUS)
            .setShowWhen(r != null)
            .setWhen(r?.timeMs ?: System.currentTimeMillis())
            .setContentIntent(open)
            // Android 16 "Live Update": shown in Samsung's Now Bar and as a status-bar chip.
            .addExtras(android.os.Bundle().apply {
                putBoolean("android.requestPromotedOngoing", true)
                putString("android.shortCriticalText", if (r != null && iconText == r.value) (iconText + r.trend) else iconText)
            })
            .build()
        nm.notify(ID, n)
        GlucoseWidget.refresh(c)
    }

    /**
     * Draws the number, plus the trend arrow to its right, as a white-on-transparent
     * bitmap for the status bar.
     */
    private fun numberIcon(s: String, trend: String): Icon {
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val angle = when (trend) {
            "⇈", "↑", "⬆" -> -90f
            "↗", "➚" -> -45f
            "→", "➔" -> 0f
            "↘", "➘" -> 45f
            "↓", "⇊", "⬇" -> 90f
            else -> null
        }
        val textW = if (angle != null) size * 0.70f else size.toFloat()
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            textSize = size * 0.9f
        }
        while (p.measureText(s) > textW && p.textSize > 10) p.textSize -= 2
        val y = size / 2f - (p.descent() + p.ascent()) / 2
        cv.drawText(s, textW / 2f, y, p)

        if (angle != null) {
            // Arrow pointing right, rotated to the trend direction.
            val cx = size * 0.86f
            val cy = size / 2f
            val len = size * 0.13f
            val arrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = size * 0.07f
                strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            }
            cv.save()
            cv.rotate(angle, cx, cy)
            val path = android.graphics.Path().apply {
                moveTo(cx - len, cy); lineTo(cx + len, cy)
                moveTo(cx + len * 0.2f, cy - len * 0.8f); lineTo(cx + len, cy); lineTo(cx + len * 0.2f, cy + len * 0.8f)
            }
            cv.drawPath(path, arrow)
            cv.restore()
        }
        return Icon.createWithBitmap(bmp)
    }
}
