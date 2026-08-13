package cat.receptari.app.data.transfer

import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.model.IngredientSection
import cat.receptari.app.domain.model.InstructionSection
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.Step
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.domain.translation.CachedTranslationField
import cat.receptari.app.domain.translation.RecipeTranslationVariant
import cat.receptari.app.domain.translation.translationSourceFingerprint
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RecipeTransferPackageDto(
    @SerialName("format_version") val formatVersion: Int,
    @SerialName("created_at_epoch_millis") val createdAtEpochMillis: Long,
    @SerialName("app_version") val appVersion: String,
    val recipes: List<TransferRecipeDto>,
)

@Serializable
internal data class TransferRecipeDto(
    val id: String,
    val title: String,
    @SerialName("original_title") val originalTitle: String?,
    @SerialName("image_entry") val imageEntry: String?,
    @SerialName("prep_time_minutes") val prepTimeMinutes: Int?,
    @SerialName("cook_time_minutes") val cookTimeMinutes: Int?,
    @SerialName("total_time_minutes") val totalTimeMinutes: Int?,
    @SerialName("base_servings") val baseServings: Int?,
    @SerialName("source_name") val sourceName: String?,
    @SerialName("source_url") val sourceUrl: String?,
    @SerialName("original_language") val originalLanguage: String?,
    @SerialName("display_language") val displayLanguage: String?,
    @SerialName("ingredient_sections") val ingredientSections: List<TransferIngredientSectionDto>,
    @SerialName("instruction_sections") val instructionSections: List<TransferInstructionSectionDto>,
    val tags: List<String>,
    val translations: List<TransferTranslationDto>,
)

@Serializable
internal data class TransferIngredientSectionDto(
    val id: String,
    val name: String?,
    @SerialName("original_name") val originalName: String?,
    val ingredients: List<TransferIngredientDto>,
)

@Serializable
internal data class TransferIngredientDto(
    val id: String,
    val quantity: Double?,
    @SerialName("quantity_max") val quantityMax: Double?,
    val unit: String?,
    val name: String?,
    val note: String?,
    @SerialName("original_text") val originalText: String,
    @SerialName("display_text") val displayText: String?,
)

@Serializable
internal data class TransferInstructionSectionDto(
    val id: String,
    val name: String?,
    @SerialName("original_name") val originalName: String?,
    val steps: List<TransferStepDto>,
)

@Serializable
internal data class TransferStepDto(
    val id: String,
    val text: String,
    @SerialName("original_text") val originalText: String?,
)

@Serializable
internal data class TransferTranslationDto(
    val language: String,
    @SerialName("source_language") val sourceLanguage: String?,
    val title: String,
    @SerialName("ingredient_sections") val ingredientSections: List<TransferFieldDto>,
    val ingredients: List<TransferFieldDto>,
    @SerialName("instruction_sections") val instructionSections: List<TransferFieldDto>,
    val steps: List<TransferFieldDto>,
)

@Serializable
internal data class TransferFieldDto(val id: String, val text: String?)

internal data class ImportedRecipeBundle(
    val recipe: Recipe,
    val translations: List<RecipeTranslationVariant>,
)

internal fun Recipe.toTransferDto(
    imageEntry: String?,
    translations: List<RecipeTranslationVariant>,
): TransferRecipeDto = TransferRecipeDto(
    id = id,
    title = title,
    originalTitle = originalTitle,
    imageEntry = imageEntry,
    prepTimeMinutes = prepTimeMinutes,
    cookTimeMinutes = cookTimeMinutes,
    totalTimeMinutes = totalTimeMinutes,
    baseServings = baseServings,
    sourceName = sourceName,
    sourceUrl = sourceUrl,
    originalLanguage = originalLanguage,
    displayLanguage = displayLanguage,
    ingredientSections = ingredientSections.map { section ->
        TransferIngredientSectionDto(
            id = section.id,
            name = section.name,
            originalName = section.originalName,
            ingredients = section.ingredients.map { ingredient ->
                TransferIngredientDto(
                    id = ingredient.id,
                    quantity = ingredient.quantity,
                    quantityMax = ingredient.quantityMax,
                    unit = ingredient.unit,
                    name = ingredient.name,
                    note = ingredient.note,
                    originalText = ingredient.originalText,
                    displayText = ingredient.displayText,
                )
            },
        )
    },
    instructionSections = instructionSections.map { section ->
        TransferInstructionSectionDto(
            id = section.id,
            name = section.name,
            originalName = section.originalName,
            steps = section.steps.map { step ->
                TransferStepDto(step.id, step.text, step.originalText)
            },
        )
    },
    tags = tags.map { it.name },
    translations = translations.map { it.toTransferDto() },
)

private fun RecipeTranslationVariant.toTransferDto(): TransferTranslationDto =
    TransferTranslationDto(
        language = language,
        sourceLanguage = sourceLanguage,
        title = title,
        ingredientSections = ingredientSections.map { TransferFieldDto(it.id, it.text) },
        ingredients = ingredients.map { TransferFieldDto(it.id, it.text) },
        instructionSections = instructionSections.map { TransferFieldDto(it.id, it.text) },
        steps = steps.map { TransferFieldDto(it.id, it.text) },
    )

internal fun TransferRecipeDto.toImportedBundle(now: Instant): ImportedRecipeBundle {
    val recipeId = UUID.randomUUID().toString()
    val ingredientSectionIds = ingredientSections.associate { it.id to UUID.randomUUID().toString() }
    val ingredientIds = ingredientSections
        .flatMap { it.ingredients }
        .associate { it.id to UUID.randomUUID().toString() }
    val instructionSectionIds = instructionSections
        .associate { it.id to UUID.randomUUID().toString() }
    val stepIds = instructionSections
        .flatMap { it.steps }
        .associate { it.id to UUID.randomUUID().toString() }

    val importedRecipe = Recipe(
        id = recipeId,
        title = title,
        originalTitle = originalTitle,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
        totalTimeMinutes = totalTimeMinutes,
        baseServings = baseServings,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        originalLanguage = originalLanguage,
        displayLanguage = displayLanguage,
        createdAt = now,
        updatedAt = now,
        ingredientSections = ingredientSections.map { section ->
            IngredientSection(
                id = ingredientSectionIds.getValue(section.id),
                name = section.name,
                originalName = section.originalName,
                ingredients = section.ingredients.map { ingredient ->
                    Ingredient(
                        id = ingredientIds.getValue(ingredient.id),
                        quantity = ingredient.quantity,
                        quantityMax = ingredient.quantityMax,
                        unit = ingredient.unit,
                        name = ingredient.name,
                        note = ingredient.note,
                        originalText = ingredient.originalText,
                        displayText = ingredient.displayText,
                    )
                },
            )
        },
        instructionSections = instructionSections.map { section ->
            InstructionSection(
                id = instructionSectionIds.getValue(section.id),
                name = section.name,
                originalName = section.originalName,
                steps = section.steps.map { step ->
                    Step(
                        id = stepIds.getValue(step.id),
                        text = step.text,
                        originalText = step.originalText,
                    )
                },
            )
        },
        tags = tags.distinctBy(Tag::normalize).map { name ->
            Tag(id = UUID.randomUUID().toString(), name = name)
        },
    )
    val sourceFingerprint = importedRecipe.translationSourceFingerprint()
    val importedTranslations = translations.map { translation ->
        RecipeTranslationVariant(
            recipeId = recipeId,
            language = translation.language,
            sourceLanguage = translation.sourceLanguage,
            sourceFingerprint = sourceFingerprint,
            title = translation.title,
            ingredientSections = translation.ingredientSections.map { field ->
                CachedTranslationField(ingredientSectionIds.getValue(field.id), field.text)
            },
            ingredients = translation.ingredients.map { field ->
                CachedTranslationField(ingredientIds.getValue(field.id), field.text)
            },
            instructionSections = translation.instructionSections.map { field ->
                CachedTranslationField(instructionSectionIds.getValue(field.id), field.text)
            },
            steps = translation.steps.map { field ->
                CachedTranslationField(stepIds.getValue(field.id), field.text)
            },
        )
    }
    return ImportedRecipeBundle(importedRecipe, importedTranslations)
}

internal fun TransferRecipeDto.isValid(): Boolean {
    if (id.isBlank() || title.isBlank()) return false
    val ingredientSectionIdSet = ingredientSections.map { it.id }.toSet()
    val ingredientIdSet = ingredientSections.flatMap { it.ingredients }.map { it.id }.toSet()
    val instructionSectionIdSet = instructionSections.map { it.id }.toSet()
    val stepIdSet = instructionSections.flatMap { it.steps }.map { it.id }.toSet()
    if (
        ingredientSectionIdSet.size != ingredientSections.size ||
        ingredientIdSet.size != ingredientSections.sumOf { it.ingredients.size } ||
        instructionSectionIdSet.size != instructionSections.size ||
        stepIdSet.size != instructionSections.sumOf { it.steps.size }
    ) return false
    if (ingredientSections.any { it.id.isBlank() || it.ingredients.any { item -> item.id.isBlank() } }) {
        return false
    }
    if (instructionSections.any { it.id.isBlank() || it.steps.any { step -> step.id.isBlank() } }) {
        return false
    }
    if (translations.map { it.language }.any { it.isBlank() }) return false
    if (translations.map { it.language }.toSet().size != translations.size) return false
    return translations.all { translation ->
        translation.ingredientSections.map { it.id }.toSet() == ingredientSectionIdSet &&
            translation.ingredients.map { it.id }.toSet() == ingredientIdSet &&
            translation.instructionSections.map { it.id }.toSet() == instructionSectionIdSet &&
            translation.steps.map { it.id }.toSet() == stepIdSet
    }
}
