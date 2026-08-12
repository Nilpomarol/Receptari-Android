package cat.receptari.app.domain.backup

import java.io.File
import java.time.Instant

data class BackupArchive(
    val file: File,
    val suggestedFileName: String,
)

data class RestorePreview(
    val createdAt: Instant,
    val recipeCount: Int,
    val imageCount: Int,
)

sealed class BackupError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidArchive(cause: Throwable? = null) :
        BackupError("The selected file is not a valid Receptari backup", cause)

    class NewerVersion : BackupError("The backup was created by a newer Receptari version")

    class Storage(cause: Throwable? = null) :
        BackupError("The backup could not be read or written", cause)
}
