package cat.receptari.app.data.nearby

import android.content.Context
import android.os.Build
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.domain.repository.RecipeTransferRepository
import cat.receptari.app.domain.transfer.NearbyDevice
import cat.receptari.app.domain.transfer.NearbyTransferFailure
import cat.receptari.app.domain.transfer.NearbyTransferMode
import cat.receptari.app.domain.transfer.NearbyTransferPhase
import cat.receptari.app.domain.transfer.NearbyTransferState
import cat.receptari.app.domain.transfer.RecipeTransferArchive
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Singleton
class NearbyRecipeTransferManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val transferRepository: RecipeTransferRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val _state = MutableStateFlow(NearbyTransferState())
    val state: StateFlow<NearbyTransferState> = _state.asStateFlow()

    private var outgoingArchive: RecipeTransferArchive? = null
    private var pendingEndpointId: String? = null
    private var connectedEndpointId: String? = null
    private var outgoingFilePayloadId: Long? = null
    private val receivedFiles = mutableMapOf<Long, Payload>()
    private var completedIncomingPayloadId: Long? = null
    private var importStarted = false

    fun startSending(archive: RecipeTransferArchive) {
        stopTransport()
        outgoingArchive = archive
        _state.value = NearbyTransferState(
            mode = NearbyTransferMode.SEND,
            phase = NearbyTransferPhase.SEARCHING,
        )
        client.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            DiscoveryOptions.Builder().setStrategy(STRATEGY).build(),
        ).addOnFailureListener { fail(NearbyTransferFailure.START) }
    }

    fun startReceiving() {
        stopTransport()
        outgoingArchive = null
        _state.value = NearbyTransferState(
            mode = NearbyTransferMode.RECEIVE,
            phase = NearbyTransferPhase.ADVERTISING,
        )
        client.startAdvertising(
            localDeviceName(),
            SERVICE_ID,
            connectionLifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build(),
        ).addOnFailureListener { fail(NearbyTransferFailure.START) }
    }

    fun connect(device: NearbyDevice) {
        if (_state.value.mode != NearbyTransferMode.SEND) return
        _state.value = _state.value.copy(
            phase = NearbyTransferPhase.CONNECTING,
            peerName = device.name,
        )
        client.stopDiscovery()
        client.requestConnection(localDeviceName(), device.endpointId, connectionLifecycleCallback)
            .addOnFailureListener { fail(NearbyTransferFailure.CONNECTION) }
    }

    fun confirmConnection() {
        val endpointId = pendingEndpointId ?: return
        client.acceptConnection(endpointId, payloadCallback)
            .addOnFailureListener { fail(NearbyTransferFailure.CONNECTION) }
        _state.value = _state.value.copy(
            phase = NearbyTransferPhase.CONNECTING,
            authenticationDigits = null,
        )
    }

    fun rejectConnection() {
        pendingEndpointId?.let(client::rejectConnection)
        pendingEndpointId = null
        restartCurrentMode()
    }

    suspend fun importReceivedPayload() {
        if (importStarted) return
        val payloadId = completedIncomingPayloadId ?: return
        val payload = receivedFiles[payloadId] ?: return
        val uri = payload.asFile()?.asUri() ?: run {
            fail(NearbyTransferFailure.TRANSFER)
            return
        }
        importStarted = true
        _state.value = _state.value.copy(
            phase = NearbyTransferPhase.IMPORTING,
            progress = null,
        )
        val result = withContext(ioDispatcher) {
            runCatching {
                val directory = File(context.cacheDir, "recipe-transfers/nearby").apply { mkdirs() }
                val localFile = File(directory, "${UUID.randomUUID()}.receptari-share")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        localFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("No incoming payload stream")
                    transferRepository.importTransfer(localFile).getOrThrow()
                } finally {
                    localFile.delete()
                    runCatching { context.contentResolver.delete(uri, null, null) }
                }
            }
        }
        result.fold(
            onSuccess = { imported ->
                _state.value = _state.value.copy(
                    phase = NearbyTransferPhase.COMPLETE,
                    result = imported,
                    progress = 1f,
                )
                sendControl(ACK_SUCCESS)
            },
            onFailure = {
                sendControl(ACK_FAILURE)
                fail(NearbyTransferFailure.IMPORT)
            },
        )
    }

    fun cancel() {
        stopTransport()
        outgoingArchive?.file?.delete()
        outgoingArchive = null
        _state.value = NearbyTransferState()
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val device = NearbyDevice(endpointId, info.endpointName)
            _state.value = _state.value.copy(
                devices = (_state.value.devices.filterNot { it.endpointId == endpointId } + device)
                    .sortedBy { it.name.lowercase() },
            )
        }

        override fun onEndpointLost(endpointId: String) {
            _state.value = _state.value.copy(
                devices = _state.value.devices.filterNot { it.endpointId == endpointId },
            )
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            pendingEndpointId = endpointId
            _state.value = _state.value.copy(
                phase = NearbyTransferPhase.CONFIRMING,
                peerName = info.endpointName,
                authenticationDigits = info.authenticationDigits,
            )
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            pendingEndpointId = null
            if (resolution.status.statusCode != ConnectionsStatusCodes.STATUS_OK) {
                fail(NearbyTransferFailure.CONNECTION)
                return
            }
            connectedEndpointId = endpointId
            client.stopAdvertising()
            client.stopDiscovery()
            when (_state.value.mode) {
                NearbyTransferMode.SEND -> sendArchive(endpointId)
                NearbyTransferMode.RECEIVE -> _state.value = _state.value.copy(
                    phase = NearbyTransferPhase.WAITING,
                )
                null -> Unit
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpointId = null
            if (_state.value.phase !in setOf(
                    NearbyTransferPhase.COMPLETE,
                    NearbyTransferPhase.ERROR,
                    NearbyTransferPhase.IDLE,
                )
            ) {
                fail(NearbyTransferFailure.DISCONNECTED)
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.FILE -> receivedFiles[payload.id] = payload
                Payload.Type.BYTES -> handleControl(payload.asBytes())
                Payload.Type.STREAM -> fail(NearbyTransferFailure.TRANSFER)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val isRecipeFile = when (_state.value.mode) {
                NearbyTransferMode.SEND -> update.payloadId == outgoingFilePayloadId
                NearbyTransferMode.RECEIVE -> receivedFiles.containsKey(update.payloadId)
                null -> false
            }
            if (!isRecipeFile) return
            val progress = if (update.totalBytes > 0L) {
                (update.bytesTransferred.toDouble() / update.totalBytes.toDouble())
                    .toFloat()
                    .coerceIn(0f, 1f)
            } else {
                null
            }
            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> _state.value = _state.value.copy(
                    phase = NearbyTransferPhase.TRANSFERRING,
                    progress = progress,
                )
                PayloadTransferUpdate.Status.SUCCESS -> {
                    if (_state.value.mode == NearbyTransferMode.RECEIVE &&
                        receivedFiles.containsKey(update.payloadId)
                    ) {
                        completedIncomingPayloadId = update.payloadId
                        _state.value = _state.value.copy(
                            phase = NearbyTransferPhase.IMPORTING,
                            progress = 1f,
                        )
                    } else if (_state.value.mode == NearbyTransferMode.SEND &&
                        update.payloadId == outgoingFilePayloadId
                    ) {
                        _state.value = _state.value.copy(
                            phase = NearbyTransferPhase.WAITING,
                            progress = 1f,
                        )
                    }
                }
                PayloadTransferUpdate.Status.FAILURE,
                PayloadTransferUpdate.Status.CANCELED,
                -> fail(NearbyTransferFailure.TRANSFER)
            }
        }
    }

    private fun sendArchive(endpointId: String) {
        val archive = outgoingArchive ?: run {
            fail(NearbyTransferFailure.TRANSFER)
            return
        }
        val payload = runCatching { Payload.fromFile(archive.file) }.getOrElse {
            fail(NearbyTransferFailure.TRANSFER)
            return
        }
        _state.value = _state.value.copy(
            phase = NearbyTransferPhase.TRANSFERRING,
            progress = 0f,
        )
        outgoingFilePayloadId = payload.id
        client.sendPayload(endpointId, payload)
            .addOnFailureListener { fail(NearbyTransferFailure.TRANSFER) }
    }

    private fun handleControl(bytes: ByteArray?) {
        if (_state.value.mode != NearbyTransferMode.SEND || bytes == null) return
        when (bytes.toString(Charsets.UTF_8)) {
            ACK_SUCCESS -> {
                outgoingArchive?.file?.delete()
                outgoingArchive = null
                _state.value = _state.value.copy(phase = NearbyTransferPhase.COMPLETE)
                connectedEndpointId?.let(client::disconnectFromEndpoint)
            }
            ACK_FAILURE -> fail(NearbyTransferFailure.IMPORT)
        }
    }

    private fun sendControl(value: String) {
        val endpointId = connectedEndpointId ?: return
        client.sendPayload(endpointId, Payload.fromBytes(value.toByteArray()))
            .addOnFailureListener { fail(NearbyTransferFailure.TRANSFER) }
    }

    private fun restartCurrentMode() {
        when (_state.value.mode) {
            NearbyTransferMode.SEND -> outgoingArchive?.let(::startSending)
            NearbyTransferMode.RECEIVE -> startReceiving()
            null -> Unit
        }
    }

    private fun fail(failure: NearbyTransferFailure) {
        client.stopAdvertising()
        client.stopDiscovery()
        _state.value = _state.value.copy(
            phase = NearbyTransferPhase.ERROR,
            failure = failure,
        )
    }

    private fun stopTransport() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        pendingEndpointId = null
        connectedEndpointId = null
        receivedFiles.clear()
        completedIncomingPayloadId = null
        outgoingFilePayloadId = null
        importStarted = false
    }

    private fun localDeviceName(): String = Build.MODEL.take(MAX_DEVICE_NAME_LENGTH)
        .ifBlank { "Receptari" } // i18n-exempt: protocol endpoint fallback

    private companion object {
        const val SERVICE_ID = "cat.receptari.app.recipe-transfer.v1"
        const val ACK_SUCCESS = "receptari:imported:v1"
        const val ACK_FAILURE = "receptari:import-failed:v1"
        const val MAX_DEVICE_NAME_LENGTH = 32
        val STRATEGY: Strategy = Strategy.P2P_POINT_TO_POINT
    }
}
