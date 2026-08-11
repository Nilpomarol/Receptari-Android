package cat.receptari.app.ui.importer.text

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
import javax.inject.Inject

data class TextImportUiState(
    val text: String = "",
    val isExtracting: Boolean = false,
) {
    val canExtract: Boolean get() = text.isNotBlank() && !isExtracting
}

sealed interface TextImportEvent {
    data class TextChanged(val value: String) : TextImportEvent
    data object Extract : TextImportEvent
}

@HiltViewModel
class TextImportViewModel @Inject constructor(
    private val aiClient: AiClient,
    private val handoff: ImportDraftHandoff,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextImportUiState())
    val uiState: StateFlow<TextImportUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<ImportEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun onEvent(event: TextImportEvent) {
        when (event) {
            is TextImportEvent.TextChanged ->
                _uiState.update { it.copy(text = event.value) }

            TextImportEvent.Extract -> extract()
        }
    }

    private fun extract() {
        val text = _uiState.value.text.trim()
        if (text.isEmpty() || _uiState.value.isExtracting) return

        _uiState.update { it.copy(isExtracting = true) }
        viewModelScope.launch {
            val result = aiClient.extractFromText(text)
            _uiState.update { it.copy(isExtracting = false) }

            result.fold(
                onSuccess = { draft ->
                    // A draft with nothing in it means the text was not a recipe. Opening an
                    // empty editor would look like the app had silently lost the paste.
                    if (draft.isEmpty) {
                        effectChannel.send(
                            ImportEffect.ShowMessage(R.string.import_nothing_found),
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
