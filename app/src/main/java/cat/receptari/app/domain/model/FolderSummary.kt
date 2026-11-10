package cat.receptari.app.domain.model

/** A folder plus how many recipes currently file under it, and a photo to show it by. */
data class FolderSummary(
    val folder: Folder,
    val recipeCount: Int,
    /** Relative image path of one recipe in the folder, or null if none has a photo. */
    val coverImagePath: String? = null,
)
