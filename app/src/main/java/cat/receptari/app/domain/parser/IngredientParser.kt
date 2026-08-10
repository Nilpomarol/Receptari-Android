package cat.receptari.app.domain.parser

/**
 * Turns a free-text ingredient line into structured fields, without ever losing the line.
 *
 * The contract, from PRD §3.2 and §14:
 *  - `2 tbsp olive oil` becomes quantity 2, unit tbsp, name "olive oil".
 *  - `Sal al gust` stays exactly that, with every structured field null.
 *  - `originalText` always survives, whatever happens.
 *
 * There is no failure mode. A line that cannot be understood is returned unstructured
 * rather than rejected, because dropping an ingredient the parser did not recognise would
 * be data loss.
 *
 * This is rule-based on purpose. It runs offline, instantly, with no API key, on every
 * ingredient the user types — the AI extractors in `data/remote` feed *into* this, they do
 * not replace it.
 */
object IngredientParser {

    private val UNICODE_FRACTIONS = mapOf(
        '½' to 0.5, '⅓' to 1.0 / 3, '⅔' to 2.0 / 3,
        '¼' to 0.25, '¾' to 0.75,
        '⅕' to 0.2, '⅖' to 0.4, '⅗' to 0.6, '⅘' to 0.8,
        '⅙' to 1.0 / 6, '⅚' to 5.0 / 6,
        '⅛' to 0.125, '⅜' to 0.375, '⅝' to 0.625, '⅞' to 0.875,
    )

    private const val FRACTION_GLYPHS = "½⅓⅔¼¾⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞"

    /** `1.5`, `1,5`, `3` — a decimal comma is normal in Catalan and Spanish sources. */
    private const val DECIMAL = """\d+(?:[.,]\d+)?"""

    /** `1/2` or a single glyph such as `½`. */
    private const val FRACTION = """(?:\d+\s*/\s*\d+|[$FRACTION_GLYPHS])"""

    private val NOTE_IN_PARENTHESES = Regex("""\(([^)]*)\)""")

    /** `1-2`, `1–2`, `1 a 2`, `2 to 3`. The separator must be a token, not just a space. */
    private val RANGE = Regex("""^($DECIMAL)\s*(?:-|–|—|\ba\b|\bto\b)\s*($DECIMAL)""")

    /** `1 1/2`, `1 ½` — a whole part followed by a fraction. */
    private val MIXED = Regex("""^(\d+)\s+($FRACTION)""")

    private val FRACTION_ONLY = Regex("""^($FRACTION)""")

    private val PLAIN = Regex("""^($DECIMAL)""")

    /**
     * Words that link a unit to the ingredient it measures. Stripped so `200 ml de nata`
     * yields the name "nata" rather than "de nata".
     */
    private val CONNECTORS = listOf(
        "de la ", "de les ", "de los ", "de las ", "dels ", "del ", "de ", "d'", "of ",
    )

    fun parse(rawText: String): ParsedIngredient? {
        val originalText = rawText.trim()
        if (originalText.isEmpty()) return null

        // Pull the note out first, so a leading parenthetical cannot derail quantity parsing.
        val noteMatch = NOTE_IN_PARENTHESES.find(originalText)
        val note = noteMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
        val withoutNote = if (noteMatch != null) {
            originalText.removeRange(noteMatch.range).replace(Regex("\\s+"), " ").trim()
        } else {
            originalText
        }

        val quantity = parseQuantity(withoutNote)
        var remainder = withoutNote.substring(quantity?.consumedChars ?: 0).trimStart()

        // A unit only counts when it sits immediately after a quantity. Without that rule
        // "2 garlic cloves" would be read as 2 cloves of something called "garlic".
        var unit: String? = null
        if (quantity != null && remainder.isNotEmpty()) {
            val token = remainder.takeWhile { !it.isWhitespace() }
            val canonical = UnitLexicon.canonicalize(token)
            if (canonical != null) {
                unit = canonical
                remainder = remainder.removePrefix(token).trimStart()
            }
        }

        if (unit != null) {
            remainder = stripConnector(remainder)
        }

        val name = remainder.trim().takeIf { it.isNotEmpty() }

        return ParsedIngredient(
            quantity = quantity?.min,
            quantityMax = quantity?.max,
            unit = unit,
            name = name,
            note = note,
            originalText = originalText,
        )
    }

    /** Parses a whole ingredient list, skipping blank lines. */
    fun parseAll(lines: List<String>): List<ParsedIngredient> = lines.mapNotNull(::parse)

    /**
     * The character range the leading quantity occupies in [text], or null when the line
     * does not start with one.
     *
     * Exists so a scaled quantity can be swapped into the original line in place, keeping
     * every word around it — the connector in "2 grans **d'**all", the plural in "1–2
     * **fulles**" — exactly as it was written. Rebuilding the line from parsed fields
     * cannot do that in Catalan or Spanish.
     */
    fun leadingQuantityRange(text: String): IntRange? {
        val trimmed = text.trimStart()
        val offset = text.length - trimmed.length
        val quantity = parseQuantity(trimmed) ?: return null
        return offset until (offset + quantity.consumedChars)
    }

    private data class Quantity(val min: Double, val max: Double?, val consumedChars: Int)

    private fun parseQuantity(text: String): Quantity? {
        RANGE.find(text)?.let { match ->
            val min = parseNumber(match.groupValues[1]) ?: return@let
            val max = parseNumber(match.groupValues[2]) ?: return@let
            return Quantity(min, max, match.value.length)
        }

        MIXED.find(text)?.let { match ->
            val whole = match.groupValues[1].toDoubleOrNull() ?: return@let
            val fraction = parseFraction(match.groupValues[2]) ?: return@let
            return Quantity(whole + fraction, null, match.value.length)
        }

        FRACTION_ONLY.find(text)?.let { match ->
            // A fraction-shaped token that will not resolve — "1/0" — is malformed. Falling
            // through to PLAIN would silently read it as its numerator, which is a wrong
            // number rather than an absent one.
            val value = parseFraction(match.groupValues[1])
            return value?.let { Quantity(it, null, match.value.length) }
        }

        PLAIN.find(text)?.let { match ->
            val value = parseNumber(match.groupValues[1]) ?: return@let
            return Quantity(value, null, match.value.length)
        }

        return null
    }

    private fun parseNumber(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

    private fun parseFraction(text: String): Double? {
        val trimmed = text.trim()
        if (trimmed.length == 1) {
            UNICODE_FRACTIONS[trimmed[0]]?.let { return it }
        }
        val parts = trimmed.split("/")
        if (parts.size != 2) return null
        val numerator = parts[0].trim().toDoubleOrNull() ?: return null
        val denominator = parts[1].trim().toDoubleOrNull() ?: return null
        if (denominator == 0.0) return null
        return numerator / denominator
    }

    private fun stripConnector(text: String): String {
        val lowered = text.lowercase()
        CONNECTORS.forEach { connector ->
            if (lowered.startsWith(connector)) {
                return text.substring(connector.length).trimStart()
            }
        }
        return text
    }
}
