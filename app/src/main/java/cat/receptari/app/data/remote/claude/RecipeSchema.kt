package cat.receptari.app.data.remote.claude

/**
 * The JSON Schema the model must fill in, and the prompt that governs how.
 *
 * Structured outputs constrain the response to this shape, so there is no prose to strip
 * and no "sometimes it wraps the JSON in a code fence" handling.
 *
 * Built as a map rather than a raw JSON string because the SDK wants a `JsonValue` — this
 * skips a parse step and lets Kotlin catch a malformed schema at compile time.
 *
 * Schema constraints that bite here: every object needs `additionalProperties: false` and a
 * complete `required` list, and there is no `minLength`/`maxLength`. Nullability uses
 * `anyOf` rather than a type array, which the API does not document as supported.
 */
internal object RecipeSchema {

    private fun nullable(type: String): Map<String, Any> = mapOf(
        "anyOf" to listOf(mapOf("type" to type), mapOf("type" to "null")),
    )

    private fun section(): Map<String, Any> = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf("name", "lines"),
        "properties" to mapOf(
            "name" to nullable("string"),
            "lines" to mapOf("type" to "array", "items" to mapOf("type" to "string")),
        ),
    )

    val MAP: Map<String, Any> = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "required" to listOf(
            "title", "prep_time_minutes", "cook_time_minutes", "total_time_minutes",
            "servings", "language", "source_name", "notes",
            "ingredient_sections", "instruction_sections",
        ),
        "properties" to mapOf(
            "title" to nullable("string"),
            "prep_time_minutes" to nullable("integer"),
            "cook_time_minutes" to nullable("integer"),
            "total_time_minutes" to nullable("integer"),
            "servings" to nullable("integer"),
            "language" to nullable("string"),
            "source_name" to nullable("string"),
            "notes" to nullable("string"),
            "ingredient_sections" to mapOf("type" to "array", "items" to section()),
            "instruction_sections" to mapOf("type" to "array", "items" to section()),
        ),
    )

    /**
     * The extraction contract (AI_INTEGRATION.md §6).
     *
     * The rules that matter are the ones about *not* helping: no inventing, no converting,
     * no tidying. The app's own parser turns lines into structure, so the model's only job
     * is to find the lines and keep them verbatim — which is what makes
     * `Ingredient.originalText` trustworthy end to end.
     */
    const val SYSTEM_PROMPT: String = """
You extract recipes into a fixed JSON structure.

Copy ingredient lines and steps exactly as the source writes them. Do not convert units,
normalise quantities, translate, expand abbreviations, or tidy wording. "2 cullerades
d'oli" stays "2 cullerades d'oli". A line you cannot interpret, such as "sal al gust", is
still an ingredient line — include it unchanged.

Use null for anything the source does not state. Never infer or estimate times, servings,
or ingredients that are not written down. An incomplete recipe is correct; a plausible
invention is not.

Preserve the source's own grouping. If ingredients or steps appear under headings such as
"Sauce" or "Per la massa", keep those as separate sections with that name. If there are no
headings, return a single section with a null name.

Put step text in `lines`, one entry per step, without leading numbers. Set `language` to
the BCP-47 code of the language the recipe is written in.
"""
}
