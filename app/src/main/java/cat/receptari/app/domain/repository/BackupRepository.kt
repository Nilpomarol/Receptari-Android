package cat.receptari.app.domain.repository

import cat.receptari.app.domain.backup.BackupArchive
import cat.receptari.app.domain.backup.RestorePreview
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.Instant

interface BackupRepository {
    /** Emits when the last export was saved successfully, or `null` if none ever was. */
    fun observeLastBackupAt(): Flow<Instant?>
    suspend fun recordBackupCompleted()
    suspend fun createBackup(): Result<BackupArchive>
    suspend fun stageRestore(archiveFile: File): Result<RestorePreview>
    suspend fun confirmRestore(): Result<Unit>
    suspend fun discardStagedRestore()
    suspend fun discardExport(archiveFile: File)
}
