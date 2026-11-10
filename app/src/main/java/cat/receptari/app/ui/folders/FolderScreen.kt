package cat.receptari.app.ui.folders

import androidx.compose.foundation.layout.Arrangement
import cat.receptari.app.core.designsystem.Sprig
import cat.receptari.app.core.designsystem.Rubric
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import cat.receptari.app.core.designsystem.PaperConfirmBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.RecipeSummary
import cat.receptari.app.ui.library.FilterSheet
import cat.receptari.app.ui.library.LibraryEvent
import cat.receptari.app.ui.library.LibraryUiState
import cat.receptari.app.ui.library.LibraryViewModel
import cat.receptari.app.ui.library.libraryControls
import cat.receptari.app.ui.library.noResultsItem
import cat.receptari.app.ui.library.recipeItems

@Composable
fun FolderRoute(
    onNavigateBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderViewModel = hiltViewModel(),
    // The recipes are listed by the library's own view model, pinned to this folder by the
    // route's folder id, so search, filters and sorting are the home page's.
    libraryViewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val library by libraryViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FolderContentsEffect.Deleted -> onNavigateBack()
            }
        }
    }

    FolderScreen(
        state = state,
        library = library,
        onEvent = viewModel::onEvent,
        onLibraryEvent = libraryViewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onOpenRecipe = onOpenRecipe,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    state: FolderContentsUiState,
    library: LibraryUiState,
    onEvent: (FolderContentsEvent) -> Unit,
    onLibraryEvent: (LibraryEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filtersOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    PaperScaffold(
        modifier = modifier,
        // The header carries the folder itself: its icon and name, the recipe count under
        // them, and the actions at the end, all flat on the paper above one hairline.
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
                    FolderTitle(
                        folder = state.folder,
                        recipeCount = state.recipes.size,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                    )
                    IconButton(onClick = { renaming = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.folders_rename),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.folders_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                libraryControls(library, onLibraryEvent, onOpenFilters = { filtersOpen = true })
            }
            when {
                library.isLoading -> Unit

                library.isLibraryEmpty -> item(key = "empty", contentType = "empty") {
                    EmptyState(
                        title = stringResource(R.string.folder_empty_title),
                        body = stringResource(R.string.folder_empty_body),
                        modifier = Modifier.padding(top = 40.dp),
                    )
                }

                library.recipes.isEmpty() -> noResultsItem(library, onLibraryEvent)

                else -> recipeItems(
                    recipes = library.recipes,
                    layout = library.listLayout,
                    onClick = { onOpenRecipe(it.id) },
                    onToggleFavorite = {
                        onLibraryEvent(LibraryEvent.ToggleFavorite(it.id, it.isFavorite))
                    },
                    itemModifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }

    if (filtersOpen) {
        FilterSheet(
            state = library,
            onEvent = onLibraryEvent,
            onDismiss = { filtersOpen = false },
        )
    }

    if (renaming) {
        var draftName by remember(state.folder?.name) {
            mutableStateOf(state.folder?.name.orEmpty())
        }
        var draftColor by remember(state.folder) {
            mutableStateOf(state.folder?.color ?: FolderColor.OLIVE)
        }
        var draftIcon by remember(state.folder) {
            mutableStateOf(state.folder?.icon ?: FolderIcon.FOLDER)
        }

        FolderEditorSheet(
            title = stringResource(R.string.folders_rename),
            name = draftName,
            onNameChange = { draftName = it },
            color = draftColor,
            onColorChange = { draftColor = it },
            icon = draftIcon,
            onIconChange = { draftIcon = it },
            confirmLabel = stringResource(R.string.edit_save),
            onConfirm = {
                onEvent(FolderContentsEvent.EditConfirmed(draftName, draftColor, draftIcon))
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }

    if (confirmDelete) {
        PaperConfirmBottomSheet(
            title = stringResource(R.string.folders_delete_confirm_title),
            subtitle = stringResource(
                R.string.folders_delete_confirm_body,
                state.folder?.name.orEmpty(),
            ),
            onDismissRequest = { confirmDelete = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onEvent(FolderContentsEvent.DeleteConfirmed)
                    },
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/** The folder's icon and name in its own colour, with the recipe count in small caps under it. */
@Composable
private fun FolderTitle(folder: Folder?, recipeCount: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            folder?.let {
                Icon(
                    imageVector = it.icon.toImageVector(),
                    contentDescription = null,
                    tint = it.color.toColor(),
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(22.dp),
                )
            }
            Text(
                text = folder?.name.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Rubric(
            text = pluralStringResource(R.plurals.folders_recipe_count, recipeCount, recipeCount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Sprig(modifier = Modifier.size(width = 64.dp, height = 32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Aside(text = body, modifier = Modifier.padding(top = 10.dp))
    }
}

@Preview
@Composable
private fun FolderScreenPreview() {
    ReceptariTheme {
        FolderScreen(
            state = FolderContentsUiState(
                folder = Folder(id = "1", name = "Postres de Nadal"),
                recipes = listOf(
                    RecipeSummary(id = "1", title = "Torró de xocolata", baseServings = 8),
                    RecipeSummary(id = "2", title = "Neules", baseServings = 20),
                ),
                isLoading = false,
            ),
            library = LibraryUiState(isLoading = false),
            onEvent = {},
            onLibraryEvent = {},
            onNavigateBack = {},
            onOpenRecipe = {},
        )
    }
}
