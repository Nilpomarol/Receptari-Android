package cat.receptari.app.domain.repository

import cat.receptari.app.domain.backup.BackupArchive
import cat.receptari.app.domain.backup.RestorePreview
import java.io.File

interface BackupRepository {
    suspend fun createBackup(): Result<BackupArchive>
    suspend fun stageRestore(archiveFile: File): Result<RestorePreview>
    suspend fun confirmRestore(): Result<Unit>
    suspend fun discardStagedRestore()
    suspend fun discardExport(archiveFile: File)
}
