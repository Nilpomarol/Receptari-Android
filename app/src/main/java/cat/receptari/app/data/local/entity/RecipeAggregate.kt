package cat.receptari.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class IngredientSectionWithIngredients(
    @Embedded val section: IngredientSectionEntity,
    @Relation(parentColumn = "id", entityColumn = "sectionId")
    val ingredients: List<IngredientEntity>,
)

data class InstructionSectionWithSteps(
    @Embedded val section: InstructionSectionEntity,
    @Relation(parentColumn = "id", entityColumn = "sectionId")
    val steps: List<StepEntity>,
)

/**
 * The whole object graph for one recipe, read in a single `@Transaction`.
 *
 * Room does not promise any ordering for `@Relation` collections, so the mapper sorts every
 * list by its `position` column before handing it to the domain.
 */
data class RecipeAggregate(
    @Embedded val recipe: RecipeEntity,

    @Relation(
        entity = IngredientSectionEntity::class,
        parentColumn = "id",
        entityColumn = "recipeId",
    )
    val ingredientSections: List<IngredientSectionWithIngredients>,

    @Relation(
        entity = InstructionSectionEntity::class,
        parentColumn = "id",
        entityColumn = "recipeId",
    )
    val instructionSections: List<InstructionSectionWithSteps>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RecipeTagCrossRef::class,
            parentColumn = "recipeId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,

    @Relation(parentColumn = "folderId", entityColumn = "id")
    val folder: FolderEntity?,

    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val cookEvents: List<CookEventEntity>,
)

/** The projection the library list reads — never the full aggregate. */
data class RecipeSummaryProjection(
    val id: String,
    val title: String,
    val imagePath: String?,
    val prepTimeMinutes: Int?,
    val cookTimeMinutes: Int?,
    val totalTimeMinutes: Int?,
    val baseServings: Int?,
    val isFavorite: Boolean,
    val rating: Int?,
    val cookCount: Int,
    val lastCookedAt: Long?,
)
