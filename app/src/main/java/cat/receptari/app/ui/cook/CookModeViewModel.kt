package cat.receptari.app.ui.cook

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.domain.model.CookingTimer
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.repository.CookingTimerRepository
import cat.receptari.app.domain.repository.CookHistoryRepository
import cat.receptari.app.domain.repository.RecipeRepository
import cat.receptari.app.domain.timer.TimerMath
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CookModeUiState(
    val recipe: Recipe? = null,
    val timers: List<CookingTimer> = emptyList(),
    val timerRemainingSeconds: Map<String, Long> = emptyMap(),
    val isLoading: Boolean = true,
)

sealed interface CookModeEffect {
    data object RequestExactAlarmPermission : CookModeEffect
    data object ExactAlarmPermissionDenied : CookModeEffect
    data object TimerStarted : CookModeEffect
    data object CookingFinished : CookModeEffect
}

@HiltViewModel
class CookModeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    recipeRepository: RecipeRepository,
    private val cookHistoryRepository: CookHistoryRepository,
    private val cookingTimerRepository: CookingTimerRepository,
    clock: Clock,
) : ViewModel() {
    private val recipeId: String = checkNotNull(savedStateHandle["recipeId"])
    private var pendingTimerAction: PendingTimerAction? = null
    private var isFinishingCooking = false

    private val effectChannel = Channel<CookModeEffect>(Channel.BUFFERED)
    val effects: Flow<CookModeEffect> = effectChannel.receiveAsFlow()

    private val clockTicks = flow {
        while (true) {
            emit(clock.millis())
            delay(TIMER_TICK_MILLIS)
        }
    }

    val uiState: StateFlow<CookModeUiState> = combine(
        recipeRepository.observeRecipe(recipeId),
        cookingTimerRepository.observeTimers(),
        clockTicks,
    ) { recipe, timers, now ->
        CookModeUiState(
            recipe = recipe,
            timers = timers,
            timerRemainingSeconds = timers.associate { timer ->
                timer.id to TimerMath.remainingSeconds(timer, now)
            },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CookModeUiState(),
    )

    fun pauseTimer(timerId: String) = viewModelScope.launch {
        cookingTimerRepository.pause(timerId)
    }

    fun resumeTimer(timerId: String) {
        requestTimerAction(PendingTimerAction.Resume(timerId))
    }

    fun addTimerMinute(timerId: String) = viewModelScope.launch {
        cookingTimerRepository.addTime(timerId, SECONDS_PER_MINUTE)
    }

    fun cancelTimer(timerId: String) = viewModelScope.launch {
        cookingTimerRepository.cancel(timerId)
    }

    fun finishCooking() {
        if (isFinishingCooking) return
        isFinishingCooking = true
        viewModelScope.launch {
            try {
                cookHistoryRepository.markCooked(recipeId)
                effectChannel.send(CookModeEffect.CookingFinished)
            } catch (error: Exception) {
                isFinishingCooking = false
                throw error
            }
        }
    }

    fun startTimer(stepId: String, label: String, durationMinutes: Int) {
        requestTimerAction(
            PendingTimerAction.Start(
                stepId = stepId,
                label = label,
                durationMinutes = durationMinutes,
            ),
        )
    }

    fun retryPendingTimerAction() {
        val action = pendingTimerAction ?: return
        viewModelScope.launch {
            if (!cookingTimerRepository.canScheduleExactAlarms()) {
                pendingTimerAction = null
                effectChannel.send(CookModeEffect.ExactAlarmPermissionDenied)
                return@launch
            }
            runTimerAction(action)
        }
    }

    fun cancelPendingTimerAction() {
        pendingTimerAction = null
    }

    private fun requestTimerAction(action: PendingTimerAction) {
        viewModelScope.launch {
            if (!cookingTimerRepository.canScheduleExactAlarms()) {
                pendingTimerAction = action
                effectChannel.send(CookModeEffect.RequestExactAlarmPermission)
                return@launch
            }
            runTimerAction(action)
        }
    }

    private suspend fun runTimerAction(action: PendingTimerAction) {
        try {
            when (action) {
                is PendingTimerAction.Start -> cookingTimerRepository.start(
                    recipeId = recipeId,
                    stepId = action.stepId,
                    label = action.label,
                    durationSeconds = action.durationMinutes * SECONDS_PER_MINUTE,
                )

                is PendingTimerAction.Resume -> cookingTimerRepository.resume(action.timerId)
            }
            pendingTimerAction = null
            effectChannel.send(CookModeEffect.TimerStarted)
        } catch (_: SecurityException) {
            pendingTimerAction = action
            effectChannel.send(CookModeEffect.RequestExactAlarmPermission)
        } catch (_: IllegalStateException) {
            pendingTimerAction = action
            effectChannel.send(CookModeEffect.RequestExactAlarmPermission)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TIMER_TICK_MILLIS = 1_000L
        const val SECONDS_PER_MINUTE = 60L
    }
}

private sealed interface PendingTimerAction {
    data class Start(
        val stepId: String,
        val label: String,
        val durationMinutes: Int,
    ) : PendingTimerAction

    data class Resume(val timerId: String) : PendingTimerAction
}
