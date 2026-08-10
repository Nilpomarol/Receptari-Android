package cat.receptari.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cat.receptari.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
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
}
