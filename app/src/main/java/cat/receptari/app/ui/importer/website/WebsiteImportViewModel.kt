package cat.receptari.app.ui.importer.website

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.R
import cat.receptari.app.domain.importer.ImportRecipeFromWebsite
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
import javax.inject.Inject

data class WebsiteImportUiState(
    val url: String = "",
    val isExtracting: Boolean = false,
) {
    val canExtract: Boolean get() = url.isNotBlank() && !isExtracting
}

sealed interface WebsiteImportEvent {
    data class UrlChanged(val value: String) : WebsiteImportEvent
    data object Extract : WebsiteImportEvent
}

@HiltViewModel
class WebsiteImportViewModel @Inject constructor(
    private val importFromWebsite: ImportRecipeFromWebsite,
    private val handoff: ImportDraftHandoff,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebsiteImportUiState())
    val uiState: StateFlow<WebsiteImportUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<ImportEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun onEvent(event: WebsiteImportEvent) {
        when (event) {
            is WebsiteImportEvent.UrlChanged -> _uiState.update { it.copy(url = event.value) }
            WebsiteImportEvent.Extract -> extract()
        }
    }

    private fun extract() {
        val url = _uiState.value.url.trim()
        if (url.isEmpty() || _uiState.value.isExtracting) return

        _uiState.update { it.copy(isExtracting = true) }
        viewModelScope.launch {
            val result = importFromWebsite(url)
            _uiState.update { it.copy(isExtracting = false) }

            result.fold(
                onSuccess = { draft ->
                    if (draft.isEmpty) {
                        effectChannel.send(
                            ImportEffect.ShowMessage(R.string.import_nothing_found),
                        )
                    } else {
                        // The URL the user typed is the recipe's source, and is worth keeping
                        // even when the page's own metadata did not mention it.
                        handoff.offer(draft.copy(sourceUrl = draft.sourceUrl ?: url))
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
