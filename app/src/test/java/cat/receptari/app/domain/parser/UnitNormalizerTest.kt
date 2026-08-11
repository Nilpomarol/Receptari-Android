package cat.receptari.app.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class UnitNormalizerTest {

    private fun normalize(text: String) =
        UnitNormalizer.normalizeIngredient(text, Locale.forLanguageTag("ca"))

    // ------------------------------------------------------------ Symbols

    @Test
    fun `spelled-out metric units become symbols`() {
        assertEquals("200 g de farina", normalize("200 grams de farina"))
        assertEquals("200 g de farina", normalize("200 gramos de farina"))
        assertEquals("1 kg de patates", normalize("1 quilo de patates"))
        assertEquals("500 ml de llet", normalize("500 mil·lilitres de llet"))
        assertEquals("2 l d'aigua", normalize("2 litres d'aigua"))
    }

    @Test
    fun `a line already using symbols is returned byte-identical`() {
        val text = "200 g de farina"
        assertEquals(text, normalize(text))
    }

    // ------------------------------------------------------------ Conversions

    @Test
    fun `ounces and pounds convert to grams`() {
        // 8 oz = 226.8 g, rounded to what a kitchen scale shows.
        assertEquals("225 g de mantega", normalize("8 oz de mantega"))
        assertEquals("455 g de pollastre", normalize("1 lb de pollastre"))
    }

    @Test
    fun `cups convert to millilitres`() {
        // Volume to volume is exact. Volume to mass is not, and is never attempted.
        assertEquals("235 ml de llet", normalize("1 cup de llet"))
        assertEquals("120 ml de llet", normalize("½ tassa de llet"))
    }

    /**
     * The guardrail that matters most. A cup of flour is 120 g, of sugar 200 g, of butter
     * 227 g — there is no correct answer without knowing the ingredient, so the unit stays
     * a volume and the reader is never handed a confident wrong number.
     */
    @Test
    fun `cups of a dry ingredient still become millilitres, never grams`() {
        assertEquals("235 ml de farina", normalize("1 cup de farina"))
        assertEquals("475 ml de sucre", normalize("2 cups de sucre"))
    }

    // ------------------------------------------------------------ Spoons

    @Test
    fun `spoon measures are abbreviated in their own language`() {
        assertEquals("2 tbsp of olive oil", normalize("2 tablespoons of olive oil"))
        assertEquals("2 cs d'oli", normalize("2 cullerades d'oli"))
        assertEquals("2 cda de aceite", normalize("2 cucharadas de aceite"))
        assertEquals("1 tsp of salt", normalize("1 teaspoon of salt"))
        assertEquals("1 cp de sal", normalize("1 culleradeta de sal"))
        assertEquals("1 cdta de sal", normalize("1 cucharadita de sal"))
    }

    @Test
    fun `spoon measures are never converted to millilitres`() {
        // A spoon is a legitimate European measure; turning it into 15 ml helps nobody.
        assertEquals("3 cs de sucre", normalize("3 cullerades de sucre"))
    }

    // ------------------------------------------------------------ What it must not touch

    @Test
    fun `an unstructured line is untouched`() {
        assertEquals("Sal al gust", normalize("Sal al gust"))
        assertEquals("Un pessic de pebre", normalize("Un pessic de pebre"))
        assertEquals("A handful of parsley", normalize("A handful of parsley"))
    }

    @Test
    fun `counting words are left alone rather than translated`() {
        // "gra"/"clove"/"diente" have no language-neutral symbol, so abbreviating them
        // would mean translating them — which is Phase 5's job, not this one's.
        assertEquals("2 grans d'all", normalize("2 grans d'all"))
        assertEquals("2 cloves of garlic", normalize("2 cloves of garlic"))
        assertEquals("3 fulles de llorer", normalize("3 fulles de llorer"))
    }

    @Test
    fun `a quantity with no unit is left alone`() {
        assertEquals("4 ous", normalize("4 ous"))
        assertEquals("2 cebes", normalize("2 cebes"))
    }

    /** The elision that made rendering-from-parts unworkable in the first place. */
    @Test
    fun `the connector and everything after it survive`() {
        assertEquals("200 g d'all", normalize("200 grams d'all"))
        assertEquals("200 g de nata", normalize("200 grams de nata"))
        assertEquals("100 g of flour", normalize("100 grams of flour"))
    }

    @Test
    fun `punctuation after the unit is not swallowed`() {
        assertEquals("200 g, ben picat", normalize("200 grams, ben picat"))
        assertEquals("200 g (opcional)", normalize("200 grams (opcional)"))
    }

    @Test
    fun `ranges are converted at both ends`() {
        assertEquals("1–2 cs d'oli", normalize("1-2 cullerades d'oli"))
    }

    @Test
    fun `leading whitespace is preserved`() {
        assertEquals("  200 g de farina", normalize("  200 grams de farina"))
    }

    // ------------------------------------------------------------ Round trip

    /**
     * Normalised text is what gets stored, and it is re-parsed on every save and scale. So
     * every abbreviation produced here has to be a spelling [UnitLexicon] still recognises —
     * otherwise scaling would silently stop working on imported recipes.
     */
    @Test
    fun `normalized output parses back to a known unit`() {
        listOf(
            "2 cullerades d'oli" to "tbsp",
            "1 culleradeta de sal" to "tsp",
            "2 cucharadas de aceite" to "tbsp",
            "1 cucharadita de sal" to "tsp",
            "2 tablespoons of oil" to "tbsp",
            "200 grams de farina" to "g",
            "8 oz de mantega" to "g",
            "1 cup de llet" to "ml",
        ).forEach { (source, expectedUnit) ->
            val normalized = normalize(source)
            assertEquals(
                "$source normalised to \"$normalized\"",
                expectedUnit,
                IngredientParser.parse(normalized)?.unit,
            )
        }
    }

    // ------------------------------------------------------------ Temperatures

    @Test
    fun `fahrenheit oven temperatures become celsius`() {
        assertEquals(
            "Escalfar el forn a 175 °C.",
            UnitNormalizer.normalizeInstruction("Escalfar el forn a 350°F."),
        )
        assertEquals(
            "Preheat to 220 °C.",
            UnitNormalizer.normalizeInstruction("Preheat to 425 degrees F."),
        )
    }

    @Test
    fun `instruction text without a temperature is untouched`() {
        val text = "Sofregir la ceba 10 minuts a foc lent."
        assertEquals(text, UnitNormalizer.normalizeInstruction(text))
    }

    /** Celsius must not be re-converted, and a bare "F" in a word must not match. */
    @Test
    fun `celsius and ordinary prose are left alone`() {
        assertEquals("Coure a 180 °C.", UnitNormalizer.normalizeInstruction("Coure a 180 °C."))
        assertEquals("Afegir 200 g de Farina.", UnitNormalizer.normalizeInstruction("Afegir 200 g de Farina."))
    }
}
