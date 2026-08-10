package cat.receptari.app.domain.model

import java.time.Instant

/**
 * One occasion on which a recipe was cooked.
 *
 * Cooked history is an event log, never a counter (PRD §4): "times cooked" and "last
 * cooked" are derived from these rows, because a recipe can be made repeatedly and a
 * boolean "done" flag would throw that away.
 */
data class CookEvent(
    val id: String,
    val recipeId: String,
    val cookedAt: Instant,
    val note: String? = null,
)
