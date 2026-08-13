package cat.receptari.app.domain.translation

import cat.receptari.app.domain.ai.AiClient
import cat.receptari.app.domain.ai.DraftRecipe
import cat.receptari.app.domain.ai.RecipeLanguage
import cat.receptari.app.domain.ai.TranslationField
import cat.receptari.app.domain.ai.TranslationRequest
import cat.receptari.app.domain.ai.TranslationResult
import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.model.IngredientSection
import cat.receptari.app.domain.model.InstructionSection
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.Step
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.domain.repository.RecipeTranslationRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslateRecipeTest {
    private val ai = FakeAiClient()
    private val cache = FakeTranslationRepository()
    private val useCase = TranslateRecipe(
        ai,
        cache,
        Clock.fixed(TRANSLATED_AT, ZoneOffset.UTC),
    )

    @Test
    fun `request excludes measurement and unrelated recipe data`() = runTest {
        ai.answer = Result.success(translationResult())

        useCase(recipe(), RecipeLanguage.CATALAN).getOrThrow()

        val request = ai.request!!
        assertEquals("ca", request.targetLanguage.languageTag)
        assertEquals("olive oil", request.ingredients.single().text)
        assertTrue(request.ingredients.single().text.none(Char::isDigit))
        assertTrue(request.toString().contains("tbsp").not())
        assertTrue(request.toString().contains("example.com").not())
        assertTrue(request.toString().contains("Grandma").not())
        assertTrue(request.toString().contains("secret note").not())
        assertTrue(request.toString().contains("Quick").not())
    }

    @Test
    fun `merge preserves source and renders localized measurement grammar`() = runTest {
        ai.answer = Result.success(translationResult())

        val translated = useCase(recipe(), RecipeLanguage.CATALAN).getOrThrow()
        val ingredient = translated.allIngredients.single()
        val step = translated.instructionSections.single().steps.single()

        assertEquals("Olive oil toast", translated.originalTitle)
        assertEquals("en", translated.originalLanguage)
        assertEquals("ca", translated.displayLanguage)
        assertEquals("2 tbsp olive oil", ingredient.originalText)
        assertEquals("2 cullerades d'oli d'oliva", ingredient.displayText)
        assertEquals("Toast the bread.", step.originalText)
        assertEquals("Torra el pa.", step.text)
        assertEquals(listOf(Tag("tag", "Quick")), translated.tags)
        assertEquals(TRANSLATED_AT, translated.updatedAt)
    }

    @Test
    fun `retranslation always starts from canonical source`() = runTest {
        ai.answer = Result.success(translationResult())
        val catalan = useCase(recipe(), RecipeLanguage.CATALAN).getOrThrow()
        ai.answer = Result.success(
            translationResult(
                title = "Tostada de aceite de oliva",
                ingredient = "aceite de oliva",
                step = "Tuesta el pan.",
            ),
        )

        val spanish = useCase(catalan, RecipeLanguage.SPANISH).getOrThrow()

        assertEquals("Olive oil toast", ai.request!!.title)
        assertEquals("olive oil", ai.request!!.ingredients.single().text)
        assertEquals("Olive oil toast", spanish.originalTitle)
        assertEquals("2 tbsp olive oil", spanish.allIngredients.single().originalText)
        assertEquals("Toast the bread.", spanish.instructionSections.single().steps.single().originalText)
        assertEquals("2 cucharadas de aceite de oliva", spanish.allIngredients.single().displayText)
    }

    @Test
    fun `cached language bypasses the model`() = runTest {
        val source = recipe()
        cache.variant = RecipeTranslationVariant(
            recipeId = source.id,
            language = "ca",
            sourceLanguage = "en",
            sourceFingerprint = source.translationSourceFingerprint(),
            title = "Pa torrat amb oli d'oliva",
            ingredientSections = listOf(CachedTranslationField("ingredients", "Ingredients")),
            ingredients = listOf(CachedTranslationField("oil", "2 cullerades d'oli d'oliva")),
            instructionSections = listOf(CachedTranslationField("instructions", "Elaboració")),
            steps = listOf(CachedTranslationField("toast", "Torra el pa.")),
        )

        val translated = useCase(source, RecipeLanguage.CATALAN).getOrThrow()

        assertNull(ai.request)
        assertEquals("Pa torrat amb oli d'oliva", translated.title)
        assertEquals("2 cullerades d'oli d'oliva", translated.allIngredients.single().displayText)
    }

    @Test
    fun `stale cached language is ignored`() = runTest {
        cache.variant = RecipeTranslationVariant(
            recipeId = "recipe",
            language = "ca",
            sourceLanguage = "en",
            sourceFingerprint = "old-source",
            title = "Stale",
            ingredientSections = listOf(CachedTranslationField("ingredients", "Ingredients")),
            ingredients = listOf(CachedTranslationField("oil", "stale")),
            instructionSections = listOf(CachedTranslationField("instructions", "Elaboració")),
            steps = listOf(CachedTranslationField("toast", "stale")),
        )
        ai.answer = Result.success(translationResult())

        val translated = useCase(recipe(), RecipeLanguage.CATALAN).getOrThrow()

        assertEquals("Torrada amb oli d'oliva", translated.title)
        assertTrue(ai.request != null)
    }

    @Test
    fun `selecting source language restores original without model call`() = runTest {
        ai.answer = Result.success(translationResult())
        val catalan = useCase(recipe(), RecipeLanguage.CATALAN).getOrThrow()
        ai.request = null

        val restored = useCase(catalan, RecipeLanguage.ENGLISH).getOrThrow()

        assertNull(ai.request)
        assertNull(restored.displayLanguage)
        assertEquals("Olive oil toast", restored.title)
        assertNull(restored.allIngredients.single().displayText)
        assertEquals("Toast the bread.", restored.instructionSections.single().steps.single().text)
    }

    @Test
    fun `changed ids reject the entire translation`() = runTest {
        ai.answer = Result.success(
            translationResult().copy(
                ingredients = listOf(TranslationField("wrong-id", "oli")),
            ),
        )

        assertTrue(useCase(recipe(), RecipeLanguage.CATALAN).isFailure)
    }

    @Test
    fun `measurement-only ingredient is omitted from request and retained`() = runTest {
        val recipe = recipe().copy(
            ingredientSections = listOf(
                IngredientSection(
                    id = "ingredients",
                    ingredients = listOf(
                        Ingredient(
                            id = "oil",
                            quantity = 200.0,
                            unit = "g",
                            originalText = "200 g",
                        ),
                    ),
                ),
            ),
        )
        ai.answer = Result.success(
            translationResult().copy(
                ingredientSections = emptyList(),
                ingredients = emptyList(),
            ),
        )

        val translated = useCase(recipe, RecipeLanguage.CATALAN).getOrThrow()

        assertTrue(ai.request!!.ingredients.isEmpty())
        assertEquals("200 g", translated.allIngredients.single().displayText)
    }

    private fun recipe() = Recipe(
        id = "recipe",
        title = "Olive oil toast",
        sourceName = "Grandma",
        sourceUrl = "https://example.com/recipe",
        notes = "secret note",
        originalLanguage = "en",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        ingredientSections = listOf(
            IngredientSection(
                id = "ingredients",
                name = "Ingredients",
                ingredients = listOf(
                    Ingredient(
                        id = "oil",
                        quantity = 2.0,
                        unit = "tbsp",
                        name = "olive oil",
                        originalText = "2 tbsp olive oil",
                    ),
                ),
            ),
        ),
        instructionSections = listOf(
            InstructionSection(
                id = "instructions",
                name = "Method",
                steps = listOf(Step("toast", "Toast the bread.")),
            ),
        ),
        tags = listOf(Tag("tag", "Quick")),
    )

    private fun translationResult(
        title: String = "Torrada amb oli d'oliva",
        ingredient: String = "oli d'oliva",
        step: String = "Torra el pa.",
    ) = TranslationResult(
        title = title,
        sourceLanguage = "en",
        ingredientSections = listOf(TranslationField("ingredients", "Ingredients")),
        ingredients = listOf(TranslationField("oil", ingredient)),
        instructionSections = listOf(TranslationField("instructions", "Elaboració")),
        steps = listOf(TranslationField("toast", step)),
    )

    private class FakeTranslationRepository : RecipeTranslationRepository {
        var variant: RecipeTranslationVariant? = null

        override suspend fun get(recipeId: String, language: String): RecipeTranslationVariant? =
            variant?.takeIf { it.recipeId == recipeId && it.language == language }

        override suspend fun getAll(
            recipeId: String,
            sourceFingerprint: String,
        ): List<RecipeTranslationVariant> = listOfNotNull(
            variant?.takeIf {
                it.recipeId == recipeId && it.sourceFingerprint == sourceFingerprint
            },
        )

        override suspend fun save(variant: RecipeTranslationVariant) {
            this.variant = variant
        }

        override suspend fun getAvailableLanguages(
            recipeId: String,
            sourceFingerprint: String,
        ): Set<String> = variant
            ?.takeIf {
                it.recipeId == recipeId &&
                    (it.sourceFingerprint.isEmpty() || it.sourceFingerprint == sourceFingerprint)
            }
            ?.let { setOf(it.language) }
            .orEmpty()
    }

    private class FakeAiClient : AiClient {
        var request: TranslationRequest? = null
        var answer: Result<TranslationResult> = Result.failure(IllegalStateException())

        override suspend fun testKey(): Result<Unit> = Result.success(Unit)
        override suspend fun extractFromText(text: String): Result<DraftRecipe> = unsupported()
        override suspend fun extractFromImages(images: List<ByteArray>): Result<DraftRecipe> = unsupported()
        override suspend fun extractFromWebContent(url: String, content: String): Result<DraftRecipe> = unsupported()
        override suspend fun translate(request: TranslationRequest): Result<TranslationResult> {
            this.request = request
            return answer
        }

        private fun unsupported(): Result<DraftRecipe> = Result.failure(UnsupportedOperationException())
    }

    private companion object {
        val TRANSLATED_AT: Instant = Instant.parse("2026-08-12T10:00:00Z")
    }
}
