package cat.receptari.app.data.remote.openai

import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.core.util.TranslationKey
import cat.receptari.app.domain.ai.AiError
import cat.receptari.app.domain.ai.RecipeTranslator
import cat.receptari.app.domain.ai.TranslationRequest
import cat.receptari.app.domain.ai.TranslationResult
import cat.receptari.app.domain.repository.ApiKeyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Recipe translation through OpenAI's Responses API (ADR-014).
 *
 * Everything vendor-specific stops here, as with `ClaudeAiClient`: the model ids, the wire
 * format, and the key. The key is read per call, sent only to `api.openai.com` in the
 * `Authorization` header, and never logged — there is no logging interceptor on [client].
 *
 * Plain OkHttp rather than an SDK: this is one POST and one GET, and OkHttp and
 * kotlinx.serialization are already in the app (ADR-014 explains why ADR-007's reasoning
 * does not carry over).
 */
@Singleton
class OpenAiTranslator @Inject constructor(
    @param:TranslationKey private val keyRepository: ApiKeyRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecipeTranslator {

    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    /** Listing models is authenticated but generates no tokens, so testing a key is free. */
    override suspend fun testKey(): Result<Unit> = call { key ->
        send(authorized(key).url("$BASE_URL/models").get().build(), key)
        Unit
    }

    override suspend fun translate(request: TranslationRequest): Result<TranslationResult> =
        call { key ->
            try {
                translate(request, MODEL, key)
            } catch (_: TranslationContractException) {
                // The larger model gets one chance only when the first produced valid JSON
                // with an unsafe set of ids or blank fields — never for an API failure.
                try {
                    translate(request, FALLBACK_MODEL, key)
                } catch (_: TranslationContractException) {
                    throw AiError.UnreadableResponse
                }
            }
        }

    private suspend fun translate(
        request: TranslationRequest,
        model: String,
        key: String,
    ): TranslationResult {
        val body = buildJsonObject {
            put("model", model)
            put("max_output_tokens", MAX_OUTPUT_TOKENS)
            // A constrained field-by-field translation gains nothing from reasoning; it only
            // adds latency to a request the user is waiting on.
            putJsonObject("reasoning") { put("effort", "none") }
            putJsonArray("input") {
                addJsonObject {
                    put("role", "system")
                    put("content", TranslationSchema.SYSTEM_PROMPT)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", json.encodeToString(request.toDto()))
                }
            }
            putJsonObject("text") {
                putJsonObject("format") {
                    put("type", "json_schema")
                    put("name", "recipe_translation")
                    put("strict", true)
                    put("schema", TranslationSchema.MAP.toJsonElement())
                }
            }
        }
        val response = send(
            authorized(key)
                .url("$BASE_URL/responses")
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build(),
            key,
        )
        return response.toTranslationResult(request)
    }

    private fun String.toTranslationResult(request: TranslationRequest): TranslationResult {
        val root = parseObject(this) ?: throw TranslationContractException()
        if (root.string("status") != "completed") {
            val reason = (root["incomplete_details"] as? JsonObject)?.string("reason")
            if (reason == "content_filter") throw AiError.Refused
            // Out of output tokens: the JSON is truncated and there is nothing to salvage.
            throw TranslationContractException()
        }
        val parts = (root["output"] as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }
            .filter { it.string("type") == "message" }
            .flatMap { (it["content"] as? JsonArray).orEmpty() }
            .mapNotNull { it as? JsonObject }
        if (parts.any { it.string("type") == "refusal" }) throw AiError.Refused
        val text = parts.firstOrNull { it.string("type") == "output_text" }?.string("text")
            ?: throw TranslationContractException()
        val dto = try {
            json.decodeFromString<TranslationResultDto>(text)
        } catch (_: Exception) {
            throw TranslationContractException()
        }
        return dto.toResult(request)
    }

    /** Resolves the key, runs [block] off the main thread, and maps failures to [AiError]. */
    private suspend fun <T> call(block: suspend (String) -> T): Result<T> =
        withContext(ioDispatcher) {
            val key = keyRepository.getKey()
                ?: return@withContext Result.failure(AiError.NoTranslationKey)
            try {
                Result.success(block(key))
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiError) {
                Result.failure(e)
            } catch (e: Throwable) {
                Result.failure(AiError.Unexpected(redact(e.message, key)))
            }
        }

    /**
     * Sends [request] and returns the body of a 2xx response. A dropped connection or a 5xx
     * is retried once (AI_INTEGRATION.md §8); a 4xx never is.
     */
    private suspend fun send(request: Request, key: String): String {
        repeat(MAX_ATTEMPTS) { attempt ->
            val last = attempt == MAX_ATTEMPTS - 1
            val (code, body) = try {
                client.newCall(request).await().use { it.code to it.body?.string().orEmpty() }
            } catch (e: IOException) {
                if (last) throw AiError.Offline else return@repeat
            }
            when {
                code in 200..299 -> return body
                code >= 500 && !last -> return@repeat
                else -> throw httpError(code, body, key)
            }
        }
        throw AiError.Offline // unreachable: the last attempt always returns or throws
    }

    private fun httpError(code: Int, body: String, key: String): AiError = when {
        code == 401 || code == 403 -> AiError.InvalidApiKey
        // OpenAI reports an exhausted balance as a 429 too; only the error code tells them
        // apart, and the user needs to top up rather than wait.
        code == 429 && errorCode(body) == "insufficient_quota" -> AiError.QuotaExceeded
        code == 429 -> AiError.RateLimited
        code >= 500 -> AiError.ServiceUnavailable
        else -> AiError.Unexpected(redact("HTTP $code ${errorMessage(body).orEmpty()}", key))
    }

    private fun errorCode(body: String): String? =
        (parseObject(body)?.get("error") as? JsonObject)?.string("code")

    private fun errorMessage(body: String): String? =
        (parseObject(body)?.get("error") as? JsonObject)?.string("message")

    private fun parseObject(text: String): JsonObject? =
        try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (_: Exception) {
            null
        }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.contentOrNull

    private fun authorized(key: String): Request.Builder =
        Request.Builder().header("Authorization", "Bearer $key")

    /** Belt and braces: the one path where an arbitrary string can reach a screen. */
    private fun redact(message: String?, key: String): String {
        val text = message ?: "Unknown error" // i18n-exempt — diagnostic, not UI copy
        return if (key.isNotEmpty()) text.replace(key, "***") else text
    }

    private companion object {
        const val BASE_URL = "https://api.openai.com/v1"

        /** ADR-014. Chosen over Claude after a side-by-side Catalan comparison. */
        const val MODEL = "gpt-6-luna"

        /** One retry when the first model's answer breaks the id/blank-field contract. */
        const val FALLBACK_MODEL = "gpt-6.1-terra"

        const val MAX_OUTPUT_TOKENS = 16_000
        const val MAX_ATTEMPTS = 2

        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

/** Suspends on an OkHttp call, cancelling the HTTP request if the coroutine is cancelled. */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(
        object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response) else response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        },
    )
}

/** The schema is a Kotlin map (see [TranslationSchema]); the request body needs JSON. */
private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(entries.associate { (k, v) -> k.toString() to v.toJsonElement() })
    is List<*> -> JsonArray(map { it.toJsonElement() })
    else -> error("Unsupported schema value") // i18n-exempt — programming error, not UI copy
}
