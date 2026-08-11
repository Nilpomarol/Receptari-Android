package cat.receptari.app.data.remote.web

import cat.receptari.app.domain.ai.DraftRecipe
import cat.receptari.app.domain.ai.DraftSection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.Duration
import java.time.format.DateTimeParseException

/**
 * Reads a schema.org `Recipe` out of a page, from JSON-LD first and microdata second.
 *
 * Written defensively on purpose: this parses documents from the open web, where every
 * optional field is optional in a different way. `recipeInstructions` alone appears as a
 * string, an array of strings, an array of `HowToStep` objects, and an array of
 * `HowToSection` objects containing `HowToStep`s — all of which occur in the wild. Anything
 * unrecognised yields null and the caller falls back to the model rather than guessing.
 */
internal object SchemaOrgRecipeParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(document: Document): DraftRecipe? =
        parseJsonLd(document) ?: parseMicrodata(document)

    // ---------------------------------------------------------------- JSON-LD

    private fun parseJsonLd(document: Document): DraftRecipe? =
        document.select("script[type=application/ld+json]")
            .asSequence()
            .mapNotNull { script -> runCatching { json.parseToJsonElement(script.data()) }.getOrNull() }
            .flatMap { it.flattenGraph() }
            .firstOrNull { it.isRecipe() }
            ?.toDraft()

    /**
     * JSON-LD arrives as a single object, an array of objects, or an object wrapping an
     * `@graph` array. Flattening all three means the search below only handles one shape.
     */
    private fun JsonElement.flattenGraph(): Sequence<JsonObject> = when (this) {
        is JsonArray -> asSequence().flatMap { it.flattenGraph() }
        is JsonObject -> {
            val graph = this["@graph"]
            if (graph != null) sequenceOf(this) + graph.flattenGraph() else sequenceOf(this)
        }
        else -> emptySequence()
    }

    /** `@type` is a string on most sites and an array on some. */
    private fun JsonObject.isRecipe(): Boolean {
        val type = this["@type"] ?: return false
        return type.asStringList().any { it.equals("Recipe", ignoreCase = true) }
    }

    private fun JsonObject.toDraft(): DraftRecipe {
        val ingredients = (this["recipeIngredient"] ?: this["ingredients"])?.asStringList().orEmpty()
        val steps = this["recipeInstructions"].toInstructionSections()

        return DraftRecipe(
            title = this["name"]?.asText(),
            prepTimeMinutes = this["prepTime"]?.asText()?.isoDurationMinutes(),
            cookTimeMinutes = this["cookTime"]?.asText()?.isoDurationMinutes(),
            totalTimeMinutes = this["totalTime"]?.asText()?.isoDurationMinutes(),
            servings = this["recipeYield"]?.asStringList()?.firstNotNullOfOrNull { it.leadingInt() },
            originalLanguage = this["inLanguage"]?.asText(),
            sourceName = this["author"].authorName(),
            notes = this["description"]?.asText(),
            ingredientSections = ingredients.toSectionOrEmpty(),
            instructionSections = steps,
        )
    }

    /** `author` is a string, an object with `name`, or an array of either. */
    private fun JsonElement?.authorName(): String? = when (this) {
        null -> null
        is JsonPrimitive -> contentOrNullIfBlank()
        is JsonObject -> this["name"]?.asText()
        is JsonArray -> firstOrNull().authorName()
    }

    private fun JsonElement?.toInstructionSections(): List<DraftSection> = when (this) {
        null -> emptyList()

        // A single blob of prose. Splitting on newlines is the only signal available, and a
        // one-line blob simply becomes one step.
        is JsonPrimitive -> content.htmlToLines().toSectionOrEmpty()

        is JsonArray -> {
            val sections = mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val types = obj["@type"]?.asStringList().orEmpty()
                if (types.none { it.equals("HowToSection", ignoreCase = true) }) {
                    null
                } else {
                    DraftSection(
                        name = obj["name"]?.asText(),
                        lines = obj["itemListElement"].toStepLines(),
                    )
                }
            }.filter { it.lines.isNotEmpty() }

            // Sections and flat steps are mutually exclusive in practice; prefer sections
            // when they are there so the source's own grouping survives.
            sections.ifEmpty { toStepLines().toSectionOrEmpty() }
        }

        else -> emptyList()
    }

    /** Flattens `HowToStep` objects, plain strings, and nested lists into step text. */
    private fun JsonElement?.toStepLines(): List<String> = when (this) {
        null -> emptyList()
        is JsonPrimitive -> content.htmlToLines()
        is JsonArray -> flatMap { element ->
            when (element) {
                is JsonPrimitive -> element.content.htmlToLines()
                is JsonObject -> (element["text"] ?: element["name"])?.asText()?.htmlToLines()
                    ?: element["itemListElement"].toStepLines()
                else -> emptyList()
            }
        }
        is JsonObject -> (this["text"] ?: this["name"])?.asText()?.htmlToLines().orEmpty()
    }

    // -------------------------------------------------------------- Microdata

    private fun parseMicrodata(document: Document): DraftRecipe? {
        val scope = document.selectFirst("[itemtype~=(?i)schema\\.org/Recipe]") ?: return null

        val ingredients = scope.prop("recipeIngredient", "ingredients").map { it.text() }
        val steps = scope.prop("recipeInstructions").flatMap { it.text().htmlToLines() }
        if (ingredients.isEmpty() && steps.isEmpty()) return null

        return DraftRecipe(
            title = scope.prop("name").firstOrNull()?.text()?.takeIf { it.isNotBlank() },
            prepTimeMinutes = scope.propContent("prepTime")?.isoDurationMinutes(),
            cookTimeMinutes = scope.propContent("cookTime")?.isoDurationMinutes(),
            totalTimeMinutes = scope.propContent("totalTime")?.isoDurationMinutes(),
            servings = scope.prop("recipeYield").firstOrNull()?.text()?.leadingInt(),
            sourceName = scope.prop("author").firstOrNull()?.text()?.takeIf { it.isNotBlank() },
            ingredientSections = ingredients.toSectionOrEmpty(),
            instructionSections = steps.toSectionOrEmpty(),
        )
    }

    private fun Element.prop(vararg names: String): List<Element> =
        names.flatMap { name -> select("[itemprop=$name]") }

    /** Times are usually on a `<time datetime="PT20M">`, so the attribute beats the text. */
    private fun Element.propContent(name: String): String? =
        selectFirst("[itemprop=$name]")?.let { element ->
            element.attr("datetime").takeIf { it.isNotBlank() }
                ?: element.attr("content").takeIf { it.isNotBlank() }
                ?: element.text().takeIf { it.isNotBlank() }
        }

    // ---------------------------------------------------------------- Helpers

    private fun JsonElement.asStringList(): List<String> = when (this) {
        is JsonPrimitive -> listOfNotNull(contentOrNullIfBlank())
        is JsonArray -> flatMap { it.asStringList() }
        is JsonObject -> listOfNotNull(this["name"]?.asText())
    }

    private fun JsonElement.asText(): String? =
        (this as? JsonPrimitive)?.contentOrNullIfBlank()
            ?: (this as? JsonObject)?.get("name")?.let { (it as? JsonPrimitive)?.contentOrNullIfBlank() }

    private fun JsonPrimitive.contentOrNullIfBlank(): String? =
        content.takeIf { it.isNotBlank() && it != "null" }?.stripHtml()

    /**
     * schema.org durations are ISO 8601 (`PT1H30M`). Some sites write plain minutes instead,
     * so that is accepted too rather than silently losing the time.
     */
    private fun String.isoDurationMinutes(): Int? {
        val trimmed = trim()
        val fromIso = runCatching { Duration.parse(trimmed) }
            .recoverCatching { throw it as? DateTimeParseException ?: it }
            .getOrNull()
            ?.toMinutes()
            ?.toInt()
        return (fromIso ?: trimmed.toIntOrNull())?.takeIf { it > 0 }
    }

    private fun String.leadingInt(): Int? =
        Regex("\\d+").find(this)?.value?.toIntOrNull()?.takeIf { it > 0 }

    /** Fields routinely contain HTML, and `<br>` is often the only separator between steps. */
    private fun String.htmlToLines(): List<String> =
        replace(Regex("(?i)<br\\s*/?>|</p>|</li>"), "\n")
            .stripHtml()
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun String.stripHtml(): String =
        if (contains('<')) org.jsoup.Jsoup.parse(this).text().trim() else trim()

    private fun List<String>.toSectionOrEmpty(): List<DraftSection> =
        filter { it.isNotBlank() }
            .let { lines -> if (lines.isEmpty()) emptyList() else listOf(DraftSection(lines = lines)) }
}
