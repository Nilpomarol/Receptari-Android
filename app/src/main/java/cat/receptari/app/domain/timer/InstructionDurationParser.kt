package cat.receptari.app.domain.timer

import java.util.Locale
import kotlin.math.roundToLong

/**
 * Finds an actionable duration in Catalan, Spanish or English instruction text.
 * Ambiguous ranges deliberately return null so the user must choose a value.
 */
object InstructionDurationParser {

    /**
     * True when the text mentions a cooking duration at all, including the ambiguous
     * ranges [parseMinutes] refuses to resolve. Callers use this to decide whether a
     * timer affordance belongs on a step; [parseMinutes] then supplies the prefill.
     */
    fun containsDuration(text: String): Boolean =
        RANGE.containsMatchIn(text) || parseMinutes(text) != null

    fun parseMinutes(text: String): Int? {
        if (RANGE.containsMatchIn(text)) return null

        val totalMinutes = DURATION.findAll(text).sumOf { match ->
            val amount = match.groupValues[1]
                .replace(',', '.')
                .toDoubleOrNull()
                ?: return@sumOf 0.0
            val unit = match.groupValues[2].lowercase(Locale.ROOT)
            amount * if (unit in HOUR_UNITS) 60.0 else 1.0
        }

        val rounded = totalMinutes.roundToLong()
        return rounded.takeIf { it in 1L..MAX_MINUTES }?.toInt()
    }

    private val HOUR_UNITS = setOf(
        "h", "hr", "hrs", "hora", "hores", "horas", "hour", "hours",
    )

    private const val UNIT_PATTERN =
        "h|hr|hrs|hora|hores|horas|hour|hours|min|minut|minuts|minuto|minutos|minute|minutes|mins"

    private val DURATION = Regex(
        pattern = """(?i)(\d+(?:[.,]\d+)?)\s*($UNIT_PATTERN)\b""",
    )
    private val RANGE = Regex(
        pattern = """(?i)\d+\s*[-–—]\s*\d+\s*($UNIT_PATTERN)\b""",
    )

    private const val MAX_MINUTES = 24L * 60L
}
