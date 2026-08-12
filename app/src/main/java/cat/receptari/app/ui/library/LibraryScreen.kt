package cat.receptari.app.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.CornerFlourish
import cat.receptari.app.core.designsystem.FilterPill
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.OrnamentalDivider
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.StarRating
import cat.receptari.app.core.designsystem.Wordmark
import cat.receptari.app.core.designsystem.pageFrame
import cat.receptari.app.core.designsystem.paperGrain
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.FolderSummary
import cat.receptari.app.domain.model.RecipeSort
import cat.receptari.app.domain.model.RecipeSummary
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.ui.folders.FolderEditorSheet
import cat.receptari.app.ui.folders.toColor
import cat.receptari.app.ui.folders.toImageVector
import coil3.compose.AsyncImage

@Composable
fun LibraryRoute(
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenRecipe = onOpenRecipe,
        onAddRecipe = onAddRecipe,
        onOpenSettings = onOpenSettings,
        onOpenFolder = onOpenFolder,
        modifier = modifier,
    )
}

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var foldersOpen by remember { mutableStateOf(false) }
    var creatingFolder by remember { mutableStateOf(false) }

    PaperScaffold(
        modifier = modifier,
        topBar = { Masthead(onOpenSettings = onOpenSettings) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddRecipe,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.library_add_recipe)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SearchField(
                query = state.searchQuery,
                onQueryChange = { onEvent(LibraryEvent.SearchQueryChanged(it)) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            FilterPills(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.padding(top = 12.dp),
            )

            SortRow(
                sort = state.sort,
                onSelect = { onEvent(LibraryEvent.SortChanged(it)) },
                folderCount = state.availableFolders.size,
                onOpenFolders = { foldersOpen = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )

            when {
                state.isLoading -> Box(Modifier.fillMaxSize())

                state.isLibraryEmpty -> EmptyState(
                    title = stringResource(R.string.library_empty_title),
                    body = stringResource(R.string.library_empty_body),
                )

                state.recipes.isEmpty() -> EmptyState(
                    title = stringResource(R.string.library_no_results_title),
                    body = stringResource(R.string.library_no_results_body),
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.recipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = { onOpenRecipe(recipe.id) },
                            onToggleFavorite = {
                                onEvent(LibraryEvent.ToggleFavorite(recipe.id, recipe.isFavorite))
                            },
                        )
                    }
                }
            }
        }
    }

    if (foldersOpen) {
        FolderLibrarySheet(
            folders = state.availableFolders,
            onOpenFolder = { folderId ->
                foldersOpen = false
                onOpenFolder(folderId)
            },
            onCreateFolder = {
                foldersOpen = false
                creatingFolder = true
            },
            onDismiss = { foldersOpen = false },
        )
    }

    if (creatingFolder) {
        var draftName by remember { mutableStateOf("") }
        var draftColor by remember(state.availableFolders.size) {
            mutableStateOf(
                FolderColor.entries[state.availableFolders.size % FolderColor.entries.size],
            )
        }
        var draftIcon by remember { mutableStateOf(FolderIcon.FOLDER) }

        FolderEditorSheet(
            title = stringResource(R.string.edit_folder_add),
            name = draftName,
            onNameChange = { draftName = it },
            color = draftColor,
            onColorChange = { draftColor = it },
            icon = draftIcon,
            onIconChange = { draftIcon = it },
            confirmLabel = stringResource(R.string.folders_add),
            onConfirm = {
                onEvent(LibraryEvent.FolderCreateRequested(draftName, draftColor, draftIcon))
                creatingFolder = false
            },
            onDismiss = { creatingFolder = false },
        )
    }
}

/**
 * The book's title page, not a top app bar.
 *
 * The wordmark is the only script on the screen and the only place the app names itself, so
 * it gets the room a title page would give it.
 */
@Composable
private fun Masthead(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 4.dp, bottom = 10.dp),
    ) {
        // Engraved sprigs flanking the title, the way a title page is decorated. They sit
        // beside the wordmark rather than in the corners: the corners are where the settings
        // button lives, and an icon on top of leaves is just clutter.
        CornerFlourish(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp),
        )
        CornerFlourish(
            mirrored = true,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
        )

        Wordmark(
            text = stringResource(R.string.library_title),
            modifier = Modifier.align(Alignment.Center),
        )

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.nav_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Search is always on screen rather than hidden behind an icon.
 *
 * A book opens to its index; making the reader tap to reveal one is a phone habit rather
 * than a book one. This uses `BasicTextField` because Material's `TextField` brings its own
 * container, indicator and 56 dp of chrome, all of which would then have to be argued back
 * out again.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, ReceptariTheme.palette.rule),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, top = 14.dp, bottom = 14.dp),
                decorationBox = { field ->
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.library_search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    field()
                },
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.library_search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Filters as a scrolling row rather than a menu.
 *
 * Which filters are on is the most useful thing to know when the list comes back empty, and
 * a dropdown hides exactly that.
 */
@Composable
private fun FilterPills(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPill(
            label = stringResource(R.string.filter_all),
            selected = !state.filter.isActive,
            onClick = { onEvent(LibraryEvent.ClearFilters) },
            icon = Icons.Outlined.GridView,
        )
        FilterPill(
            label = stringResource(R.string.filter_favorites),
            selected = state.filter.favoritesOnly,
            onClick = { onEvent(LibraryEvent.ToggleFavoritesFilter) },
            icon = Icons.Default.FavoriteBorder,
        )
        FilterPill(
            label = stringResource(R.string.filter_never_cooked),
            selected = state.filter.neverCooked,
            onClick = { onEvent(LibraryEvent.ToggleNeverCookedFilter) },
            icon = Icons.Outlined.Restaurant,
        )
        FilterPill(
            label = stringResource(R.string.filter_recently_cooked),
            selected = state.filter.cookedSinceEpochMillis != null,
            onClick = { onEvent(LibraryEvent.ToggleRecentlyCookedFilter) },
            icon = Icons.Outlined.Schedule,
        )
        state.availableTags.forEach { tag ->
            FilterPill(
                label = tag.name,
                selected = tag.id in state.filter.tagIds,
                onClick = { onEvent(LibraryEvent.ToggleTagFilter(tag.id)) },
                icon = Icons.Outlined.LocalOffer,
            )
        }
    }
}

/**
 * The shelf of folders, above the recipe list rather than folded into the filter row — a
 * folder is a place you go into, not a toggle you leave on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderLibrarySheet(
    folders: List<FolderSummary>,
    onOpenFolder: (String) -> Unit,
    onCreateFolder: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .paperGrain()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Hairline(modifier = Modifier.width(54.dp))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .paperGrain()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            OrnamentHeading(title = stringResource(R.string.folders_title))
            Button(
                onClick = onCreateFolder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.edit_folder_add),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            if (folders.isEmpty()) {
                Aside(
                    text = stringResource(R.string.folders_library_empty),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 430.dp)
                        .padding(top = 12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    gridItems(folders, key = { it.folder.id }) { summary ->
                        FolderTile(
                            summary = summary,
                            onClick = { onOpenFolder(summary.folder.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTile(summary: FolderSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PaperCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 108.dp),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column {
            Icon(
                imageVector = summary.folder.icon.toImageVector(),
                contentDescription = null,
                tint = summary.folder.color.toColor(),
            )
            Text(
                text = summary.folder.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = pluralStringResource(
                    R.plurals.folders_recipe_count,
                    summary.recipeCount,
                    summary.recipeCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SortRow(
    sort: RecipeSort,
    onSelect: (RecipeSort) -> Unit,
    folderCount: Int,
    onOpenFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onOpenFolders) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = pluralStringResource(R.plurals.folders_count, folderCount, folderCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.library_sort),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box {
            TextButton(onClick = { expanded = true }) {
                Text(
                    text = sort.label(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                RecipeSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label()) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                        leadingIcon = { RadioButton(selected = option == sort, onClick = null) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecipeCard(
    recipe: RecipeSummary,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaperCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RecipePlate(imagePath = recipe.imagePath)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val minutes = recipe.effectiveTimeMinutes
                // Zero or negative servings would be a broken import, not "for nobody".
                val servings = recipe.baseServings?.takeIf { it > 0 }

                if (minutes != null || servings != null) {
                    Row(
                        modifier = Modifier.padding(top = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        minutes?.let {
                            MetaItem(
                                icon = Icons.Outlined.Schedule,
                                label = stringResource(R.string.common_minutes_short, it),
                            )
                        }
                        servings?.let {
                            MetaItem(
                                icon = Icons.Outlined.Group,
                                label = pluralStringResource(R.plurals.common_servings, it, it),
                            )
                        }
                    }
                }

                recipe.rating?.let { rating ->
                    StarRating(
                        rating = rating,
                        contentDescription = stringResource(R.string.common_rating_value, rating),
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }

                if (recipe.cookCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.detail_cook_count,
                            recipe.cookCount,
                            recipe.cookCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                // A recipe with nothing but a name leaves the card looking unfinished. A
                // printer's mark is the honest filler: it says there is nothing more to say,
                // where invented metadata or an "add details" prompt would be noise.
                val hasMeta = minutes != null ||
                    servings != null ||
                    recipe.rating != null ||
                    recipe.cookCount > 0
                if (!hasMeta) {
                    OrnamentalDivider(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .width(88.dp),
                    )
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (recipe.isFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = stringResource(
                        if (recipe.isFavorite) {
                            R.string.detail_favorite_remove
                        } else {
                            R.string.detail_favorite_add
                        },
                    ),
                    tint = if (recipe.isFavorite) {
                        ReceptariTheme.palette.heart
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/** One piece of card metadata: a small icon and its figure. */
@Composable
private fun MetaItem(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The photograph, framed like one pasted into the book — or its empty mount. */
@Composable
private fun RecipePlate(imagePath: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    // pageFrame sits outside the clip: inside it, the outer half of the stroke would be
    // clipped away and the frame would render at half its width.
    val frame = modifier
        .size(76.dp)
        .pageFrame(shape, ReceptariTheme.palette.rule, inset = 0.dp)
        .clip(shape)

    if (imagePath == null) {
        Box(
            modifier = frame.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Restaurant,
                contentDescription = null,
                tint = ReceptariTheme.palette.rule,
                modifier = Modifier.size(26.dp),
            )
        }
    } else {
        AsyncImage(
            model = imagePath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = frame,
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
        Aside(
            text = body,
            modifier = Modifier.padding(top = 10.dp),
        )
        OrnamentalDivider(modifier = Modifier.padding(top = 20.dp))
    }
}

@Composable
private fun RecipeSort.label(): String = stringResource(
    when (this) {
        RecipeSort.Alphabetical -> R.string.sort_alphabetical
        RecipeSort.RecentlyAdded -> R.string.sort_recently_added
        RecipeSort.RecentlyCooked -> R.string.sort_recently_cooked
        RecipeSort.MostCooked -> R.string.sort_most_cooked
        RecipeSort.HighestRated -> R.string.sort_highest_rated
        RecipeSort.CookingTime -> R.string.sort_cooking_time
    },
)

@Preview
@Composable
private fun LibraryScreenPreview() {
    ReceptariTheme {
        LibraryScreen(
            state = LibraryUiState(
                recipes = listOf(
                    RecipeSummary(
                        id = "1",
                        title = "Fricandó amb moixernons",
                        prepTimeMinutes = 20,
                        cookTimeMinutes = 80,
                        baseServings = 4,
                        rating = 4,
                        cookCount = 3,
                        isFavorite = true,
                    ),
                    // Only a cook time, so that is the whole time.
                    RecipeSummary(
                        id = "2",
                        title = "Crema catalana",
                        cookTimeMinutes = 25,
                        baseServings = 6,
                        rating = 5,
                    ),
                    // A stated total wins over parts that disagree with it.
                    RecipeSummary(
                        id = "3",
                        title = "Truita de patates",
                        prepTimeMinutes = 10,
                        cookTimeMinutes = 15,
                        totalTimeMinutes = 40,
                        baseServings = 2,
                    ),
                    // Nothing but a name: the case the printer's mark exists for.
                    RecipeSummary(id = "4", title = "Escudella"),
                ),
                availableTags = listOf(Tag(id = "t1", name = "Postres")),
                isLoading = false,
            ),
            onEvent = {},
            onOpenRecipe = {},
            onAddRecipe = {},
            onOpenSettings = {},
            onOpenFolder = {},
        )
    }
}

@Preview
@Composable
private fun LibraryScreenEmptyPreview() {
    ReceptariTheme {
        LibraryScreen(
            state = LibraryUiState(isLoading = false),
            onEvent = {},
            onOpenRecipe = {},
            onAddRecipe = {},
            onOpenSettings = {},
            onOpenFolder = {},
        )
    }
}
