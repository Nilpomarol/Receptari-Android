package cat.receptari.app.domain.repository

import cat.receptari.app.domain.model.CookingTimer
import kotlinx.coroutines.flow.Flow

interface CookingTimerRepository {

    fun observeTimers(): Flow<List<CookingTimer>>

    fun canScheduleExactAlarms(): Boolean

    suspend fun start(
        recipeId: String,
        stepId: String?,
        label: String,
        durationSeconds: Long,
    )

    suspend fun pause(timerId: String)

    suspend fun resume(timerId: String)

    suspend fun addTime(timerId: String, seconds: Long)

    suspend fun cancel(timerId: String)

    suspend fun cancelForRecipe(recipeId: String)

    suspend fun complete(timerId: String)

    suspend fun refreshNotifications()

    suspend fun restoreAfterBoot()
}
