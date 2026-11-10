package cat.receptari.app.ui.navigation

/**
 * Routes. Argument names must match what the ViewModels read out of `SavedStateHandle`
 * (`recipeId`), so they are defined once here.
 */
object Destination {
    const val LIBRARY = "library"
    const val IMPORT_TEXT = "import/text"
    const val SETTINGS = "settings"
    const val FAVORITES = "favorites"
    const val COLLECTIONS = "collections"

    /** The four pages the bottom bar switches between (ADR-012). */
    val TopLevel = setOf(LIBRARY, FAVORITES, COLLECTIONS, SETTINGS)

    /** Set on [FAVORITES] so the shared library page pins its favourites filter. */
    const val FAVORITES_ONLY_ARG = "favoritesOnly"

    const val RECIPE_ID_ARG = "recipeId"
    const val FOLDER_ID_ARG = "folderId"

    const val DETAIL_ROUTE = "recipe/{$RECIPE_ID_ARG}"
    fun detail(recipeId: String) = "recipe/$recipeId"

    const val COOK_ROUTE = "recipe/{$RECIPE_ID_ARG}/cook"
    fun cook(recipeId: String) = "recipe/$recipeId/cook"

    const val FOLDER_ROUTE = "folder/{$FOLDER_ID_ARG}"
    fun folder(folderId: String) = "folder/$folderId"

    /** Editing an existing recipe carries an id; creating a new one does not. */
    const val EDIT_ROUTE = "edit?$RECIPE_ID_ARG={$RECIPE_ID_ARG}"
    fun edit(recipeId: String? = null) =
        if (recipeId == null) "edit" else "edit?$RECIPE_ID_ARG=$recipeId"
}
