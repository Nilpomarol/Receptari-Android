package cat.receptari.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "recipe_translations",
    primaryKeys = ["recipeId", "language"],
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RecipeTranslationEntity(
    val recipeId: String,
    val language: String,
    val payloadJson: String,
    val updatedAt: Long,
)
