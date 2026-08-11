package cat.receptari.app.ui.edit

import cat.receptari.app.domain.ai.DraftRecipe
import cat.receptari.app.domain.ai.DraftSection
import cat.receptari.app.domain.parser.UnitNormalizer

/**
 * Fills the editor form from an extracted draft.
 *
 * The draft carries ingredient *lines*, and the form holds ingredient *lines*, so nothing is
 * parsed or reformatted on the way in. Parsing happens on save, exactly as it does for a
 * hand-typed recipe — which is what keeps `Ingredient.originalText` equal to the text the
 * source actually provided (AGENTS.md §3.1).
 */
internal fun DraftRecipe.toFormState(): RecipeEditUiState = RecipeEditUiState(
    isNew = true,
    isLoading = false,
    title = title.orEmpty(),
    prepTime = prepTimeMinutes?.toString().orEmpty(),
    cookTime = cookTimeMinutes?.toString().orEmpty(),
    totalTime = totalTimeMinutes?.toString().orEmpty(),
    servings = servings?.toString().orEmpty(),
    notes = notes.orEmpty(),
    sourceName = sourceName.orEmpty(),
    sourceUrl = sourceUrl.orEmpty(),
    // Units are tidied on the way into the form, not on the way out of the model. The
    // extractor still copies the source verbatim — normalising here keeps it deterministic,
    // testable and free, and puts the result in front of the user in an editable field
    // before anything is stored (PRD §14).
    ingredientSections = ingredientSections.toFormSections(UnitNormalizer::normalizeIngredient),
    instructionSections = instructionSections.toFormSections(UnitNormalizer::normalizeInstruction),
)

/**
 * A partial extraction is a normal outcome, so an absent section becomes one empty editable
 * section rather than nothing at all — the user finishes the recipe by typing into it.
 */
private fun List<DraftSection>.toFormSections(normalize: (String) -> String): List<FormSection> =
    map { section ->
        FormSection(
            name = section.name.orEmpty(),
            lines = section.lines
                .map { FormLine(text = normalize(it)) }
                .ifEmpty { listOf(FormLine()) },
        )
    }.ifEmpty { listOf(FormSection()) }
