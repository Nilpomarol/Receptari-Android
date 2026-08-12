package cat.receptari.app.ui.navigation

/**
 * Routes. Argument names must match what the ViewModels read out of `SavedStateHandle`
 * (`recipeId`), so they are defined once here.
 */
object Destination {
    const val LIBRARY = "library"
    const val IMPORT = "import"
    const val IMPORT_TEXT = "import/text"
    const val IMPORT_WEBSITE = "import/website"
    const val IMPORT_IMAGE = "import/image"
    const val SETTINGS = "settings"

    const val RECIPE_ID_ARG = "recipeId"
    const val FOLDER_ID_ARG = "folderId"

    const val DETAIL_ROUTE = "recipe/{$RECIPE_ID_ARG}"
    fun detail(recipeId: String) = "recipe/$recipeId"

    const val FOLDER_ROUTE = "folder/{$FOLDER_ID_ARG}"
    fun folder(folderId: String) = "folder/$folderId"

    /** Editing an existing recipe carries an id; creating a new one does not. */
    const val EDIT_ROUTE = "edit?$RECIPE_ID_ARG={$RECIPE_ID_ARG}"
    fun edit(recipeId: String? = null) =
        if (recipeId == null) "edit" else "edit?$RECIPE_ID_ARG=$recipeId"
}
