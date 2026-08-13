package cat.receptari.app.domain.transfer

enum class NearbyTransferMode { SEND, RECEIVE }

enum class NearbyTransferPhase {
    IDLE,
    SEARCHING,
    ADVERTISING,
    CONNECTING,
    CONFIRMING,
    WAITING,
    TRANSFERRING,
    IMPORTING,
    COMPLETE,
    ERROR,
}

enum class NearbyTransferFailure {
    START,
    CONNECTION,
    TRANSFER,
    IMPORT,
    DISCONNECTED,
}

data class NearbyDevice(
    val endpointId: String,
    val name: String,
)

data class NearbyTransferState(
    val mode: NearbyTransferMode? = null,
    val phase: NearbyTransferPhase = NearbyTransferPhase.IDLE,
    val devices: List<NearbyDevice> = emptyList(),
    val peerName: String? = null,
    val authenticationDigits: String? = null,
    val progress: Float? = null,
    val result: RecipeTransferResult? = null,
    val failure: NearbyTransferFailure? = null,
)
