package cat.receptari.app.domain.ai

/**
 * The only AI abstraction the app knows.
 *
 * Nothing outside `data/remote/claude/` knows the vendor, the model id, or the wire format
 * (AGENTS.md §7). That boundary is what makes ADR-002's device-direct decision reversible:
 * moving to a backend proxy, or off Claude entirely, is a change behind this interface.
 */
interface AiClient {

    /** Verifies the configured key with the cheapest possible request. */
    suspend fun testKey(): Result<Unit>

    suspend fun extractFromText(text: String): Result<DraftRecipe>

    /**
     * @param images JPEG bytes, in reading order — a recipe spanning several cookbook pages
     * is one recipe and must be sent as one request (PRD §9).
     */
    suspend fun extractFromImages(images: List<ByteArray>): Result<DraftRecipe>

    /** @param content page text already stripped of markup by the caller. */
    suspend fun extractFromWebContent(url: String, content: String): Result<DraftRecipe>

    /** Translates only the structured, user-reviewable fields in [request] (PRD §11). */
    suspend fun translate(request: TranslationRequest): Result<TranslationResult>
}

/**
 * Failures worth telling the user apart. Everything else is [Unexpected].
 *
 * These exist because "it didn't work" is useless when the user is paying for their own
 * key: a missing key, a rejected key, and an exhausted quota need three different actions
 * (PRD §8, AI_INTEGRATION.md §8).
 */
sealed class AiError(message: String) : Exception(message) {
    data object NoApiKey : AiError("No API key configured")
    data object InvalidApiKey : AiError("The API key was rejected")
    data object RateLimited : AiError("Rate limited")
    data object QuotaExceeded : AiError("Credit or quota exhausted")
    data object Offline : AiError("No network connection")
    data object Refused : AiError("The model declined to answer")
    data object UnreadableResponse : AiError("The model's answer could not be read")
    data class Unexpected(val detail: String) : AiError(detail)
}
