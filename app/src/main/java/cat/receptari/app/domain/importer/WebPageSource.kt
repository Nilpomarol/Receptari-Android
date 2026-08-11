package cat.receptari.app.domain.importer

import cat.receptari.app.domain.ai.DraftRecipe

/**
 * Fetches a web page and reports what it found.
 *
 * The split matters: a page that publishes schema.org Recipe metadata has already answered
 * the question, and calling the model on it would cost money to produce a worse answer
 * (AGENTS.md §7). So the source returns the structured recipe when there is one, and the
 * readable text when there is not.
 */
interface WebPageSource {
    suspend fun load(url: String): Result<WebPage>
}

data class WebPage(
    val url: String,
    /** Parsed from JSON-LD or microdata, or null when the page carries neither. */
    val structured: DraftRecipe?,
    /** The page's visible text, markup stripped, for the AI fallback. */
    val text: String,
)

/** Import failures that are not the model's doing. */
sealed class ImportError(message: String) : Exception(message) {
    data object Offline : ImportError("No network connection")
    data object PageUnreachable : ImportError("The page could not be loaded")
    data object InvalidUrl : ImportError("That is not a valid web address")
    data object NoRecipeFound : ImportError("No recipe was found")
}
