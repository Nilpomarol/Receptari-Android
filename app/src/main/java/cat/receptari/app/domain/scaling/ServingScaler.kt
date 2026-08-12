package cat.receptari.app.domain.scaling

import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.model.IngredientSection
import kotlin.math.abs

/**
 * Scales ingredient quantities for a chosen number of servings.
 *
 * Pure, display-time only. Nothing here reads or writes storage: the stored base recipe
 * must be unchanged by the multiplier (PRD §3.3, §14), so the output is always a
 * [ScaledIngredient] and never an [Ingredient].
 */
object ServingScaler {

    private const val EPSILON = 1e-9

    /**
     * The multiplier to apply, or null when scaling is not meaningful — the recipe has no
     * base serving count, or either count is zero or negative. A null factor means the UI
     * should not offer the multiplier at all.
     */
    fun factor(baseServings: Int?, targetServings: Int): Double? {
        if (baseServings == null || baseServings <= 0) return null
        if (targetServings <= 0) return null
        return targetServings.toDouble() / baseServings
    }

    fun scale(ingredient: Ingredient, factor: Double?): ScaledIngredient {
        val applies = factor != null && abs(factor - 1.0) > EPSILON && ingredient.quantity != null

        return ScaledIngredient(
            id = ingredient.id,
            // An ingredient with no numeric quantity — "sal al gust", "un grapat de julivert"
            // — passes through completely untouched (PRD §3.3).
            quantity = if (applies) ingredient.quantity!! * factor!! else ingredient.quantity,
            quantityMax = if (applies) ingredient.quantityMax?.times(factor!!) else ingredient.quantityMax,
            unit = ingredient.unit,
            name = ingredient.name,
            note = ingredient.note,
            originalText = ingredient.originalText,
            displayText = ingredient.displayText,
            wasScaled = applies,
        )
    }

    fun scaleSection(section: IngredientSection, factor: Double?): ScaledIngredientSection =
        ScaledIngredientSection(
            id = section.id,
            name = section.name,
            ingredients = section.ingredients.map { scale(it, factor) },
        )

    fun scaleSections(
        sections: List<IngredientSection>,
        factor: Double?,
    ): List<ScaledIngredientSection> = sections.map { scaleSection(it, factor) }
}
