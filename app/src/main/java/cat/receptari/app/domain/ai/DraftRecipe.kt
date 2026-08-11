package cat.receptari.app.domain.ai

/**
 * The output of every extraction path — website, image, or pasted text — before the user
 * has reviewed it.
 *
 * Every field is nullable: a source that yields only a title is a *partial success*, not a
 * failure, and the editor opens with whatever was recovered (PRD §8).
 *
 * Ingredients arrive as **lines of text**, not structured fields. The extractor's job is to
 * find the lines and group them; turning "200 ml de nata" into quantity/unit/name is
 * `IngredientParser`'s job. That split means `originalText` is guaranteed by construction,
 * the structuring is the same code path the manual editor uses, and it is covered by the
 * fixture corpus rather than by whatever the model felt like returning.
 */
data class DraftRecipe(
    val title: String? = null,
    val prepTimeMinutes: Int? = null,
    val cookTimeMinutes: Int? = null,
    val totalTimeMinutes: Int? = null,
    val servings: Int? = null,
    val originalLanguage: String? = null,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val notes: String? = null,
    val ingredientSections: List<DraftSection> = emptyList(),
    val instructionSections: List<DraftSection> = emptyList(),
) {
    /** True when nothing usable came back and there is no point opening the editor. */
    val isEmpty: Boolean
        get() = title.isNullOrBlank() &&
            ingredientSections.all { it.lines.isEmpty() } &&
            instructionSections.all { it.lines.isEmpty() }
}

/**
 * A named group of lines — ingredients or steps. [name] is null for an unnamed group,
 * which is the common case.
 */
data class DraftSection(
    val name: String? = null,
    val lines: List<String> = emptyList(),
)
