package cat.receptari.app.domain.model

/**
 * A named group of steps, e.g. "Prepare sauce". [name] is null for the single unnamed
 * section a recipe without groupings has.
 */
data class InstructionSection(
    val id: String,
    val name: String? = null,
    val steps: List<Step> = emptyList(),
)

/**
 * One instruction step. Display numbering runs continuously across sections (PRD §3.4) and
 * is computed at render time, not stored.
 *
 * [originalText] holds the pre-translation text when the recipe has been translated
 * (ADR-005).
 */
data class Step(
    val id: String,
    val text: String,
    val originalText: String? = null,
)
