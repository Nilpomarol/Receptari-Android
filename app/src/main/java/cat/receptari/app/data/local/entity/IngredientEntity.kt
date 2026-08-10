package cat.receptari.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredient_sections",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class IngredientSectionEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val name: String?,
    val position: Int,
)

/**
 * [originalText] is NOT NULL by design. Every other structured column may be null at the
 * same time and the row is still valid — "Sal al gust" is a real ingredient (PRD §3.2).
 */
@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = IngredientSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sectionId")],
)
data class IngredientEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val position: Int,
    val quantity: Double?,
    val quantityMax: Double?,
    val unit: String?,
    val name: String?,
    val note: String?,
    val originalText: String,
)
