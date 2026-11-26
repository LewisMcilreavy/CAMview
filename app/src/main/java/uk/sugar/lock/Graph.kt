package uk.sugar.lock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Draws the last few hours of readings as a small line graph with the target range shaded. */
object Graph {
    data class Style(val line: Int, val band: Int, val grid: Int, val dot: Int)

    /** White on the coloured widget. */
    val WIDGET = Style(Color.WHITE, Color.argb(46, 255, 255, 255), Color.argb(70, 255, 255, 255), Color.WHITE)

    /** Dark on the app's light screen. */
    val APP = Style(Color.rgb(19, 32, 58), Color.argb(40, 46, 125, 50), Color.argb(60, 19, 32, 58), Color.rgb(232, 80, 74))

    fun render(c: Context, w: Int, h: Int, st: Style, spanMs: Long = 3 * 60 * 60 * 1000L): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val now = System.currentTimeMillis()
        val pts = History.load(c).filter { now - it.timeMs in 0..spanMs }

        val low = Range.lowMmol(c)
        val high = Range.highMmol(c)
        val maxV = maxOf(15.0, (pts.maxOfOrNull { it.mmol } ?: 0.0) + 1.0)
        val minV = minOf(2.5, (pts.minOfOrNull { it.mmol } ?: 99.0) - 0.5).coerceAtLeast(0.0)
        val pad = h * 0.08f
        fun x(t: Long) = ((t - (now - spanMs)).toFloat() / spanMs) * (w - pad * 2) + pad
        fun y(v: Double) = (h - pad) - ((v - minV) / (maxV - minV)).toFloat() * (h - pad * 2)

        // Target range band.
        val band = Paint().apply { color = st.band }
        cv.drawRoundRect(RectF(pad, y(high), w - pad, y(low)), h * 0.04f, h * 0.04f, band)

        // Hour markers.
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = st.grid; strokeWidth = h * 0.012f; pathEffect = DashPathEffect(floatArrayOf(h * 0.03f, h * 0.04f), 0f)
        }
        var t = now - 60 * 60 * 1000L
        while (t > now - spanMs) { cv.drawLine(x(t), pad, x(t), h - pad, grid); t -= 60 * 60 * 1000L }

        if (pts.isEmpty()) return bmp

        // Line, broken where there's a gap of over 15 minutes.
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = st.line; style = Paint.Style.STROKE; strokeWidth = h * 0.035f
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val path = Path()
        pts.forEachIndexed { i, p ->
            if (i == 0 || p.timeMs - pts[i - 1].timeMs > 15 * 60_000L) path.moveTo(x(p.timeMs), y(p.mmol))
            else path.lineTo(x(p.timeMs), y(p.mmol))
        }
        cv.drawPath(path, line)

        // Latest reading.
        val last = pts.last()
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = st.dot }
        cv.drawCircle(x(last.timeMs), y(last.mmol), h * 0.055f, dot)
        return bmp
    }
}
