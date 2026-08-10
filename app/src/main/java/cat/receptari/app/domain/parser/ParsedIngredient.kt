package cat.receptari.app.domain.parser

/**
 * The result of parsing one ingredient line.
 *
 * Deliberately has no id — creating identity is the caller's job, which keeps the parser a
 * pure function of its input and therefore trivially testable.
 *
 * Every structured field is nullable and null is a normal, expected outcome. [originalText]
 * is the one guarantee: it always holds what the source actually said.
 */
data class ParsedIngredient(
    val quantity: Double? = null,
    val quantityMax: Double? = null,
    val unit: String? = null,
    val name: String? = null,
    val note: String? = null,
    val originalText: String,
)
