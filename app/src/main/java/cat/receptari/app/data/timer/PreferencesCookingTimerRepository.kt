package cat.receptari.app.data.timer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.domain.model.CookingTimer
import cat.receptari.app.domain.model.CookingTimerStatus
import cat.receptari.app.domain.repository.CookingTimerRepository
import cat.receptari.app.domain.timer.TimerMath
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.cookingTimerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cooking_timer",
)

@Singleton
class PreferencesCookingTimerRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val scheduler: AndroidCookingTimerScheduler,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CookingTimerRepository {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override fun observeTimers(): Flow<List<CookingTimer>> = timersFlow().flowOn(ioDispatcher)

    override fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExactAlarms()

    override suspend fun start(
        recipeId: String,
        stepId: String?,
        label: String,
        durationSeconds: Long,
    ): Unit = mutate {
        check(scheduler.canScheduleExactAlarms())
        val safeDuration = durationSeconds.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)
        val timer = CookingTimer(
            id = UUID.randomUUID().toString(),
            recipeId = recipeId,
            stepId = stepId,
            label = label,
            durationSeconds = safeDuration,
            status = CookingTimerStatus.RUNNING,
            endsAtEpochMillis = clock.millis() + safeDuration * MILLIS_PER_SECOND,
        )
        val timers = currentTimers() + timer
        save(timers)
        scheduler.schedule(timer)
        scheduler.syncNotifications(timers)
    }

    override suspend fun pause(timerId: String): Unit = mutate {
        val timers = currentTimers()
        val timer = timers.timer(timerId)?.takeIf { it.status == CookingTimerStatus.RUNNING }
            ?: return@mutate
        val paused = timer.copy(
            status = CookingTimerStatus.PAUSED,
            endsAtEpochMillis = null,
            remainingSeconds = TimerMath.remainingSeconds(timer, clock.millis()),
        )
        val updated = timers.replace(paused)
        save(updated)
        scheduler.cancelAlarm(timerId)
        scheduler.syncNotifications(updated)
    }

    override suspend fun resume(timerId: String): Unit = mutate {
        check(scheduler.canScheduleExactAlarms())
        val timers = currentTimers()
        val timer = timers.timer(timerId)?.takeIf { it.status == CookingTimerStatus.PAUSED }
            ?: return@mutate
        val remaining = timer.remainingSeconds?.coerceAtLeast(1L) ?: return@mutate
        val resumed = timer.copy(
            status = CookingTimerStatus.RUNNING,
            endsAtEpochMillis = clock.millis() + remaining * MILLIS_PER_SECOND,
            remainingSeconds = null,
        )
        val updated = timers.replace(resumed)
        save(updated)
        scheduler.schedule(resumed)
        scheduler.syncNotifications(updated)
    }

    override suspend fun addTime(timerId: String, seconds: Long): Unit = mutate {
        val timers = currentTimers()
        val timer = timers.timer(timerId) ?: return@mutate
        val updatedDuration = (timer.durationSeconds + seconds.coerceAtLeast(0L))
            .coerceAtMost(MAX_DURATION_SECONDS)
        val effectiveAddition = updatedDuration - timer.durationSeconds
        val changed = when (timer.status) {
            CookingTimerStatus.RUNNING -> timer.copy(
                durationSeconds = updatedDuration,
                endsAtEpochMillis = requireNotNull(timer.endsAtEpochMillis) +
                    effectiveAddition * MILLIS_PER_SECOND,
            )

            CookingTimerStatus.PAUSED -> timer.copy(
                durationSeconds = updatedDuration,
                remainingSeconds = ((timer.remainingSeconds ?: 0L) + effectiveAddition)
                    .coerceAtMost(MAX_DURATION_SECONDS),
            )
        }
        val updated = timers.replace(changed)
        save(updated)
        if (changed.status == CookingTimerStatus.RUNNING) scheduler.schedule(changed)
        scheduler.syncNotifications(updated)
    }

    override suspend fun cancel(timerId: String): Unit = mutate {
        val timers = currentTimers()
        if (timers.none { it.id == timerId }) return@mutate
        val remaining = timers.filterNot { it.id == timerId }
        save(remaining)
        scheduler.cancel(timerId)
        scheduler.syncNotifications(remaining)
    }

    override suspend fun cancelForRecipe(recipeId: String): Unit = mutate {
        val timers = currentTimers()
        val removed = timers.filter { it.recipeId == recipeId }
        if (removed.isEmpty()) return@mutate
        val remaining = timers - removed.toSet()
        save(remaining)
        removed.forEach { scheduler.cancel(it.id) }
        scheduler.syncNotifications(remaining)
    }

    override suspend fun complete(timerId: String): Unit = mutate {
        val timers = currentTimers()
        val timer = timers.timer(timerId) ?: return@mutate
        val remaining = timers.filterNot { it.id == timerId }
        save(remaining)
        scheduler.showFinished(timer)
        scheduler.syncNotifications(remaining)
    }

    override suspend fun refreshNotifications(): Unit = mutate {
        scheduler.syncNotifications(currentTimers())
    }

    override suspend fun restoreAfterBoot(): Unit = mutate {
        val now = clock.millis()
        val timers = currentTimers()
        val expired = timers.filter {
            it.status == CookingTimerStatus.RUNNING && TimerMath.remainingSeconds(it, now) == 0L
        }
        val remaining = timers - expired.toSet()
        if (expired.isNotEmpty()) save(remaining)
        expired.forEach(scheduler::showFinished)
        if (scheduler.canScheduleExactAlarms()) {
            remaining.filter { it.status == CookingTimerStatus.RUNNING }.forEach(scheduler::schedule)
        }
        scheduler.syncNotifications(remaining)
    }

    private fun timersFlow(): Flow<List<CookingTimer>> = context.cookingTimerDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::timersFromPreferences)

    private suspend fun currentTimers(): List<CookingTimer> = timersFlow().first()

    private suspend fun save(timers: List<CookingTimer>) {
        context.cookingTimerDataStore.edit { preferences ->
            preferences.clear()
            if (timers.isNotEmpty()) {
                preferences[TIMERS_JSON] = json.encodeToString(timers.map(TimerDto::fromDomain))
            }
        }
    }

    private suspend fun mutate(block: suspend () -> Unit) {
        withContext(ioDispatcher) { mutex.withLock { block() } }
    }

    private fun timersFromPreferences(preferences: Preferences): List<CookingTimer> =
        preferences[TIMERS_JSON]
            ?.let { encoded ->
                runCatching { json.decodeFromString<List<TimerDto>>(encoded) }
                    .getOrDefault(emptyList())
                    .mapNotNull(TimerDto::toDomain)
            }
            .orEmpty()

    private companion object {
        val TIMERS_JSON = stringPreferencesKey("timers_json")
        const val MILLIS_PER_SECOND = 1_000L
        const val MIN_DURATION_SECONDS = 60L
        const val MAX_DURATION_SECONDS = 24L * 60L * 60L
    }
}

private fun List<CookingTimer>.timer(id: String): CookingTimer? = firstOrNull { it.id == id }

private fun List<CookingTimer>.replace(timer: CookingTimer): List<CookingTimer> =
    map { current -> if (current.id == timer.id) timer else current }

@Serializable
private data class TimerDto(
    val id: String,
    val recipeId: String,
    val stepId: String?,
    val label: String,
    val durationSeconds: Long,
    val status: String,
    val endsAtEpochMillis: Long?,
    val remainingSeconds: Long?,
) {
    fun toDomain(): CookingTimer? {
        val parsedStatus = runCatching { CookingTimerStatus.valueOf(status) }.getOrNull() ?: return null
        return CookingTimer(
            id = id,
            recipeId = recipeId,
            stepId = stepId,
            label = label,
            durationSeconds = durationSeconds,
            status = parsedStatus,
            endsAtEpochMillis = endsAtEpochMillis,
            remainingSeconds = remainingSeconds,
        )
    }

    companion object {
        fun fromDomain(timer: CookingTimer): TimerDto = TimerDto(
            id = timer.id,
            recipeId = timer.recipeId,
            stepId = timer.stepId,
            label = timer.label,
            durationSeconds = timer.durationSeconds,
            status = timer.status.name,
            endsAtEpochMillis = timer.endsAtEpochMillis,
            remainingSeconds = timer.remainingSeconds,
        )
    }
}
