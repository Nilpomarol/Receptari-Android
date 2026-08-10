package cat.receptari.app.domain.scaling

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

/**
 * Renders a numeric quantity the way a person writes it on a recipe card.
 *
 * Scaling produces values like `0.6666666666666666` and `133.33333333333334`; showing
 * those would make the serving multiplier feel broken, so anything close to a familiar
 * fraction becomes a glyph and everything else is capped at two decimals.
 */
object QuantityFormatter {

    /**
     * How close a value must be to a fraction to be shown as one. Loose enough that a
     * quantity parsed from "1/3" (often stored as 0.33) still reads as ⅓, tight enough
     * that 0.6 does not masquerade as ⅝.
     */
    private const val EPSILON = 0.005

    /**
     * Fractions are only used below this value. "1 ⅓ cups" is how a recipe is written;
     * "133 ⅓ g" is not — nobody weighs a third of a gram. Small quantities are spoons,
     * cups and whole items; large ones are metric weights and volumes.
     */
    private const val MAX_FRACTIONAL_VALUE = 10.0

    private val FRACTIONS = listOf(
        0.125 to "⅛",
        0.25 to "¼",
        1.0 / 3 to "⅓",
        0.375 to "⅜",
        0.5 to "½",
        0.625 to "⅝",
        2.0 / 3 to "⅔",
        0.75 to "¾",
        0.875 to "⅞",
    )

    private const val EN_DASH = "–"

    fun format(value: Double, locale: Locale = Locale.getDefault()): String {
        if (!value.isFinite()) return ""

        val sign = if (value < 0) "-" else ""
        val magnitude = abs(value)
        val whole = floor(magnitude)
        val fraction = magnitude - whole

        if (fraction < EPSILON) return sign + whole.toLong().toString()
        if (1.0 - fraction < EPSILON) return sign + (whole.toLong() + 1).toString()

        val glyph = if (magnitude < MAX_FRACTIONAL_VALUE) {
            FRACTIONS.firstOrNull { abs(fraction - it.first) < EPSILON }?.second
        } else {
            null
        }
        if (glyph != null) {
            val wholePart = whole.toLong()
            return sign + if (wholePart == 0L) glyph else "$wholePart $glyph"
        }

        return sign + decimalFormat(locale).format(magnitude)
    }

    /**
     * Renders `1–2 onions`. A null [max] means this was never a range, so it renders as a
     * plain quantity rather than as `1–1`.
     */
    fun formatRange(min: Double, max: Double?, locale: Locale = Locale.getDefault()): String {
        val minText = format(min, locale)
        if (max == null || abs(max - min) < EPSILON) return minText
        return minText + EN_DASH + format(max, locale)
    }

    /**
     * Renders a scaled quantity at a precision that makes sense for what is being measured.
     *
     * Scaling produces arithmetically correct nonsense: 10.67 egg yolks, 266.67 g of sugar.
     * Nobody cracks two thirds of a yolk or weighs sugar to the centigram, so how much
     * precision to keep depends on the unit.
     */
    fun formatQuantity(
        value: Double,
        unit: String?,
        locale: Locale = Locale.getDefault(),
    ): String = format(roundForDisplay(value, unit), locale)

    fun formatRange(
        min: Double,
        max: Double?,
        unit: String?,
        locale: Locale = Locale.getDefault(),
    ): String = formatRange(
        min = roundForDisplay(min, unit),
        max = max?.let { roundForDisplay(it, unit) },
        locale = locale,
    )

    private fun roundForDisplay(value: Double, unit: String?): Double = when {
        // No unit means a countable thing — eggs, onions, bay leaves. Halves are plausible
        // in small numbers ("1 ½ cebes"); past a handful they are not, so round to whole.
        unit == null ->
            if (value >= COUNTABLE_WHOLE_THRESHOLD) round(value) else value

        // Grams and millilitres are the units that grow large and get silly decimals. Round
        // to what a kitchen scale can actually show.
        unit in SCALE_MEASURED_UNITS -> when {
            value >= 100 -> round(value / 5) * 5
            value >= 10 -> round(value)
            else -> value
        }

        // Spoons, cups, cloves, pinches: halves and thirds are how these are really written,
        // and the numbers stay small. Leave them alone.
        else -> value
    }

    private val SCALE_MEASURED_UNITS = setOf("g", "ml")

    private const val COUNTABLE_WHOLE_THRESHOLD = 5.0

    // DecimalFormat is not thread-safe, so it is built per call rather than cached.
    private fun decimalFormat(locale: Locale) =
        DecimalFormat("0.##", DecimalFormatSymbols(locale))
}
