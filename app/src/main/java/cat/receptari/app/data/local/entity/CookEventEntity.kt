package cat.receptari.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per occasion the recipe was cooked. There is deliberately no `timesCooked`
 * counter on `recipes` — both the count and the last-cooked date are derived from here
 * (PRD §4).
 */
@Entity(
    tableName = "cook_events",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId"), Index("cookedAt")],
)
data class CookEventEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val cookedAt: Long,
    val note: String?,
)
