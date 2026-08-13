package cat.receptari.app.data.translation

import cat.receptari.app.data.local.entity.RecipeTranslationEntity
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.translation.CachedTranslationField
import cat.receptari.app.domain.translation.RecipeTranslationVariant
import cat.receptari.app.domain.translation.translationSourceFingerprint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class RecipeTranslationPayloadDto(
    @SerialName("source_language") val sourceLanguage: String?,
    @SerialName("source_fingerprint") val sourceFingerprint: String,
    val title: String,
    @SerialName("ingredient_sections") val ingredientSections: List<CachedFieldDto>,
    val ingredients: List<CachedFieldDto>,
    @SerialName("instruction_sections") val instructionSections: List<CachedFieldDto>,
    val steps: List<CachedFieldDto>,
)

@Serializable
private data class CachedFieldDto(
    val id: String,
    val text: String?,
)

internal fun Recipe.toTranslationEntity(json: Json): RecipeTranslationEntity? {
    val language = displayLanguage ?: return null
    val payload = RecipeTranslationPayloadDto(
        sourceLanguage = originalLanguage,
        sourceFingerprint = translationSourceFingerprint(),
        title = title,
        ingredientSections = ingredientSections.map { CachedFieldDto(it.id, it.name) },
        ingredients = allIngredients.map {
            CachedFieldDto(it.id, it.displayText ?: it.originalText)
        },
        instructionSections = instructionSections.map { CachedFieldDto(it.id, it.name) },
        steps = instructionSections.flatMap { section ->
            section.steps.map { CachedFieldDto(it.id, it.text) }
        },
    )
    return RecipeTranslationEntity(
        recipeId = id,
        language = language,
        payloadJson = json.encodeToString(payload),
        updatedAt = updatedAt.toEpochMilli(),
    )
}

internal fun RecipeTranslationEntity.toDomain(json: Json): RecipeTranslationVariant? =
    runCatching {
        val payload = json.decodeFromString<RecipeTranslationPayloadDto>(payloadJson)
        RecipeTranslationVariant(
            recipeId = recipeId,
            language = language,
            sourceLanguage = payload.sourceLanguage,
            sourceFingerprint = payload.sourceFingerprint,
            title = payload.title,
            ingredientSections = payload.ingredientSections.map(CachedFieldDto::toDomain),
            ingredients = payload.ingredients.map(CachedFieldDto::toDomain),
            instructionSections = payload.instructionSections.map(CachedFieldDto::toDomain),
            steps = payload.steps.map(CachedFieldDto::toDomain),
        )
    }.getOrNull()

internal fun RecipeTranslationVariant.toEntity(
    json: Json,
    updatedAt: Long,
): RecipeTranslationEntity {
    val payload = RecipeTranslationPayloadDto(
        sourceLanguage = sourceLanguage,
        sourceFingerprint = sourceFingerprint,
        title = title,
        ingredientSections = ingredientSections.map { CachedFieldDto(it.id, it.text) },
        ingredients = ingredients.map { CachedFieldDto(it.id, it.text) },
        instructionSections = instructionSections.map { CachedFieldDto(it.id, it.text) },
        steps = steps.map { CachedFieldDto(it.id, it.text) },
    )
    return RecipeTranslationEntity(
        recipeId = recipeId,
        language = language,
        payloadJson = json.encodeToString(payload),
        updatedAt = updatedAt,
    )
}

private fun CachedFieldDto.toDomain(): CachedTranslationField = CachedTranslationField(id, text)
