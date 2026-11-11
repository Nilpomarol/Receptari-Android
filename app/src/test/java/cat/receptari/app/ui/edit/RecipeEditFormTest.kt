package cat.receptari.app.ui.edit

import androidx.lifecycle.SavedStateHandle
import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.ui.edit.RecipeEditImageLifecycleTest.FakeImageStore
import cat.receptari.app.ui.edit.RecipeEditImageLifecycleTest.FakeRecipeRepository
import cat.receptari.app.ui.importer.ImportDraftHandoff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditFormTest {

    private val repository = FakeRecipeRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saving keeps the rating, tags, folder and favourite set on the recipe page`() = runTest {
        repository.recipe = Recipe(
            id = RECIPE_ID,
            title = "Arròs negre",
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        val viewModel = editor()

        // Changed on the recipe page while the editor was open.
        val folder = Folder(id = "f", name = "Arrossos", color = FolderColor.OLIVE, icon = FolderIcon.FOLDER)
        val tags = listOf(Tag(id = "t", name = "Diumenge"))
        repository.recipe = repository.recipe!!.copy(isFavorite = true, rating = 5, tags = tags, folder = folder)

        viewModel.onEvent(RecipeEditEvent.TitleChanged("Arròs negre amb allioli"))
        viewModel.onEvent(RecipeEditEvent.Save)

        val saved = repository.saved.single()
        assertEquals("Arròs negre amb allioli", saved.title)
        assertTrue(saved.isFavorite)
        assertEquals(5, saved.rating)
        assertEquals(tags, saved.tags)
        assertEquals(folder, saved.folder)
    }

    @Test
    fun `a pasted block becomes one line per row, below the line it landed on`() = runTest {
        val viewModel = editor(handle = SavedStateHandle())
        val section = viewModel.uiState.value.ingredientSections.single()
        val line = section.lines.single()
        val parts = splitEnteredText("200 g de farina\n\n2 ous\r\nSal al gust")!!

        viewModel.onEvent(
            RecipeEditEvent.LineSplit(
                SectionKind.Ingredients,
                section.id,
                line.id,
                parts.first(),
                parts.drop(1).map { FormLine(text = it) },
            ),
        )

        val lines = viewModel.uiState.value.ingredientSections.single().lines
        assertEquals(listOf("200 g de farina", "2 ous", "Sal al gust"), lines.map { it.text })
        assertEquals(line.id, lines.first().id)
    }

    @Test
    fun `splitting entered text`() {
        assertNull(splitEnteredText("200 g de farina"))
        // Enter at the end of a step opens an empty one below.
        assertEquals(listOf("Daura el pollastre.", ""), splitEnteredText("Daura el pollastre.\n"))
        // Enter mid-line splits it at the cursor.
        assertEquals(listOf("Daura", " el pollastre."), splitEnteredText("Daura\n el pollastre."))
        // Pasting into an empty line does not leave it behind empty.
        assertEquals(listOf("1 ou", "Sal"), splitEnteredText("\n1 ou\n\nSal\n"))
        // Text inside a line is never touched.
        assertEquals(listOf("  ½ tassa  ", "x"), splitEnteredText("  ½ tassa  \nx"))
    }

    private fun editor(
        handle: SavedStateHandle = SavedStateHandle(mapOf("recipeId" to RECIPE_ID)),
    ) = RecipeEditViewModel(
        savedStateHandle = handle,
        recipeRepository = repository,
        imageStore = FakeImageStore(),
        clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
        importDraftHandoff = ImportDraftHandoff(),
    )

    private companion object {
        const val RECIPE_ID = "recipe-1"
    }
}
