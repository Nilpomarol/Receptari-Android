package cat.receptari.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import cat.receptari.app.domain.ai.AiClient
import cat.receptari.app.domain.ai.DraftRecipe
import cat.receptari.app.domain.ai.RecipeLanguage
import cat.receptari.app.domain.ai.TranslationRequest
import cat.receptari.app.domain.ai.TranslationResult
import cat.receptari.app.domain.model.CookEvent
import cat.receptari.app.domain.model.CookingTimer
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.RecipeQuery
import cat.receptari.app.domain.model.RecipeSummary
import cat.receptari.app.domain.repository.CookHistoryRepository
import cat.receptari.app.domain.repository.CookingTimerRepository
import cat.receptari.app.domain.repository.ImageStore
import cat.receptari.app.domain.repository.RecipeRepository
import cat.receptari.app.domain.repository.RecipeTranslationRepository
import cat.receptari.app.domain.translation.RecipeTranslationVariant
import cat.receptari.app.domain.translation.TranslateRecipe
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailTranslationTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selecting a new language saves it without opening an editor`() = runTest {
        val recipeRepository = FakeRecipeRepository(sourceRecipe())
        val translations = FakeTranslationRepository()
        val ai = FakeAiClient()
        val viewModel = detailViewModel(recipeRepository, translations, ai)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()

        viewModel.onEvent(RecipeDetailEvent.Translate(RecipeLanguage.CATALAN))
        runCurrent()

        assertEquals("Pa amb oli", recipeRepository.saved!!.title)
        assertEquals("ca", recipeRepository.saved!!.displayLanguage)
        assertEquals(RecipeDetailEffect.TranslationApplied, viewModel.effects.first())
        assertTrue(RecipeLanguage.CATALAN in viewModel.uiState.value.translatedLanguages)
    }

    @Test
    fun `detail state exposes valid cached languages for picker indicators`() = runTest {
        val translations = FakeTranslationRepository(available = setOf("ca", "es"))
        val viewModel = detailViewModel(
            recipeRepository = FakeRecipeRepository(sourceRecipe()),
            translations = translations,
            ai = FakeAiClient(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()

        assertEquals(
            setOf(RecipeLanguage.CATALAN, RecipeLanguage.SPANISH),
            viewModel.uiState.value.translatedLanguages,
        )
    }

    private fun detailViewModel(
        recipeRepository: FakeRecipeRepository,
        translations: FakeTranslationRepository,
        ai: FakeAiClient,
    ): RecipeDetailViewModel {
        val clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
        return RecipeDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("recipeId" to RECIPE_ID)),
            recipeRepository = recipeRepository,
            cookHistoryRepository = FakeCookHistoryRepository(),
            cookingTimerRepository = FakeCookingTimerRepository(),
            imageStore = FakeImageStore(),
            translateRecipe = TranslateRecipe(ai, translations, clock),
            translationRepository = translations,
            clock = clock,
        )
    }

    private fun sourceRecipe(): Recipe = Recipe(
        id = RECIPE_ID,
        title = "Olive oil toast",
        originalLanguage = "en",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private class FakeTranslationRepository(
        private val available: Set<String> = emptySet(),
    ) : RecipeTranslationRepository {
        override suspend fun get(
            recipeId: String,
            language: String,
        ): RecipeTranslationVariant? = null

        override suspend fun getAvailableLanguages(
            recipeId: String,
            sourceFingerprint: String,
        ): Set<String> = available
    }

    private class FakeAiClient : AiClient {
        override suspend fun translate(request: TranslationRequest): Result<TranslationResult> =
            Result.success(
                TranslationResult(
                    title = "Pa amb oli",
                    sourceLanguage = "en",
                    ingredientSections = emptyList(),
                    ingredients = emptyList(),
                    instructionSections = emptyList(),
                    steps = emptyList(),
                ),
            )

        override suspend fun testKey(): Result<Unit> = Result.success(Unit)
        override suspend fun extractFromText(text: String): Result<DraftRecipe> = unsupported()
        override suspend fun extractFromImages(images: List<ByteArray>): Result<DraftRecipe> = unsupported()
        override suspend fun extractFromWebContent(
            url: String,
            content: String,
        ): Result<DraftRecipe> = unsupported()

        private fun unsupported(): Result<DraftRecipe> =
            Result.failure(UnsupportedOperationException())
    }

    private class FakeRecipeRepository(initial: Recipe) : RecipeRepository {
        private val recipe = MutableStateFlow<Recipe?>(initial)
        var saved: Recipe? = null

        override fun observeRecipe(id: String): Flow<Recipe?> = recipe
        override suspend fun getRecipe(id: String): Recipe? = recipe.value
        override suspend fun save(recipe: Recipe) {
            saved = recipe
            this.recipe.value = recipe
        }
        override fun observeSummaries(query: RecipeQuery): Flow<List<RecipeSummary>> = flowOf(emptyList())
        override suspend fun delete(id: String) = Unit
        override suspend fun setFavorite(id: String, isFavorite: Boolean) = Unit
    }

    private class FakeCookHistoryRepository : CookHistoryRepository {
        override fun observeEvents(recipeId: String): Flow<List<CookEvent>> = flowOf(emptyList())
        override suspend fun markCooked(recipeId: String, cookedAt: Instant?, note: String?) = Unit
        override suspend fun deleteEvent(id: String) = Unit
    }

    private class FakeCookingTimerRepository : CookingTimerRepository {
        override fun observeTimers(): Flow<List<CookingTimer>> = flowOf(emptyList())
        override fun canScheduleExactAlarms(): Boolean = true
        override suspend fun start(recipeId: String, stepId: String?, label: String, durationSeconds: Long) = Unit
        override suspend fun pause(timerId: String) = Unit
        override suspend fun resume(timerId: String) = Unit
        override suspend fun addTime(timerId: String, seconds: Long) = Unit
        override suspend fun cancel(timerId: String) = Unit
        override suspend fun cancelForRecipe(recipeId: String) = Unit
        override suspend fun complete(timerId: String) = Unit
        override suspend fun refreshNotifications() = Unit
        override suspend fun restoreAfterBoot() = Unit
    }

    private class FakeImageStore : ImageStore {
        override suspend fun save(recipeId: String, bytes: ByteArray): String = error("unused")
        override suspend fun delete(relativePath: String) = Unit
        override fun absolutePathOf(relativePath: String): String = relativePath
    }

    private companion object {
        const val RECIPE_ID = "recipe"
    }
}
