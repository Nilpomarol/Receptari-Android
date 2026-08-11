package cat.receptari.app.domain.parser

import cat.receptari.app.domain.scaling.QuantityFormatter
import java.util.Locale
import kotlin.math.round

/**
 * Tidies units into the form a European kitchen actually writes them in: symbols instead of
 * words, metric instead of imperial.
 *
 * Three rules govern what this is allowed to do, and they are the whole design:
 *
 * 1. **It rewrites the measurement, never the rest of the line.** Only the leading
 *    `quantity [unit]` span is replaced; the connector, the ingredient name, the note and
 *    the punctuation after it are copied through untouched. This is the same surgical
 *    substitution scaling uses, and for the same reason — rebuilding a line from parsed
 *    fields produces broken Catalan ("3 grans all" for "3 grans d'all").
 *
 * 2. **Every conversion is exact arithmetic.** Ounces and pounds are mass, cups are volume,
 *    Fahrenheit is a formula. What is deliberately *absent* is any volume-to-mass
 *    conversion: a cup of flour is 120 g, of sugar 200 g, of butter 227 g. Guessing that
 *    from an ingredient name would silently produce a recipe that fails in the oven, and a
 *    wrong number that looks tidy is worse than a right one that looks foreign.
 *
 * 3. **Abbreviating is not translating.** A spoon measure is a perfectly good European unit,
 *    so it is shortened within the language it was written in — `cullerades` to `cs`,
 *    `cucharadas` to `cda`, `tablespoons` to `tbsp` — and never converted to millilitres or
 *    turned into another language's word. Translation is Phase 5's problem, not this one's.
 *
 * Anything this does not recognise is returned unchanged. Leaving a line alone is always a
 * valid outcome.
 */
object UnitNormalizer {

    /** Units already written the way they should be shown. Identical in ca/es/en. */
    private val METRIC_SYMBOLS = setOf("g", "kg", "mg", "ml", "l")

    private data class Conversion(val unit: String, val factor: Double)

    /**
     * Mass to mass, volume to volume. Cups are American rather than European, so they become
     * millilitres; spoons are not here on purpose (see rule 3).
     */
    private val CONVERSIONS = mapOf(
        "oz" to Conversion("g", 28.349523125),
        "lb" to Conversion("g", 453.59237),
        "cup" to Conversion("ml", 236.5882365),
    )

    /**
     * Spoon spellings collapsed onto the shorthand of their own language. Every value here
     * is a spelling [UnitLexicon] already recognises, so a normalised line still parses.
     */
    private val SPOON_ABBREVIATIONS = mapOf(
        "tablespoon" to "tbsp", "tablespoons" to "tbsp", "tbs" to "tbsp", "tbsp" to "tbsp",
        "teaspoon" to "tsp", "teaspoons" to "tsp", "tsp" to "tsp",
        "cullerada" to "cs", "cullerades" to "cs", "cs" to "cs",
        "culleradeta" to "cp", "culleradetes" to "cp", "cp" to "cp",
        "cucharada" to "cda", "cucharadas" to "cda", "cda" to "cda", "cdas" to "cda",
        "cucharadita" to "cdta", "cucharaditas" to "cdta", "cdta" to "cdta", "cdtas" to "cdta",
    )

    /**
     * Rewrites the measurement at the start of an ingredient line.
     *
     * Returns [text] unchanged when there is no quantity, no unit, nothing worth changing,
     * or a unit this does not convert — "2 grans d'all" and "sal al gust" both come back
     * exactly as they went in.
     */
    fun normalizeIngredient(text: String, locale: Locale = Locale.getDefault()): String {
        val measurement = IngredientParser.leadingMeasurement(text) ?: return text
        val unit = measurement.unit ?: return text
        val sourceToken = measurement.unitText ?: return text

        val target = targetFor(unit, sourceToken) ?: return text

        // Nothing to gain from rewriting a line that is already correct — and not rewriting
        // it keeps the stored text byte-identical to what the source wrote.
        if (target.factor == 1.0 && target.unit == sourceToken) return text

        val quantity = QuantityFormatter.formatRange(
            min = convert(measurement.quantity, target),
            max = measurement.quantityMax?.let { convert(it, target) },
            unit = target.unit,
            locale = locale,
        )

        return text.take(measurement.range.first) +
            "$quantity ${target.unit}" +
            text.substring(measurement.range.last + 1)
    }

    /**
     * Converts Fahrenheit oven temperatures inside a step to Celsius.
     *
     * Steps are prose, so this is the one thing worth touching in them and the only pattern
     * unambiguous enough to be safe. Rounded to 5 °C because ovens are dialled, not
     * calibrated: 350 °F is 176.7 °C, which every European recipe writes as 175 °C.
     */
    fun normalizeInstruction(text: String): String =
        FAHRENHEIT.replace(text) { match ->
            val fahrenheit = match.groupValues[1].toDoubleOrNull()
                ?: return@replace match.value
            val celsius = round((fahrenheit - 32) * 5 / 9 / 5) * 5
            "${celsius.toLong()} °C"
        }

    private val FAHRENHEIT =
        Regex("""(\d{2,3})\s*(?:°\s*F|℉|degrees\s+F(?:ahrenheit)?|º\s*F)\b""", RegexOption.IGNORE_CASE)

    private fun targetFor(unit: String, sourceToken: String): Conversion? {
        CONVERSIONS[unit]?.let { return it }
        if (unit in METRIC_SYMBOLS) return Conversion(unit, 1.0)
        SPOON_ABBREVIATIONS[sourceToken.lowercase(Locale.ROOT)]?.let {
            return Conversion(it, 1.0)
        }
        return null
    }

    private fun convert(value: Double, target: Conversion): Double =
        if (target.factor == 1.0) value else value * target.factor
}
