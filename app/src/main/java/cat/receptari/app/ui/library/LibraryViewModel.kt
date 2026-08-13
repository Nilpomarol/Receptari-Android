package cat.receptari.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.FolderSummary
import cat.receptari.app.domain.model.RecipeFilter
import cat.receptari.app.domain.model.RecipeQuery
import cat.receptari.app.domain.model.RecipeSort
import cat.receptari.app.domain.model.RecipeSummary
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.domain.repository.FolderRepository
import cat.receptari.app.domain.repository.ImageStore
import cat.receptari.app.domain.repository.RecipeRepository
import cat.receptari.app.domain.repository.RecipeTransferRepository
import cat.receptari.app.domain.repository.TagRepository
import cat.receptari.app.domain.transfer.RecipeTransferArchive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class LibraryUiState(
    val recipes: List<RecipeSummary> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val availableFolders: List<FolderSummary> = emptyList(),
    val searchQuery: String = "",
    val sort: RecipeSort = RecipeSort.RecentlyAdded,
    val filter: RecipeFilter = RecipeFilter(),
    val isLoading: Boolean = true,
    val isSelectionMode: Boolean = false,
    val selectedRecipeIds: Set<String> = emptySet(),
    val isSharingSelection: Boolean = false,
) {
    /** Empty because there are no recipes at all, rather than because nothing matched. */
    val isLibraryEmpty: Boolean
        get() = recipes.isEmpty() && searchQuery.isBlank() && !filter.isActive

}

sealed interface LibraryEvent {
    data class SearchQueryChanged(val query: String) : LibraryEvent
    data class SortChanged(val sort: RecipeSort) : LibraryEvent
    data object ToggleFavoritesFilter : LibraryEvent
    data object ToggleNeverCookedFilter : LibraryEvent
    data object ToggleRecentlyCookedFilter : LibraryEvent
    data class ToggleTagFilter(val tagId: String) : LibraryEvent
    data object ClearFilters : LibraryEvent
    data class ToggleFavorite(val id: String, val isFavorite: Boolean) : LibraryEvent
    data class FolderCreateRequested(val name: String, val color: FolderColor, val icon: FolderIcon) :
        LibraryEvent
    data object StartSelection : LibraryEvent
    data object ClearSelection : LibraryEvent
    data class ToggleSelection(val recipeId: String) : LibraryEvent
    data object SelectAllVisible : LibraryEvent
    data object ShareSelection : LibraryEvent
}

sealed interface LibraryEffect {
    data class ShareReady(val archive: RecipeTransferArchive) : LibraryEffect
    data object ShareFailed : LibraryEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    tagRepository: TagRepository,
    private val folderRepository: FolderRepository,
    private val imageStore: ImageStore,
    private val transferRepository: RecipeTransferRepository,
    private val clock: Clock,
) : ViewModel() {

    private val query = MutableStateFlow(RecipeQuery())
    private val isSelectionMode = MutableStateFlow(false)
    private val selectedRecipeIds = MutableStateFlow<Set<String>>(emptySet())
    private val isSharingSelection = MutableStateFlow(false)
    private val effectChannel = Channel<LibraryEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private val selectionState = combine(
        isSelectionMode,
        selectedRecipeIds,
        isSharingSelection,
    ) { selecting, selected, sharing -> Triple(selecting, selected, sharing) }

    val uiState: StateFlow<LibraryUiState> = combine(
        query,
        query.flatMapLatest { recipeRepository.observeSummaries(it) },
        tagRepository.observeAll(),
        folderRepository.observeAllWithCounts(),
        selectionState,
    ) { currentQuery, recipes, tags, folders, selection ->
        LibraryUiState(
            // Stored image paths are relative so they survive a reinstall; the image
            // loader needs a real location, so they are resolved here on the way to the UI.
            recipes = recipes.map { summary ->
                summary.copy(imagePath = summary.imagePath?.let(imageStore::absolutePathOf))
            },
            availableTags = tags,
            availableFolders = folders,
            searchQuery = currentQuery.searchQuery,
            sort = currentQuery.sort,
            filter = currentQuery.filter,
            isLoading = false,
            isSelectionMode = selection.first,
            selectedRecipeIds = selection.second,
            isSharingSelection = selection.third,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = LibraryUiState(),
    )

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.SearchQueryChanged ->
                query.update { it.copy(searchQuery = event.query) }

            is LibraryEvent.SortChanged ->
                query.update { it.copy(sort = event.sort) }

            LibraryEvent.ToggleFavoritesFilter ->
                query.update { it.withFilter { filter -> filter.copy(favoritesOnly = !filter.favoritesOnly) } }

            LibraryEvent.ToggleNeverCookedFilter ->
                query.update {
                    it.withFilter { filter ->
                        // Never-cooked and recently-cooked are mutually exclusive; holding
                        // both would always return nothing.
                        filter.copy(
                            neverCooked = !filter.neverCooked,
                            cookedSinceEpochMillis = null,
                        )
                    }
                }

            LibraryEvent.ToggleRecentlyCookedFilter ->
                query.update {
                    it.withFilter { filter ->
                        val enabled = filter.cookedSinceEpochMillis != null
                        filter.copy(
                            cookedSinceEpochMillis = if (enabled) null else recentThreshold(),
                            neverCooked = false,
                        )
                    }
                }

            is LibraryEvent.ToggleTagFilter ->
                query.update {
                    it.withFilter { filter ->
                        val tags = filter.tagIds.toMutableSet()
                        if (!tags.add(event.tagId)) tags.remove(event.tagId)
                        filter.copy(tagIds = tags)
                    }
                }

            LibraryEvent.ClearFilters ->
                query.update { it.copy(filter = RecipeFilter()) }

            is LibraryEvent.ToggleFavorite -> viewModelScope.launch {
                recipeRepository.setFavorite(event.id, !event.isFavorite)
            }

            is LibraryEvent.FolderCreateRequested -> {
                val name = event.name.trim()
                if (name.isEmpty()) return
                viewModelScope.launch { folderRepository.create(name, event.color, event.icon) }
            }

            LibraryEvent.StartSelection -> isSelectionMode.value = true

            LibraryEvent.ClearSelection -> {
                isSelectionMode.value = false
                selectedRecipeIds.value = emptySet()
            }

            is LibraryEvent.ToggleSelection -> selectedRecipeIds.update { selected ->
                selected.toMutableSet().apply {
                    if (!add(event.recipeId)) remove(event.recipeId)
                }
            }

            LibraryEvent.SelectAllVisible -> selectedRecipeIds.value =
                uiState.value.recipes.mapTo(mutableSetOf()) { it.id }

            LibraryEvent.ShareSelection -> shareSelection()
        }
    }

    private fun shareSelection() {
        val selected = selectedRecipeIds.value
        if (selected.isEmpty() || isSharingSelection.value) return
        isSharingSelection.value = true
        viewModelScope.launch {
            transferRepository.createTransfer(selected).fold(
                onSuccess = { archive -> effectChannel.send(LibraryEffect.ShareReady(archive)) },
                onFailure = { effectChannel.send(LibraryEffect.ShareFailed) },
            )
            isSharingSelection.value = false
        }
    }

    private fun recentThreshold(): Long =
        Instant.now(clock).minus(Duration.ofDays(RECENT_DAYS)).toEpochMilli()

    private inline fun RecipeQuery.withFilter(transform: (RecipeFilter) -> RecipeFilter) =
        copy(filter = transform(filter))

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val RECENT_DAYS = 30L
    }
}
