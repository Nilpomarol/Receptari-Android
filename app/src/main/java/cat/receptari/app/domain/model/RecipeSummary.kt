package cat.receptari.app.domain.model

import java.time.Instant

/**
 * The projection the library list reads. Deliberately not the full [Recipe] aggregate —
 * a list of 300 recipes must not pull every ingredient row into memory.
 *
 * The three time columns are all carried even though the card prints one, because which one
 * that is depends on the other two — see [CookingTime].
 */
data class RecipeSummary(
    val id: String,
    val title: String,
    val imagePath: String? = null,
    val prepTimeMinutes: Int? = null,
    val cookTimeMinutes: Int? = null,
    val totalTimeMinutes: Int? = null,
    val baseServings: Int? = null,
    val isFavorite: Boolean = false,
    val rating: Int? = null,
    val cookCount: Int = 0,
    val lastCookedAt: Instant? = null,
) {
    /** The single duration worth printing on a card. See [CookingTime.effective]. */
    val effectiveTimeMinutes: Int?
        get() = CookingTime.effective(totalTimeMinutes, prepTimeMinutes, cookTimeMinutes)
}
