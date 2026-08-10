package cat.receptari.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cat.receptari.app.data.local.entity.CookEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CookEventDao {

    @Query("SELECT * FROM cook_events WHERE recipeId = :recipeId ORDER BY cookedAt DESC")
    fun observeForRecipe(recipeId: String): Flow<List<CookEventEntity>>

    @Insert
    suspend fun insert(event: CookEventEntity)

    @Query("DELETE FROM cook_events WHERE id = :id")
    suspend fun delete(id: String)
}
