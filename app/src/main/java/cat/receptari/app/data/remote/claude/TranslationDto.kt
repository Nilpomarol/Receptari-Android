package cat.receptari.app.data.remote.claude

import cat.receptari.app.domain.ai.TranslationField
import cat.receptari.app.domain.ai.TranslationRequest
import cat.receptari.app.domain.ai.TranslationResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TranslationRequestDto(
    @SerialName("target_language") val targetLanguage: String,
    val title: String,
    @SerialName("ingredient_sections") val ingredientSections: List<TranslationFieldDto>,
    val ingredients: List<TranslationFieldDto>,
    @SerialName("instruction_sections") val instructionSections: List<TranslationFieldDto>,
    val steps: List<TranslationFieldDto>,
)

@Serializable
internal data class TranslationResultDto(
    val title: String,
    @SerialName("source_language") val sourceLanguage: String?,
    @SerialName("ingredient_sections") val ingredientSections: List<TranslationFieldDto>,
    val ingredients: List<TranslationFieldDto>,
    @SerialName("instruction_sections") val instructionSections: List<TranslationFieldDto>,
    val steps: List<TranslationFieldDto>,
)

@Serializable
internal data class TranslationFieldDto(val id: String, val text: String)

internal fun TranslationRequest.toDto(): TranslationRequestDto = TranslationRequestDto(
    targetLanguage = targetLanguage.languageTag,
    title = title,
    ingredientSections = ingredientSections.map { it.toDto() },
    ingredients = ingredients.map { it.toDto() },
    instructionSections = instructionSections.map { it.toDto() },
    steps = steps.map { it.toDto() },
)

private fun TranslationField.toDto() = TranslationFieldDto(id, text)

internal fun TranslationResultDto.toResult(request: TranslationRequest): TranslationResult {
    val result = TranslationResult(
        title = title.trim(),
        sourceLanguage = sourceLanguage?.trim()?.takeIf { it.isNotEmpty() },
        ingredientSections = ingredientSections.map { it.toField() },
        ingredients = ingredients.map { it.toField() },
        instructionSections = instructionSections.map { it.toField() },
        steps = steps.map { it.toField() },
    )
    if (!result.matches(request)) throw TranslationContractException()
    return result
}

private fun TranslationFieldDto.toField() = TranslationField(id, text.trim())

private fun TranslationResult.matches(request: TranslationRequest): Boolean =
    title.isNotBlank() &&
        sourceLanguage.isValidLanguageTag() &&
        ingredientSections.matches(request.ingredientSections) &&
        ingredients.matches(request.ingredients) &&
        instructionSections.matches(request.instructionSections) &&
        steps.matches(request.steps)

private fun List<TranslationField>.matches(requested: List<TranslationField>): Boolean =
    size == requested.size &&
        all { it.text.isNotBlank() } &&
        map { it.id }.toSet() == requested.map { it.id }.toSet()

private fun String?.isValidLanguageTag(): Boolean =
    this == null || LANGUAGE_TAG.matches(this)

private val LANGUAGE_TAG = Regex("^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$")

internal class TranslationContractException : Exception()
