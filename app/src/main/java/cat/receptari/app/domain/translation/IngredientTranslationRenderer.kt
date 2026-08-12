package cat.receptari.app.domain.translation

import cat.receptari.app.domain.ai.RecipeLanguage
import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.parser.IngredientParser

/**
 * Separates translation from measurement rendering.
 *
 * The model sees only ingredient meaning. Kotlin owns quantities, localized unit labels,
 * plural choice, and the connector between a measure and its ingredient, so a stronger model
 * is never asked to guess around text deliberately removed from its payload.
 */
object IngredientTranslationRenderer {

    data class PreparedIngredient(
        val sourceText: String,
        private val quantityText: String?,
        private val unit: String?,
        private val quantity: Double?,
        private val quantityMax: Double?,
    ) {
        fun render(translatedText: String, target: RecipeLanguage): String {
            val translated = translatedText.trim()
            if (quantityText == null) return translated
            if (unit == null) return "$quantityText $translated".trimEnd()

            val unitText = localizedUnit(
                unit = unit,
                singular = quantityMax == null && quantity != null && quantity > 0.0 && quantity <= 1.0,
                target = target,
            )
            if (translated.isEmpty()) return "$quantityText $unitText"
            val connector = connector(target, translated)
            return "$quantityText $unitText$connector$translated".trimEnd()
        }
    }

    fun prepare(ingredient: Ingredient): PreparedIngredient {
        val source = ingredient.originalText
        val parsed = IngredientParser.parse(source)
        val quantityRange = IngredientParser.leadingQuantityRange(source)
        val quantityText = quantityRange?.let { source.substring(it) }
        val semanticText = when {
            quantityText == null -> source
            parsed?.name != null -> buildString {
                append(parsed.name)
                parsed.note?.let { append(" (").append(it).append(')') }
            }
            parsed?.unit != null -> ""
            else -> source.substring(quantityRange.last + 1).trimStart()
        }

        return PreparedIngredient(
            sourceText = semanticText,
            quantityText = quantityText,
            unit = parsed?.unit,
            quantity = parsed?.quantity,
            quantityMax = parsed?.quantityMax,
        )
    }

    private fun connector(target: RecipeLanguage, translated: String): String = when (target) {
        RecipeLanguage.CATALAN -> if (translated.startsWithCatalanElision()) " d'" else " de "
        RecipeLanguage.SPANISH -> " de "
        RecipeLanguage.ENGLISH -> " of "
    }

    private fun String.startsWithCatalanElision(): Boolean =
        trimStart().firstOrNull()?.lowercaseChar() in CATALAN_ELISION_INITIALS

    private fun localizedUnit(unit: String, singular: Boolean, target: RecipeLanguage): String {
        UNIT_LABELS[unit]?.get(target)?.let { labels ->
            return if (singular) labels.first else labels.second
        }
        return unit
    }

    private val CATALAN_ELISION_INITIALS = setOf(
        'a', 'e', 'i', 'o', 'u', 'h',
        'à', 'è', 'é', 'í', 'ï', 'ò', 'ó', 'ú', 'ü',
    )

    private val UNIT_LABELS: Map<String, Map<RecipeLanguage, Pair<String, String>>> = mapOf(
        "tbsp" to labels("cullerada", "cullerades", "cucharada", "cucharadas", "tablespoon", "tablespoons"),
        "tsp" to labels("culleradeta", "culleradetes", "cucharadita", "cucharaditas", "teaspoon", "teaspoons"),
        "cup" to labels("tassa", "tasses", "taza", "tazas", "cup", "cups"),
        "pinch" to labels("pessic", "pessics", "pizca", "pizcas", "pinch", "pinches"),
        "clove" to labels("gra", "grans", "diente", "dientes", "clove", "cloves"),
        "can" to labels("llauna", "llaunes", "lata", "latas", "can", "cans"),
        "slice" to labels("llesca", "llesques", "rebanada", "rebanadas", "slice", "slices"),
        "bunch" to labels("manat", "manats", "manojo", "manojos", "bunch", "bunches"),
        "sprig" to labels("branqueta", "branquetes", "ramita", "ramitas", "sprig", "sprigs"),
        "leaf" to labels("fulla", "fulles", "hoja", "hojas", "leaf", "leaves"),
        "drop" to labels("gota", "gotes", "gota", "gotas", "drop", "drops"),
        "package" to labels("paquet", "paquets", "paquete", "paquetes", "package", "packages"),
        "unit" to labels("unitat", "unitats", "unidad", "unidades", "unit", "units"),
    )

    private fun labels(
        caSingular: String,
        caPlural: String,
        esSingular: String,
        esPlural: String,
        enSingular: String,
        enPlural: String,
    ): Map<RecipeLanguage, Pair<String, String>> = mapOf(
        RecipeLanguage.CATALAN to (caSingular to caPlural),
        RecipeLanguage.SPANISH to (esSingular to esPlural),
        RecipeLanguage.ENGLISH to (enSingular to enPlural),
    )
}
