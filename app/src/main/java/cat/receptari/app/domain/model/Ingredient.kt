package cat.receptari.app.domain.model

/**
 * A named group of ingredients. [name] is null for the single unnamed section a recipe
 * without groupings has; the UI hides the header in that case.
 */
data class IngredientSection(
    val id: String,
    val name: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val originalName: String? = null,
)

/**
 * One ingredient line.
 *
 * Every structured field is nullable — `"Salt to taste"`, `"a handful of parsley"`, and
 * `"1–2 onions"` are all valid and must survive storage (PRD §3.2). [originalText] is the
 * one field that is never null and never lost: it is the source text exactly as the user or
 * the import gave it, and it is what renders whenever parsing came up empty.
 */
data class Ingredient(
    val id: String,
    val quantity: Double? = null,
    val quantityMax: Double? = null,
    val unit: String? = null,
    val name: String? = null,
    val note: String? = null,
    val originalText: String,
    val displayText: String? = null,
) {
    /** True when this line has a numeric quantity and can therefore be scaled. */
    val isScalable: Boolean
        get() = quantity != null

    /** True when the parser recovered nothing beyond the raw text. */
    val isUnstructured: Boolean
        get() = quantity == null && unit == null && name == null
}
