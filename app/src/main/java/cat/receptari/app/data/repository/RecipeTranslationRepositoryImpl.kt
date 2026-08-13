package cat.receptari.app.data.repository

import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.data.local.dao.RecipeDao
import cat.receptari.app.data.translation.toDomain
import cat.receptari.app.data.translation.toEntity
import cat.receptari.app.domain.repository.RecipeTranslationRepository
import cat.receptari.app.domain.translation.RecipeTranslationVariant
import javax.inject.Inject
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class RecipeTranslationRepositoryImpl @Inject constructor(
    private val recipeDao: RecipeDao,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecipeTranslationRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun get(recipeId: String, language: String): RecipeTranslationVariant? =
        withContext(ioDispatcher) {
            recipeDao.getTranslation(recipeId, language)?.toDomain(json)
        }

    override suspend fun getAll(
        recipeId: String,
        sourceFingerprint: String,
    ): List<RecipeTranslationVariant> = withContext(ioDispatcher) {
        recipeDao.getTranslations(recipeId)
            .mapNotNull { entity -> entity.toDomain(json) }
            .filter { variant ->
                variant.sourceFingerprint.isEmpty() ||
                    variant.sourceFingerprint == sourceFingerprint
            }
    }

    override suspend fun save(variant: RecipeTranslationVariant): Unit =
        withContext(ioDispatcher) {
            recipeDao.upsertTranslation(variant.toEntity(json, clock.millis()))
        }

    override suspend fun getAvailableLanguages(
        recipeId: String,
        sourceFingerprint: String,
    ): Set<String> = withContext(ioDispatcher) {
        recipeDao.getTranslations(recipeId)
            .mapNotNull { entity -> entity.toDomain(json) }
            .filter { variant ->
                variant.sourceFingerprint.isEmpty() ||
                    variant.sourceFingerprint == sourceFingerprint
            }
            .mapTo(mutableSetOf()) { variant -> variant.language }
    }
}
