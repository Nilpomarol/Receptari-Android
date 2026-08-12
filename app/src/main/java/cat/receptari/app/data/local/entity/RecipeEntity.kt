package cat.receptari.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Timestamps are stored as epoch milliseconds rather than via a TypeConverter — the
 * conversion to `Instant` happens in the mapper, which keeps the schema free of Room
 * converters entirely.
 */
@Entity(
    tableName = "recipes",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            // Deleting a folder unfiles its recipes; it must never delete them.
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("title"),
        Index("isFavorite"),
        Index("updatedAt"),
        Index("folderId"),
    ],
)
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val originalTitle: String?,
    val imagePath: String?,
    val prepTimeMinutes: Int?,
    val cookTimeMinutes: Int?,
    val totalTimeMinutes: Int?,
    val baseServings: Int?,
    val isFavorite: Boolean,
    val rating: Int?,
    val notes: String?,
    val sourceName: String?,
    val sourceUrl: String?,
    val originalLanguage: String?,
    val displayLanguage: String?,
    val folderId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Denormalized search index (PRD §6). Not a source of truth — it is rewritten whenever a
 * recipe is saved and can be dropped and rebuilt at any time.
 */
@Fts4
@Entity(tableName = "recipe_fts")
data class RecipeFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Int = 0,
    val recipeId: String,
    val title: String,
    val notes: String,
    val ingredientNames: String,
    val tagNames: String,
)
