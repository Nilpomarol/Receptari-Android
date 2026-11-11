package cat.receptari.app.domain.ai

/**
 * Translates the structured, user-reviewable fields of a recipe (PRD §11).
 *
 * Separate from [AiClient] because translation runs on a different provider than import
 * (ADR-014): import is transcription, where a small Claude model is enough, and translation
 * is fluent Catalan, where it was not. Nothing outside `data/remote/` knows which vendor
 * answers either interface.
 */
interface RecipeTranslator {

    /** Verifies the configured translation key with a request that generates no tokens. */
    suspend fun testKey(): Result<Unit>

    suspend fun translate(request: TranslationRequest): Result<TranslationResult>
}
