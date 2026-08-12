package cat.receptari.app.data.remote.claude

import android.util.Base64
import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.domain.ai.AiClient
import cat.receptari.app.domain.ai.AiError
import cat.receptari.app.domain.ai.DraftRecipe
import cat.receptari.app.domain.ai.TranslationRequest
import cat.receptari.app.domain.ai.TranslationResult
import cat.receptari.app.domain.repository.ApiKeyRepository
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.TextBlockParam
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Claude implementation of [AiClient] (ADR-002, ADR-007).
 *
 * Everything vendor-specific stops here: the model id, the wire format, and the SDK types.
 * Callers see [DraftRecipe] and [AiError] only.
 *
 * The SDK is blocking, so every call is wrapped in [withContext] on the IO dispatcher.
 */
@Singleton
class ClaudeAiClient @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AiClient {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Built lazily and reused so requests share a connection pool, but re-created when the
     * key changes — the user can replace it in Settings at any time.
     */
    private var cached: Pair<String, AnthropicClient>? = null

    @Synchronized
    private fun clientFor(apiKey: String): AnthropicClient {
        cached?.let { (key, client) -> if (key == apiKey) return client }
        val client = AnthropicOkHttpClient.builder().apiKey(apiKey).build()
        cached = apiKey to client
        return client
    }

    override suspend fun testKey(): Result<Unit> = call { client ->
        // Deliberately not a structured-output request: this only needs to prove the key is
        // accepted, and one token is the cheapest way to ask.
        client.messages().create(
            MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(1)
                .addUserMessage("Hi") // i18n-exempt — model prompt, never shown to the user
                .build(),
        )
        Unit
    }

    override suspend fun extractFromText(text: String): Result<DraftRecipe> = call { client ->
        client.messages()
            .create(extractionParams().addUserMessage(text).build())
            .toDraft()
    }

    override suspend fun extractFromWebContent(url: String, content: String): Result<DraftRecipe> =
        call { client ->
            client.messages()
                .create(extractionParams().addUserMessage("$url\n\n$content").build())
                .toDraft(sourceUrl = url)
        }

    override suspend fun extractFromImages(images: List<ByteArray>): Result<DraftRecipe> =
        call { client ->
            // Images first, instruction last: a trailing instruction is the documented
            // ordering for multi-image prompts, and a recipe spanning several photographed
            // pages has to arrive as one message to be read as one recipe (PRD §9).
            val blocks = images.map { bytes ->
                ContentBlockParam.ofImage(
                    ImageBlockParam.builder()
                        .source(
                            Base64ImageSource.builder()
                                .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                                .data(
                                    Base64.encodeToString(
                                        // A 12 MP phone photo is ~4 MB and gets resized
                                        // server-side anyway, so shrinking here costs
                                        // nothing in quality and saves the user's data.
                                        JpegDownscaler.downscale(bytes),
                                        Base64.NO_WRAP,
                                    ),
                                )
                                .build(),
                        )
                        .build(),
                )
            } + ContentBlockParam.ofText(
                TextBlockParam.builder().text(IMAGE_INSTRUCTION).build(),
            )

            client.messages()
                .create(extractionParams().addUserMessageOfBlockParams(blocks).build())
                .toDraft()
        }

    override suspend fun translate(request: TranslationRequest): Result<TranslationResult> =
        call { client ->
            try {
                client.translate(request, TRANSLATION_MODEL)
            } catch (_: TranslationContractException) {
                // Haiku is the cost-conscious default. Sonnet gets one chance only when
                // Haiku produced valid JSON with an unsafe set of ids or blank fields.
                try {
                    client.translate(request, MODEL)
                } catch (_: TranslationContractException) {
                    throw AiError.UnreadableResponse
                }
            }
        }

    private fun AnthropicClient.translate(
        request: TranslationRequest,
        model: String,
    ): TranslationResult {
        val message = messages().create(
            MessageCreateParams.builder()
                .model(model)
                .maxTokens(TRANSLATION_MAX_TOKENS)
                .system(TranslationSchema.SYSTEM_PROMPT)
                .outputConfig(
                    OutputConfig.builder()
                        .format(
                            JsonOutputFormat.builder()
                                .schema(
                                    JsonOutputFormat.Schema.builder()
                                        .additionalProperties(
                                            TranslationSchema.MAP.mapValues { (_, value) ->
                                                JsonValue.from(value)
                                            },
                                        )
                                        .build(),
                                )
                                .build(),
                        )
                        .build(),
                )
                .addUserMessage(json.encodeToString(request.toDto()))
                .build(),
        )
        return message.toTranslationResult(request)
    }

    private fun Message.toTranslationResult(request: TranslationRequest): TranslationResult {
        when (stopReason().orElse(null)) {
            StopReason.REFUSAL -> throw AiError.Refused
            StopReason.MAX_TOKENS -> throw TranslationContractException()
            else -> Unit
        }
        val body = content().firstOrNull { it.isText() }?.asText()?.text()
            ?: throw TranslationContractException()
        val dto = try {
            json.decodeFromString<TranslationResultDto>(body)
        } catch (_: Exception) {
            throw TranslationContractException()
        }
        return dto.toResult(request)
    }

    private fun extractionParams(): MessageCreateParams.Builder =
        MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(MAX_TOKENS)
            .system(RecipeSchema.SYSTEM_PROMPT)
            // No extended thinking: this is transcription against a fixed schema, not
            // reasoning. Thinking would add latency and cost to a request the user is
            // waiting on, and pays for itself nowhere in a copy-it-verbatim task.
            .outputConfig(
                OutputConfig.builder()
                    .format(
                        JsonOutputFormat.builder()
                            .schema(
                                JsonOutputFormat.Schema.builder()
                                    .additionalProperties(
                                        RecipeSchema.MAP.mapValues { (_, value) -> JsonValue.from(value) },
                                    )
                                    .build(),
                            )
                            .build(),
                    )
                    .build(),
            )

    private fun Message.toDraft(sourceUrl: String? = null): DraftRecipe {
        when (stopReason().orElse(null)) {
            // A refusal is not a bug and not a network problem — say so plainly rather than
            // showing the user an empty editor.
            StopReason.REFUSAL -> throw AiError.Refused
            // The JSON is truncated mid-token, so there is nothing to salvage.
            StopReason.MAX_TOKENS -> throw AiError.UnreadableResponse
            else -> Unit
        }

        val body = content().firstOrNull { it.isText() }?.asText()?.text()
            ?: throw AiError.UnreadableResponse

        val dto = try {
            json.decodeFromString<ExtractionDto>(body)
        } catch (e: Exception) {
            // Structured output should make this unreachable; treat it as unreadable rather
            // than crashing, and never echo the body, which could be anything.
            throw AiError.UnreadableResponse
        }
        return dto.toDraft(sourceUrl)
    }

    /**
     * Resolves the key, runs [block] off the main thread, and turns every failure into a
     * typed [AiError].
     */
    private suspend fun <T> call(block: (AnthropicClient) -> T): Result<T> =
        withContext(ioDispatcher) {
            val apiKey = apiKeyRepository.getKey()
                ?: return@withContext Result.failure(AiError.NoApiKey)
            try {
                Result.success(block(clientFor(apiKey)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Result.failure(e.toAiError(apiKey))
            }
        }

    private fun Throwable.toAiError(apiKey: String): AiError = when (this) {
        is AiError -> this
        is UnauthorizedException, is PermissionDeniedException -> AiError.InvalidApiKey
        is RateLimitException -> AiError.RateLimited
        // Credit exhaustion arrives as a 400, not a 402 or 429, and is only distinguishable
        // by the message. A user paying for their own key needs to be told to top it up, not
        // to "try again later".
        is BadRequestException ->
            if (message?.contains(CREDIT_MARKER, ignoreCase = true) == true) {
                AiError.QuotaExceeded
            } else {
                AiError.Unexpected(redact(message, apiKey))
            }
        is AnthropicIoException, is IOException -> AiError.Offline
        is AnthropicServiceException -> AiError.Unexpected(redact(message, apiKey))
        else -> AiError.Unexpected(redact(message, apiKey))
    }

    /**
     * Belt and braces. SDK exceptions carry the response body, not the request headers, so
     * the key should never appear here — but [AiError.Unexpected] is the one path where an
     * arbitrary string reaches a screen, and the key must never be one of them (AGENTS.md §7).
     */
    private fun redact(message: String?, apiKey: String): String {
        val text = message ?: "Unknown error" // i18n-exempt — diagnostic, not UI copy
        return if (apiKey.isNotEmpty()) text.replace(apiKey, "***") else text
    }

    private companion object {
        /** ADR-002. Changing this is a decision, not a tweak. */
        const val MODEL = "claude-sonnet-5"

        /** Translation is constrained and reviewable, so the cheaper model is sufficient. */
        const val TRANSLATION_MODEL = "claude-haiku-4-5"

        /** Comfortably above the longest realistic recipe; truncation is unrecoverable. */
        const val MAX_TOKENS = 8_192L
        const val TRANSLATION_MAX_TOKENS = 8_192L

        const val CREDIT_MARKER = "credit balance"

        const val IMAGE_INSTRUCTION =
            "These images are pages of a single recipe, in order. Extract it."
    }
}
