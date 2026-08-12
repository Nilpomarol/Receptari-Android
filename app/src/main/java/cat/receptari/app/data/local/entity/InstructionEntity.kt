package cat.receptari.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "instruction_sections",
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
data class InstructionSectionEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val name: String?,
    val originalName: String?,
    val position: Int,
)

@Entity(
    tableName = "steps",
    foreignKeys = [
        ForeignKey(
            entity = InstructionSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sectionId")],
)
data class StepEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val position: Int,
    val text: String,
    val originalText: String?,
)
