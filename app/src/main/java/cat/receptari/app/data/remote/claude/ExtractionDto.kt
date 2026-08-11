package cat.receptari.app.data.remote.claude

import cat.receptari.app.domain.ai.DraftRecipe
import cat.receptari.app.domain.ai.DraftSection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire shape of a structured-output response. Mirrors [RecipeSchema].
 *
 * Kept separate from [DraftRecipe] so the JSON contract can change without touching the
 * domain, and so nothing in `domain/` needs a serialization annotation.
 */
@Serializable
internal data class ExtractionDto(
    val title: String? = null,
    @SerialName("prep_time_minutes") val prepTimeMinutes: Int? = null,
    @SerialName("cook_time_minutes") val cookTimeMinutes: Int? = null,
    @SerialName("total_time_minutes") val totalTimeMinutes: Int? = null,
    val servings: Int? = null,
    val language: String? = null,
    @SerialName("source_name") val sourceName: String? = null,
    val notes: String? = null,
    @SerialName("ingredient_sections") val ingredientSections: List<SectionDto> = emptyList(),
    @SerialName("instruction_sections") val instructionSections: List<SectionDto> = emptyList(),
)

@Serializable
internal data class SectionDto(
    val name: String? = null,
    val lines: List<String> = emptyList(),
)

internal fun ExtractionDto.toDraft(sourceUrl: String? = null): DraftRecipe = DraftRecipe(
    title = title?.trim()?.takeIf { it.isNotEmpty() },
    prepTimeMinutes = prepTimeMinutes?.takeIf { it > 0 },
    cookTimeMinutes = cookTimeMinutes?.takeIf { it > 0 },
    totalTimeMinutes = totalTimeMinutes?.takeIf { it > 0 },
    servings = servings?.takeIf { it > 0 },
    originalLanguage = language?.trim()?.takeIf { it.isNotEmpty() },
    sourceName = sourceName?.trim()?.takeIf { it.isNotEmpty() },
    sourceUrl = sourceUrl,
    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
    ingredientSections = ingredientSections.toDraftSections(),
    instructionSections = instructionSections.toDraftSections(),
)

/**
 * Blank lines are dropped here rather than trusted away: structured output guarantees the
 * shape of the response, not that every string in it is meaningful.
 */
private fun List<SectionDto>.toDraftSections(): List<DraftSection> =
    mapNotNull { section ->
        val lines = section.lines.map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            null
        } else {
            DraftSection(name = section.name?.trim()?.takeIf { it.isNotEmpty() }, lines = lines)
        }
    }
