package cat.receptari.app.domain.transfer

import cat.receptari.app.domain.model.Recipe
import java.security.MessageDigest

/** A transient duplicate key. It is deliberately not persisted as sync metadata. */
public fun Recipe.transferContentFingerprint(): String {
    val sourceIsDisplayed = displayLanguage == null
    val source = buildString {
        appendValue(if (sourceIsDisplayed) title else originalTitle ?: title)
        appendValue(prepTimeMinutes?.toString())
        appendValue(cookTimeMinutes?.toString())
        appendValue(totalTimeMinutes?.toString())
        appendValue(baseServings?.toString())
        appendValue(sourceUrl)
        ingredientSections.forEach { section ->
            appendValue(if (sourceIsDisplayed) section.name else section.originalName ?: section.name)
            section.ingredients.forEach { ingredient -> appendValue(ingredient.originalText) }
        }
        instructionSections.forEach { section ->
            appendValue(if (sourceIsDisplayed) section.name else section.originalName ?: section.name)
            section.steps.forEach { step ->
                appendValue(if (sourceIsDisplayed) step.text else step.originalText ?: step.text)
            }
        }
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(source.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}

private fun StringBuilder.appendValue(value: String?) {
    append(value?.length ?: -1).append(':').append(value.orEmpty()).append(';')
}
