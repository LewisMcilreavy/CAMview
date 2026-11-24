package uk.sugar.lock

import android.content.Context

/** Keeps the last reading so the lock-screen card survives restarts. */
object Store {
    private const val P = "sugar"

    fun save(c: Context, r: Reading, raw: String) = c.getSharedPreferences(P, 0).edit()
        .putString("v", r.value).putString("u", r.unit).putString("t", r.trend)
        .putLong("time", r.timeMs).putString("raw", raw).apply()

    fun saveRaw(c: Context, raw: String) =
        c.getSharedPreferences(P, 0).edit().putString("raw", raw).apply()

    fun load(c: Context): Reading? {
        val p = c.getSharedPreferences(P, 0)
        val v = p.getString("v", null) ?: return null
        return Reading(v, p.getString("u", "")!!, p.getString("t", "")!!, p.getLong("time", 0))
    }

    fun raw(c: Context): String = c.getSharedPreferences(P, 0).getString("raw", "") ?: ""
}
