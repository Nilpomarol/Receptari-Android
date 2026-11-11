package cat.receptari.app.core.util

import javax.inject.Qualifier

/**
 * Marks the OpenAI key, used only for recipe translation (ADR-014). An unqualified
 * `ApiKeyRepository` is the Claude key.
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class TranslationKey
