package cat.receptari.app.data.backup

import cat.receptari.app.domain.backup.BackupError
import cat.receptari.app.domain.backup.RestorePreview
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal data class BackupContents(
    val schemaVersion: Int,
    val recipeCount: Int,
    val images: List<File>,
)

@Serializable
private data class BackupManifest(
    @SerialName("format_version") val formatVersion: Int,
    @SerialName("database_schema_version") val databaseSchemaVersion: Int,
    @SerialName("created_at_epoch_millis") val createdAtEpochMillis: Long,
    @SerialName("app_version") val appVersion: String,
    @SerialName("recipe_count") val recipeCount: Int,
    @SerialName("image_count") val imageCount: Int,
)

internal object BackupArchiveCodec {
    const val FORMAT_VERSION = 1
    const val DATABASE_ENTRY = "database/receptari.db"
    const val MANIFEST_ENTRY = "manifest.json"
    private const val IMAGE_PREFIX = "images/"
    private const val MAX_ENTRIES = 20_000
    private const val MAX_ENTRY_BYTES = 512L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 2L * 1024 * 1024 * 1024
    private val json = Json { ignoreUnknownKeys = true }

    fun write(
        output: File,
        database: File,
        contents: BackupContents,
        createdAt: Instant,
        appVersion: String,
    ) {
        val manifest = BackupManifest(
            formatVersion = FORMAT_VERSION,
            databaseSchemaVersion = contents.schemaVersion,
            createdAtEpochMillis = createdAt.toEpochMilli(),
            appVersion = appVersion,
            recipeCount = contents.recipeCount,
            imageCount = contents.images.size,
        )
        ZipOutputStream(FileOutputStream(output)).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(json.encodeToString(BackupManifest.serializer(), manifest).toByteArray())
            zip.closeEntry()
            zip.addFile(DATABASE_ENTRY, database)
            contents.images.forEach { image -> zip.addFile("$IMAGE_PREFIX${image.name}", image) }
        }
    }

    fun extractAndValidate(
        archive: File,
        destination: File,
        currentSchemaVersion: Int,
        inspectDatabase: (File) -> BackupContents,
    ): RestorePreview {
        destination.deleteRecursively()
        destination.mkdirs()
        val seen = mutableSetOf<String>()
        var totalBytes = 0L
        var manifest: BackupManifest? = null

        try {
            ZipFile(archive).use { zip ->
                val entries = zip.entries().asSequence().toList()
                if (entries.size > MAX_ENTRIES) throw BackupError.InvalidArchive()
                entries.forEach { entry ->
                    val name = entry.name
                    if (!seen.add(name) || !isAllowedEntry(name, entry.isDirectory)) {
                        throw BackupError.InvalidArchive()
                    }
                    if (entry.size > MAX_ENTRY_BYTES) throw BackupError.InvalidArchive()
                    if (entry.isDirectory) return@forEach
                    val target = File(destination, name)
                    ensureInside(destination, target)
                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        target.outputStream().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var entryBytes = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                entryBytes += read
                                totalBytes += read
                                if (entryBytes > MAX_ENTRY_BYTES || totalBytes > MAX_TOTAL_BYTES) {
                                    throw BackupError.InvalidArchive()
                                }
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                    if (name == MANIFEST_ENTRY) {
                        manifest = runCatching {
                            json.decodeFromString(BackupManifest.serializer(), target.readText())
                        }.getOrElse { throw BackupError.InvalidArchive(it) }
                    }
                }
            }

            val parsed = manifest ?: throw BackupError.InvalidArchive()
            if (parsed.formatVersion != FORMAT_VERSION) throw BackupError.InvalidArchive()
            if (parsed.databaseSchemaVersion > currentSchemaVersion) throw BackupError.NewerVersion()
            if (parsed.databaseSchemaVersion < 1 || parsed.recipeCount < 0 || parsed.imageCount < 0) {
                throw BackupError.InvalidArchive()
            }
            val database = File(destination, DATABASE_ENTRY)
            if (!database.isFile) throw BackupError.InvalidArchive()
            File(destination, IMAGE_PREFIX).mkdirs()
            val actual = inspectDatabase(database)
            val imageCount = File(destination, IMAGE_PREFIX).listFiles()?.count { it.isFile } ?: 0
            if (
                actual.schemaVersion != parsed.databaseSchemaVersion ||
                actual.recipeCount != parsed.recipeCount ||
                imageCount != parsed.imageCount
            ) {
                throw BackupError.InvalidArchive()
            }
            val archivedImageNames = File(destination, IMAGE_PREFIX)
                .listFiles().orEmpty().filter { it.isFile }.map { it.name }.toSet()
            if (!actual.images.all { it.name in archivedImageNames }) {
                throw BackupError.InvalidArchive()
            }
            return RestorePreview(
                createdAt = Instant.ofEpochMilli(parsed.createdAtEpochMillis),
                recipeCount = parsed.recipeCount,
                imageCount = parsed.imageCount,
            )
        } catch (error: BackupError) {
            destination.deleteRecursively()
            throw error
        } catch (error: Exception) {
            destination.deleteRecursively()
            throw BackupError.InvalidArchive(error)
        }
    }

    private fun ZipOutputStream.addFile(entryName: String, file: File) {
        putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(this) }
        closeEntry()
    }

    private fun isAllowedEntry(name: String, directory: Boolean): Boolean {
        if (name.startsWith('/') || name.contains('\\') || name.split('/').any { it == ".." }) {
            return false
        }
        if (directory) return name == "database/" || name == IMAGE_PREFIX
        if (name == MANIFEST_ENTRY || name == DATABASE_ENTRY) return true
        return name.startsWith(IMAGE_PREFIX) &&
            name.removePrefix(IMAGE_PREFIX).isNotBlank() &&
            !name.removePrefix(IMAGE_PREFIX).contains('/')
    }

    private fun ensureInside(root: File, target: File) {
        val rootPath = root.canonicalFile.toPath()
        if (!target.canonicalFile.toPath().startsWith(rootPath)) throw BackupError.InvalidArchive()
    }
}
