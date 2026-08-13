package cat.receptari.app.domain.repository

import cat.receptari.app.domain.translation.RecipeTranslationVariant

interface RecipeTranslationRepository {
    suspend fun get(recipeId: String, language: String): RecipeTranslationVariant?

    suspend fun getAll(
        recipeId: String,
        sourceFingerprint: String,
    ): List<RecipeTranslationVariant>

    suspend fun save(variant: RecipeTranslationVariant)

    suspend fun getAvailableLanguages(
        recipeId: String,
        sourceFingerprint: String,
    ): Set<String>
}
