package cat.receptari.app.data.repository

import android.content.Context
import cat.receptari.app.BuildConfig
import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.data.backup.BackupArchiveCodec
import cat.receptari.app.data.backup.BackupContents
import cat.receptari.app.data.backup.BackupDatabaseInspector
import cat.receptari.app.data.backup.BackupPaths
import cat.receptari.app.data.local.ReceptariDatabase
import cat.receptari.app.domain.backup.BackupArchive
import cat.receptari.app.domain.backup.BackupError
import cat.receptari.app.domain.backup.RestorePreview
import cat.receptari.app.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Clock
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: ReceptariDatabase,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BackupRepository {

    override suspend fun createBackup(): Result<BackupArchive> = withContext(ioDispatcher) {
        runCatching {
            val exports = BackupPaths.exports(context).apply { mkdirs() }
            exports.listFiles()?.forEach(File::delete)
            val snapshot = File(exports, "database.snapshot")
            val output = File(exports, "receptari-export.receptari")
            snapshot.delete()
            output.delete()
            val escapedPath = snapshot.absolutePath.replace("'", "''")
            database.openHelper.writableDatabase.execSQL("VACUUM INTO '$escapedPath'")
            val contents = BackupDatabaseInspector.inspect(snapshot, File(context.filesDir, "images"))
            BackupArchiveCodec.write(
                output = output,
                database = snapshot,
                contents = contents,
                createdAt = clock.instant(),
                appVersion = BuildConfig.VERSION_NAME,
            )
            snapshot.delete()
            val date = DateTimeFormatter.ISO_LOCAL_DATE
                .withZone(ZoneOffset.UTC)
                .format(clock.instant())
            BackupArchive(output, "receptari-$date.receptari")
        }.recoverCatching { error ->
            throw if (error is BackupError) error else BackupError.Storage(error)
        }
    }

    override suspend fun stageRestore(archiveFile: File): Result<RestorePreview> =
        withContext(ioDispatcher) {
            runCatching {
                BackupPaths.readyMarker(context).delete()
                BackupArchiveCodec.extractAndValidate(
                    archive = archiveFile,
                    destination = BackupPaths.staged(context),
                    currentSchemaVersion = ReceptariDatabase.SCHEMA_VERSION,
                    inspectDatabase = { file ->
                        BackupDatabaseInspector.inspect(file, File(BackupPaths.staged(context), "images"))
                    },
                )
            }.recoverCatching { error ->
                throw if (error is BackupError) error else BackupError.Storage(error)
            }.also { archiveFile.delete() }
        }

    override suspend fun confirmRestore(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val staged = BackupPaths.staged(context)
            if (!File(staged, BackupArchiveCodec.DATABASE_ENTRY).isFile) {
                throw BackupError.InvalidArchive()
            }
            val root = BackupPaths.root(context).apply { mkdirs() }
            File(root, "restore.ready.tmp").apply {
                writeText("ready")
                if (!renameTo(BackupPaths.readyMarker(context))) throw BackupError.Storage()
            }
            Unit
        }
    }

    override suspend fun discardStagedRestore(): Unit = withContext(ioDispatcher) {
        BackupPaths.root(context).deleteRecursively()
    }

    override suspend fun discardExport(archiveFile: File): Unit = withContext(ioDispatcher) {
        archiveFile.parentFile?.deleteRecursively()
    }
}
