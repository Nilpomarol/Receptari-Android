package cat.receptari.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * The library card has room for exactly one duration, so something has to decide which of
 * the three stored times is the honest one to print. These cases are that decision.
 */
class CookingTimeTest {

    @Test
    fun `a stored total wins over everything else`() {
        assertEquals(90, CookingTime.effective(total = 90, prep = 15, cook = 30))
    }

    /**
     * Deliberate: a recipe whose stored total disagrees with its parts keeps the stored
     * total. The user typed it, and quietly correcting them to 45 would be the app deciding
     * it knows the recipe better than they do.
     */
    @Test
    fun `a stored total is trusted even when it disagrees with the parts`() {
        assertEquals(90, CookingTime.effective(total = 90, prep = 15, cook = 30))
        assertEquals(20, CookingTime.effective(total = 20, prep = 60, cook = 60))
    }

    @Test
    fun `prep and cook together are summed`() {
        assertEquals(45, CookingTime.effective(total = null, prep = 15, cook = 30))
    }

    @Test
    fun `cook alone is the whole time`() {
        assertEquals(30, CookingTime.effective(total = null, prep = null, cook = 30))
    }

    @Test
    fun `prep alone is the whole time`() {
        assertEquals(15, CookingTime.effective(total = null, prep = 15, cook = null))
    }

    @Test
    fun `nothing recorded stays nothing`() {
        assertNull(CookingTime.effective(total = null, prep = null, cook = null))
    }

    // --- the derived total is narrower than the effective time --------------------

    @Test
    fun `a total is only derived when both parts are present`() {
        assertEquals(45, CookingTime.derivedTotal(prep = 15, cook = 30))
        assertNull(CookingTime.derivedTotal(prep = null, cook = 30))
        assertNull(CookingTime.derivedTotal(prep = 15, cook = null))
        assertNull(CookingTime.derivedTotal(prep = null, cook = null))
    }

    /**
     * A single time is not a total. The detail screen prints prep and cook on their own
     * line, and adding "Total 30 min" underneath "Cocció 30 min" says nothing twice.
     */
    @Test
    fun `one part alone does not become a total`() {
        assertNull(CookingTime.derivedTotal(prep = null, cook = 30))
        assertEquals(30, CookingTime.effective(total = null, prep = null, cook = 30))
    }

    // --- the models expose the same rule ------------------------------------------

    @Test
    fun `a summary reports the same time the rule gives`() {
        val summary = RecipeSummary(
            id = "1",
            title = "Fricandó",
            prepTimeMinutes = 20,
            cookTimeMinutes = 80,
        )

        assertEquals(100, summary.effectiveTimeMinutes)
    }

    @Test
    fun `a recipe reports the same time the rule gives`() {
        val recipe = Recipe(
            id = "1",
            title = "Fricandó",
            cookTimeMinutes = 80,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )

        assertEquals(80, recipe.effectiveTimeMinutes)
        assertNull(recipe.derivedTotalMinutes)
    }

    @Test
    fun `zero is a real answer, not a missing one`() {
        assertEquals(0, CookingTime.effective(total = 0, prep = null, cook = null))
        assertEquals(30, CookingTime.effective(total = null, prep = 0, cook = 30))
    }
}
