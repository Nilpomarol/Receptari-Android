package cat.receptari.app.data.transfer

import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.model.IngredientSection
import cat.receptari.app.domain.model.InstructionSection
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.Step
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.domain.transfer.transferContentFingerprint
import cat.receptari.app.domain.translation.CachedTranslationField
import cat.receptari.app.domain.translation.RecipeTranslationVariant
import cat.receptari.app.domain.translation.translationSourceFingerprint
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeTransferMappingTest {
    @Test
    fun `import assigns fresh ids and remaps translation fields without losing source text`() {
        val source = recipe()
        val translation = RecipeTranslationVariant(
            recipeId = source.id,
            language = "ca",
            sourceLanguage = "en",
            sourceFingerprint = source.translationSourceFingerprint(),
            title = "Pa torrat",
            ingredientSections = listOf(CachedTranslationField("ingredient-section", null)),
            ingredients = listOf(CachedTranslationField("ingredient", "1 llesca de pa")),
            instructionSections = listOf(CachedTranslationField("instruction-section", null)),
            steps = listOf(CachedTranslationField("step", "Torra el pa.")),
        )

        val imported = source.toTransferDto(null, listOf(translation))
            .toImportedBundle(Instant.parse("2026-08-13T10:00:00Z"))

        assertNotEquals(source.id, imported.recipe.id)
        assertNotEquals(source.allIngredients.single().id, imported.recipe.allIngredients.single().id)
        assertEquals("1 slice of sourdough bread", imported.recipe.allIngredients.single().originalText)
        assertEquals(source.transferContentFingerprint(), imported.recipe.transferContentFingerprint())
        assertFalse(imported.recipe.isFavorite)
        assertNull(imported.recipe.rating)
        assertNull(imported.recipe.notes)
        assertNull(imported.recipe.folder)
        assertEquals(0, imported.recipe.cookCount)

        val importedTranslation = imported.translations.single()
        assertEquals(imported.recipe.id, importedTranslation.recipeId)
        assertEquals(imported.recipe.allIngredients.single().id, importedTranslation.ingredients.single().id)
        assertEquals(imported.recipe.translationSourceFingerprint(), importedTranslation.sourceFingerprint)
    }

    private fun recipe(): Recipe = Recipe(
        id = "recipe",
        title = "Pa torrat",
        originalTitle = "Toast",
        prepTimeMinutes = 2,
        baseServings = 1,
        isFavorite = true,
        rating = 5,
        notes = "Private note",
        originalLanguage = "en",
        displayLanguage = "ca",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        ingredientSections = listOf(
            IngredientSection(
                id = "ingredient-section",
                ingredients = listOf(
                    Ingredient(
                        id = "ingredient",
                        quantity = 1.0,
                        unit = "slice",
                        name = "sourdough bread",
                        originalText = "1 slice of sourdough bread",
                        displayText = "1 llesca de pa de massa mare",
                    ),
                ),
            ),
        ),
        instructionSections = listOf(
            InstructionSection(
                id = "instruction-section",
                steps = listOf(Step("step", "Torra el pa.", "Toast the bread.")),
            ),
        ),
        tags = listOf(Tag("tag", "Breakfast")),
        cookCount = 4,
        lastCookedAt = Instant.EPOCH,
    )
}
