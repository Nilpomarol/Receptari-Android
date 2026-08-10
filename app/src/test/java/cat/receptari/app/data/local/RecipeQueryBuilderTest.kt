package cat.receptari.app.data.local

import cat.receptari.app.domain.model.RecipeFilter
import cat.receptari.app.domain.model.RecipeQuery
import cat.receptari.app.domain.model.RecipeSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeQueryBuilderTest {

    // --- FTS match expressions ----------------------------------------------------

    @Test
    fun `terms get a wildcard so partial matches work`() {
        // PRD §6: searching "chick" must find "chicken".
        assertEquals("chick*", RecipeQueryBuilder.matchExpression("chick"))
    }

    @Test
    fun `multiple words become multiple wildcard terms`() {
        assertEquals("pollastre* salsa*", RecipeQueryBuilder.matchExpression("pollastre salsa"))
    }

    @Test
    fun `blank input produces no expression at all`() {
        assertNull(RecipeQueryBuilder.matchExpression(""))
        assertNull(RecipeQueryBuilder.matchExpression("   "))
    }

    @Test
    fun `FTS syntax characters are stripped rather than passed through`() {
        // These are operators to FTS4. Passing them raw makes SQLite throw on ordinary
        // typing — a quote or a hyphen is not an error the user should ever see.
        assertEquals("pollastre*", RecipeQueryBuilder.matchExpression("\"pollastre\""))
        assertEquals("pa*", RecipeQueryBuilder.matchExpression("pa-"))
        assertEquals("all*", RecipeQueryBuilder.matchExpression("(all)"))
        assertEquals("sopa*", RecipeQueryBuilder.matchExpression("sopa:"))
        assertEquals("ceba*", RecipeQueryBuilder.matchExpression("^ceba*"))
    }

    @Test
    fun `input made only of punctuation yields no expression`() {
        assertNull(RecipeQueryBuilder.matchExpression("\"\""))
        assertNull(RecipeQueryBuilder.matchExpression("---"))
        assertNull(RecipeQueryBuilder.matchExpression("*"))
    }

    @Test
    fun `accented and non-latin characters survive`() {
        assertEquals("tomàquet*", RecipeQueryBuilder.matchExpression("tomàquet"))
        assertEquals("ñora*", RecipeQueryBuilder.matchExpression("ñora"))
    }

    // --- generated SQL ------------------------------------------------------------

    @Test
    fun `an unfiltered query has no where clause`() {
        val query = RecipeQueryBuilder.build(RecipeQuery())

        assertTrue(!query.sql.contains("WHERE"))
        assertEquals(0, query.argCount)
    }

    @Test
    fun `the favourites filter needs no bound argument`() {
        val query = RecipeQueryBuilder.build(
            RecipeQuery(filter = RecipeFilter(favoritesOnly = true)),
        )

        assertTrue(query.sql.contains("r.isFavorite = 1"))
        assertEquals(0, query.argCount)
    }

    @Test
    fun `search text is bound as an argument, never interpolated`() {
        val query = RecipeQueryBuilder.build(RecipeQuery(searchQuery = "pollastre"))

        assertTrue(query.sql.contains("recipe_fts MATCH ?"))
        assertTrue(!query.sql.contains("pollastre"))
        assertEquals(1, query.argCount)
    }

    @Test
    fun `multiple tags become one placeholder each`() {
        val query = RecipeQueryBuilder.build(
            RecipeQuery(filter = RecipeFilter(tagIds = setOf("a", "b", "c"))),
        )

        assertEquals(3, query.argCount)
        // All three tags must be present on a recipe, not just any one of them.
        assertTrue(query.sql.contains("COUNT(DISTINCT tagId) = 3"))
    }

    @Test
    fun `never cooked filters on the absence of any cook event`() {
        val query = RecipeQueryBuilder.build(RecipeQuery(filter = RecipeFilter(neverCooked = true)))

        assertTrue(query.sql.contains("NOT EXISTS"))
    }

    @Test
    fun `filters combine with AND`() {
        val query = RecipeQueryBuilder.build(
            RecipeQuery(
                searchQuery = "pollastre",
                filter = RecipeFilter(favoritesOnly = true, neverCooked = true),
            ),
        )

        assertEquals(2, query.sql.split(" AND ").size - 1)
    }

    @Test
    fun `never-cooked recipes sort last when ordering by last cooked`() {
        val query = RecipeQueryBuilder.build(RecipeQuery(sort = RecipeSort.RecentlyCooked))

        // SQLite puts NULL first on DESC, which would show never-cooked recipes at the top
        // of a "recently cooked" list.
        assertTrue(query.sql.contains("lastCookedAt IS NULL, lastCookedAt DESC"))
    }

    @Test
    fun `unrated recipes sort last when ordering by rating`() {
        val query = RecipeQueryBuilder.build(RecipeQuery(sort = RecipeSort.HighestRated))

        assertTrue(query.sql.contains("r.rating IS NULL, r.rating DESC"))
    }

    @Test
    fun `every sort produces an order by clause`() {
        RecipeSort.entries.forEach { sort ->
            val query = RecipeQueryBuilder.build(RecipeQuery(sort = sort))
            assertTrue("no ORDER BY for $sort", query.sql.contains("ORDER BY"))
        }
    }
}
