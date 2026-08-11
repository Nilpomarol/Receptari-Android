package cat.receptari.app.ui.edit

import androidx.lifecycle.SavedStateHandle
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.RecipeQuery
import cat.receptari.app.domain.model.RecipeSummary
import cat.receptari.app.domain.repository.ImageStore
import cat.receptari.app.domain.repository.RecipeRepository
import cat.receptari.app.ui.importer.ImportDraftHandoff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Regression tests for the recipe image lifecycle.
 *
 * These exist because of a real data-loss bug: picking a replacement image deleted the
 * previously saved file immediately, so backing out of the editor left a saved recipe
 * pointing at a file that no longer existed. It was invisible in the UI — just a blank
 * thumbnail — and only showed up by reading the database off a device.
 *
 * The rule these lock in: **nothing is deleted until the outcome of the edit is known.**
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditImageLifecycleTest {

    private val originalImage = "images/original.jpg"

    private val imageStore = FakeImageStore()
    private val repository = FakeRecipeRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- the bug ------------------------------------------------------------------

    @Test
    fun `picking a new image does not delete the saved one`() = runTest {
        val viewModel = editorFor(existingImage = originalImage)

        viewModel.onEvent(RecipeEditEvent.ImagePicked(ByteArray(0)))

        // The saved recipe still references this file until the edit is saved.
        assertTrue("deleted the saved image too early", imageStore.deleted.isEmpty())
    }

    @Test
    fun `discarding keeps the saved image and removes only what this edit wrote`() = runTest {
        val viewModel = editorFor(existingImage = originalImage)

        viewModel.onEvent(RecipeEditEvent.ImagePicked(ByteArray(0)))
        val picked = imageStore.saved.single()
        viewModel.discardChanges()

        assertEquals(listOf(picked), imageStore.deleted)
        assertFalse("destroyed the saved recipe's image", originalImage in imageStore.deleted)
    }

    @Test
    fun `removing an image then discarding leaves the file alone`() = runTest {
        val viewModel = editorFor(existingImage = originalImage)

        viewModel.onEvent(RecipeEditEvent.ImageRemoved)
        viewModel.discardChanges()

        assertTrue(imageStore.deleted.isEmpty())
    }

    // --- cleanup on save ----------------------------------------------------------

    @Test
    fun `saving deletes the superseded image and keeps the new one`() = runTest {
        val viewModel = editorFor(existingImage = originalImage)

        viewModel.onEvent(RecipeEditEvent.ImagePicked(ByteArray(0)))
        val picked = imageStore.saved.single()
        viewModel.onEvent(RecipeEditEvent.Save)

        assertEquals(listOf(originalImage), imageStore.deleted)
        assertEquals(picked, repository.saved.single().imagePath)
    }

    @Test
    fun `changing your mind several times leaves only the saved image`() = runTest {
        val viewModel = editorFor(existingImage = originalImage)

        repeat(3) { viewModel.onEvent(RecipeEditEvent.ImagePicked(ByteArray(0))) }
        viewModel.onEvent(RecipeEditEvent.Save)

        val kept = repository.saved.single().imagePath
        val expectedDeletions = (imageStore.saved - kept) + originalImage

        assertEquals(expectedDeletions.toSet(), imageStore.deleted.toSet())
        assertFalse(kept in imageStore.deleted)
    }

    @Test
    fun `removing the image deletes the file only once the edit is saved`() = runTest {
        val viewModel = editorFor(existingImage = originalImage)

        viewModel.onEvent(RecipeEditEvent.ImageRemoved)
        assertTrue(imageStore.deleted.isEmpty())

        viewModel.onEvent(RecipeEditEvent.Save)

        assertEquals(listOf(originalImage), imageStore.deleted)
        assertEquals(null, repository.saved.single().imagePath)
    }

    @Test
    fun `saving a recipe that never had an image deletes nothing`() = runTest {
        val viewModel = editorFor(existingImage = null)

        viewModel.onEvent(RecipeEditEvent.Save)

        assertTrue(imageStore.deleted.isEmpty())
    }

    // --- helpers ------------------------------------------------------------------

    private fun editorFor(existingImage: String?): RecipeEditViewModel {
        repository.recipe = Recipe(
            id = RECIPE_ID,
            title = "Arròs negre",
            imagePath = existingImage,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )

        return RecipeEditViewModel(
            savedStateHandle = SavedStateHandle(mapOf("recipeId" to RECIPE_ID)),
            recipeRepository = repository,
            imageStore = imageStore,
            clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            importDraftHandoff = ImportDraftHandoff(),
        )
    }

    private class FakeImageStore : ImageStore {
        val saved = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        private var counter = 0

        override suspend fun save(recipeId: String, bytes: ByteArray): String =
            "images/$recipeId-${++counter}.jpg".also { saved += it }

        override suspend fun delete(relativePath: String) {
            deleted += relativePath
        }

        override fun absolutePathOf(relativePath: String): String = "/data/$relativePath"
    }

    private class FakeRecipeRepository : RecipeRepository {
        var recipe: Recipe? = null
        val saved = mutableListOf<Recipe>()

        override fun observeSummaries(query: RecipeQuery): Flow<List<RecipeSummary>> =
            flowOf(emptyList())

        override fun observeRecipe(id: String): Flow<Recipe?> = flowOf(recipe)

        override suspend fun getRecipe(id: String): Recipe? = recipe

        override suspend fun save(recipe: Recipe) {
            saved += recipe
            this.recipe = recipe
        }

        override suspend fun delete(id: String) = Unit

        override suspend fun setFavorite(id: String, isFavorite: Boolean) = Unit
    }

    private companion object {
        const val RECIPE_ID = "recipe-1"
    }
}
