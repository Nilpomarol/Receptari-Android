package cat.receptari.app.ui.folders

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tag
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
import cat.receptari.app.core.designsystem.CardRow
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.Rubric
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.FolderSummary
import cat.receptari.app.domain.repository.FolderRepository
import cat.receptari.app.domain.repository.ImageStore
import cat.receptari.app.domain.repository.TagRepository
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionsUiState(
    val folders: List<FolderSummary> = emptyList(),
    val tagCount: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    tagRepository: TagRepository,
    imageStore: ImageStore,
) : ViewModel() {

    val uiState: StateFlow<CollectionsUiState> = combine(
        folderRepository.observeAllWithCounts(),
        tagRepository.observeAll().map { it.size },
    ) { folders, tagCount ->
        CollectionsUiState(
            folders = folders.map { summary ->
                summary.copy(coverImagePath = summary.coverImagePath?.let(imageStore::absolutePathOf))
            },
            tagCount = tagCount,
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
    onOpenTags: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectionsScreen(
        state = state,
        onOpenFolder = onOpenFolder,
        onOpenTags = onOpenTags,
        onCreateFolder = viewModel::createFolder,
        modifier = modifier,
    )
}

/**
 * The folders as a grid of photo tiles, like the library's: each is fronted by one of its own
 * photographs, or by its icon on a tinted plate when it has none, with the name and the
 * recipe count under it. The last tile starts a new folder.
 */
@Composable
fun CollectionsScreen(
    state: CollectionsUiState,
    onOpenFolder: (String) -> Unit,
    onOpenTags: () -> Unit,
    onCreateFolder: (String, FolderColor, FolderIcon) -> Unit,
    modifier: Modifier = Modifier,
) {
    var creating by remember { mutableStateOf(false) }

    PaperScaffold(modifier = modifier) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = PageMargin,
                end = PageMargin,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 20.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.folders_title),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Rubric(
                            text = stringResource(R.string.folders_subtitle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            // Tags are the other way of filing a recipe, so they open from here, above the folders.
            item(key = "tags", span = { GridItemSpan(maxLineSpan) }) {
                PaperCard(
                    modifier = Modifier.padding(bottom = 6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                ) {
                    CardRow(
                        icon = Icons.Outlined.Tag,
                        title = stringResource(R.string.tags_title),
                        subtitle = pluralStringResource(R.plurals.tags_count, state.tagCount, state.tagCount),
                        onClick = onOpenTags,
                    )
                }
            }

            if (!state.isLoading && state.folders.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Aside(
                        text = stringResource(R.string.folders_library_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                }
            }

            items(state.folders, key = { it.folder.id }) { summary ->
                FolderTile(summary = summary, onClick = { onOpenFolder(summary.folder.id) })
            }

            item(key = "new") {
                NewFolderTile(onClick = { creating = true })
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
private fun FolderTile(
    summary: FolderSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val folderColor = summary.folder.color.toColor()
    val shape = MaterialTheme.shapes.medium
    Column(modifier = modifier.clickable(interactionSource = null, indication = null, onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(TilePhotoRatio)
                .clip(shape)
                .clickable(onClick = onClick),
        ) {
            val onPhoto = summary.coverImagePath != null
            if (onPhoto) {
                AsyncImage(
                    model = summary.coverImagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // A faint scrim at the foot keeps the count legible on any photograph.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.55f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.5f),
                            ),
                        ),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(folderColor.copy(alpha = 0.12f))
                        .border(1.dp, ReceptariTheme.palette.rule.copy(alpha = 0.6f), shape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = summary.folder.icon.toImageVector(),
                        contentDescription = null,
                        tint = folderColor,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // The folder's own colour, kept as a small mark now that the tile is not tinted by it.
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(folderColor))
                Text(
                    text = pluralStringResource(
                        R.plurals.folders_recipe_count,
                        summary.recipeCount,
                        summary.recipeCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (onPhoto) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = summary.folder.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** The last tile of the grid: an empty frame with a plus, which starts a new folder. */
@Composable
private fun NewFolderTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    Column(modifier = modifier.clickable(interactionSource = null, indication = null, onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(TilePhotoRatio)
                .clip(shape)
                .border(1.dp, ReceptariTheme.palette.rule, shape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = stringResource(R.string.edit_folder_add),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private val PageMargin = 20.dp
private const val TilePhotoRatio = 4f / 3f

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
            onOpenTags = {},
            onCreateFolder = { _, _, _ -> },
        )
    }
}
