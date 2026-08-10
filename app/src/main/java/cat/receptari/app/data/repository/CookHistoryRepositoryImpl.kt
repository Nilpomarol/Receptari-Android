package cat.receptari.app.data.repository

import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.data.local.dao.CookEventDao
import cat.receptari.app.data.local.entity.CookEventEntity
import cat.receptari.app.data.local.mapper.toDomain
import cat.receptari.app.domain.model.CookEvent
import cat.receptari.app.domain.repository.CookHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CookHistoryRepositoryImpl @Inject constructor(
    private val cookEventDao: CookEventDao,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CookHistoryRepository {

    override fun observeEvents(recipeId: String): Flow<List<CookEvent>> =
        cookEventDao.observeForRecipe(recipeId)
            .map { events -> events.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun markCooked(
        recipeId: String,
        cookedAt: Instant?,
        note: String?,
    ): Unit = withContext(ioDispatcher) {
        cookEventDao.insert(
            CookEventEntity(
                id = UUID.randomUUID().toString(),
                recipeId = recipeId,
                cookedAt = (cookedAt ?: Instant.now(clock)).toEpochMilli(),
                note = note,
            ),
        )
    }

    override suspend fun deleteEvent(id: String): Unit = withContext(ioDispatcher) {
        cookEventDao.delete(id)
    }
}
