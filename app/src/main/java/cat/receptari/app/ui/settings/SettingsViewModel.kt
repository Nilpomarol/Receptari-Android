package cat.receptari.app.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.R
import cat.receptari.app.core.util.TranslationKey
import cat.receptari.app.domain.ai.AiClient
import cat.receptari.app.domain.ai.RecipeTranslator
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

/** Which of the two keys a key action is about: Claude imports, OpenAI translates (ADR-014). */
enum class KeyProvider { Claude, OpenAi }

data class SettingsUiState(
    val hasKey: Boolean = false,
    val hasTranslationKey: Boolean = false,
    /**
     * Held in memory only while the user is typing. Never logged, never persisted in plain
     * text — [ApiKeyRepository] encrypts before it touches disk.
     */
    val keyInput: String = "",
    val keyVisible: Boolean = false,
    /** The key whose entry sheet is open, if any. */
    val editingKey: KeyProvider? = null,
    val testingKey: KeyProvider? = null,
    /** The key the remove confirmation is asking about, if it is showing. */
    val removingKey: KeyProvider? = null,
    val isBackupBusy: Boolean = false,
    val isTransferBusy: Boolean = false,
    val restorePreview: RestorePreview? = null,
    val lastBackupAt: Instant? = null,
) {
    val canSave: Boolean get() = keyInput.isNotBlank()
}

sealed interface SettingsEvent {
    data class KeyChanged(val value: String) : SettingsEvent
    data object ToggleKeyVisible : SettingsEvent
    data class EditKey(val provider: KeyProvider) : SettingsEvent
    data object CancelEditKey : SettingsEvent
    data object SaveKey : SettingsEvent
    data class TestKey(val provider: KeyProvider) : SettingsEvent
    data class RequestRemoveKey(val provider: KeyProvider) : SettingsEvent
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
    @param:TranslationKey private val translationKeyRepository: ApiKeyRepository,
    private val aiClient: AiClient,
    private val translator: RecipeTranslator,
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

        translationKeyRepository.observeHasKey()
            .onEach { hasKey -> _uiState.update { it.copy(hasTranslationKey = hasKey) } }
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

            is SettingsEvent.EditKey ->
                _uiState.update { it.copy(editingKey = event.provider, keyInput = "", keyVisible = false) }

            SettingsEvent.CancelEditKey -> clearInput()

            SettingsEvent.SaveKey -> saveKey()

            is SettingsEvent.TestKey -> testKey(event.provider)

            is SettingsEvent.RequestRemoveKey ->
                _uiState.update { it.copy(removingKey = event.provider) }

            SettingsEvent.DismissRemoveDialog ->
                _uiState.update { it.copy(removingKey = null) }

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

    private fun repository(provider: KeyProvider): ApiKeyRepository = when (provider) {
        KeyProvider.Claude -> apiKeyRepository
        KeyProvider.OpenAi -> translationKeyRepository
    }

    private fun saveKey() {
        val provider = _uiState.value.editingKey ?: return
        val key = _uiState.value.keyInput.trim()
        if (key.isEmpty()) return
        viewModelScope.launch {
            repository(provider).setKey(key)
            clearInput()
            _messages.send(SettingsMessage(R.string.settings_api_key_saved))
        }
    }

    private fun testKey(provider: KeyProvider) {
        if (_uiState.value.testingKey != null) return
        _uiState.update { it.copy(testingKey = provider) }
        viewModelScope.launch {
            val result = when (provider) {
                KeyProvider.Claude -> aiClient.testKey()
                KeyProvider.OpenAi -> translator.testKey()
            }
            _uiState.update { it.copy(testingKey = null) }
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
        val provider = _uiState.value.removingKey ?: return
        viewModelScope.launch {
            repository(provider).clearKey()
            _uiState.update { it.copy(removingKey = null) }
            clearInput()
            _messages.send(SettingsMessage(R.string.settings_api_key_removed))
        }
    }

    /** Drops the typed key from memory as soon as it is no longer needed. */
    private fun clearInput() {
        _uiState.update { it.copy(keyInput = "", keyVisible = false, editingKey = null) }
    }
}

@StringRes
private fun Throwable.backupMessageRes(): Int = when (this) {
    is BackupError.NewerVersion -> R.string.settings_restore_newer_version
    is BackupError.InvalidArchive -> R.string.settings_restore_invalid
    else -> R.string.settings_backup_failed
}
