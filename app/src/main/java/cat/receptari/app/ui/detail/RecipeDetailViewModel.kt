package cat.receptari.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.CookingTimer
import cat.receptari.app.domain.ai.RecipeLanguage
import cat.receptari.app.domain.repository.CookHistoryRepository
import cat.receptari.app.domain.repository.CookingTimerRepository
import cat.receptari.app.domain.repository.ImageStore
import cat.receptari.app.domain.repository.RecipeRepository
import cat.receptari.app.domain.repository.RecipeTranslationRepository
import cat.receptari.app.domain.repository.RecipeTransferRepository
import cat.receptari.app.domain.scaling.ScaledIngredientSection
import cat.receptari.app.domain.scaling.ServingScaler
import cat.receptari.app.domain.timer.TimerMath
import cat.receptari.app.domain.translation.TranslateRecipe
import cat.receptari.app.domain.translation.translationSourceFingerprint
import cat.receptari.app.domain.transfer.RecipeTransferArchive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.Clock
import javax.inject.Inject

import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.domain.repository.FolderRepository
import cat.receptari.app.domain.repository.TagRepository

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val imagePath: String? = null,
    val ingredientSections: List<ScaledIngredientSection> = emptyList(),
    val servings: Int? = null,
    val isScaled: Boolean = false,
    val timers: List<CookingTimer> = emptyList(),
    val timerRemainingSeconds: Map<String, Long> = emptyMap(),
    val availableFolders: List<Folder> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val isLoading: Boolean = true,
    val isTranslating: Boolean = false,
    val isSharing: Boolean = false,
    val translatedLanguages: Set<RecipeLanguage> = emptySet(),
) {
    val isMissing: Boolean
        get() = !isLoading && recipe == null
}

sealed interface RecipeDetailEvent {
    data object IncreaseServings : RecipeDetailEvent
    data object DecreaseServings : RecipeDetailEvent
    data object ResetServings : RecipeDetailEvent
    data object ToggleFavorite : RecipeDetailEvent
    data object MarkCooked : RecipeDetailEvent
    data object Delete : RecipeDetailEvent
    data class SetRating(val rating: Int?) : RecipeDetailEvent
    data class SetFolder(val folder: Folder?) : RecipeDetailEvent
    data class AddTag(val tagName: String) : RecipeDetailEvent
    data class RemoveTag(val tagName: String) : RecipeDetailEvent
    data class FolderCreateRequested(
        val name: String,
        val color: FolderColor = FolderColor.OLIVE,
        val icon: FolderIcon = FolderIcon.FOLDER,
    ) : RecipeDetailEvent
    data class StartTimer(
        val stepId: String?,
        val label: String,
        val durationMinutes: Int,
    ) : RecipeDetailEvent
    data class PauseTimer(val timerId: String) : RecipeDetailEvent
    data class ResumeTimer(val timerId: String) : RecipeDetailEvent
    data class AddTimerMinute(val timerId: String) : RecipeDetailEvent
    data class CancelTimer(val timerId: String) : RecipeDetailEvent
    data object RetryPendingTimerAction : RecipeDetailEvent
    data object CancelPendingTimerAction : RecipeDetailEvent
    data object RefreshTimerNotifications : RecipeDetailEvent
    data class Translate(val target: RecipeLanguage) : RecipeDetailEvent
    data object Share : RecipeDetailEvent
}

sealed interface RecipeDetailEffect {
    data object MarkedCooked : RecipeDetailEffect
    data object Deleted : RecipeDetailEffect
    data object RequestExactAlarmPermission : RecipeDetailEffect
    data object TimerStarted : RecipeDetailEffect
    data object ExactAlarmPermissionDenied : RecipeDetailEffect
    data object TranslationApplied : RecipeDetailEffect
    data class TranslationFailed(val error: Throwable) : RecipeDetailEffect
    data class ShareReady(val archive: RecipeTransferArchive) : RecipeDetailEffect
    data object ShareFailed : RecipeDetailEffect
}

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val cookHistoryRepository: CookHistoryRepository,
    private val cookingTimerRepository: CookingTimerRepository,
    private val folderRepository: FolderRepository,
    private val tagRepository: TagRepository,
    imageStore: ImageStore,
    private val translateRecipe: TranslateRecipe,
    private val translationRepository: RecipeTranslationRepository,
    private val transferRepository: RecipeTransferRepository,
    private val clock: Clock,
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle["recipeId"])

    /**
     * The chosen serving count, or null while it matches the recipe's own. Kept only here —
     * changing it must never write to storage (PRD §3.3).
     */
    private val targetServings = MutableStateFlow<Int?>(null)
    private val isTranslating = MutableStateFlow(false)
    private val isSharing = MutableStateFlow(false)
    private val translatedLanguages = MutableStateFlow<Set<RecipeLanguage>>(emptySet())
    private var pendingTimerAction: PendingTimerAction? = null

    private val effectChannel = Channel<RecipeDetailEffect>(Channel.BUFFERED)
    val effects: Flow<RecipeDetailEffect> = effectChannel.receiveAsFlow()

    private val clockTicks = flow {
        while (true) {
            emit(clock.millis())
            delay(TIMER_TICK_MILLIS)
        }
    }

    private val metadataState = combine(
        folderRepository.observeAll(),
        tagRepository.observeAll(),
        isTranslating,
        translatedLanguages,
        isSharing,
    ) { folders, tags, translating, languages, sharing ->
        MetaDataGroup(folders, tags, translating, languages, sharing)
    }

    val uiState: StateFlow<RecipeDetailUiState> = combine(
        recipeRepository.observeRecipe(recipeId),
        targetServings,
        cookingTimerRepository.observeTimers(),
        clockTicks,
        metadataState,
    ) { recipe, target, timers, now, meta ->
        if (recipe == null) return@combine RecipeDetailUiState(isLoading = false)

        val servings = target ?: recipe.baseServings
        val factor = ServingScaler.factor(recipe.baseServings, servings ?: 0)

        RecipeDetailUiState(
            recipe = recipe,
            imagePath = recipe.imagePath?.let(imageStore::absolutePathOf),
            ingredientSections = ServingScaler.scaleSections(recipe.ingredientSections, factor),
            servings = servings,
            isScaled = servings != null && servings != recipe.baseServings,
            timers = timers,
            timerRemainingSeconds = timers.associate { timer ->
                timer.id to TimerMath.remainingSeconds(timer, now)
            },
            availableFolders = meta.folders,
            availableTags = meta.tags,
            isLoading = false,
            isTranslating = meta.isTranslating,
            translatedLanguages = meta.translatedLanguages,
            isSharing = meta.isSharing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = RecipeDetailUiState(),
    )

    init {
        viewModelScope.launch {
            val recipe = recipeRepository.getRecipe(recipeId) ?: return@launch
            val languages = translationRepository.getAvailableLanguages(
                recipeId = recipeId,
                sourceFingerprint = recipe.translationSourceFingerprint(),
            )
            translatedLanguages.value = RecipeLanguage.entries
                .filterTo(mutableSetOf()) { language ->
                    languages.any { it.matches(language) }
                }
        }
    }

    fun onEvent(event: RecipeDetailEvent) {
        when (event) {
            RecipeDetailEvent.IncreaseServings -> adjustServings(+1)
            RecipeDetailEvent.DecreaseServings -> adjustServings(-1)
            RecipeDetailEvent.ResetServings -> targetServings.value = null

            RecipeDetailEvent.ToggleFavorite -> viewModelScope.launch {
                val recipe = uiState.value.recipe ?: return@launch
                recipeRepository.setFavorite(recipe.id, !recipe.isFavorite)
            }

            is RecipeDetailEvent.SetRating -> viewModelScope.launch {
                val recipe = uiState.value.recipe ?: return@launch
                recipeRepository.save(recipe.copy(rating = event.rating))
            }

            is RecipeDetailEvent.SetFolder -> viewModelScope.launch {
                val recipe = uiState.value.recipe ?: return@launch
                recipeRepository.save(recipe.copy(folder = event.folder))
            }

            is RecipeDetailEvent.FolderCreateRequested -> viewModelScope.launch {
                val name = event.name.trim()
                if (name.isEmpty()) return@launch
                val recipe = uiState.value.recipe ?: return@launch
                val folder = folderRepository.create(name, event.color, event.icon)
                recipeRepository.save(recipe.copy(folder = folder))
            }

            is RecipeDetailEvent.AddTag -> viewModelScope.launch {
                val recipe = uiState.value.recipe ?: return@launch
                val cleanName = event.tagName.trim()
                if (cleanName.isEmpty()) return@launch
                if (recipe.tags.any { it.name.equals(cleanName, ignoreCase = true) }) return@launch
                val existingNames = recipe.tags.map { it.name }
                val newTags = tagRepository.findOrCreate(existingNames + cleanName)
                recipeRepository.save(recipe.copy(tags = newTags))
            }

            is RecipeDetailEvent.RemoveTag -> viewModelScope.launch {
                val recipe = uiState.value.recipe ?: return@launch
                val updatedTags = recipe.tags.filterNot { it.name.equals(event.tagName, ignoreCase = true) }
                recipeRepository.save(recipe.copy(tags = updatedTags))
            }

            RecipeDetailEvent.MarkCooked -> viewModelScope.launch {
                cookHistoryRepository.markCooked(recipeId)
                effectChannel.send(RecipeDetailEffect.MarkedCooked)
            }

            RecipeDetailEvent.Delete -> viewModelScope.launch {
                cookingTimerRepository.cancelForRecipe(recipeId)
                recipeRepository.delete(recipeId)
                effectChannel.send(RecipeDetailEffect.Deleted)
            }

            is RecipeDetailEvent.StartTimer -> requestTimerAction(
                PendingTimerAction.Start(
                    stepId = event.stepId,
                    label = event.label,
                    durationMinutes = event.durationMinutes,
                ),
            )

            is RecipeDetailEvent.PauseTimer -> viewModelScope.launch {
                cookingTimerRepository.pause(event.timerId)
            }

            is RecipeDetailEvent.ResumeTimer -> requestTimerAction(
                PendingTimerAction.Resume(event.timerId),
            )

            is RecipeDetailEvent.AddTimerMinute -> viewModelScope.launch {
                cookingTimerRepository.addTime(event.timerId, SECONDS_PER_MINUTE)
            }

            is RecipeDetailEvent.CancelTimer -> viewModelScope.launch {
                cookingTimerRepository.cancel(event.timerId)
            }

            RecipeDetailEvent.RetryPendingTimerAction -> retryPendingTimerAction()
            RecipeDetailEvent.CancelPendingTimerAction -> pendingTimerAction = null
            RecipeDetailEvent.RefreshTimerNotifications -> viewModelScope.launch {
                cookingTimerRepository.refreshNotifications()
            }
            is RecipeDetailEvent.Translate -> translate(event.target)
            RecipeDetailEvent.Share -> share()
        }
    }

    private fun share() {
        if (isSharing.value) return
        isSharing.value = true
        viewModelScope.launch {
            transferRepository.createTransfer(setOf(recipeId)).fold(
                onSuccess = { archive -> effectChannel.send(RecipeDetailEffect.ShareReady(archive)) },
                onFailure = { effectChannel.send(RecipeDetailEffect.ShareFailed) },
            )
            isSharing.value = false
        }
    }

    private fun translate(target: RecipeLanguage) {
        if (isTranslating.value) return
        val recipe = uiState.value.recipe ?: return
        isTranslating.value = true
        viewModelScope.launch {
            translateRecipe(recipe, target)
                .onSuccess { translated ->
                    runCatching { recipeRepository.save(translated) }
                        .onSuccess {
                            if (translated.displayLanguage != null) {
                                translatedLanguages.value += target
                            }
                            effectChannel.send(RecipeDetailEffect.TranslationApplied)
                        }
                        .onFailure { error ->
                            effectChannel.send(RecipeDetailEffect.TranslationFailed(error))
                        }
                    isTranslating.value = false
                }
                .onFailure { error ->
                    isTranslating.value = false
                    effectChannel.send(RecipeDetailEffect.TranslationFailed(error))
                }
        }
    }

    private fun requestTimerAction(action: PendingTimerAction) {
        viewModelScope.launch {
            if (!cookingTimerRepository.canScheduleExactAlarms()) {
                pendingTimerAction = action
                effectChannel.send(RecipeDetailEffect.RequestExactAlarmPermission)
                return@launch
            }
            runTimerAction(action)
        }
    }

    private fun retryPendingTimerAction() {
        val action = pendingTimerAction ?: return
        viewModelScope.launch {
            if (!cookingTimerRepository.canScheduleExactAlarms()) {
                pendingTimerAction = null
                effectChannel.send(RecipeDetailEffect.ExactAlarmPermissionDenied)
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
            effectChannel.send(RecipeDetailEffect.TimerStarted)
        } catch (_: SecurityException) {
            pendingTimerAction = action
            effectChannel.send(RecipeDetailEffect.RequestExactAlarmPermission)
        } catch (_: IllegalStateException) {
            pendingTimerAction = action
            effectChannel.send(RecipeDetailEffect.RequestExactAlarmPermission)
        }
    }

    private fun adjustServings(delta: Int) {
        val current = uiState.value.servings ?: return
        targetServings.value = (current + delta).coerceIn(MIN_SERVINGS, MAX_SERVINGS)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MIN_SERVINGS = 1
        const val MAX_SERVINGS = 99
        const val TIMER_TICK_MILLIS = 1_000L
        const val SECONDS_PER_MINUTE = 60L
    }
}

private sealed interface PendingTimerAction {
    data class Start(
        val stepId: String?,
        val label: String,
        val durationMinutes: Int,
    ) : PendingTimerAction

    data class Resume(val timerId: String) : PendingTimerAction
}

private fun String.matches(language: RecipeLanguage): Boolean =
    startsWith(language.languageTag, ignoreCase = true)

private data class MetaDataGroup(
    val folders: List<Folder>,
    val tags: List<Tag>,
    val isTranslating: Boolean,
    val translatedLanguages: Set<RecipeLanguage>,
    val isSharing: Boolean,
)

