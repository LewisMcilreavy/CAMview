package uk.sugar.lock

import android.content.Context

/** The last few hours of readings (time, mmol/L), kept on the phone for the graph. */
object History {
    private const val KEY = "graph_history"
    const val KEEP_MS = 4 * 60 * 60 * 1000L

    data class Point(val timeMs: Long, val mmol: Double)

    fun load(c: Context): List<Point> =
        (c.getSharedPreferences("sugar", 0).getString(KEY, "") ?: "").split(';').mapNotNull {
            val p = it.split(':')
            if (p.size != 2) return@mapNotNull null
            val t = p[0].toLongOrNull() ?: return@mapNotNull null
            val v = p[1].toDoubleOrNull() ?: return@mapNotNull null
            Point(t, v)
        }

    fun add(c: Context, r: Reading) {
        val mmol = GlucoseParser.mmol(r) ?: return // LOW/HIGH aren't plotted
        val pts = load(c).filter { r.timeMs - it.timeMs in 0..KEEP_MS }.toMutableList()
        val last = pts.lastOrNull()
        // A repeat of the same reading within a minute isn't a new point.
        if (last != null && r.timeMs - last.timeMs < 50_000 && last.mmol == mmol) return
        pts += Point(r.timeMs, mmol)
        c.getSharedPreferences("sugar", 0).edit()
            .putString(KEY, pts.joinToString(";") { "${it.timeMs}:${"%.2f".format(java.util.Locale.ROOT, it.mmol)}" })
            .apply()
    }
}
