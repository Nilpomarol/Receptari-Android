package cat.receptari.app.domain.importer

import cat.receptari.app.domain.ai.AiClient
import cat.receptari.app.domain.ai.DraftRecipe
import javax.inject.Inject

/**
 * Website import, in the order the PRD requires: JSON-LD / microdata first, the model only
 * as a fallback (PRD §7, AGENTS.md §7).
 *
 * Most recipe sites publish schema.org metadata, so the common path costs nothing, is
 * instant, and is exact. The model is what happens when a page is just prose.
 */
class ImportRecipeFromWebsite @Inject constructor(
    private val webPageSource: WebPageSource,
    private val aiClient: AiClient,
) {
    suspend operator fun invoke(url: String): Result<DraftRecipe> {
        val page = webPageSource.load(url).getOrElse { return Result.failure(it) }

        page.structured?.takeUnless { it.isEmpty }?.let { return Result.success(it) }

        if (page.text.isBlank()) return Result.failure(ImportError.NoRecipeFound)

        return aiClient.extractFromWebContent(page.url, page.text)
    }
}
