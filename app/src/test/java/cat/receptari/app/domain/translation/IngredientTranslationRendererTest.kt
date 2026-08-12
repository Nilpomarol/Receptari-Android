package cat.receptari.app.domain.translation

import cat.receptari.app.domain.ai.RecipeLanguage
import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.parser.IngredientParser
import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientTranslationRendererTest {

    @Test
    fun `localizes count unit plural and Catalan elision`() {
        assertEquals(
            "2 grans d'all",
            prepared("2 cloves garlic").render("all", RecipeLanguage.CATALAN),
        )
    }

    @Test
    fun `localizes spoon and Spanish connector`() {
        assertEquals(
            "2 cucharadas de aceite de oliva",
            prepared("2 tbsp olive oil").render("aceite de oliva", RecipeLanguage.SPANISH),
        )
    }

    @Test
    fun `uses singular unit for a fraction up to one`() {
        assertEquals(
            "½ culleradeta de sal",
            prepared("½ teaspoon salt").render("sal", RecipeLanguage.CATALAN),
        )
    }

    @Test
    fun `metric unit keeps symbol and gains target connector`() {
        assertEquals(
            "200 g de farina",
            prepared("200 g flour").render("farina", RecipeLanguage.CATALAN),
        )
    }

    @Test
    fun `quantity without unit does not gain connector`() {
        assertEquals(
            "2 cebes",
            prepared("2 onions").render("cebes", RecipeLanguage.CATALAN),
        )
    }

    @Test
    fun `unstructured ingredient is translated as a complete line`() {
        assertEquals(
            "sal al gust",
            prepared("salt to taste").render("sal al gust", RecipeLanguage.CATALAN),
        )
    }

    @Test
    fun `measurement-only ingredient remains complete`() {
        assertEquals(
            "200 g",
            prepared("200 g").render("", RecipeLanguage.CATALAN),
        )
    }

    private fun prepared(text: String): IngredientTranslationRenderer.PreparedIngredient {
        val parsed = IngredientParser.parse(text)!!
        return IngredientTranslationRenderer.prepare(
            Ingredient(
                id = "ingredient",
                quantity = parsed.quantity,
                quantityMax = parsed.quantityMax,
                unit = parsed.unit,
                name = parsed.name,
                note = parsed.note,
                originalText = parsed.originalText,
            ),
        )
    }
}
