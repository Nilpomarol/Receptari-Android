package cat.receptari.app.domain.model

import java.time.Instant

/**
 * A recipe as the app reasons about it: the full aggregate, assembled by the repository.
 *
 * Ordering of sections, ingredients, and steps is expressed by list order. The `position`
 * columns that back it exist only in the data layer.
 */
data class Recipe(
    val id: String,
    val title: String,
    val originalTitle: String? = null,
    val imagePath: String? = null,
    val prepTimeMinutes: Int? = null,
    val cookTimeMinutes: Int? = null,
    val totalTimeMinutes: Int? = null,
    val baseServings: Int? = null,
    val isFavorite: Boolean = false,
    val rating: Int? = null,
    val notes: String? = null,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val originalLanguage: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val ingredientSections: List<IngredientSection> = emptyList(),
    val instructionSections: List<InstructionSection> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val cookCount: Int = 0,
    val lastCookedAt: Instant? = null,
) {
    /** Flattened ingredients across every section, in display order. */
    val allIngredients: List<Ingredient>
        get() = ingredientSections.flatMap { it.ingredients }

    /** True when the serving multiplier can be offered at all (PRD §3.3). */
    val isScalable: Boolean
        get() = baseServings != null && baseServings > 0
}
