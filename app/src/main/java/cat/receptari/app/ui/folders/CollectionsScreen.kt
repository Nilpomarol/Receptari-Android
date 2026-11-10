package cat.receptari.app.ui.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.Rubric
import cat.receptari.app.core.designsystem.Sprig
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.FolderSummary
import cat.receptari.app.domain.repository.FolderRepository
import cat.receptari.app.domain.repository.ImageStore
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionsUiState(
    val folders: List<FolderSummary> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    imageStore: ImageStore,
) : ViewModel() {

    val uiState: StateFlow<CollectionsUiState> = folderRepository.observeAllWithCounts()
        .map { folders ->
            CollectionsUiState(
                folders = folders.map { summary ->
                    summary.copy(coverImagePath = summary.coverImagePath?.let(imageStore::absolutePathOf))
                },
                isLoading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionsUiState())

    fun createFolder(name: String, color: FolderColor, icon: FolderIcon) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { folderRepository.create(trimmed, color, icon) }
    }
}

@Composable
fun CollectionsRoute(
    onOpenFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectionsScreen(
        state = state,
        onOpenFolder = onOpenFolder,
        onCreateFolder = viewModel::createFolder,
        modifier = modifier,
    )
}

/** The folders as a shelf of illustrated banners, each fronted by one of its own photos. */
@Composable
fun CollectionsScreen(
    state: CollectionsUiState,
    onOpenFolder: (String) -> Unit,
    onCreateFolder: (String, FolderColor, FolderIcon) -> Unit,
    modifier: Modifier = Modifier,
) {
    var creating by remember { mutableStateOf(false) }

    PaperScaffold(modifier = modifier) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = PageMargin,
                end = PageMargin,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 20.dp, bottom = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.folders_title),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Sprig(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(width = 64.dp, height = 32.dp),
                        )
                    }
                    Rubric(
                        text = stringResource(R.string.folders_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (!state.isLoading && state.folders.isEmpty()) {
                item(key = "empty") {
                    Aside(
                        text = stringResource(R.string.folders_library_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                }
            }

            items(state.folders, key = { it.folder.id }) { summary ->
                FolderBanner(summary = summary, onClick = { onOpenFolder(summary.folder.id) })
            }

            item(key = "new") {
                PaperCard(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.edit_folder_add),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        var color by remember {
            mutableStateOf(FolderColor.entries[state.folders.size % FolderColor.entries.size])
        }
        var icon by remember { mutableStateOf(FolderIcon.FOLDER) }
        FolderEditorSheet(
            title = stringResource(R.string.edit_folder_add),
            name = name,
            onNameChange = { name = it },
            color = color,
            onColorChange = { color = it },
            icon = icon,
            onIconChange = { icon = it },
            confirmLabel = stringResource(R.string.folders_add),
            onConfirm = {
                onCreateFolder(name, color, icon)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun FolderBanner(
    summary: FolderSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.colorScheme.surfaceContainerLow
    val folderColor = summary.folder.color.toColor()
    PaperCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.height(BannerHeight)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Sprig(modifier = Modifier.size(width = 40.dp, height = 20.dp))
                Text(
                    text = summary.folder.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Rubric(
                    text = pluralStringResource(
                        R.plurals.folders_recipe_count,
                        summary.recipeCount,
                        summary.recipeCount,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.9f),
            ) {
                if (summary.coverImagePath != null) {
                    AsyncImage(
                        model = summary.coverImagePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // The photo dissolves into the card on its left edge, like a plate
                    // printed into the page rather than pasted onto it.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0f to card,
                                    0.35f to card.copy(alpha = 0f),
                                ),
                            ),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(folderColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = summary.folder.icon.toImageVector(),
                            contentDescription = null,
                            tint = folderColor,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
            }
        }
    }
}

private val PageMargin = 20.dp
private val BannerHeight = 124.dp

@Preview
@Composable
private fun CollectionsScreenPreview() {
    ReceptariTheme {
        CollectionsScreen(
            state = CollectionsUiState(
                folders = listOf(
                    FolderSummary(
                        folder = Folder(id = "1", name = "Cuina catalana", normalizedName = "cuina catalana"),
                        recipeCount = 12,
                    ),
                    FolderSummary(
                        folder = Folder(id = "2", name = "Postres", normalizedName = "postres"),
                        recipeCount = 4,
                    ),
                ),
                isLoading = false,
            ),
            onOpenFolder = {},
            onCreateFolder = { _, _, _ -> },
        )
    }
}
