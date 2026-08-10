package cat.receptari.app.domain.model

import java.time.Instant

/**
 * The projection the library list reads. Deliberately not the full [Recipe] aggregate —
 * a list of 300 recipes must not pull every ingredient row into memory.
 */
data class RecipeSummary(
    val id: String,
    val title: String,
    val imagePath: String? = null,
    val totalTimeMinutes: Int? = null,
    val isFavorite: Boolean = false,
    val rating: Int? = null,
    val cookCount: Int = 0,
    val lastCookedAt: Instant? = null,
)
