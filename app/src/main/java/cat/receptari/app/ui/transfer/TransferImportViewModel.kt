package cat.receptari.app.ui.transfer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.domain.repository.RecipeTransferRepository
import cat.receptari.app.domain.transfer.RecipeTransferError
import cat.receptari.app.domain.transfer.RecipeTransferResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface TransferImportUiState {
    data object Importing : TransferImportUiState
    data class Imported(val result: RecipeTransferResult) : TransferImportUiState
    data class Failed(val error: RecipeTransferError) : TransferImportUiState
}

@HiltViewModel
class TransferImportViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val transferRepository: RecipeTransferRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TransferImportUiState>(TransferImportUiState.Importing)
    val uiState: StateFlow<TransferImportUiState> = _uiState.asStateFlow()
    private var started = false

    fun import(uri: Uri?) {
        if (started) return
        started = true
        if (uri == null) {
            _uiState.value = TransferImportUiState.Failed(RecipeTransferError.InvalidPackage())
            return
        }
        viewModelScope.launch {
            val localFile = withContext(ioDispatcher) { copyIncoming(uri) }
            if (localFile == null) {
                _uiState.value = TransferImportUiState.Failed(RecipeTransferError.InvalidPackage())
                return@launch
            }
            transferRepository.importTransfer(localFile).fold(
                onSuccess = { result -> _uiState.value = TransferImportUiState.Imported(result) },
                onFailure = { error ->
                    _uiState.value = TransferImportUiState.Failed(
                        error as? RecipeTransferError ?: RecipeTransferError.Storage(error),
                    )
                },
            )
            withContext(ioDispatcher) { localFile.delete() }
        }
    }

    private fun copyIncoming(uri: Uri): File? = runCatching {
        val directory = File(context.cacheDir, "recipe-transfers/received").apply { mkdirs() }
        val target = File(directory, "${UUID.randomUUID()}.receptari-share")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_PACKAGE_BYTES) error("Transfer package is too large")
                    output.write(buffer, 0, read)
                }
            }
        } ?: error("No input stream")
        target
    }.getOrNull()

    private companion object {
        const val MAX_PACKAGE_BYTES = 2L * 1024 * 1024 * 1024
    }
}
