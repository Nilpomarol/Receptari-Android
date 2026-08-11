package cat.receptari.app.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.R
import cat.receptari.app.domain.ai.AiClient
import cat.receptari.app.domain.repository.ApiKeyRepository
import cat.receptari.app.ui.common.aiMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val hasKey: Boolean = false,
    /**
     * Held in memory only while the user is typing. Never logged, never persisted in plain
     * text — [ApiKeyRepository] encrypts before it touches disk.
     */
    val keyInput: String = "",
    val keyVisible: Boolean = false,
    val isEditingKey: Boolean = false,
    val isTesting: Boolean = false,
    val showRemoveDialog: Boolean = false,
) {
    val canSave: Boolean get() = keyInput.isNotBlank() && !isTesting
}

sealed interface SettingsEvent {
    data class KeyChanged(val value: String) : SettingsEvent
    data object ToggleKeyVisible : SettingsEvent
    data object EditKey : SettingsEvent
    data object CancelEditKey : SettingsEvent
    data object SaveKey : SettingsEvent
    data object TestKey : SettingsEvent
    data object RequestRemoveKey : SettingsEvent
    data object ConfirmRemoveKey : SettingsEvent
    data object DismissRemoveDialog : SettingsEvent
}

/** One-shot feedback. Everything the user needs to hear about here fits in a snackbar. */
data class SettingsMessage(@param:StringRes val messageRes: Int)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val aiClient: AiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _messages = Channel<SettingsMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        apiKeyRepository.observeHasKey()
            .onEach { hasKey -> _uiState.update { it.copy(hasKey = hasKey) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.KeyChanged ->
                _uiState.update { it.copy(keyInput = event.value) }

            SettingsEvent.ToggleKeyVisible ->
                _uiState.update { it.copy(keyVisible = !it.keyVisible) }

            SettingsEvent.EditKey ->
                _uiState.update { it.copy(isEditingKey = true, keyInput = "", keyVisible = false) }

            SettingsEvent.CancelEditKey -> clearInput()

            SettingsEvent.SaveKey -> saveKey()

            SettingsEvent.TestKey -> testKey()

            SettingsEvent.RequestRemoveKey ->
                _uiState.update { it.copy(showRemoveDialog = true) }

            SettingsEvent.DismissRemoveDialog ->
                _uiState.update { it.copy(showRemoveDialog = false) }

            SettingsEvent.ConfirmRemoveKey -> removeKey()
        }
    }

    private fun saveKey() {
        val key = _uiState.value.keyInput.trim()
        if (key.isEmpty()) return
        viewModelScope.launch {
            apiKeyRepository.setKey(key)
            clearInput()
            _messages.send(SettingsMessage(R.string.settings_api_key_saved))
        }
    }

    private fun testKey() {
        if (_uiState.value.isTesting) return
        _uiState.update { it.copy(isTesting = true) }
        viewModelScope.launch {
            val result = aiClient.testKey()
            _uiState.update { it.copy(isTesting = false) }
            _messages.send(
                SettingsMessage(
                    result.fold(
                        onSuccess = { R.string.settings_api_key_valid },
                        onFailure = { it.aiMessageRes() },
                    ),
                ),
            )
        }
    }

    private fun removeKey() {
        viewModelScope.launch {
            apiKeyRepository.clearKey()
            _uiState.update { it.copy(showRemoveDialog = false) }
            clearInput()
            _messages.send(SettingsMessage(R.string.settings_api_key_removed))
        }
    }

    /** Drops the typed key from memory as soon as it is no longer needed. */
    private fun clearInput() {
        _uiState.update { it.copy(keyInput = "", keyVisible = false, isEditingKey = false) }
    }
}
