package cat.receptari.app.data.transfer

import cat.receptari.app.domain.transfer.RecipeTransferError
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json

internal object RecipeTransferArchiveCodec {
    const val FORMAT_VERSION = 1
    const val MANIFEST_ENTRY = "manifest.json"
    const val IMAGE_PREFIX = "images/"
    private const val MAX_RECIPES = 1_000
    private const val MAX_ENTRIES = MAX_RECIPES + 1
    private const val MAX_MANIFEST_BYTES = 32L * 1024 * 1024
    private const val MAX_IMAGE_BYTES = 32L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 2L * 1024 * 1024 * 1024
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun write(
        output: File,
        transfer: RecipeTransferPackageDto,
        images: Map<String, File>,
    ) {
        ZipOutputStream(output.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(json.encodeToString(RecipeTransferPackageDto.serializer(), transfer).toByteArray())
            zip.closeEntry()
            images.forEach { (entryName, image) ->
                zip.putNextEntry(ZipEntry(entryName))
                image.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    fun extractAndValidate(archive: File, destination: File): RecipeTransferPackageDto {
        destination.deleteRecursively()
        destination.mkdirs()
        var manifest: RecipeTransferPackageDto? = null
        var totalBytes = 0L
        val seen = mutableSetOf<String>()
        try {
            ZipFile(archive).use { zip ->
                val entries = zip.entries().asSequence().toList()
                if (entries.size > MAX_ENTRIES) throw RecipeTransferError.InvalidPackage()
                entries.forEach { entry ->
                    val name = entry.name
                    if (entry.isDirectory || !seen.add(name) || !isAllowedEntry(name)) {
                        throw RecipeTransferError.InvalidPackage()
                    }
                    val maxBytes = if (name == MANIFEST_ENTRY) MAX_MANIFEST_BYTES else MAX_IMAGE_BYTES
                    if (entry.size > maxBytes) throw RecipeTransferError.InvalidPackage()
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
                                if (entryBytes > maxBytes || totalBytes > MAX_TOTAL_BYTES) {
                                    throw RecipeTransferError.InvalidPackage()
                                }
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                    if (name == MANIFEST_ENTRY) {
                        manifest = runCatching {
                            json.decodeFromString(
                                RecipeTransferPackageDto.serializer(),
                                target.readText(),
                            )
                        }.getOrElse { throw RecipeTransferError.InvalidPackage(it) }
                    }
                }
            }
            val parsed = manifest ?: throw RecipeTransferError.InvalidPackage()
            if (parsed.formatVersion > FORMAT_VERSION) throw RecipeTransferError.NewerVersion()
            if (parsed.formatVersion < 1 || parsed.recipes.isEmpty() || parsed.recipes.size > MAX_RECIPES) {
                throw RecipeTransferError.InvalidPackage()
            }
            if (parsed.recipes.map { it.id }.toSet().size != parsed.recipes.size) {
                throw RecipeTransferError.InvalidPackage()
            }
            if (parsed.recipes.any { !it.isValid() }) throw RecipeTransferError.InvalidPackage()
            val declaredImages = parsed.recipes.mapNotNull { it.imageEntry }.toSet()
            val actualImages = seen.filterTo(mutableSetOf()) { it.startsWith(IMAGE_PREFIX) }
            if (
                declaredImages.size != parsed.recipes.count { it.imageEntry != null } ||
                declaredImages != actualImages ||
                declaredImages.any { !File(destination, it).isFile }
            ) throw RecipeTransferError.InvalidPackage()
            return parsed
        } catch (error: RecipeTransferError) {
            destination.deleteRecursively()
            throw error
        } catch (error: Exception) {
            destination.deleteRecursively()
            throw RecipeTransferError.InvalidPackage(error)
        }
    }

    private fun isAllowedEntry(name: String): Boolean {
        if (name.startsWith('/') || name.contains('\\') || name.split('/').any { it == ".." }) {
            return false
        }
        if (name == MANIFEST_ENTRY) return true
        return name.startsWith(IMAGE_PREFIX) &&
            name.removePrefix(IMAGE_PREFIX).matches(Regex("[a-zA-Z0-9_-]+\\.jpg"))
    }

    private fun ensureInside(root: File, target: File) {
        if (!target.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())) {
            throw RecipeTransferError.InvalidPackage()
        }
    }
}
