package cat.receptari.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.repository.CookHistoryRepository
import cat.receptari.app.domain.repository.ImageStore
import cat.receptari.app.domain.repository.RecipeRepository
import cat.receptari.app.domain.scaling.ScaledIngredientSection
import cat.receptari.app.domain.scaling.ServingScaler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val imagePath: String? = null,
    val ingredientSections: List<ScaledIngredientSection> = emptyList(),
    val servings: Int? = null,
    val isScaled: Boolean = false,
    val isLoading: Boolean = true,
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
}

sealed interface RecipeDetailEffect {
    data object MarkedCooked : RecipeDetailEffect
    data object Deleted : RecipeDetailEffect
}

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val cookHistoryRepository: CookHistoryRepository,
    imageStore: ImageStore,
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle["recipeId"])

    /**
     * The chosen serving count, or null while it matches the recipe's own. Kept only here —
     * changing it must never write to storage (PRD §3.3).
     */
    private val targetServings = MutableStateFlow<Int?>(null)

    private val effectChannel = Channel<RecipeDetailEffect>(Channel.BUFFERED)
    val effects: Flow<RecipeDetailEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<RecipeDetailUiState> = combine(
        recipeRepository.observeRecipe(recipeId),
        targetServings,
    ) { recipe, target ->
        if (recipe == null) return@combine RecipeDetailUiState(isLoading = false)

        val servings = target ?: recipe.baseServings
        val factor = ServingScaler.factor(recipe.baseServings, servings ?: 0)

        RecipeDetailUiState(
            recipe = recipe,
            imagePath = recipe.imagePath?.let(imageStore::absolutePathOf),
            ingredientSections = ServingScaler.scaleSections(recipe.ingredientSections, factor),
            servings = servings,
            isScaled = servings != null && servings != recipe.baseServings,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = RecipeDetailUiState(),
    )

    fun onEvent(event: RecipeDetailEvent) {
        when (event) {
            RecipeDetailEvent.IncreaseServings -> adjustServings(+1)
            RecipeDetailEvent.DecreaseServings -> adjustServings(-1)
            RecipeDetailEvent.ResetServings -> targetServings.value = null

            RecipeDetailEvent.ToggleFavorite -> viewModelScope.launch {
                val recipe = uiState.value.recipe ?: return@launch
                recipeRepository.setFavorite(recipe.id, !recipe.isFavorite)
            }

            RecipeDetailEvent.MarkCooked -> viewModelScope.launch {
                cookHistoryRepository.markCooked(recipeId)
                effectChannel.send(RecipeDetailEffect.MarkedCooked)
            }

            RecipeDetailEvent.Delete -> viewModelScope.launch {
                recipeRepository.delete(recipeId)
                effectChannel.send(RecipeDetailEffect.Deleted)
            }
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
    }
}
