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
    val displayLanguage: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val ingredientSections: List<IngredientSection> = emptyList(),
    val instructionSections: List<InstructionSection> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val folder: Folder? = null,
    val cookCount: Int = 0,
    val lastCookedAt: Instant? = null,
) {
    /** Flattened ingredients across every section, in display order. */
    val allIngredients: List<Ingredient>
        get() = ingredientSections.flatMap { it.ingredients }

    /** True when the serving multiplier can be offered at all (PRD §3.3). */
    val isScalable: Boolean
        get() = baseServings != null && baseServings > 0

    /** The single duration worth printing where there is room for one. */
    val effectiveTimeMinutes: Int?
        get() = CookingTime.effective(totalTimeMinutes, prepTimeMinutes, cookTimeMinutes)

    /**
     * A total the recipe implies but does not state. Null when it would only repeat a figure
     * already on screen — see [CookingTime.derivedTotal].
     */
    val derivedTotalMinutes: Int?
        get() = if (totalTimeMinutes != null) {
            null
        } else {
            CookingTime.derivedTotal(prepTimeMinutes, cookTimeMinutes)
        }
}
