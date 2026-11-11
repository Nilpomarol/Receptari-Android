package cat.receptari.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import cat.receptari.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE UNICODE ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE normalizedName IN (:normalizedNames)")
    suspend fun findByNormalizedNames(normalizedNames: List<String>): List<TagEntity>

    @Upsert
    suspend fun upsertAll(tags: List<TagEntity>)

    /**
     * Removes tags no recipe references any more. Tags exist only as labels on recipes, so
     * an orphan is dead weight in the tag picker.
     */
    @Query(
        """
        DELETE FROM tags
        WHERE id NOT IN (SELECT DISTINCT tagId FROM recipe_tag_cross_ref)
        """,
    )
    suspend fun deleteOrphans()

    // --- managing tags library-wide ---------------------------------------------------

    @Query(
        """
        SELECT t.id, t.name, t.normalizedName, COUNT(c.recipeId) AS recipeCount
        FROM tags t
        LEFT JOIN recipe_tag_cross_ref c ON c.tagId = t.id
        GROUP BY t.id
        ORDER BY t.name COLLATE UNICODE ASC
        """,
    )
    fun observeAllWithCounts(): Flow<List<TagCountRow>>

    @Query("SELECT recipeId FROM recipe_tag_cross_ref WHERE tagId = :tagId")
    suspend fun recipeIdsWithTag(tagId: String): List<String>

    @Query("UPDATE tags SET name = :name, normalizedName = :normalizedName WHERE id = :id")
    suspend fun updateName(id: String, name: String, normalizedName: String)

    /** Gives every recipe tagged [sourceId] the [targetId] tag too; recipes with both keep one link. */
    @Query(
        """
        INSERT OR IGNORE INTO recipe_tag_cross_ref (recipeId, tagId)
        SELECT recipeId, :targetId FROM recipe_tag_cross_ref WHERE tagId = :sourceId
        """,
    )
    suspend fun relink(sourceId: String, targetId: String)

    /** Links to the tag cascade away with it; the recipes themselves are untouched. */
    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Rewrites the tag names held in the search index for [recipeIds]. The index is an FTS4
     * table, outside the foreign keys, so a rename or delete does not reach it by itself.
     */
    @Query(
        """
        UPDATE recipe_fts
        SET tagNames = COALESCE(
            (SELECT group_concat(t.name, ' ')
             FROM recipe_tag_cross_ref c JOIN tags t ON t.id = c.tagId
             WHERE c.recipeId = recipe_fts.recipeId),
            ''
        )
        WHERE recipeId IN (:recipeIds)
        """,
    )
    suspend fun refreshSearchTagNames(recipeIds: List<String>)

    /**
     * Renames a tag. When the new name is already another tag's, the two become one: the
     * recipes of [id] move to that tag and [id] goes away.
     */
    @Transaction
    suspend fun renameOrMerge(id: String, name: String, normalizedName: String) {
        val affected = recipeIdsWithTag(id)
        val clash = findByNormalizedNames(listOf(normalizedName)).firstOrNull { it.id != id }
        if (clash != null) {
            relink(sourceId = id, targetId = clash.id)
            deleteById(id)
        } else {
            updateName(id, name, normalizedName)
        }
        if (affected.isNotEmpty()) refreshSearchTagNames(affected)
    }

    @Transaction
    suspend fun deleteTag(id: String) {
        val affected = recipeIdsWithTag(id)
        deleteById(id)
        if (affected.isNotEmpty()) refreshSearchTagNames(affected)
    }
}

data class TagCountRow(
    @Embedded val tag: TagEntity,
    val recipeCount: Int,
)
