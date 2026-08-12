package cat.receptari.app.domain.model

/** A folder plus how many recipes currently file under it, for the management screen. */
data class FolderSummary(
    val folder: Folder,
    val recipeCount: Int,
)
