package cat.receptari.app.domain.translation

import cat.receptari.app.domain.model.Recipe
import java.security.MessageDigest

data class CachedTranslationField(
    val id: String,
    val text: String?,
)

/** A locally cached language overlay. Measurements and recipe structure stay shared. */
data class RecipeTranslationVariant(
    val recipeId: String,
    val language: String,
    val sourceLanguage: String?,
    val sourceFingerprint: String,
    val title: String,
    val ingredientSections: List<CachedTranslationField>,
    val ingredients: List<CachedTranslationField>,
    val instructionSections: List<CachedTranslationField>,
    val steps: List<CachedTranslationField>,
)

/** Changes whenever canonical translatable text or its stable field structure changes. */
fun Recipe.translationSourceFingerprint(): String {
    val sourceIsDisplayed = displayLanguage == null
    val source = buildString {
        appendField("title", if (sourceIsDisplayed) title else originalTitle ?: title)
        ingredientSections.forEach { section ->
            appendField(
                "ingredient-section:${section.id}",
                if (sourceIsDisplayed) section.name else section.originalName ?: section.name,
            )
            section.ingredients.forEach { ingredient ->
                appendField("ingredient:${ingredient.id}", ingredient.originalText)
            }
        }
        instructionSections.forEach { section ->
            appendField(
                "instruction-section:${section.id}",
                if (sourceIsDisplayed) section.name else section.originalName ?: section.name,
            )
            section.steps.forEach { step ->
                appendField(
                    "step:${step.id}",
                    if (sourceIsDisplayed) step.text else step.originalText ?: step.text,
                )
            }
        }
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(source.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}

private fun StringBuilder.appendField(key: String, value: String?) {
    append(key.length).append(':').append(key)
    append(value?.length ?: -1).append(':').append(value.orEmpty()).append(';')
}
