package cat.receptari.app.domain.scaling

import cat.receptari.app.domain.model.Ingredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServingScalerTest {

    private fun ingredient(
        quantity: Double? = null,
        quantityMax: Double? = null,
        unit: String? = null,
        name: String? = null,
        originalText: String = "",
    ) = Ingredient(
        id = "i1",
        quantity = quantity,
        quantityMax = quantityMax,
        unit = unit,
        name = name,
        originalText = originalText,
    )

    // --- factor -------------------------------------------------------------------

    @Test
    fun `factor is the ratio of target to base servings`() {
        assertEquals(1.5, ServingScaler.factor(baseServings = 4, targetServings = 6)!!, 1e-9)
        assertEquals(0.5, ServingScaler.factor(baseServings = 4, targetServings = 2)!!, 1e-9)
    }

    @Test
    fun `factor is null when the recipe has no base servings`() {
        assertNull(ServingScaler.factor(baseServings = null, targetServings = 6))
    }

    @Test
    fun `factor is null for nonsensical serving counts`() {
        assertNull(ServingScaler.factor(baseServings = 0, targetServings = 6))
        assertNull(ServingScaler.factor(baseServings = -2, targetServings = 6))
        assertNull(ServingScaler.factor(baseServings = 4, targetServings = 0))
    }

    // --- scaling ------------------------------------------------------------------

    @Test
    fun `the PRD example scales 400 g to 600 g`() {
        val scaled = ServingScaler.scale(
            ingredient(quantity = 400.0, unit = "g", name = "pollastre", originalText = "400 g pollastre"),
            factor = ServingScaler.factor(baseServings = 4, targetServings = 6),
        )

        assertEquals(600.0, scaled.quantity!!, 1e-9)
        assertTrue(scaled.wasScaled)
    }

    @Test
    fun `ranges scale on both bounds`() {
        val scaled = ServingScaler.scale(
            ingredient(quantity = 1.0, quantityMax = 2.0, name = "ceba", originalText = "1–2 cebes"),
            factor = 2.0,
        )

        assertEquals(2.0, scaled.quantity!!, 1e-9)
        assertEquals(4.0, scaled.quantityMax!!, 1e-9)
    }

    @Test
    fun `ingredients without a numeric quantity are left alone`() {
        val salt = ingredient(name = "sal", originalText = "Sal al gust")

        val scaled = ServingScaler.scale(salt, factor = 3.0)

        assertNull(scaled.quantity)
        assertFalse(scaled.wasScaled)
        assertEquals("Sal al gust", scaled.originalText)
    }

    @Test
    fun `a null factor leaves every quantity untouched`() {
        val scaled = ServingScaler.scale(ingredient(quantity = 400.0, originalText = "400 g"), factor = null)

        assertEquals(400.0, scaled.quantity!!, 1e-9)
        assertFalse(scaled.wasScaled)
    }

    @Test
    fun `a factor of one is not reported as scaled`() {
        val scaled = ServingScaler.scale(ingredient(quantity = 400.0, originalText = "400 g"), factor = 1.0)

        assertEquals(400.0, scaled.quantity!!, 1e-9)
        assertFalse(scaled.wasScaled)
    }

    // --- the data-integrity guarantees --------------------------------------------

    @Test
    fun `scaling never mutates the stored ingredient`() {
        val stored = ingredient(quantity = 400.0, quantityMax = 500.0, originalText = "400–500 g")

        ServingScaler.scale(stored, factor = 2.0)

        assertEquals(400.0, stored.quantity!!, 1e-9)
        assertEquals(500.0, stored.quantityMax!!, 1e-9)
    }

    @Test
    fun `original text survives scaling untouched`() {
        val scaled = ServingScaler.scale(
            ingredient(quantity = 2.0, unit = "tbsp", name = "oli", originalText = "2 cullerades d'oli"),
            factor = 3.0,
        )

        assertEquals("2 cullerades d'oli", scaled.originalText)
    }

    @Test
    fun `unit name and note pass through unchanged`() {
        val source = ingredient(quantity = 2.0, unit = "tbsp", name = "oli d'oliva", originalText = "x")
            .copy(note = "verge extra")

        val scaled = ServingScaler.scale(source, factor = 2.0)

        assertEquals("tbsp", scaled.unit)
        assertEquals("oli d'oliva", scaled.name)
        assertEquals("verge extra", scaled.note)
    }
}
