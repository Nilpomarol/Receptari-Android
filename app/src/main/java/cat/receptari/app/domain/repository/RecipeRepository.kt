package cat.receptari.app.domain.repository

import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.RecipeQuery
import cat.receptari.app.domain.model.RecipeSummary
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {

    /** The library list. Emits a projection, never the full aggregate. */
    fun observeSummaries(query: RecipeQuery = RecipeQuery()): Flow<List<RecipeSummary>>

    fun observeRecipe(id: String): Flow<Recipe?>

    suspend fun getRecipe(id: String): Recipe?

    /** Full aggregates used by explicit library transfer, never by the list screen. */
    suspend fun getAllRecipes(): List<Recipe>

    /**
     * Writes the recipe and its whole object graph atomically, stamping `updatedAt`.
     * Tag identity is resolved here, so callers may pass freshly built [Recipe.tags]
     * without worrying about duplicating an existing tag.
     */
    suspend fun save(recipe: Recipe)

    /** Deletes the recipe, its children, and its image file. */
    suspend fun delete(id: String)

    suspend fun setFavorite(id: String, isFavorite: Boolean)
}
