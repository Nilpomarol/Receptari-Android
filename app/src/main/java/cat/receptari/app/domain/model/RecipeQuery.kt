package cat.receptari.app.domain.model

/** Sorting options offered by the library (PRD §5). */
enum class RecipeSort {
    Alphabetical,
    RecentlyAdded,
    RecentlyCooked,
    MostCooked,
    HighestRated,
    CookingTime,
}

/** Filters offered by the library (PRD §5). They combine with AND. */
data class RecipeFilter(
    val favoritesOnly: Boolean = false,
    val tagIds: Set<String> = emptySet(),
    val neverCooked: Boolean = false,
    val cookedSinceEpochMillis: Long? = null,
) {
    val isActive: Boolean
        get() = favoritesOnly || tagIds.isNotEmpty() || neverCooked || cookedSinceEpochMillis != null

    val activeCount: Int
        get() = listOf(
            favoritesOnly,
            tagIds.isNotEmpty(),
            neverCooked,
            cookedSinceEpochMillis != null,
        ).count { it }
}

/**
 * A full library query: free-text search (PRD §6), filters, and a sort.
 *
 * [searchQuery] is blank in the common case, which skips the search-index join entirely.
 */
data class RecipeQuery(
    val searchQuery: String = "",
    val filter: RecipeFilter = RecipeFilter(),
    val sort: RecipeSort = RecipeSort.RecentlyAdded,
)
