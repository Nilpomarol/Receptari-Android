package cat.receptari.app.data.translation

import cat.receptari.app.data.local.entity.RecipeTranslationEntity
import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.model.IngredientSection
import cat.receptari.app.domain.model.InstructionSection
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.Step
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeTranslationCacheMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `translation survives cache round trip`() {
        val recipe = translatedRecipe()

        val entity = recipe.toTranslationEntity(json)
        assertNotNull(entity)
        val restored = entity!!.toDomain(json)
        assertNotNull(restored)

        assertEquals("recipe", restored!!.recipeId)
        assertEquals("ca", restored.language)
        assertEquals("en", restored.sourceLanguage)
        assertEquals(EXPECTED_SOURCE_FINGERPRINT, restored.sourceFingerprint)
        assertEquals("Pa amb oli", restored.title)
        assertEquals("2 cullerades d'oli d'oliva", restored.ingredients.single().text)
        assertEquals("Torra el pa.", restored.steps.single().text)
    }

    @Test
    fun `source display does not create a translation cache entry`() {
        assertNull(translatedRecipe().copy(displayLanguage = null).toTranslationEntity(json))
    }

    @Test
    fun `corrupt cached payload is ignored`() {
        val entity = RecipeTranslationEntity(
            recipeId = "recipe",
            language = "ca",
            payloadJson = "not json",
            updatedAt = 1L,
        )

        assertNull(entity.toDomain(json))
    }

    private fun translatedRecipe(): Recipe = Recipe(
        id = "recipe",
        title = "Pa amb oli",
        originalTitle = "Olive oil toast",
        originalLanguage = "en",
        displayLanguage = "ca",
        createdAt = Instant.ofEpochMilli(1L),
        updatedAt = Instant.ofEpochMilli(2L),
        ingredientSections = listOf(
            IngredientSection(
                id = "ingredients",
                name = "Ingredients",
                originalName = "Ingredients",
                ingredients = listOf(
                    Ingredient(
                        id = "oil",
                        quantity = 2.0,
                        unit = "tbsp",
                        name = "olive oil",
                        originalText = "2 tbsp olive oil",
                        displayText = "2 cullerades d'oli d'oliva",
                    ),
                ),
            ),
        ),
        instructionSections = listOf(
            InstructionSection(
                id = "instructions",
                name = "Preparació",
                originalName = "Method",
                steps = listOf(
                    Step(
                        id = "toast",
                        text = "Torra el pa.",
                        originalText = "Toast the bread.",
                    ),
                ),
            ),
        ),
    )

    private companion object {
        const val EXPECTED_SOURCE_FINGERPRINT =
            "ceafaf38f41e29ac94bc17ee25a2f5152282f28055519a51ba2d2a3b666f88a2"
    }
}
