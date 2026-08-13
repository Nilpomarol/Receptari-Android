package cat.receptari.app.data.transfer

import cat.receptari.app.domain.transfer.RecipeTransferError
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecipeTransferArchiveCodecTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `archive round trip preserves recipes translations and image`() {
        val image = temporaryFolder.newFile("photo.jpg").apply { writeText("image bytes") }
        val archive = File(temporaryFolder.root, "recipes.receptari-share")
        val transfer = packageDto(formatVersion = 1, imageEntry = "images/0.jpg")

        RecipeTransferArchiveCodec.write(
            output = archive,
            transfer = transfer,
            images = mapOf("images/0.jpg" to image),
        )

        val destination = temporaryFolder.newFolder("extracted")
        val decoded = RecipeTransferArchiveCodec.extractAndValidate(archive, destination)

        assertEquals(transfer, decoded)
        assertEquals("image bytes", File(destination, "images/0.jpg").readText())
    }

    @Test
    fun `archive from a newer transfer format is rejected`() {
        val archive = File(temporaryFolder.root, "newer.receptari-share")
        RecipeTransferArchiveCodec.write(
            output = archive,
            transfer = packageDto(formatVersion = 2),
            images = emptyMap(),
        )

        val result = runCatching {
            RecipeTransferArchiveCodec.extractAndValidate(
                archive,
                temporaryFolder.newFolder("newer"),
            )
        }

        assertTrue(result.exceptionOrNull() is RecipeTransferError.NewerVersion)
    }

    @Test
    fun `path traversal entry is rejected without writing outside staging`() {
        val archive = File(temporaryFolder.root, "unsafe.receptari-share")
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("../outside.jpg"))
            zip.write("unsafe".toByteArray())
            zip.closeEntry()
        }
        val destination = temporaryFolder.newFolder("unsafe")

        val result = runCatching {
            RecipeTransferArchiveCodec.extractAndValidate(archive, destination)
        }

        assertTrue(result.exceptionOrNull() is RecipeTransferError.InvalidPackage)
        assertFalse(File(temporaryFolder.root, "outside.jpg").exists())
        assertFalse(destination.exists())
    }

    private fun packageDto(
        formatVersion: Int,
        imageEntry: String? = null,
    ): RecipeTransferPackageDto = RecipeTransferPackageDto(
        formatVersion = formatVersion,
        createdAtEpochMillis = 1L,
        appVersion = "test",
        recipes = listOf(
            TransferRecipeDto(
                id = "recipe",
                title = "Pa amb tomàquet",
                originalTitle = null,
                imageEntry = imageEntry,
                prepTimeMinutes = 5,
                cookTimeMinutes = null,
                totalTimeMinutes = null,
                baseServings = 2,
                sourceName = null,
                sourceUrl = null,
                originalLanguage = "ca",
                displayLanguage = null,
                ingredientSections = listOf(
                    TransferIngredientSectionDto(
                        id = "ingredients",
                        name = null,
                        originalName = null,
                        ingredients = listOf(
                            TransferIngredientDto(
                                id = "bread",
                                quantity = 2.0,
                                quantityMax = null,
                                unit = "llesques",
                                name = "pa",
                                note = null,
                                originalText = "2 llesques de pa",
                                displayText = null,
                            ),
                        ),
                    ),
                ),
                instructionSections = listOf(
                    TransferInstructionSectionDto(
                        id = "instructions",
                        name = null,
                        originalName = null,
                        steps = listOf(TransferStepDto("toast", "Torra el pa.", null)),
                    ),
                ),
                tags = listOf("Ràpid"),
                translations = listOf(
                    TransferTranslationDto(
                        language = "en",
                        sourceLanguage = "ca",
                        title = "Tomato bread",
                        ingredientSections = listOf(TransferFieldDto("ingredients", null)),
                        ingredients = listOf(TransferFieldDto("bread", "2 slices of bread")),
                        instructionSections = listOf(TransferFieldDto("instructions", null)),
                        steps = listOf(TransferFieldDto("toast", "Toast the bread.")),
                    ),
                ),
            ),
        ),
    )
}
