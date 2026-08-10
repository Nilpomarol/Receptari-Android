package cat.receptari.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cat.receptari.app.ui.detail.RecipeDetailRoute
import cat.receptari.app.ui.edit.RecipeEditRoute
import cat.receptari.app.ui.importer.ImportRoute
import cat.receptari.app.ui.library.LibraryRoute
import cat.receptari.app.ui.settings.SettingsRoute

@Composable
fun ReceptariNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
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
            )
        }

        composable(Destination.SETTINGS) {
            SettingsRoute(onNavigateBack = navController::popBackStack)
        }
    }
}
