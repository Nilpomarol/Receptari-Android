package cat.receptari.app.data.local.mapper

import cat.receptari.app.data.local.entity.CookEventEntity
import cat.receptari.app.data.local.entity.FolderEntity
import cat.receptari.app.data.local.entity.FolderWithCount
import cat.receptari.app.data.local.entity.IngredientEntity
import cat.receptari.app.data.local.entity.IngredientSectionEntity
import cat.receptari.app.data.local.entity.InstructionSectionEntity
import cat.receptari.app.data.local.entity.RecipeAggregate
import cat.receptari.app.data.local.entity.RecipeEntity
import cat.receptari.app.data.local.entity.RecipeFtsEntity
import cat.receptari.app.data.local.entity.RecipeSummaryProjection
import cat.receptari.app.data.local.entity.RecipeTagCrossRef
import cat.receptari.app.data.local.entity.StepEntity
import cat.receptari.app.data.local.entity.TagEntity
import cat.receptari.app.domain.model.CookEvent
import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.FolderSummary
import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.model.IngredientSection
import cat.receptari.app.domain.model.InstructionSection
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.RecipeSummary
import cat.receptari.app.domain.model.Step
import cat.receptari.app.domain.model.Tag
import java.time.Instant

/**
 * Everything the write path needs for one recipe, already decomposed into rows.
 *
 * The `position` columns exist only here: the domain expresses ordering as list order, and
 * this is where that is translated in both directions.
 */
data class RecipeWriteModel(
    val recipe: RecipeEntity,
    val ingredientSections: List<IngredientSectionEntity>,
    val ingredients: List<IngredientEntity>,
    val instructionSections: List<InstructionSectionEntity>,
    val steps: List<StepEntity>,
    val tagLinks: List<RecipeTagCrossRef>,
    val searchIndex: RecipeFtsEntity,
)

// --- read: entities -> domain -----------------------------------------------------

fun RecipeAggregate.toDomain(): Recipe = Recipe(
    id = recipe.id,
    title = recipe.title,
    originalTitle = recipe.originalTitle,
    imagePath = recipe.imagePath,
    prepTimeMinutes = recipe.prepTimeMinutes,
    cookTimeMinutes = recipe.cookTimeMinutes,
    totalTimeMinutes = recipe.totalTimeMinutes,
    baseServings = recipe.baseServings,
    isFavorite = recipe.isFavorite,
    rating = recipe.rating,
    notes = recipe.notes,
    sourceName = recipe.sourceName,
    sourceUrl = recipe.sourceUrl,
    originalLanguage = recipe.originalLanguage,
    createdAt = Instant.ofEpochMilli(recipe.createdAt),
    updatedAt = Instant.ofEpochMilli(recipe.updatedAt),
    // Room makes no ordering promise for @Relation collections, so sort explicitly.
    ingredientSections = ingredientSections
        .sortedBy { it.section.position }
        .map { sectionWithIngredients ->
            IngredientSection(
                id = sectionWithIngredients.section.id,
                name = sectionWithIngredients.section.name,
                ingredients = sectionWithIngredients.ingredients
                    .sortedBy { it.position }
                    .map { it.toDomain() },
            )
        },
    instructionSections = instructionSections
        .sortedBy { it.section.position }
        .map { sectionWithSteps ->
            InstructionSection(
                id = sectionWithSteps.section.id,
                name = sectionWithSteps.section.name,
                steps = sectionWithSteps.steps
                    .sortedBy { it.position }
                    .map { it.toDomain() },
            )
        },
    tags = tags.map { it.toDomain() }.sortedBy { it.normalizedName },
    folder = folder?.toDomain(),
    cookCount = cookEvents.size,
    lastCookedAt = cookEvents.maxOfOrNull { it.cookedAt }?.let(Instant::ofEpochMilli),
)

fun IngredientEntity.toDomain(): Ingredient = Ingredient(
    id = id,
    quantity = quantity,
    quantityMax = quantityMax,
    unit = unit,
    name = name,
    note = note,
    originalText = originalText,
)

fun StepEntity.toDomain(): Step = Step(id = id, text = text, originalText = originalText)

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name, normalizedName = normalizedName)

fun FolderEntity.toDomain(): Folder = Folder(
    id = id,
    name = name,
    normalizedName = normalizedName,
    color = color.toFolderColor(),
    icon = icon.toFolderIcon(),
)

fun FolderWithCount.toDomain(): FolderSummary = FolderSummary(
    folder = Folder(
        id = id,
        name = name,
        normalizedName = normalizedName,
        color = color.toFolderColor(),
        icon = icon.toFolderIcon(),
    ),
    recipeCount = recipeCount,
)

/** Falls back to the default rather than crashing on a value an older/newer build wrote. */
private fun String.toFolderColor(): FolderColor =
    FolderColor.entries.find { it.name == this } ?: FolderColor.OLIVE

private fun String.toFolderIcon(): FolderIcon =
    FolderIcon.entries.find { it.name == this } ?: FolderIcon.FOLDER

fun CookEventEntity.toDomain(): CookEvent = CookEvent(
    id = id,
    recipeId = recipeId,
    cookedAt = Instant.ofEpochMilli(cookedAt),
    note = note,
)

fun RecipeSummaryProjection.toDomain(): RecipeSummary = RecipeSummary(
    id = id,
    title = title,
    imagePath = imagePath,
    prepTimeMinutes = prepTimeMinutes,
    cookTimeMinutes = cookTimeMinutes,
    totalTimeMinutes = totalTimeMinutes,
    baseServings = baseServings,
    isFavorite = isFavorite,
    rating = rating,
    cookCount = cookCount,
    lastCookedAt = lastCookedAt?.let(Instant::ofEpochMilli),
)

// --- write: domain -> entities ----------------------------------------------------

/**
 * @param resolvedTags the tags as they exist in the database. Tag identity is resolved by
 * the repository before this point, so a tag the user re-typed does not become a duplicate
 * row with a fresh id.
 */
fun Recipe.toWriteModel(resolvedTags: List<Tag>): RecipeWriteModel {
    val recipeEntity = RecipeEntity(
        id = id,
        title = title,
        originalTitle = originalTitle,
        imagePath = imagePath,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
        totalTimeMinutes = totalTimeMinutes,
        baseServings = baseServings,
        isFavorite = isFavorite,
        rating = rating,
        notes = notes,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        originalLanguage = originalLanguage,
        folderId = folder?.id,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

    val ingredientSectionEntities = ingredientSections.mapIndexed { index, section ->
        IngredientSectionEntity(
            id = section.id,
            recipeId = id,
            name = section.name,
            position = index,
        )
    }

    val ingredientEntities = ingredientSections.flatMap { section ->
        section.ingredients.mapIndexed { index, ingredient ->
            IngredientEntity(
                id = ingredient.id,
                sectionId = section.id,
                position = index,
                quantity = ingredient.quantity,
                quantityMax = ingredient.quantityMax,
                unit = ingredient.unit,
                name = ingredient.name,
                note = ingredient.note,
                originalText = ingredient.originalText,
            )
        }
    }

    val instructionSectionEntities = instructionSections.mapIndexed { index, section ->
        InstructionSectionEntity(
            id = section.id,
            recipeId = id,
            name = section.name,
            position = index,
        )
    }

    val stepEntities = instructionSections.flatMap { section ->
        section.steps.mapIndexed { index, step ->
            StepEntity(
                id = step.id,
                sectionId = section.id,
                position = index,
                text = step.text,
                originalText = step.originalText,
            )
        }
    }

    return RecipeWriteModel(
        recipe = recipeEntity,
        ingredientSections = ingredientSectionEntities,
        ingredients = ingredientEntities,
        instructionSections = instructionSectionEntities,
        steps = stepEntities,
        tagLinks = resolvedTags.map { RecipeTagCrossRef(recipeId = id, tagId = it.id) },
        searchIndex = buildSearchIndex(resolvedTags),
    )
}

/**
 * Ingredients contribute their parsed name when there is one and their raw text otherwise,
 * so "Sal al gust" is still findable even though nothing was structured out of it.
 */
private fun Recipe.buildSearchIndex(resolvedTags: List<Tag>) = RecipeFtsEntity(
    recipeId = id,
    title = title,
    notes = notes.orEmpty(),
    ingredientNames = allIngredients
        .joinToString(" ") { it.name ?: it.originalText },
    tagNames = resolvedTags.joinToString(" ") { it.name },
)
