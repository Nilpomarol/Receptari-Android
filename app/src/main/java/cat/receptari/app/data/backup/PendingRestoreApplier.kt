package cat.receptari.app.data.backup

import android.content.Context
import cat.receptari.app.data.local.ReceptariDatabase
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

enum class RestoreApplyResult { NONE, SUCCESS, FAILURE }

class PendingRestoreApplier(private val context: Context) {
    fun applyIfPending(): RestoreApplyResult {
        val root = BackupPaths.root(context)
        val staged = BackupPaths.staged(context)
        val marker = BackupPaths.readyMarker(context)
        if (!marker.isFile) return RestoreApplyResult.NONE
        val stagedDatabase = File(staged, BackupArchiveCodec.DATABASE_ENTRY)
        val stagedImages = File(staged, "images")
        if (!stagedDatabase.isFile || !stagedImages.isDirectory) {
            root.deleteRecursively()
            return RestoreApplyResult.FAILURE
        }

        val rollback = File(root, "rollback").apply { deleteRecursively(); mkdirs() }
        val currentDatabase = context.getDatabasePath(ReceptariDatabase.NAME)
        val currentImages = File(context.filesDir, "images")
        val sidecars = listOf(File("${currentDatabase.path}-wal"), File("${currentDatabase.path}-shm"))
        return try {
            currentDatabase.parentFile?.mkdirs()
            moveIfPresent(currentDatabase, File(rollback, ReceptariDatabase.NAME))
            sidecars.forEach { moveIfPresent(it, File(rollback, it.name)) }
            moveIfPresent(currentImages, File(rollback, "images"))
            move(stagedDatabase, currentDatabase)
            move(stagedImages, currentImages)
            root.deleteRecursively()
            RestoreApplyResult.SUCCESS
        } catch (_: Exception) {
            // Never retry a half-consumed staging directory. If rollback itself fails,
            // retain the rollback files for recovery instead of deleting the last copy.
            marker.delete()
            val rolledBack = runCatching {
                currentDatabase.delete()
                sidecars.forEach(File::delete)
                currentImages.deleteRecursively()
                moveIfPresent(File(rollback, ReceptariDatabase.NAME), currentDatabase)
                sidecars.forEach { moveIfPresent(File(rollback, it.name), it) }
                moveIfPresent(File(rollback, "images"), currentImages)
            }.isSuccess
            if (rolledBack) root.deleteRecursively()
            RestoreApplyResult.FAILURE
        }
    }

    private fun moveIfPresent(source: File, destination: File) {
        if (source.exists()) move(source, destination)
    }

    private fun move(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

object BackupPaths {
    fun root(context: Context): File = File(context.noBackupFilesDir, "backup_restore")
    fun staged(context: Context): File = File(root(context), "staged")
    fun readyMarker(context: Context): File = File(root(context), "restore.ready")
    fun exports(context: Context): File = File(context.cacheDir, "backup_exports")
}
