package cat.receptari.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cat.receptari.app.data.local.entity.FolderEntity
import cat.receptari.app.data.local.entity.FolderWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query(
        """
        SELECT f.*, COUNT(r.id) AS recipeCount
        FROM folders f
        LEFT JOIN recipes r ON r.folderId = f.id
        GROUP BY f.id
        ORDER BY f.name COLLATE NOCASE ASC
        """,
    )
    fun observeAllWithCounts(): Flow<List<FolderWithCount>>

    @Query("SELECT * FROM folders WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): FolderEntity?

    @Upsert
    suspend fun upsert(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: String)
}
