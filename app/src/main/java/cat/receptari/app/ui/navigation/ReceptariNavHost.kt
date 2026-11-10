package cat.receptari.app.ui.navigation

import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cat.receptari.app.core.designsystem.PaperActionButton
import cat.receptari.app.core.designsystem.PaperNavigationItem
import cat.receptari.app.ui.importer.AddRecipeSheet
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.PaperNavigationBar
import cat.receptari.app.ui.folders.CollectionsRoute
import cat.receptari.app.ui.detail.RecipeDetailRoute
import cat.receptari.app.ui.cook.CookModeRoute
import cat.receptari.app.ui.edit.RecipeEditRoute
import cat.receptari.app.ui.folders.FolderRoute
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

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Destination.TopLevel
    var adding by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
    Box(modifier = Modifier.weight(1f)) {
    NavHost(
        navController = navController,
        startDestination = Destination.LIBRARY,
        modifier = Modifier
            .fillMaxSize()
            // The bar below pads itself for the gesture area; the page above must not too.
            .then(
                if (showBottomBar) {
                    Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                } else {
                    Modifier
                },
            ),
    ) {
        composable(Destination.LIBRARY) {
            LibraryRoute(
                onOpenRecipe = { navController.navigate(Destination.detail(it)) },
                onAddRecipe = { adding = true },
            )
        }

        composable(
            route = Destination.FAVORITES,
            arguments = listOf(
                navArgument(Destination.FAVORITES_ONLY_ARG) {
                    type = NavType.BoolType
                    defaultValue = true
                },
            ),
        ) {
            LibraryRoute(
                onOpenRecipe = { navController.navigate(Destination.detail(it)) },
                onAddRecipe = { adding = true },
            )
        }

        composable(Destination.COLLECTIONS) {
            CollectionsRoute(onOpenFolder = { navController.navigate(Destination.folder(it)) })
        }

        composable(
            route = Destination.DETAIL_ROUTE,
            arguments = listOf(navArgument(Destination.RECIPE_ID_ARG) { type = NavType.StringType }),
        ) {
            RecipeDetailRoute(
                onNavigateBack = navController::popBackStack,
                onEditRecipe = { navController.navigate(Destination.edit(it)) },
                onStartCooking = { navController.navigate(Destination.cook(it)) },
                onOpenSettings = { navController.navigateToTab(Destination.SETTINGS) },
            )
        }

        composable(
            route = Destination.COOK_ROUTE,
            arguments = listOf(navArgument(Destination.RECIPE_ID_ARG) { type = NavType.StringType }),
        ) {
            CookModeRoute(onNavigateBack = navController::popBackStack)
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
            SettingsRoute()
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

    // Adding sits over the pages that hold recipes, not in the bar.
    if (currentRoute in AddableRoutes) {
        PaperActionButton(
            icon = Icons.Default.Add,
            contentDescription = stringResource(R.string.library_add_recipe),
            onClick = { adding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
    }

    if (showBottomBar) {
        PaperNavigationBar(
            selectedIndex = TopLevelTabs.indexOfFirst { it.route == currentRoute },
            itemCount = TopLevelTabs.size,
        ) {
            TopLevelTabs.forEach { tab ->
                PaperNavigationItem(
                    selected = currentRoute == tab.route,
                    onClick = { navController.navigateToTab(tab.route) },
                    icon = tab.icon,
                    selectedIcon = tab.selectedIcon,
                    label = stringResource(tab.label),
                )
            }
        }
    }
    }

    if (adding) {
        AddRecipeSheet(
            onDismiss = { adding = false },
            onImportFromWebsite = { navController.navigate(Destination.IMPORT_WEBSITE) },
            onImportFromText = { navController.navigate(Destination.IMPORT_TEXT) },
            onImportFromImage = { navController.navigate(Destination.IMPORT_IMAGE) },
            onCreateManually = { navController.navigate(Destination.edit()) },
        )
    }
}

private val AddableRoutes = setOf(Destination.LIBRARY, Destination.FAVORITES)

private data class TopLevelTab(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val TopLevelTabs = listOf(
    TopLevelTab(
        Destination.LIBRARY,
        R.string.nav_recipes,
        Icons.AutoMirrored.Outlined.MenuBook,
        Icons.AutoMirrored.Filled.MenuBook,
    ),
    TopLevelTab(
        Destination.FAVORITES,
        R.string.filter_favorites,
        Icons.Outlined.FavoriteBorder,
        Icons.Filled.Favorite,
    ),
    TopLevelTab(
        Destination.COLLECTIONS,
        R.string.folders_title,
        Icons.Outlined.CollectionsBookmark,
        Icons.Filled.CollectionsBookmark,
    ),
    TopLevelTab(
        Destination.SETTINGS,
        R.string.nav_settings,
        Icons.Outlined.Settings,
        Icons.Filled.Settings,
    ),
)

/** Switches tabs; a tab always opens fresh at its root (scroll, filters, pushed pages are dropped). */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
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
