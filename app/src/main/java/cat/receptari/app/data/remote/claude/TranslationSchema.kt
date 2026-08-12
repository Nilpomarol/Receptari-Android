package cat.receptari.app.data.remote.claude

internal object TranslationSchema {
    private val field: Map<String, Any> = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf("id", "text"),
        "properties" to mapOf(
            "id" to mapOf("type" to "string"),
            "text" to mapOf("type" to "string"),
        ),
    )

    private fun fields(): Map<String, Any> =
        mapOf("type" to "array", "items" to field)

    val MAP: Map<String, Any> = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf(
            "title",
            "source_language",
            "ingredient_sections",
            "ingredients",
            "instruction_sections",
            "steps",
        ),
        "properties" to mapOf(
            "title" to mapOf("type" to "string"),
            "source_language" to mapOf(
                "anyOf" to listOf(
                    mapOf("type" to "string"),
                    mapOf("type" to "null"),
                ),
            ),
            "ingredient_sections" to fields(),
            "ingredients" to fields(),
            "instruction_sections" to fields(),
            "steps" to fields(),
        ),
    )

    const val SYSTEM_PROMPT: String = """
Translate recipe text into the requested target language.

Return every supplied item exactly once with exactly the same id and array membership. Translate
only `text`; never alter, invent, omit, merge, split, or reorder items. Ingredient measurements
have already been removed. Do not add quantities or units. Keep culinary meaning, qualifiers,
doneness cues, temperatures, punctuation, and formatting precise. Ingredient fields contain only
the semantic name and qualifier: do not add a quantity, unit, or leading connector such as
`of`, `de`, or `d'`; the app renders those deterministically. Set `source_language` to the BCP-47
code of the language supplied in the fields, or null only if it cannot be identified.
"""
}
