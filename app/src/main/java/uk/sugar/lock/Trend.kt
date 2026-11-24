package uk.sugar.lock

import android.content.Context

/**
 * Works out a trend arrow from recent readings when CamAPS doesn't give one as text.
 * Uses the rate of change over the last 5–20 minutes, so it works with both
 * 1-minute (Libre 3) and 5-minute (Dexcom) sensors.
 * Thresholds follow the usual CGM arrow bands, in mg/dL per minute.
 */
object Trend {
    private const val KEY = "history"
    private const val KEEP_MS = 25 * 60_000L

    fun update(c: Context, r: Reading): String {
        val v = r.value.toDoubleOrNull() ?: return ""
        val mgdl = if (r.unit == "mmol/L") v * 18.0 else v
        val p = c.getSharedPreferences("sugar", 0)

        val hist = (p.getString(KEY, "") ?: "").split(';').mapNotNull {
            val parts = it.split(':')
            if (parts.size == 2) parts[0].toLongOrNull()?.let { t -> t to (parts[1].toDoubleOrNull() ?: return@mapNotNull null) } else null
        }.filter { r.timeMs - it.first in 0..KEEP_MS }.toMutableList()

        // Same value re-posted within a minute: don't add a duplicate point.
        val last = hist.lastOrNull()
        if (last == null || r.timeMs - last.first >= 50_000 || last.second != mgdl) hist += r.timeMs to mgdl
        p.edit().putString(KEY, hist.joinToString(";") { "${it.first}:${it.second}" }).apply()

        // Compare with the oldest point that is 5–20 minutes back.
        val ref = hist.firstOrNull { r.timeMs - it.first in 5 * 60_000L..20 * 60_000L } ?: return ""
        val rate = (mgdl - ref.second) / ((r.timeMs - ref.first) / 60_000.0)
        return when {
            rate > 3 -> "⇈"
            rate > 2 -> "↑"
            rate > 1 -> "↗"
            rate >= -1 -> "→"
            rate >= -2 -> "↘"
            rate >= -3 -> "↓"
            else -> "⇊"
        }
    }
}
