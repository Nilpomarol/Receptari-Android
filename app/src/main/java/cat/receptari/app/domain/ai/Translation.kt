package cat.receptari.app.domain.ai

enum class RecipeLanguage(val languageTag: String) {
    CATALAN("ca"),
    SPANISH("es"),
    ENGLISH("en"),
}

data class TranslationField(val id: String, val text: String)

data class TranslationRequest(
    val targetLanguage: RecipeLanguage,
    val title: String,
    val ingredientSections: List<TranslationField>,
    val ingredients: List<TranslationField>,
    val instructionSections: List<TranslationField>,
    val steps: List<TranslationField>,
)

data class TranslationResult(
    val title: String,
    val sourceLanguage: String?,
    val ingredientSections: List<TranslationField>,
    val ingredients: List<TranslationField>,
    val instructionSections: List<TranslationField>,
    val steps: List<TranslationField>,
)
