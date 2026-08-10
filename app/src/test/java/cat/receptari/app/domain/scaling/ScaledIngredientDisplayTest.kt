package cat.receptari.app.domain.scaling

import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.parser.IngredientParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * How an ingredient line reads on screen.
 *
 * These exist because rebuilding a line from `quantity + unit + name` produced grammatically
 * broken Catalan — "2 grans all" instead of "2 grans d'all", "200 ml nata" instead of
 * "200 ml de nata" — since the parser strips the connector to isolate the name.
 */
class ScaledIngredientDisplayTest {

    private val ca = Locale.forLanguageTag("ca")

    private fun scaled(text: String, factor: Double?): ScaledIngredient {
        val parsed = IngredientParser.parse(text)!!
        val ingredient = Ingredient(
            id = "i1",
            quantity = parsed.quantity,
            quantityMax = parsed.quantityMax,
            unit = parsed.unit,
            name = parsed.name,
            note = parsed.note,
            originalText = parsed.originalText,
        )
        return ServingScaler.scale(ingredient, factor)
    }

    @Test
    fun `an unscaled line reads exactly as it was written`() {
        assertEquals("2 grans d'all", scaled("2 grans d'all", null).displayText(ca))
        assertEquals("200 ml de nata", scaled("200 ml de nata", null).displayText(ca))
        assertEquals("Sal al gust", scaled("Sal al gust", null).displayText(ca))
    }

    @Test
    fun `scaling swaps the quantity and keeps every word around it`() {
        assertEquals("6 grans d'all", scaled("2 grans d'all", 3.0).displayText(ca))
        assertEquals("400 ml de nata", scaled("200 ml de nata", 2.0).displayText(ca))
        assertEquals("1200 g de vedella", scaled("800 g de vedella", 1.5).displayText(ca))
    }

    @Test
    fun `the connector survives scaling`() {
        // The bug this guards: "3 grans all" and "150 ml vi blanc".
        val garlic = scaled("2 grans d'all", 1.5).displayText(ca)
        val wine = scaled("100 ml de vi blanc", 1.5).displayText(ca)

        assertEquals("3 grans d'all", garlic)
        assertEquals("150 ml de vi blanc", wine)
    }

    @Test
    fun `plural wording in the source is preserved when scaling a range`() {
        assertEquals("2–4 fulles de llorer", scaled("1–2 fulles de llorer", 2.0).displayText(ca))
    }

    @Test
    fun `ingredients without a quantity are never rewritten`() {
        assertEquals("Sal al gust", scaled("Sal al gust", 3.0).displayText(ca))
        assertEquals("Farina per enfarinar", scaled("Farina per enfarinar", 3.0).displayText(ca))
        assertEquals("A handful of parsley", scaled("A handful of parsley", 2.0).displayText(ca))
    }

    @Test
    fun `fractions scale into readable quantities`() {
        assertEquals("4 ½ tomàquets madurs", scaled("3 tomàquets madurs", 1.5).displayText(ca))
        assertEquals("3 cups flour", scaled("1 1/2 cups flour", 2.0).displayText(ca))
    }

    // --- rounding that suits what is being measured -------------------------------

    @Test
    fun `countable items do not end up in fractions of a thing`() {
        // 8 rovells scaled 6→8 servings is 10.666…; you cannot crack two thirds of a yolk.
        assertEquals("11 rovells d'ou", scaled("8 rovells d'ou", 8.0 / 6).displayText(ca))
    }

    @Test
    fun `small counts keep their halves and thirds`() {
        // The noun stays as the source wrote it — only the quantity is substituted, so a
        // singular source stays singular. Rewriting nouns would need real morphology.
        assertEquals("1 ½ ceba", scaled("1 ceba", 1.5).displayText(ca))
        assertEquals("4 ½ tomàquets madurs", scaled("3 tomàquets madurs", 1.5).displayText(ca))
    }

    @Test
    fun `grams round to something a kitchen scale can show`() {
        // 200 g scaled 6→8 is 266.66…
        assertEquals("265 g de sucre", scaled("200 g de sucre", 8.0 / 6).displayText(ca))
        assertEquals("53 g de midó", scaled("40 g de midó", 8.0 / 6).displayText(ca))
    }

    @Test
    fun `millilitres round the same way as grams`() {
        assertEquals("265 ml de nata", scaled("200 ml de nata", 8.0 / 6).displayText(ca))
    }

    @Test
    fun `whole gram amounts are left exactly alone`() {
        assertEquals("1200 g de vedella", scaled("800 g de vedella", 1.5).displayText(ca))
        assertEquals("150 ml de vi blanc", scaled("100 ml de vi blanc", 1.5).displayText(ca))
    }

    @Test
    fun `litres keep their fractions because the numbers stay small`() {
        assertEquals("1 ⅓ l de llet", scaled("1 l de llet", 8.0 / 6).displayText(ca))
    }

    @Test
    fun `spoons and cups keep their fractions`() {
        assertEquals("1 ½ cullerada d'oli", scaled("1 cullerada d'oli", 1.5).displayText(ca))
        assertEquals("3 cups flour", scaled("1 1/2 cups flour", 2.0).displayText(ca))
    }

    @Test
    fun `a unit written against the number keeps its spacing`() {
        assertEquals("400g farina", scaled("200g farina", 2.0).displayText(ca))
    }

    @Test
    fun `leading whitespace does not shift the substitution`() {
        val ingredient = Ingredient(
            id = "i1",
            quantity = 2.0,
            unit = "g",
            name = "sal",
            originalText = "2 g de sal",
        )
        assertEquals("4 g de sal", ServingScaler.scale(ingredient, 2.0).displayText(ca))
    }
}
