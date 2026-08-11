package cat.receptari.app.ui.importer.image

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.R
import cat.receptari.app.domain.ai.AiClient
import cat.receptari.app.ui.common.aiMessageRes
import cat.receptari.app.ui.importer.ImportDraftHandoff
import cat.receptari.app.ui.importer.ImportEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * One photographed page. Identity is the id, not the bytes — comparing image arrays on every
 * recomposition would be both wrong (arrays use reference equality) and wasteful.
 */
data class ImportPage(
    val id: String = UUID.randomUUID().toString(),
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean = other is ImportPage && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

data class ImageImportUiState(
    val pages: List<ImportPage> = emptyList(),
    val isExtracting: Boolean = false,
) {
    val canExtract: Boolean get() = pages.isNotEmpty() && !isExtracting
}

sealed interface ImageImportEvent {
    data class PagesAdded(val images: List<ByteArray>) : ImageImportEvent
    data class PageRemoved(val id: String) : ImageImportEvent
    data object Extract : ImageImportEvent
}

@HiltViewModel
class ImageImportViewModel @Inject constructor(
    private val aiClient: AiClient,
    private val handoff: ImportDraftHandoff,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageImportUiState())
    val uiState: StateFlow<ImageImportUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<ImportEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun onEvent(event: ImageImportEvent) {
        when (event) {
            is ImageImportEvent.PagesAdded -> _uiState.update { state ->
                state.copy(pages = state.pages + event.images.map { ImportPage(bytes = it) })
            }

            is ImageImportEvent.PageRemoved -> _uiState.update { state ->
                state.copy(pages = state.pages.filterNot { it.id == event.id })
            }

            ImageImportEvent.Extract -> extract()
        }
    }

    private fun extract() {
        val pages = _uiState.value.pages
        if (pages.isEmpty() || _uiState.value.isExtracting) return

        _uiState.update { it.copy(isExtracting = true) }
        viewModelScope.launch {
            // All pages go in one request: a recipe spread over two cookbook pages is one
            // recipe, and sending them separately would produce two half-recipes (PRD §9).
            val result = aiClient.extractFromImages(pages.map { it.bytes })
            _uiState.update { it.copy(isExtracting = false) }

            result.fold(
                onSuccess = { draft ->
                    if (draft.isEmpty) {
                        effectChannel.send(
                            ImportEffect.ShowMessage(R.string.import_nothing_found_image),
                        )
                    } else {
                        handoff.offer(draft)
                        effectChannel.send(ImportEffect.DraftReady)
                    }
                },
                onFailure = { error ->
                    effectChannel.send(ImportEffect.ShowMessage(error.aiMessageRes()))
                },
            )
        }
    }
}
