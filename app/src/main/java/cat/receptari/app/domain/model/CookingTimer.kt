package cat.receptari.app.domain.model

/**
 * The one active kitchen timer.
 *
 * This is transient cooking state, not part of a recipe and not a cooked-history event.
 * A running timer stores an absolute deadline; a paused timer stores its remaining time.
 */
data class CookingTimer(
    val id: String,
    val recipeId: String,
    val stepId: String? = null,
    val label: String,
    val durationSeconds: Long,
    val status: CookingTimerStatus,
    val endsAtEpochMillis: Long? = null,
    val remainingSeconds: Long? = null,
)

enum class CookingTimerStatus {
    RUNNING,
    PAUSED,
}
