package cat.receptari.app.ui.tags

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.Rubric
import cat.receptari.app.ui.library.FilterSheet
import cat.receptari.app.ui.library.LibraryEvent
import cat.receptari.app.ui.library.LibraryUiState
import cat.receptari.app.ui.library.LibraryViewModel
import cat.receptari.app.ui.library.libraryControls
import cat.receptari.app.ui.library.noResultsItem
import cat.receptari.app.ui.library.recipeItems

/**
 * One tag's recipes. The list is the library's own view model, pinned to the tag by the
 * route's tag id, so search, filters and sorting work as on the home page.
 */
@Composable
fun TagRecipesRoute(
    tagId: String,
    onNavigateBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val library by viewModel.uiState.collectAsStateWithLifecycle()
    TagRecipesScreen(
        name = library.availableTags.firstOrNull { it.id == tagId }?.name.orEmpty(),
        library = library,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onOpenRecipe = onOpenRecipe,
        modifier = modifier,
    )
}

@Composable
fun TagRecipesScreen(
    name: String,
    library: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filtersOpen by remember { mutableStateOf(false) }

    PaperScaffold(
        modifier = modifier,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            text = hashName(name),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Rubric(
                            text = pluralStringResource(
                                R.plurals.folders_recipe_count,
                                library.recipes.size,
                                library.recipes.size,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Hairline()
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        ) {
            if (!library.isLoading && !library.isLibraryEmpty) {
                libraryControls(library, onEvent, onOpenFilters = { filtersOpen = true })
            }
            when {
                library.isLoading -> Unit

                library.isLibraryEmpty -> item(key = "empty") {
                    Aside(
                        text = stringResource(R.string.tag_recipes_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 40.dp),
                    )
                }

                library.recipes.isEmpty() -> noResultsItem(library, onEvent)

                else -> recipeItems(
                    recipes = library.recipes,
                    layout = library.listLayout,
                    onClick = { onOpenRecipe(it.id) },
                    onToggleFavorite = { onEvent(LibraryEvent.ToggleFavorite(it.id, it.isFavorite)) },
                    itemModifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }

    if (filtersOpen) {
        FilterSheet(state = library, onEvent = onEvent, onDismiss = { filtersOpen = false })
    }
}
