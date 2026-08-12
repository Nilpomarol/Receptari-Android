package cat.receptari.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import cat.receptari.app.data.local.entity.CookEventEntity
import cat.receptari.app.data.local.entity.FolderEntity
import cat.receptari.app.data.local.entity.IngredientEntity
import cat.receptari.app.data.local.entity.IngredientSectionEntity
import cat.receptari.app.data.local.entity.InstructionSectionEntity
import cat.receptari.app.data.local.entity.RecipeAggregate
import cat.receptari.app.data.local.entity.RecipeEntity
import cat.receptari.app.data.local.entity.RecipeFtsEntity
import cat.receptari.app.data.local.entity.RecipeSummaryProjection
import cat.receptari.app.data.local.entity.RecipeTagCrossRef
import cat.receptari.app.data.local.entity.RecipeTranslationEntity
import cat.receptari.app.data.local.entity.StepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeAggregate(id: String): Flow<RecipeAggregate?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getAggregate(id: String): RecipeAggregate?

    @Query(
        "SELECT * FROM recipe_translations " +
            "WHERE recipeId = :recipeId AND language = :language",
    )
    suspend fun getTranslation(recipeId: String, language: String): RecipeTranslationEntity?

    @Query("SELECT * FROM recipe_translations WHERE recipeId = :recipeId")
    suspend fun getTranslations(recipeId: String): List<RecipeTranslationEntity>

    /**
     * The filtered/sorted library list. Raw because the WHERE and ORDER BY vary with the
     * user's filters; [cat.receptari.app.data.local.RecipeQueryBuilder] owns the SQL.
     *
     * `observedEntities` is what makes the Flow re-emit: without it Room has no idea which
     * tables the query touches, so favouriting a recipe or marking one cooked would leave
     * a stale list on screen.
     */
    @RewriteQueriesToDropUnusedColumns
    @RawQuery(
        observedEntities = [
            RecipeEntity::class,
            CookEventEntity::class,
            RecipeTagCrossRef::class,
            RecipeFtsEntity::class,
            FolderEntity::class,
        ],
    )
    fun observeSummaries(query: SupportSQLiteQuery): Flow<List<RecipeSummaryProjection>>

    @Query("SELECT recipeId FROM recipe_fts WHERE recipe_fts MATCH :query")
    suspend fun searchIds(query: String): List<String>

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeRow(id: String)

    /**
     * Deletes a recipe and its search index entry.
     *
     * The index needs deleting by hand: `recipe_fts` is an FTS4 virtual table, and SQLite
     * does not allow foreign keys on those, so the cascade that clears sections,
     * ingredients, steps, tag links and cook events does not reach it. Without this the
     * index keeps pointing at a recipe id that no longer exists and search returns
     * phantoms.
     */
    @Transaction
    suspend fun deleteRecipe(id: String) {
        deleteSearchIndex(id)
        deleteRecipeRow(id)
    }

    @Query("UPDATE recipes SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long)

    // --- write path ---------------------------------------------------------------
    //
    // Children are replaced wholesale rather than diffed: an edit can reorder, merge and
    // split sections arbitrarily, and cascade-delete-then-insert is both simpler and
    // impossible to get subtly wrong. Foreign keys cascade, so deleting the sections takes
    // their ingredients and steps with them.

    @Upsert
    suspend fun upsertRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM ingredient_sections WHERE recipeId = :recipeId")
    suspend fun deleteIngredientSections(recipeId: String)

    @Query("DELETE FROM instruction_sections WHERE recipeId = :recipeId")
    suspend fun deleteInstructionSections(recipeId: String)

    @Insert
    suspend fun insertIngredientSections(sections: List<IngredientSectionEntity>)

    @Insert
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Insert
    suspend fun insertInstructionSections(sections: List<InstructionSectionEntity>)

    @Insert
    suspend fun insertSteps(steps: List<StepEntity>)

    @Query("DELETE FROM recipe_tag_cross_ref WHERE recipeId = :recipeId")
    suspend fun deleteTagLinks(recipeId: String)

    @Insert
    suspend fun insertTagLinks(links: List<RecipeTagCrossRef>)

    @Query("DELETE FROM recipe_fts WHERE recipeId = :recipeId")
    suspend fun deleteSearchIndex(recipeId: String)

    @Insert
    suspend fun insertSearchIndex(entry: RecipeFtsEntity)

    @Upsert
    suspend fun upsertTranslation(translation: RecipeTranslationEntity)

    /**
     * Writes a recipe and its entire object graph atomically, including the search index —
     * a half-written recipe or a stale index must never be observable.
     */
    @Transaction
    suspend fun saveAggregate(
        recipe: RecipeEntity,
        ingredientSections: List<IngredientSectionEntity>,
        ingredients: List<IngredientEntity>,
        instructionSections: List<InstructionSectionEntity>,
        steps: List<StepEntity>,
        tagLinks: List<RecipeTagCrossRef>,
        searchIndex: RecipeFtsEntity,
        translation: RecipeTranslationEntity? = null,
    ) {
        upsertRecipe(recipe)

        deleteIngredientSections(recipe.id)
        deleteInstructionSections(recipe.id)
        insertIngredientSections(ingredientSections)
        insertIngredients(ingredients)
        insertInstructionSections(instructionSections)
        insertSteps(steps)

        deleteTagLinks(recipe.id)
        insertTagLinks(tagLinks)

        deleteSearchIndex(recipe.id)
        insertSearchIndex(searchIndex)

        translation?.let { upsertTranslation(it) }
    }
}
