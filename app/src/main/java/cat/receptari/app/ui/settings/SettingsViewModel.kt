package cat.receptari.app.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.R
import cat.receptari.app.domain.ai.AiClient
import cat.receptari.app.domain.backup.BackupArchive
import cat.receptari.app.domain.backup.BackupError
import cat.receptari.app.domain.backup.RestorePreview
import cat.receptari.app.domain.repository.ApiKeyRepository
import cat.receptari.app.domain.repository.BackupRepository
import cat.receptari.app.domain.repository.CookingTimerRepository
import cat.receptari.app.domain.repository.RecipeTransferRepository
import cat.receptari.app.domain.transfer.RecipeTransferArchive
import cat.receptari.app.ui.common.aiMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
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
    val isBackupBusy: Boolean = false,
    val isTransferBusy: Boolean = false,
    val restorePreview: RestorePreview? = null,
    val lastBackupAt: Instant? = null,
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
    data object CreateBackup : SettingsEvent
    data object ChooseRestore : SettingsEvent
    data object ConfirmRestore : SettingsEvent
    data object DismissRestore : SettingsEvent
    data object ShareLibrary : SettingsEvent
}

/** One-shot feedback. Everything the user needs to hear about here fits in a snackbar. */
data class SettingsMessage(@param:StringRes val messageRes: Int)

sealed interface SettingsEffect {
    data class SaveBackup(val archive: BackupArchive) : SettingsEffect
    data object OpenBackup : SettingsEffect
    data object RestartAfterRestore : SettingsEffect
    data class ShareRecipes(val archive: RecipeTransferArchive) : SettingsEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val aiClient: AiClient,
    private val backupRepository: BackupRepository,
    private val cookingTimerRepository: CookingTimerRepository,
    private val transferRepository: RecipeTransferRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _messages = Channel<SettingsMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        apiKeyRepository.observeHasKey()
            .onEach { hasKey -> _uiState.update { it.copy(hasKey = hasKey) } }
            .launchIn(viewModelScope)

        backupRepository.observeLastBackupAt()
            .onEach { at -> _uiState.update { it.copy(lastBackupAt = at) } }
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
            SettingsEvent.CreateBackup -> createBackup()
            SettingsEvent.ChooseRestore -> viewModelScope.launch {
                backupRepository.discardStagedRestore()
                _effects.send(SettingsEffect.OpenBackup)
            }
            SettingsEvent.ConfirmRestore -> confirmRestore()
            SettingsEvent.DismissRestore -> viewModelScope.launch {
                backupRepository.discardStagedRestore()
                _uiState.update { it.copy(restorePreview = null) }
            }
            SettingsEvent.ShareLibrary -> shareLibrary()
        }
    }

    private fun shareLibrary() {
        if (_uiState.value.isTransferBusy) return
        _uiState.update { it.copy(isTransferBusy = true) }
        viewModelScope.launch {
            transferRepository.createTransfer().fold(
                onSuccess = { archive -> _effects.send(SettingsEffect.ShareRecipes(archive)) },
                onFailure = { _messages.send(SettingsMessage(R.string.transfer_share_failed)) },
            )
            _uiState.update { it.copy(isTransferBusy = false) }
        }
    }

    fun onRestoreFileSelected(file: File?) {
        if (file == null) {
            viewModelScope.launch {
                _messages.send(SettingsMessage(R.string.settings_restore_invalid))
            }
            return
        }
        _uiState.update { it.copy(isBackupBusy = true) }
        viewModelScope.launch {
            backupRepository.stageRestore(file).fold(
                onSuccess = { preview ->
                    _uiState.update { it.copy(isBackupBusy = false, restorePreview = preview) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isBackupBusy = false) }
                    _messages.send(SettingsMessage(error.backupMessageRes()))
                },
            )
        }
    }

    fun onBackupSaved(archive: BackupArchive, succeeded: Boolean) {
        viewModelScope.launch {
            backupRepository.discardExport(archive.file)
            if (succeeded) backupRepository.recordBackupCompleted()
            _messages.send(
                SettingsMessage(
                    if (succeeded) R.string.settings_backup_exported
                    else R.string.settings_backup_failed,
                ),
            )
        }
    }

    fun onBackupSaveCancelled(archive: BackupArchive) {
        viewModelScope.launch { backupRepository.discardExport(archive.file) }
    }

    private fun createBackup() {
        if (_uiState.value.isBackupBusy) return
        _uiState.update { it.copy(isBackupBusy = true) }
        viewModelScope.launch {
            backupRepository.createBackup().fold(
                onSuccess = { archive -> _effects.send(SettingsEffect.SaveBackup(archive)) },
                onFailure = { error -> _messages.send(SettingsMessage(error.backupMessageRes())) },
            )
            _uiState.update { it.copy(isBackupBusy = false) }
        }
    }

    private fun confirmRestore() {
        if (_uiState.value.isBackupBusy) return
        _uiState.update { it.copy(isBackupBusy = true) }
        viewModelScope.launch {
            // Timers are intentionally excluded. Cancel their alarms before recipe ids change.
            cookingTimerRepository.observeTimers().first().forEach { timer ->
                cookingTimerRepository.cancel(timer.id)
            }
            backupRepository.confirmRestore().fold(
                onSuccess = { _effects.send(SettingsEffect.RestartAfterRestore) },
                onFailure = { error ->
                    _uiState.update { it.copy(isBackupBusy = false, restorePreview = null) }
                    _messages.send(SettingsMessage(error.backupMessageRes()))
                },
            )
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

@StringRes
private fun Throwable.backupMessageRes(): Int = when (this) {
    is BackupError.NewerVersion -> R.string.settings_restore_newer_version
    is BackupError.InvalidArchive -> R.string.settings_restore_invalid
    else -> R.string.settings_backup_failed
}
