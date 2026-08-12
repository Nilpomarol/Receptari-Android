package cat.receptari.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [Index(value = ["normalizedName"], unique = true)],
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    /** [cat.receptari.app.domain.model.FolderColor] name. */
    val color: String,
    /** [cat.receptari.app.domain.model.FolderIcon] name. */
    val icon: String,
)

/** A folder plus how many recipes currently file under it, for the management screen. */
data class FolderWithCount(
    val id: String,
    val name: String,
    val normalizedName: String,
    val color: String,
    val icon: String,
    val recipeCount: Int,
)
