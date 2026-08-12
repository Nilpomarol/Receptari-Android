package cat.receptari.app.domain.translation

import cat.receptari.app.domain.ai.AiClient
import cat.receptari.app.domain.ai.RecipeLanguage
import cat.receptari.app.domain.ai.TranslationField
import cat.receptari.app.domain.ai.TranslationRequest
import cat.receptari.app.domain.ai.TranslationResult
import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.model.IngredientSection
import cat.receptari.app.domain.model.InstructionSection
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.Step
import cat.receptari.app.domain.parser.IngredientParser
import cat.receptari.app.domain.repository.RecipeTranslationRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class TranslateRecipe @Inject constructor(
    private val aiClient: AiClient,
    private val translationRepository: RecipeTranslationRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(recipe: Recipe, target: RecipeLanguage): Result<Recipe> {
        val now = Instant.now(clock)
        if (recipe.originalLanguage.matches(target)) {
            return runCatching { recipe.restoreOriginal(now) }
        }

        translationRepository.get(recipe.id, target.languageTag)?.let { cached ->
            val currentSource = recipe.translationSourceFingerprint()
            if (cached.sourceFingerprint.isEmpty() || cached.sourceFingerprint == currentSource) {
                runCatching { recipe.apply(cached, now) }.getOrNull()?.let { translated ->
                    return Result.success(translated)
                }
            }
        }

        val prepared = PreparedTranslation.from(recipe, target)
        return aiClient.translate(prepared.request).mapCatching { result ->
            prepared.merge(result, now)
        }
    }
}

private data class PreparedIngredient(
    val id: String,
    val renderer: IngredientTranslationRenderer.PreparedIngredient,
)

private data class PreparedTranslation(
    val recipe: Recipe,
    val target: RecipeLanguage,
    val ingredients: Map<String, PreparedIngredient>,
    val request: TranslationRequest,
) {
    fun merge(result: TranslationResult, now: Instant): Recipe {
        val ingredientSections = result.ingredientSections.requireExactIds(
            request.ingredientSections,
            "ingredient sections",
        )
        val translatedIngredients = result.ingredients.requireExactIds(
            request.ingredients,
            "ingredients",
        )
        val instructionSections = result.instructionSections.requireExactIds(
            request.instructionSections,
            "instruction sections",
        )
        val steps = result.steps.requireExactIds(request.steps, "steps")
        require(result.title.isNotBlank()) { "Translation omitted the title" }

        return recipe.copy(
            title = result.title.trim(),
            originalTitle = recipe.sourceTitle(),
            originalLanguage = recipe.originalLanguage
                ?: recipe.displayLanguage
                ?: result.sourceLanguage,
            displayLanguage = target.languageTag,
            updatedAt = now,
            ingredientSections = recipe.ingredientSections.map { section ->
                val originalName = section.sourceName(recipe)
                section.copy(
                    name = originalName?.let { ingredientSections.getValue(section.id) },
                    originalName = originalName,
                    ingredients = section.ingredients.map { ingredient ->
                        val prepared = ingredients.getValue(ingredient.id)
                        ingredient.copy(
                            displayText = prepared.renderer.render(
                                translatedText = translatedIngredients[ingredient.id].orEmpty(),
                                target = target,
                            ),
                        )
                    },
                )
            },
            instructionSections = recipe.instructionSections.map { section ->
                val originalName = section.sourceName(recipe)
                section.copy(
                    name = originalName?.let { instructionSections.getValue(section.id) },
                    originalName = originalName,
                    steps = section.steps.map { step ->
                        val originalText = step.sourceText(recipe)
                        step.copy(
                            text = steps.getValue(step.id),
                            originalText = originalText,
                        )
                    },
                )
            },
            // Tags are a library-wide taxonomy, not recipe prose. Translating one recipe's
            // tag creates duplicate global tags, so the v1 translation overlay leaves them.
            tags = recipe.tags,
        )
    }

    companion object {
        fun from(recipe: Recipe, target: RecipeLanguage): PreparedTranslation {
            val preparedIngredients = recipe.allIngredients.associate { ingredient ->
                ingredient.id to PreparedIngredient(
                    id = ingredient.id,
                    renderer = IngredientTranslationRenderer.prepare(ingredient),
                )
            }

            return PreparedTranslation(
                recipe = recipe,
                target = target,
                ingredients = preparedIngredients,
                request = TranslationRequest(
                    targetLanguage = target,
                    title = recipe.sourceTitle(),
                    ingredientSections = recipe.ingredientSections.mapNotNull { section ->
                        section.sourceName(recipe)?.let {
                            TranslationField(section.id, it)
                        }
                    },
                    ingredients = preparedIngredients.values
                        .filter { it.renderer.sourceText.isNotBlank() }
                        .map { TranslationField(it.id, it.renderer.sourceText) },
                    instructionSections = recipe.instructionSections.mapNotNull { section ->
                        section.sourceName(recipe)?.let {
                            TranslationField(section.id, it)
                        }
                    },
                    steps = recipe.instructionSections.flatMap { section ->
                        section.steps.map { step ->
                            TranslationField(step.id, step.sourceText(recipe))
                        }
                    },
                ),
            )
        }
    }
}

private fun Recipe.apply(variant: RecipeTranslationVariant, now: Instant): Recipe {
    require(variant.recipeId == id) { "Cached translation belongs to another recipe" }
    val ingredientSections = variant.ingredientSections.requireExactNullableIds(
        this.ingredientSections.map { it.id },
        "ingredient sections",
    )
    val ingredients = variant.ingredients.requireExactNullableIds(
        allIngredients.map { it.id },
        "ingredients",
    )
    val instructionSections = variant.instructionSections.requireExactNullableIds(
        this.instructionSections.map { it.id },
        "instruction sections",
    )
    val steps = variant.steps.requireExactNullableIds(
        this.instructionSections.flatMap { section -> section.steps.map { it.id } },
        "steps",
    )
    require(variant.title.isNotBlank()) { "Cached translation omitted the title" }
    require(ingredients.values.all { !it.isNullOrBlank() }) { "Cached ingredient is blank" }
    require(steps.values.all { !it.isNullOrBlank() }) { "Cached step is blank" }

    return copy(
        title = variant.title,
        originalTitle = originalTitle ?: title,
        originalLanguage = originalLanguage ?: variant.sourceLanguage,
        displayLanguage = variant.language,
        updatedAt = now,
        ingredientSections = this.ingredientSections.map { section ->
            section.copy(
                name = ingredientSections.getValue(section.id),
                originalName = section.originalName ?: section.name,
                ingredients = section.ingredients.map { ingredient ->
                    ingredient.copy(displayText = ingredients.getValue(ingredient.id))
                },
            )
        },
        instructionSections = this.instructionSections.map { section ->
            section.copy(
                name = instructionSections.getValue(section.id),
                originalName = section.originalName ?: section.name,
                steps = section.steps.map { step ->
                    step.copy(
                        text = checkNotNull(steps.getValue(step.id)),
                        originalText = step.originalText ?: step.text,
                    )
                },
            )
        },
    )
}

private fun Recipe.restoreOriginal(now: Instant): Recipe = copy(
    title = originalTitle ?: title,
    displayLanguage = null,
    updatedAt = now,
    ingredientSections = ingredientSections.map { section ->
        section.copy(
            name = section.originalName ?: section.name,
            ingredients = section.ingredients.map(Ingredient::restoreOriginal),
        )
    },
    instructionSections = instructionSections.map { section ->
        section.copy(
            name = section.originalName ?: section.name,
            steps = section.steps.map { step -> step.copy(text = step.originalText ?: step.text) },
        )
    },
)

private fun Ingredient.restoreOriginal(): Ingredient {
    val parsed = IngredientParser.parse(originalText) ?: return copy(displayText = null)
    return copy(
        quantity = parsed.quantity,
        quantityMax = parsed.quantityMax,
        unit = parsed.unit,
        name = parsed.name,
        note = parsed.note,
        displayText = null,
    )
}

private fun String?.matches(target: RecipeLanguage): Boolean =
    this?.startsWith(target.languageTag, ignoreCase = true) == true

private fun Recipe.sourceTitle(): String =
    if (displayLanguage == null) title else originalTitle ?: title

private fun IngredientSection.sourceName(recipe: Recipe): String? =
    if (recipe.displayLanguage == null) name else originalName ?: name

private fun InstructionSection.sourceName(recipe: Recipe): String? =
    if (recipe.displayLanguage == null) name else originalName ?: name

private fun Step.sourceText(recipe: Recipe): String =
    if (recipe.displayLanguage == null) text else originalText ?: text

private fun List<TranslationField>.requireExactIds(
    requested: List<TranslationField>,
    label: String,
): Map<String, String> {
    val expected = requested.map { it.id }.toSet()
    val actual = map { it.id }.toSet()
    require(size == actual.size && actual == expected) { "Translation changed $label" }
    require(all { it.text.isNotBlank() }) { "Translation left blank $label" }
    return associate { it.id to it.text.trim() }
}

private fun List<CachedTranslationField>.requireExactNullableIds(
    requestedIds: List<String>,
    label: String,
): Map<String, String?> {
    val expected = requestedIds.toSet()
    val actual = map { it.id }.toSet()
    require(size == actual.size && actual == expected) { "Cached translation changed $label" }
    return associate { it.id to it.text?.trim()?.takeIf(String::isNotEmpty) }
}
