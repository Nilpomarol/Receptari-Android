package cat.receptari.app.domain.parser

import java.text.Normalizer
import java.util.Locale

/**
 * Maps the many ways a unit gets written — across Catalan, Spanish and English, singular
 * and plural, abbreviated and spelled out — onto one canonical token.
 *
 * All three languages are needed from the start: recipes get imported in whatever language
 * the source was written in, long before anything is translated (ADR-004).
 *
 * The canonical tokens are internal identifiers, not display text. Rendering "tbsp" as
 * "cullerada" is a UI concern and belongs in string resources.
 */
object UnitLexicon {

    private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")

    private val SPELLINGS: Map<String, List<String>> = mapOf(
        "g" to listOf(
            "g", "gr", "grs", "gram", "grams", "grame", "grames",
            "gramo", "gramos",
        ),
        "kg" to listOf(
            "kg", "kgs", "kilo", "kilos", "kilogram", "kilograms", "kilogramme",
            "quilo", "quilos", "quilogram", "quilograms", "kilogramo", "kilogramos",
        ),
        "mg" to listOf("mg", "milligram", "milligrams", "mil·ligram", "miligramo", "miligramos"),
        "ml" to listOf(
            "ml", "cc", "milliliter", "milliliters", "millilitre", "millilitres",
            "mil·lilitre", "mil·lilitres", "mililitro", "mililitros",
        ),
        "l" to listOf("l", "lt", "liter", "liters", "litre", "litres", "litro", "litros"),
        "tbsp" to listOf(
            "tbsp", "tbs", "tablespoon", "tablespoons",
            "cullerada", "cullerades", "cs",
            "cucharada", "cucharadas", "cda", "cdas",
        ),
        "tsp" to listOf(
            "tsp", "teaspoon", "teaspoons",
            "culleradeta", "culleradetes", "cp",
            "cucharadita", "cucharaditas", "cdta", "cdtas",
        ),
        "cup" to listOf("cup", "cups", "tassa", "tasses", "taza", "tazas"),
        "oz" to listOf("oz", "ounce", "ounces", "unca", "unces", "onza", "onzas"),
        "lb" to listOf("lb", "lbs", "pound", "pounds", "lliura", "lliures", "libra", "libras"),
        "pinch" to listOf("pinch", "pinches", "pessic", "pessics", "pizca", "pizcas"),
        "clove" to listOf(
            "clove", "cloves", "gra", "grans", "dent", "dents", "diente", "dientes",
        ),
        "can" to listOf("can", "cans", "tin", "tins", "llauna", "llaunes", "lata", "latas"),
        "slice" to listOf(
            "slice", "slices", "llesca", "llesques", "rodanxa", "rodanxes",
            "rebanada", "rebanadas", "rodaja", "rodajas",
        ),
        "bunch" to listOf(
            "bunch", "bunches", "manat", "manats", "manoll", "manolls",
            "manojo", "manojos", "ram", "rams",
        ),
        "sprig" to listOf("sprig", "sprigs", "branqueta", "branquetes", "ramita", "ramitas"),
        "leaf" to listOf("leaf", "leaves", "fulla", "fulles", "hoja", "hojas"),
        "drop" to listOf("drop", "drops", "gota", "gotes", "gotas"),
        "package" to listOf(
            "package", "packages", "packet", "packets",
            "paquet", "paquets", "paquete", "paquetes", "sobre", "sobres",
        ),
        "unit" to listOf(
            "unit", "units", "unitat", "unitats", "unidad", "unidades",
            "piece", "pieces", "peca", "peces", "pieza", "piezas", "ud", "uds",
        ),
    )

    /** Normalized spelling -> canonical token. */
    private val LOOKUP: Map<String, String> =
        SPELLINGS.entries
            .flatMap { (canonical, spellings) -> spellings.map { normalize(it) to canonical } }
            .toMap()

    /** Every canonical token, for validation and for the unit picker in the editor. */
    val canonicalUnits: Set<String> = SPELLINGS.keys

    /**
     * Resolves a single token to its canonical unit, or null when the token is not a unit.
     * Returning null is the common case — most words following a quantity are ingredient
     * names, not units.
     */
    fun canonicalize(token: String): String? {
        val cleaned = token.trim().trimEnd('.', ',', ':')
        if (cleaned.isEmpty()) return null
        return LOOKUP[normalize(cleaned)]
    }

    fun isUnit(token: String): Boolean = canonicalize(token) != null

    /**
     * Lowercased and accent-stripped so "unça"/"unca" and "mil·lilitre"/"millilitre" all
     * collapse onto one key — imported text is inconsistent about both.
     */
    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace("·", "")
            .lowercase(Locale.ROOT)
}
