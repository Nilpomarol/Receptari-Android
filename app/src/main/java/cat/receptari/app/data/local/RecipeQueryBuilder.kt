package cat.receptari.app.data.local

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import cat.receptari.app.domain.model.RecipeQuery
import cat.receptari.app.domain.model.RecipeSort

/**
 * Builds the library list query. Filtering and sorting happen in SQL, not in Kotlin — the
 * list has to stay responsive with a few hundred recipes and sorting by derived cook counts
 * in memory would mean loading every cook event.
 */
object RecipeQueryBuilder {

    fun build(query: RecipeQuery): SupportSQLiteQuery {
        val conditions = mutableListOf<String>()
        val arguments = mutableListOf<Any>()

        if (query.filter.favoritesOnly) {
            conditions += "r.isFavorite = 1"
        }

        matchExpression(query.searchQuery)?.let { match ->
            conditions += "r.id IN (SELECT recipeId FROM recipe_fts WHERE recipe_fts MATCH ?)"
            arguments += match
        }

        if (query.filter.tagIds.isNotEmpty()) {
            // HAVING COUNT(DISTINCT ...) makes multiple tags an AND: a recipe must carry
            // every selected tag, not just one of them.
            val placeholders = query.filter.tagIds.joinToString(",") { "?" }
            conditions += """
                r.id IN (
                    SELECT recipeId FROM recipe_tag_cross_ref
                    WHERE tagId IN ($placeholders)
                    GROUP BY recipeId
                    HAVING COUNT(DISTINCT tagId) = ${query.filter.tagIds.size}
                )
            """.trimIndent()
            arguments.addAll(query.filter.tagIds)
        }

        if (query.filter.neverCooked) {
            conditions += "NOT EXISTS (SELECT 1 FROM cook_events e WHERE e.recipeId = r.id)"
        }

        query.filter.cookedSinceEpochMillis?.let { since ->
            conditions += "EXISTS (SELECT 1 FROM cook_events e WHERE e.recipeId = r.id AND e.cookedAt >= ?)"
            arguments += since
        }

        val where = if (conditions.isEmpty()) "" else "WHERE " + conditions.joinToString(" AND ")

        val sql = """
            SELECT r.id AS id,
                   r.title AS title,
                   r.imagePath AS imagePath,
                   r.totalTimeMinutes AS totalTimeMinutes,
                   r.isFavorite AS isFavorite,
                   r.rating AS rating,
                   COUNT(c.id) AS cookCount,
                   MAX(c.cookedAt) AS lastCookedAt
            FROM recipes r
            LEFT JOIN cook_events c ON c.recipeId = r.id
            $where
            GROUP BY r.id
            ORDER BY ${orderBy(query.sort)}
        """.trimIndent()

        return SimpleSQLiteQuery(sql, arguments.toTypedArray())
    }

    /**
     * Turns what the user typed into an FTS4 MATCH expression.
     *
     * Every term gets a `*` so partial matches work (PRD §6: "chick" finds "chicken"), and
     * everything that is not a letter or digit is dropped — FTS4 treats characters like
     * `"`, `*`, `(`, `-` and `:` as syntax, so passing raw input through would throw a
     * SQLite error on perfectly ordinary typing.
     */
    fun matchExpression(searchQuery: String): String? {
        val terms = searchQuery
            .split(Regex("\\s+"))
            .map { term -> term.filter { it.isLetterOrDigit() } }
            .filter { it.isNotEmpty() }

        if (terms.isEmpty()) return null

        // Space-separated terms are an implicit AND in FTS4.
        return terms.joinToString(" ") { "$it*" }
    }

    private fun orderBy(sort: RecipeSort): String = when (sort) {
        RecipeSort.Alphabetical -> "r.title COLLATE NOCASE ASC"
        RecipeSort.RecentlyAdded -> "r.createdAt DESC"
        // SQLite sorts NULL first on DESC, so push never-cooked recipes to the bottom.
        RecipeSort.RecentlyCooked -> "lastCookedAt IS NULL, lastCookedAt DESC"
        RecipeSort.MostCooked -> "cookCount DESC, r.title COLLATE NOCASE ASC"
        RecipeSort.HighestRated -> "r.rating IS NULL, r.rating DESC, r.title COLLATE NOCASE ASC"
        RecipeSort.CookingTime -> "r.totalTimeMinutes IS NULL, r.totalTimeMinutes ASC"
    }
}
