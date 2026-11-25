package uk.sugar.lock

/** A single reading pulled out of a CamAPS FX notification. */
data class Reading(
    val value: String,      // as displayed, e.g. "6.4" or "115"
    val unit: String,       // "mmol/L" or "mg/dL"
    val trend: String,      // arrow if CamAPS put one in the text, else ""
    val timeMs: Long,
)

/**
 * Pulls a glucose value out of the text CamAPS FX puts in its notifications.
 *
 * CamAPS also posts notifications about insulin (e.g. "Bolus 2.4 U"), carbs and the
 * pump, so this is careful about what it accepts:
 *   1. a number written with a glucose unit ("6.4 mmol/L", "115 mg/dL") always wins
 *   2. otherwise, any notification that talks about insulin, carbs or the pump is ignored
 *   3. numbers with other units (2.4 U, 30 g, 45%, 2 h, 10 min) and clock times are
 *      removed before looking for a bare number
 *   4. a bare decimal in the mmol/L range (2.0–33.3), or a whole number in the mg/dL
 *      range (40–600)
 * CamAPSListener also rejects values that jump faster than glucose can change.
 */
object GlucoseParser {
    private val withUnit = Regex("""(\d{1,3}(?:[.,]\d)?)\s*(mmol/?l|mg/?dl)""", RegexOption.IGNORE_CASE)

    /** Words that mean the notification is about something other than glucose. */
    private val notGlucose = Regex(
        """\b(bolus\w*|insulin|units?|iu|delivered|deliver\w*|iob|carb\w*|cob|pump|reservoir|""" +
            """cartridge|cannula|battery|basal|temp\w*|occlusion|prime\w*|refill\w*|meal|food)\b""",
        RegexOption.IGNORE_CASE
    )

    /** Numbers that carry a non-glucose unit, plus clock times and dates. */
    private val otherQuantities = Regex(
        """\d+(?:[.,]\d+)?\s*(?:u\b|units?\b|iu\b|g\b|grams?\b|%|h\b|hrs?\b|hours?\b|mins?\b|minutes?\b|mmol/mol\b)""" +
            """|\d{1,2}:\d{2}(?::\d{2})?|\d{1,2}/\d{1,2}(?:/\d{2,4})?""",
        RegexOption.IGNORE_CASE
    )

    private val decimal = Regex("""(?<![\d.,])(\d{1,2}[.,]\d)(?![\d.,])""")
    private val whole = Regex("""(?<![\d.,:])(\d{2,3})(?![\d.,:%])""")
    private val arrows = Regex("""[↑↗→↘↓⇈⇊⬆⬇➚➘➔]+""")

    fun parse(texts: List<String>, timeMs: Long): Reading? {
        val all = texts.filter { it.isNotBlank() }.joinToString("  ")
        if (all.isBlank()) return null
        val trend = arrows.find(all)?.value ?: ""

        // 1. Explicit glucose unit: trust it.
        withUnit.find(all)?.let { m ->
            val unit = if (m.groupValues[2].lowercase().startsWith("mmol")) "mmol/L" else "mg/dL"
            return Reading(m.groupValues[1].replace(',', '.'), unit, trend, timeMs)
        }

        // 2. Anything about insulin, carbs or the pump is not a glucose reading.
        if (notGlucose.containsMatchIn(all)) return null

        // 3. Remove numbers that are clearly something else.
        val cleaned = otherQuantities.replace(all, " ")

        // 4. Bare numbers in a plausible glucose range.
        decimal.findAll(cleaned).map { it.groupValues[1].replace(',', '.') }
            .firstOrNull { it.toDouble() in 2.0..33.3 }
            ?.let { return Reading(it, "mmol/L", trend, timeMs) }
        whole.findAll(cleaned).map { it.groupValues[1] }
            .firstOrNull { it.toInt() in 40..600 }
            ?.let { return Reading(it, "mg/dL", trend, timeMs) }

        // Sensor out-of-range readings, written in capitals on their own.
        if (Regex("""\bLOW\b""").containsMatchIn(cleaned)) return Reading("LOW", "", "", timeMs)
        if (Regex("""\bHIGH\b""").containsMatchIn(cleaned)) return Reading("HIGH", "", "", timeMs)
        return null
    }

    /** Value in mmol/L, or null for LOW/HIGH. */
    fun mmol(r: Reading): Double? {
        val v = r.value.toDoubleOrNull() ?: return null
        return if (r.unit == "mg/dL") v / 18.0 else v
    }
}
