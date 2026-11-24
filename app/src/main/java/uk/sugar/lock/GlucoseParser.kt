package uk.sugar.lock

/** A single reading pulled out of a CamAPS FX notification. */
data class Reading(
    val value: String,      // as displayed, e.g. "6.4" or "115"
    val unit: String,       // "mmol/L" or "mg/dL"
    val trend: String,      // arrow if CamAPS put one in the text, else ""
    val timeMs: Long,
)

/**
 * Pulls a glucose value out of whatever text CamAPS FX puts in its notification.
 * CamAPS doesn't publish its notification format, so this is deliberately forgiving:
 *   1. a number written with a unit ("6.4 mmol/L", "115 mg/dL")
 *   2. otherwise a decimal in the mmol/L range (2.0–33.3)
 *   3. otherwise a whole number in the mg/dL range (40–600)
 * It also understands CamAPS-style "LOW"/"HIGH" readings.
 */
object GlucoseParser {
    private val withUnit = Regex("""(\d{1,3}(?:[.,]\d)?)\s*(mmol/?l|mg/?dl)""", RegexOption.IGNORE_CASE)
    private val decimal = Regex("""(?<![\d.,])(\d{1,2}[.,]\d)(?![\d.,])""")
    private val whole = Regex("""(?<![\d.,:])(\d{2,3})(?![\d.,:%])""")
    private val arrows = Regex("""[↑↗→↘↓⇈⇊⬆⬇➚➘➔]+""")

    fun parse(texts: List<String>, timeMs: Long): Reading? {
        val all = texts.filter { it.isNotBlank() }.joinToString("  ")
        if (all.isBlank()) return null
        val trend = arrows.find(all)?.value ?: ""

        withUnit.find(all)?.let { m ->
            val unit = if (m.groupValues[2].lowercase().startsWith("mmol")) "mmol/L" else "mg/dL"
            return Reading(m.groupValues[1].replace(',', '.'), unit, trend, timeMs)
        }
        decimal.findAll(all).map { it.groupValues[1].replace(',', '.') }
            .firstOrNull { it.toDouble() in 2.0..33.3 }
            ?.let { return Reading(it, "mmol/L", trend, timeMs) }
        whole.findAll(all).map { it.groupValues[1] }
            .firstOrNull { it.toInt() in 40..600 }
            ?.let { return Reading(it, "mg/dL", trend, timeMs) }

        if (Regex("""\bLOW\b""", RegexOption.IGNORE_CASE).containsMatchIn(all)) return Reading("LOW", "", "", timeMs)
        if (Regex("""\bHIGH\b""", RegexOption.IGNORE_CASE).containsMatchIn(all)) return Reading("HIGH", "", "", timeMs)
        return null
    }
}
