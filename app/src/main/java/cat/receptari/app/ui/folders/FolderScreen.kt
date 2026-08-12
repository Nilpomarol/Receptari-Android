package cat.receptari.app.ui.folders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import cat.receptari.app.core.designsystem.OrnamentalDivider
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.RecipeSummary
import cat.receptari.app.ui.library.RecipeCard

@Composable
fun FolderRoute(
    onNavigateBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FolderContentsEffect.Deleted -> onNavigateBack()
            }
        }
    }

    FolderScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onOpenRecipe = onOpenRecipe,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    state: FolderContentsUiState,
    onEvent: (FolderContentsEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    PaperScaffold(
        modifier = modifier,
        topBar = {
            PaperTopBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        state.folder?.let { folder ->
                            Icon(
                                imageVector = folder.icon.toImageVector(),
                                contentDescription = null,
                                tint = folder.color.toColor(),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(22.dp),
                            )
                        }
                        Text(state.folder?.name.orEmpty())
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { renaming = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.folders_rename),
                        )
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.folders_delete),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding))

            state.recipes.isEmpty() -> EmptyState(
                title = stringResource(R.string.folder_empty_title),
                body = stringResource(R.string.folder_empty_body),
                modifier = Modifier.padding(innerPadding),
            )

            else -> LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.recipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onClick = { onOpenRecipe(recipe.id) },
                        onToggleFavorite = {
                            onEvent(FolderContentsEvent.ToggleFavorite(recipe.id, recipe.isFavorite))
                        },
                    )
                }
            }
        }
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
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text(stringResource(R.string.folders_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.folders_delete_confirm_body,
                        state.folder?.name.orEmpty(),
                    ),
                )
            },
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

@Composable
private fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OrnamentalDivider(modifier = Modifier.padding(bottom = 20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Aside(text = body, modifier = Modifier.padding(top = 10.dp))
        OrnamentalDivider(modifier = Modifier.padding(top = 20.dp))
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
            onEvent = {},
            onNavigateBack = {},
            onOpenRecipe = {},
        )
    }
}
