package cat.receptari.app.data.repository

import android.content.Context
import cat.receptari.app.BuildConfig
import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.data.transfer.RecipeTransferArchiveCodec
import cat.receptari.app.data.transfer.RecipeTransferPackageDto
import cat.receptari.app.data.transfer.toImportedBundle
import cat.receptari.app.data.transfer.toTransferDto
import cat.receptari.app.domain.repository.ImageStore
import cat.receptari.app.domain.repository.RecipeRepository
import cat.receptari.app.domain.repository.RecipeTransferRepository
import cat.receptari.app.domain.repository.RecipeTranslationRepository
import cat.receptari.app.domain.transfer.RecipeTransferArchive
import cat.receptari.app.domain.transfer.RecipeTransferError
import cat.receptari.app.domain.transfer.RecipeTransferResult
import cat.receptari.app.domain.transfer.transferContentFingerprint
import cat.receptari.app.domain.translation.translationSourceFingerprint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class RecipeTransferRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val recipeRepository: RecipeRepository,
    private val translationRepository: RecipeTranslationRepository,
    private val imageStore: ImageStore,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecipeTransferRepository {

    override suspend fun createTransfer(recipeIds: Set<String>?): Result<RecipeTransferArchive> =
        withContext(ioDispatcher) {
            runCatching {
                val allRecipes = recipeRepository.getAllRecipes()
                val recipes = if (recipeIds == null) {
                    allRecipes
                } else {
                    allRecipes.filter { it.id in recipeIds }
                }
                if (recipes.isEmpty()) throw RecipeTransferError.EmptySelection()

                val root = File(context.cacheDir, "recipe-transfers/outgoing").apply { mkdirs() }
                // A target selected in the Sharesheet may read the URI asynchronously. A
                // new share must not delete a package that is still in flight.
                val archive = File(root, "${UUID.randomUUID()}.receptari-share")
                val images = mutableMapOf<String, File>()
                val recipeDtos = recipes.mapIndexed { index, recipe ->
                    val image = recipe.imagePath
                        ?.let(imageStore::absolutePathOf)
                        ?.let(::File)
                        ?.takeIf(File::isFile)
                    val imageEntry = image?.let { "${RecipeTransferArchiveCodec.IMAGE_PREFIX}$index.jpg" }
                    if (image != null && imageEntry != null) images[imageEntry] = image
                    val translations = translationRepository.getAll(
                        recipeId = recipe.id,
                        sourceFingerprint = recipe.translationSourceFingerprint(),
                    )
                    recipe.toTransferDto(imageEntry, translations)
                }
                RecipeTransferArchiveCodec.write(
                    output = archive,
                    transfer = RecipeTransferPackageDto(
                        formatVersion = RecipeTransferArchiveCodec.FORMAT_VERSION,
                        createdAtEpochMillis = clock.millis(),
                        appVersion = BuildConfig.VERSION_NAME,
                        recipes = recipeDtos,
                    ),
                    images = images,
                )
                val date = DateTimeFormatter.ISO_LOCAL_DATE
                    .withZone(ZoneOffset.UTC)
                    .format(clock.instant())
                RecipeTransferArchive(archive, "receptari-share-$date.receptari-share")
            }.recoverCatching { error ->
                throw when (error) {
                    is RecipeTransferError -> error
                    else -> RecipeTransferError.Storage(error)
                }
            }
        }

    override suspend fun importTransfer(packageFile: File): Result<RecipeTransferResult> =
        withContext(ioDispatcher) {
            val staging = File(context.cacheDir, "recipe-transfers/incoming")
            val importedIds = mutableListOf<String>()
            val createdImagePaths = mutableListOf<String>()
            runCatching {
                val transfer = RecipeTransferArchiveCodec.extractAndValidate(packageFile, staging)
                val fingerprints = recipeRepository.getAllRecipes()
                    .mapTo(mutableSetOf()) { it.transferContentFingerprint() }
                var skipped = 0
                var translationCount = 0
                var imageCount = 0
                transfer.recipes.forEach { transferred ->
                    val bundle = transferred.toImportedBundle(clock.instant())
                    val fingerprint = bundle.recipe.transferContentFingerprint()
                    if (!fingerprints.add(fingerprint)) {
                        skipped += 1
                        return@forEach
                    }
                    val imagePath = transferred.imageEntry?.let { entry ->
                        File(staging, entry).readBytes().let { bytes ->
                            imageStore.save(bundle.recipe.id, bytes)
                        }
                    }
                    imagePath?.let(createdImagePaths::add)
                    val recipe = bundle.recipe.copy(imagePath = imagePath)
                    recipeRepository.save(recipe)
                    importedIds += recipe.id
                    bundle.translations.forEach { translationRepository.save(it) }
                    translationCount += bundle.translations.size
                    if (imagePath != null) imageCount += 1
                }
                RecipeTransferResult(
                    importedRecipeCount = importedIds.size,
                    skippedDuplicateCount = skipped,
                    importedTranslationCount = translationCount,
                    importedImageCount = imageCount,
                )
            }.recoverCatching { error ->
                importedIds.asReversed().forEach { id -> runCatching { recipeRepository.delete(id) } }
                createdImagePaths.forEach { path -> runCatching { imageStore.delete(path) } }
                throw when (error) {
                    is RecipeTransferError -> error
                    else -> RecipeTransferError.Storage(error)
                }
            }.also {
                staging.deleteRecursively()
            }
        }
}
