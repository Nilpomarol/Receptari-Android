package cat.receptari.app.ui.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.data.nearby.NearbyRecipeTransferManager
import cat.receptari.app.domain.transfer.NearbyDevice
import cat.receptari.app.domain.transfer.NearbyTransferMode
import cat.receptari.app.domain.transfer.NearbyTransferPhase
import cat.receptari.app.domain.transfer.NearbyTransferState
import cat.receptari.app.domain.transfer.RecipeTransferArchive
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NearbyTransferViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val manager: NearbyRecipeTransferManager,
) : ViewModel() {
    val state: StateFlow<NearbyTransferState> = manager.state
    val mode: NearbyTransferMode = checkNotNull(
        savedStateHandle.get<String>(NearbyTransferActivity.EXTRA_MODE)
            ?.let(NearbyTransferMode::valueOf),
    )
    private val archivePath: String? = savedStateHandle[NearbyTransferActivity.EXTRA_ARCHIVE_PATH]
    private var started = false

    init {
        viewModelScope.launch {
            manager.state.collect { current ->
                if (mode == NearbyTransferMode.RECEIVE &&
                    current.phase == NearbyTransferPhase.IMPORTING
                ) {
                    manager.importReceivedPayload()
                }
            }
        }
    }

    fun start() {
        if (started) return
        started = true
        when (mode) {
            NearbyTransferMode.SEND -> {
                val file = archivePath?.let(::File)
                if (file?.isFile == true) {
                    manager.startSending(RecipeTransferArchive(file, file.name))
                } else {
                    manager.cancel()
                }
            }
            NearbyTransferMode.RECEIVE -> manager.startReceiving()
        }
    }

    fun connect(device: NearbyDevice) = manager.connect(device)
    fun confirmConnection() = manager.confirmConnection()
    fun rejectConnection() = manager.rejectConnection()
    fun cancel() = manager.cancel()

    override fun onCleared() {
        manager.cancel()
        super.onCleared()
    }
}
