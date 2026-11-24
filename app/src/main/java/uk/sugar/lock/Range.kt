package uk.sugar.lock

import android.content.Context
import java.time.LocalDate

/** Target range (stored in mmol/L) and today's time-in-range, time-weighted. */
object Range {
    enum class Cls { LOW, IN, HIGH }

    private fun p(c: Context) = c.getSharedPreferences("sugar", 0)

    fun lowMmol(c: Context) = p(c).getFloat("range_low", 3.9f).toDouble()
    fun highMmol(c: Context) = p(c).getFloat("range_high", 10.0f).toDouble()
    fun set(c: Context, lowMmol: Double, highMmol: Double) =
        p(c).edit().putFloat("range_low", lowMmol.toFloat()).putFloat("range_high", highMmol.toFloat()).apply()

    fun classify(c: Context, r: Reading): Cls {
        if (r.value == "LOW") return Cls.LOW
        if (r.value == "HIGH") return Cls.HIGH
        val v = r.value.toDoubleOrNull() ?: return Cls.IN
        val mmol = if (r.unit == "mg/dL") v / 18.0 else v
        return when {
            mmol < lowMmol(c) -> Cls.LOW
            mmol > highMmol(c) -> Cls.HIGH
            else -> Cls.IN
        }
    }

    /**
     * Credits the time since the previous reading to the range that reading was in
     * (gaps over 15 min aren't counted). Resets at midnight.
     */
    fun record(c: Context, r: Reading) {
        val prefs = p(c)
        val today = LocalDate.now().toString()
        val e = prefs.edit()
        if (prefs.getString("tir_date", "") != today) {
            e.putString("tir_date", today).putLong("tir_in", 0).putLong("tir_low", 0).putLong("tir_high", 0)
        } else {
            val lastT = prefs.getLong("tir_last_t", 0)
            val gap = r.timeMs - lastT
            if (lastT > 0 && gap in 1..15 * 60_000L) {
                val key = when (prefs.getString("tir_last_cls", "IN")) {
                    "LOW" -> "tir_low"; "HIGH" -> "tir_high"; else -> "tir_in"
                }
                e.putLong(key, prefs.getLong(key, 0) + gap)
            }
        }
        e.putLong("tir_last_t", r.timeMs).putString("tir_last_cls", classify(c, r).name).apply()
    }

    /** Percent in range today, or null until there's at least 30 minutes of data. */
    fun percentToday(c: Context): Int? {
        val prefs = p(c)
        if (prefs.getString("tir_date", "") != LocalDate.now().toString()) return null
        val i = prefs.getLong("tir_in", 0)
        val total = i + prefs.getLong("tir_low", 0) + prefs.getLong("tir_high", 0)
        if (total < 30 * 60_000L) return null
        return Math.round(i * 100.0 / total).toInt()
    }
}
