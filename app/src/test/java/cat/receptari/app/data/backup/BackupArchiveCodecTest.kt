package cat.receptari.app.data.backup

import cat.receptari.app.domain.backup.BackupError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupArchiveCodecTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `archive round trip preserves database images and preview metadata`() {
        val source = temporaryFolder.newFolder("source")
        val database = File(source, "receptari.db").apply { writeText("database bytes") }
        val image = File(source, "photo.jpg").apply { writeText("image bytes") }
        val archive = File(temporaryFolder.root, "library.receptari")
        val createdAt = Instant.parse("2026-08-12T09:30:00Z")

        BackupArchiveCodec.write(
            output = archive,
            database = database,
            contents = BackupContents(schemaVersion = 5, recipeCount = 12, images = listOf(image)),
            createdAt = createdAt,
            appVersion = "0.1.0",
        )

        val destination = temporaryFolder.newFolder("restored")
        val preview = BackupArchiveCodec.extractAndValidate(
            archive = archive,
            destination = destination,
            currentSchemaVersion = 5,
            inspectDatabase = { extractedDatabase ->
                assertEquals("database bytes", extractedDatabase.readText())
                val extractedImage = File(destination, "images/photo.jpg")
                assertEquals("image bytes", extractedImage.readText())
                BackupContents(5, 12, listOf(extractedImage))
            },
        )

        assertEquals(createdAt, preview.createdAt)
        assertEquals(12, preview.recipeCount)
        assertEquals(1, preview.imageCount)
    }

    @Test
    fun `backup from a newer schema is rejected and staging is removed`() {
        val archive = validArchive(schemaVersion = 6)
        val destination = temporaryFolder.newFolder("newer")

        val result = runCatching {
            BackupArchiveCodec.extractAndValidate(archive, destination, 5) {
                BackupContents(6, 1, emptyList())
            }
        }

        assertTrue(result.exceptionOrNull() is BackupError.NewerVersion)
        assertFalse(destination.exists())
    }

    @Test
    fun `unknown archive entries are rejected before extraction`() {
        val archive = File(temporaryFolder.root, "unsafe.receptari")
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("../outside.txt"))
            zip.write("unsafe".toByteArray())
            zip.closeEntry()
        }
        val destination = temporaryFolder.newFolder("unsafe-stage")

        val result = runCatching {
            BackupArchiveCodec.extractAndValidate(archive, destination, 5) {
                error("Database inspector must not run")
            }
        }

        assertTrue(result.exceptionOrNull() is BackupError.InvalidArchive)
        assertFalse(File(temporaryFolder.root, "outside.txt").exists())
        assertFalse(destination.exists())
    }

    private fun validArchive(schemaVersion: Int): File {
        val source = temporaryFolder.newFolder("source-$schemaVersion")
        val database = File(source, "receptari.db").apply { writeText("db") }
        return File(temporaryFolder.root, "schema-$schemaVersion.receptari").also { archive ->
            BackupArchiveCodec.write(
                output = archive,
                database = database,
                contents = BackupContents(schemaVersion, 1, emptyList()),
                createdAt = Instant.EPOCH,
                appVersion = "test",
            )
        }
    }
}
