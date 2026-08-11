package cat.receptari.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cat.receptari.app.data.local.dao.CookEventDao
import cat.receptari.app.data.local.dao.RecipeDao
import cat.receptari.app.data.local.entity.CookEventEntity
import cat.receptari.app.data.local.mapper.toDomain
import cat.receptari.app.data.local.mapper.toWriteModel
import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.model.RecipeQuery
import cat.receptari.app.domain.model.RecipeSort
import cat.receptari.app.domain.model.IngredientSection
import cat.receptari.app.domain.model.InstructionSection
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.Step
import cat.receptari.app.domain.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RecipeDaoTest {

    private lateinit var database: ReceptariDatabase
    private lateinit var recipeDao: RecipeDao
    private lateinit var cookEventDao: CookEventDao

    private val now = Instant.ofEpochMilli(1_700_000_000_000)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReceptariDatabase::class.java,
        ).build()
        recipeDao = database.recipeDao()
        cookEventDao = database.cookEventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- round trip ---------------------------------------------------------------

    @Test
    fun savesAndReadsBackTheWholeAggregate() = runBlocking {
        val recipe = sampleRecipe()

        save(recipe)
        val loaded = recipeDao.getAggregate(recipe.id)?.toDomain()

        assertNotNull(loaded)
        assertEquals("Pollastre amb salsa", loaded!!.title)
        assertEquals(4, loaded.baseServings)
        assertEquals(2, loaded.ingredientSections.size)
        assertEquals(2, loaded.instructionSections.size)
        assertEquals(1, loaded.tags.size)
    }

    @Test
    fun preservesSectionAndIngredientOrder() = runBlocking {
        val recipe = sampleRecipe()

        save(recipe)
        val loaded = recipeDao.getAggregate(recipe.id)!!.toDomain()

        assertEquals(listOf("Salsa", "Pollastre"), loaded.ingredientSections.map { it.name })
        assertEquals(
            listOf("nata", "mantega", "all"),
            loaded.ingredientSections[0].ingredients.map { it.name },
        )
        assertEquals(
            listOf("Talla la ceba.", "Cou fins que estigui tova."),
            loaded.instructionSections[0].steps.map { it.text },
        )
    }

    @Test
    fun originalTextSurvivesTheRoundTrip() = runBlocking {
        val recipe = sampleRecipe()

        save(recipe)
        val loaded = recipeDao.getAggregate(recipe.id)!!.toDomain()

        assertEquals(
            listOf("200 ml de nata", "1 cullerada de mantega", "2 grans d'all"),
            loaded.ingredientSections[0].ingredients.map { it.originalText },
        )
    }

    @Test
    fun anIngredientWithNothingStructuredIsStillStored() = runBlocking {
        val recipe = sampleRecipe()

        save(recipe)
        val loaded = recipeDao.getAggregate(recipe.id)!!.toDomain()
        val salt = loaded.ingredientSections[1].ingredients.last()

        assertNull(salt.quantity)
        assertNull(salt.unit)
        assertEquals("Sal al gust", salt.originalText)
    }

    @Test
    fun savingTwiceReplacesChildrenRatherThanDuplicatingThem() = runBlocking {
        val recipe = sampleRecipe()
        save(recipe)

        val edited = recipe.copy(
            title = "Pollastre rostit",
            ingredientSections = listOf(
                IngredientSection(
                    id = "sec-1",
                    name = "Base",
                    ingredients = listOf(
                        Ingredient(id = "ing-1", name = "pollastre", originalText = "1 pollastre"),
                    ),
                ),
            ),
        )
        save(edited)

        val loaded = recipeDao.getAggregate(recipe.id)!!.toDomain()
        assertEquals("Pollastre rostit", loaded.title)
        assertEquals(1, loaded.ingredientSections.size)
        assertEquals(1, loaded.ingredientSections[0].ingredients.size)
    }

    // --- cooked history is derived, never stored ----------------------------------

    @Test
    fun cookCountAndLastCookedComeFromTheEventLog() = runBlocking {
        val recipe = sampleRecipe()
        save(recipe)

        cookEventDao.insert(event(recipe.id, "e1", now))
        cookEventDao.insert(event(recipe.id, "e2", now.plusSeconds(86_400)))
        cookEventDao.insert(event(recipe.id, "e3", now.plusSeconds(172_800)))

        val loaded = recipeDao.getAggregate(recipe.id)!!.toDomain()

        assertEquals(3, loaded.cookCount)
        assertEquals(now.plusSeconds(172_800), loaded.lastCookedAt)
    }

    @Test
    fun summariesCarryTheDerivedCookCounts() = runBlocking {
        val recipe = sampleRecipe()
        save(recipe)
        cookEventDao.insert(event(recipe.id, "e1", now))
        cookEventDao.insert(event(recipe.id, "e2", now.plusSeconds(60)))

        val summaries = recipeDao.observeSummaries(RecipeQueryBuilder.build(RecipeQuery())).first()

        assertEquals(1, summaries.size)
        assertEquals(2, summaries[0].cookCount)
        assertEquals(now.plusSeconds(60).toEpochMilli(), summaries[0].lastCookedAt)
    }

    @Test
    fun aRecipeNeverCookedReportsZero() = runBlocking {
        save(sampleRecipe())

        val summaries = recipeDao.observeSummaries(RecipeQueryBuilder.build(RecipeQuery())).first()

        assertEquals(0, summaries[0].cookCount)
        assertNull(summaries[0].lastCookedAt)
    }

    // --- what the library card reads ----------------------------------------------

    @Test
    fun summariesCarryTheTimePartsAndTheServings() = runBlocking {
        save(
            sampleRecipe().copy(
                prepTimeMinutes = 20,
                cookTimeMinutes = 80,
                baseServings = 6,
            ),
        )

        val summary = recipeDao.observeSummaries(RecipeQueryBuilder.build(RecipeQuery()))
            .first()
            .single()

        // The card prints one duration, but which one depends on all three, so the
        // projection has to carry the parts rather than just the total.
        assertEquals(20, summary.prepTimeMinutes)
        assertEquals(80, summary.cookTimeMinutes)
        assertNull(summary.totalTimeMinutes)
        assertEquals(6, summary.baseServings)
        assertEquals(100, summary.toDomain().effectiveTimeMinutes)
    }

    /**
     * Sorting by cooking time has to agree with the figure the card prints, or the list
     * orders by one number and shows another. Both sides implement
     * [cat.receptari.app.domain.model.CookingTime]; this is what pins them together.
     */
    @Test
    fun sortingByCookingTimeUsesTheSameFallbackTheCardPrints() = runBlocking {
        // Bare recipes, not copies of the sample: copying would reuse its section,
        // ingredient and step ids and the second save would fail on the primary key.
        save(timedRecipe("total-90", total = 90, prep = 5))
        save(timedRecipe("parts-45", prep = 15, cook = 30))
        save(timedRecipe("cook-30", cook = 30))
        save(timedRecipe("prep-15", prep = 15))
        save(timedRecipe("untimed"))

        val order = recipeDao
            .observeSummaries(
                RecipeQueryBuilder.build(RecipeQuery(sort = RecipeSort.CookingTime)),
            )
            .first()
            .map { it.id }

        // Untimed last: a recipe with no time recorded is not a zero-minute recipe.
        assertEquals(listOf("prep-15", "cook-30", "parts-45", "total-90", "untimed"), order)
    }

    // --- deletion cascades --------------------------------------------------------

    @Test
    fun deletingARecipeTakesItsChildrenWithIt() = runBlocking {
        val recipe = sampleRecipe()
        save(recipe)
        cookEventDao.insert(event(recipe.id, "e1", now))

        recipeDao.deleteRecipe(recipe.id)

        assertNull(recipeDao.getAggregate(recipe.id))
        assertTrue(cookEventDao.observeForRecipe(recipe.id).first().isEmpty())
        assertTrue(recipeDao.searchIds("pollastre").isEmpty())
    }

    // --- search index -------------------------------------------------------------

    @Test
    fun searchMatchesTitleIngredientsAndTags() = runBlocking {
        val recipe = sampleRecipe()
        save(recipe)

        assertEquals(listOf(recipe.id), recipeDao.searchIds("pollastre"))
        assertEquals(listOf(recipe.id), recipeDao.searchIds("nata"))
        assertEquals(listOf(recipe.id), recipeDao.searchIds("Sopar"))
    }

    @Test
    fun searchSupportsPartialMatches() = runBlocking {
        val recipe = sampleRecipe()
        save(recipe)

        // PRD §6: searching "poll" must find "pollastre".
        assertEquals(listOf(recipe.id), recipeDao.searchIds("poll*"))
    }

    @Test
    fun searchFindsUnstructuredIngredientsByTheirRawText() = runBlocking {
        val recipe = sampleRecipe()
        save(recipe)

        // "Sal al gust" parsed to nothing structured, so the index falls back to its text.
        assertEquals(listOf(recipe.id), recipeDao.searchIds("gust"))
    }

    @Test
    fun editingARecipeRefreshesItsSearchIndex() = runBlocking {
        val recipe = sampleRecipe()
        save(recipe)

        save(recipe.copy(title = "Vedella estofada"))

        assertTrue(recipeDao.searchIds("Pollastre amb salsa").isEmpty())
        assertEquals(listOf(recipe.id), recipeDao.searchIds("Vedella"))
    }

    // --- helpers ------------------------------------------------------------------

    private suspend fun save(recipe: Recipe) {
        val write = recipe.toWriteModel(recipe.tags)
        database.tagDao().upsertAll(
            recipe.tags.map {
                cat.receptari.app.data.local.entity.TagEntity(it.id, it.name, it.normalizedName)
            },
        )
        recipeDao.saveAggregate(
            recipe = write.recipe,
            ingredientSections = write.ingredientSections,
            ingredients = write.ingredients,
            instructionSections = write.instructionSections,
            steps = write.steps,
            tagLinks = write.tagLinks,
            searchIndex = write.searchIndex,
        )
    }

    private fun event(recipeId: String, id: String, at: Instant) = CookEventEntity(
        id = id,
        recipeId = recipeId,
        cookedAt = at.toEpochMilli(),
        note = null,
    )

    /** A recipe with times and nothing else, so several can coexist without id clashes. */
    private fun timedRecipe(
        id: String,
        prep: Int? = null,
        cook: Int? = null,
        total: Int? = null,
    ) = Recipe(
        id = id,
        title = id,
        prepTimeMinutes = prep,
        cookTimeMinutes = cook,
        totalTimeMinutes = total,
        createdAt = now,
        updatedAt = now,
    )

    private fun sampleRecipe() = Recipe(
        id = "recipe-1",
        title = "Pollastre amb salsa",
        baseServings = 4,
        createdAt = now,
        updatedAt = now,
        ingredientSections = listOf(
            IngredientSection(
                id = "sec-sauce",
                name = "Salsa",
                ingredients = listOf(
                    Ingredient("i1", 200.0, null, "ml", "nata", null, "200 ml de nata"),
                    Ingredient("i2", 1.0, null, "tbsp", "mantega", null, "1 cullerada de mantega"),
                    Ingredient("i3", 2.0, null, "clove", "all", null, "2 grans d'all"),
                ),
            ),
            IngredientSection(
                id = "sec-chicken",
                name = "Pollastre",
                ingredients = listOf(
                    Ingredient("i4", 500.0, null, "g", "pollastre", null, "500 g de pollastre"),
                    Ingredient("i5", null, null, null, null, null, "Sal al gust"),
                ),
            ),
        ),
        instructionSections = listOf(
            InstructionSection(
                id = "isec-1",
                name = "Prepara la salsa",
                steps = listOf(
                    Step("s1", "Talla la ceba."),
                    Step("s2", "Cou fins que estigui tova."),
                ),
            ),
            InstructionSection(
                id = "isec-2",
                name = "Cou el pollastre",
                steps = listOf(Step("s3", "Afegeix el pollastre.")),
            ),
        ),
        tags = listOf(Tag(id = "tag-1", name = "Sopar")),
    )
}
