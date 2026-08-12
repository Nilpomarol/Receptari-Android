package cat.receptari.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cat.receptari.app.ui.detail.RecipeDetailRoute
import cat.receptari.app.ui.edit.RecipeEditRoute
import cat.receptari.app.ui.folders.FolderRoute
import cat.receptari.app.ui.importer.ImportRoute
import cat.receptari.app.ui.importer.image.ImageImportRoute
import cat.receptari.app.ui.importer.text.TextImportRoute
import cat.receptari.app.ui.importer.website.WebsiteImportRoute
import cat.receptari.app.ui.library.LibraryRoute
import cat.receptari.app.ui.settings.SettingsRoute

@Composable
fun ReceptariNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openRecipeId: String? = null,
    onRecipeOpened: () -> Unit = {},
) {
    LaunchedEffect(openRecipeId) {
        openRecipeId?.let { recipeId ->
            navController.navigate(Destination.detail(recipeId)) {
                launchSingleTop = true
            }
            onRecipeOpened()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Destination.LIBRARY,
        modifier = modifier,
    ) {
        composable(Destination.LIBRARY) {
            LibraryRoute(
                onOpenRecipe = { navController.navigate(Destination.detail(it)) },
                onAddRecipe = { navController.navigate(Destination.IMPORT) },
                onOpenSettings = { navController.navigate(Destination.SETTINGS) },
                onOpenFolder = { navController.navigate(Destination.folder(it)) },
            )
        }

        composable(
            route = Destination.DETAIL_ROUTE,
            arguments = listOf(navArgument(Destination.RECIPE_ID_ARG) { type = NavType.StringType }),
        ) {
            RecipeDetailRoute(
                onNavigateBack = navController::popBackStack,
                onEditRecipe = { navController.navigate(Destination.edit(it)) },
            )
        }

        composable(
            route = Destination.EDIT_ROUTE,
            arguments = listOf(
                navArgument(Destination.RECIPE_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val editingExisting =
                backStackEntry.arguments?.getString(Destination.RECIPE_ID_ARG) != null

            RecipeEditRoute(
                onNavigateBack = navController::popBackStack,
                onSaved = { recipeId ->
                    if (editingExisting) {
                        // Editing returns to the detail screen already on the back stack.
                        navController.popBackStack()
                    } else {
                        // A newly created recipe should land on its own page, and going
                        // back from there belongs in the library rather than the empty form.
                        navController.popBackStack(Destination.LIBRARY, inclusive = false)
                        navController.navigate(Destination.detail(recipeId))
                    }
                },
            )
        }

        composable(Destination.IMPORT) {
            ImportRoute(
                onNavigateBack = navController::popBackStack,
                onCreateManually = {
                    navController.popBackStack()
                    navController.navigate(Destination.edit())
                },
                onImportFromText = { navController.navigate(Destination.IMPORT_TEXT) },
                onImportFromWebsite = { navController.navigate(Destination.IMPORT_WEBSITE) },
                onImportFromImage = { navController.navigate(Destination.IMPORT_IMAGE) },
            )
        }

        composable(Destination.IMPORT_WEBSITE) {
            WebsiteImportRoute(
                onNavigateBack = navController::popBackStack,
                onDraftReady = { navController.toDraftEditor() },
            )
        }

        composable(Destination.IMPORT_TEXT) {
            TextImportRoute(
                onNavigateBack = navController::popBackStack,
                // The source screens have done their job once the draft exists; going back
                // from the editor belongs in the library, not in the paste box.
                onDraftReady = { navController.toDraftEditor() },
            )
        }

        composable(Destination.IMPORT_IMAGE) {
            ImageImportRoute(
                onNavigateBack = navController::popBackStack,
                onDraftReady = { navController.toDraftEditor() },
            )
        }

        composable(Destination.SETTINGS) {
            SettingsRoute(onNavigateBack = navController::popBackStack)
        }

        composable(
            route = Destination.FOLDER_ROUTE,
            arguments = listOf(navArgument(Destination.FOLDER_ID_ARG) { type = NavType.StringType }),
        ) {
            FolderRoute(
                onNavigateBack = navController::popBackStack,
                onOpenRecipe = { navController.navigate(Destination.detail(it)) },
            )
        }
    }
}

/**
 * Clears the import screens off the back stack and opens the editor on the pending draft.
 * Shared by all three import sources so they behave identically once extraction succeeds.
 */
private fun NavHostController.toDraftEditor() {
    popBackStack(Destination.LIBRARY, inclusive = false)
    navigate(Destination.edit())
}
