package cat.receptari.app.data.repository

import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.data.local.RecipeQueryBuilder
import cat.receptari.app.data.local.dao.RecipeDao
import cat.receptari.app.data.local.mapper.toDomain
import cat.receptari.app.data.local.mapper.toWriteModel
import cat.receptari.app.data.translation.toTranslationEntity
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.RecipeQuery
import cat.receptari.app.domain.model.RecipeSummary
import cat.receptari.app.domain.repository.ImageStore
import cat.receptari.app.domain.repository.RecipeRepository
import cat.receptari.app.domain.repository.TagRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.json.Json

class RecipeRepositoryImpl @Inject constructor(
    private val recipeDao: RecipeDao,
    private val tagRepository: TagRepository,
    private val imageStore: ImageStore,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecipeRepository {

    private val json = Json { encodeDefaults = true }

    override fun observeSummaries(query: RecipeQuery): Flow<List<RecipeSummary>> =
        recipeDao.observeSummaries(RecipeQueryBuilder.build(query))
            .map { projections -> projections.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeRecipe(id: String): Flow<Recipe?> =
        recipeDao.observeAggregate(id)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun getRecipe(id: String): Recipe? = withContext(ioDispatcher) {
        recipeDao.getAggregate(id)?.toDomain()
    }

    override suspend fun save(recipe: Recipe): Unit = withContext(ioDispatcher) {
        // Resolve tag identity first: the editor hands over tags built from text the user
        // typed, which may collide with tags that already exist under a different id.
        val resolvedTags = tagRepository.findOrCreate(recipe.tags.map { it.name })

        val stamped = recipe.copy(updatedAt = Instant.now(clock))
        val write = stamped.toWriteModel(resolvedTags)
        val translation = stamped.toTranslationEntity(json)

        recipeDao.saveAggregate(
            recipe = write.recipe,
            ingredientSections = write.ingredientSections,
            ingredients = write.ingredients,
            instructionSections = write.instructionSections,
            steps = write.steps,
            tagLinks = write.tagLinks,
            searchIndex = write.searchIndex,
            translation = translation,
        )
    }

    override suspend fun delete(id: String): Unit = withContext(ioDispatcher) {
        // Read the image path before the row disappears, otherwise the file is orphaned
        // in internal storage forever.
        val imagePath = recipeDao.getAggregate(id)?.recipe?.imagePath
        recipeDao.deleteRecipe(id)
        imagePath?.let { imageStore.delete(it) }
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean): Unit =
        withContext(ioDispatcher) {
            recipeDao.setFavorite(id, isFavorite, Instant.now(clock).toEpochMilli())
        }
}
