package cat.receptari.app.ui.folders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.RecipeFilter
import cat.receptari.app.domain.model.RecipeQuery
import cat.receptari.app.domain.model.RecipeSummary
import cat.receptari.app.domain.repository.FolderRepository
import cat.receptari.app.domain.repository.ImageStore
import cat.receptari.app.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The recipes filed under one folder, plus the folder itself so its live name is shown. */
data class FolderContentsUiState(
    val folder: Folder? = null,
    val recipes: List<RecipeSummary> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface FolderContentsEvent {
    data class ToggleFavorite(val id: String, val isFavorite: Boolean) : FolderContentsEvent
    data class EditConfirmed(val name: String, val color: FolderColor, val icon: FolderIcon) :
        FolderContentsEvent
    data object DeleteConfirmed : FolderContentsEvent
}

sealed interface FolderContentsEffect {
    /** The folder is gone; the screen has nothing left to show. */
    data object Deleted : FolderContentsEffect
}

@HiltViewModel
class FolderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val folderRepository: FolderRepository,
    private val imageStore: ImageStore,
) : ViewModel() {

    private val folderId: String = checkNotNull(savedStateHandle["folderId"])

    private val effectChannel = Channel<FolderContentsEffect>(Channel.BUFFERED)
    val effects: Flow<FolderContentsEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<FolderContentsUiState> = combine(
        folderRepository.observeAll(),
        recipeRepository.observeSummaries(RecipeQuery(filter = RecipeFilter(folderId = folderId))),
    ) { folders, recipes ->
        FolderContentsUiState(
            folder = folders.find { it.id == folderId },
            recipes = recipes.map { summary ->
                summary.copy(imagePath = summary.imagePath?.let(imageStore::absolutePathOf))
            },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = FolderContentsUiState(),
    )

    fun onEvent(event: FolderContentsEvent) {
        when (event) {
            is FolderContentsEvent.ToggleFavorite -> viewModelScope.launch {
                recipeRepository.setFavorite(event.id, !event.isFavorite)
            }

            is FolderContentsEvent.EditConfirmed -> {
                val name = event.name.trim()
                if (name.isEmpty()) return
                viewModelScope.launch {
                    folderRepository.update(folderId, name, event.color, event.icon)
                }
            }

            FolderContentsEvent.DeleteConfirmed -> viewModelScope.launch {
                folderRepository.delete(folderId)
                effectChannel.send(FolderContentsEffect.Deleted)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
