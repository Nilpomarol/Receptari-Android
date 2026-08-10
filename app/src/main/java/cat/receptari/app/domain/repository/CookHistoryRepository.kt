package cat.receptari.app.domain.repository

import cat.receptari.app.domain.model.CookEvent
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface CookHistoryRepository {

    fun observeEvents(recipeId: String): Flow<List<CookEvent>>

    /**
     * Records one occasion of cooking the recipe. Called repeatedly over a recipe's life —
     * this is an append, never a status flag (PRD §4).
     */
    suspend fun markCooked(recipeId: String, cookedAt: Instant? = null, note: String? = null)

    suspend fun deleteEvent(id: String)
}
