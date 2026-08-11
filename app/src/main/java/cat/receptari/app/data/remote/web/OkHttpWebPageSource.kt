package cat.receptari.app.data.remote.web

import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.domain.importer.ImportError
import cat.receptari.app.domain.importer.WebPage
import cat.receptari.app.domain.importer.WebPageSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpWebPageSource @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WebPageSource {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun load(url: String): Result<WebPage> = withContext(ioDispatcher) {
        val normalized = normalize(url) ?: return@withContext Result.failure(ImportError.InvalidUrl)

        try {
            val request = Request.Builder()
                .url(normalized)
                // Some recipe sites serve a stripped page to unknown clients, which loses
                // exactly the JSON-LD block worth having.
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "ca,es;q=0.9,en;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(ImportError.PageUnreachable)
                }
                val html = response.body?.string()
                    ?: return@withContext Result.failure(ImportError.PageUnreachable)

                val document = Jsoup.parse(html, normalized)
                // Scripts and styles are text too, and would otherwise dominate the payload
                // sent to the model.
                document.select("script, style, noscript, nav, header, footer").remove()

                Result.success(
                    WebPage(
                        url = normalized,
                        structured = SchemaOrgRecipeParser.parse(Jsoup.parse(html, normalized)),
                        text = document.body()?.wholeText().orEmpty().collapseBlankLines(),
                    ),
                )
            }
        } catch (e: UnknownHostException) {
            Result.failure(ImportError.Offline)
        } catch (e: IOException) {
            Result.failure(ImportError.PageUnreachable)
        } catch (e: IllegalArgumentException) {
            Result.failure(ImportError.InvalidUrl)
        }
    }

    /** Accepts what a person would paste: bare hosts get `https://`, and only http(s) is allowed. */
    private fun normalize(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.contains("://") -> return null
            else -> "https://$trimmed"
        }
        return withScheme.takeIf { it.contains('.') }
    }

    private fun String.collapseBlankLines(): String =
        lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
            .take(MAX_TEXT_CHARS)

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

        /** Enough for any recipe page; caps what a hostile or enormous page can cost. */
        const val MAX_TEXT_CHARS = 60_000
    }
}
