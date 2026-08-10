package cat.receptari.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.RecipeSort
import cat.receptari.app.domain.model.RecipeSummary
import coil3.compose.AsyncImage

@Composable
fun LibraryRoute(
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onOpenSettings: () -> Unit,
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
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            if (state.isSearchActive) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { onEvent(LibraryEvent.SearchQueryChanged(it)) },
                    onClose = { onEvent(LibraryEvent.SearchActiveChanged(false)) },
                )
            } else {
                LibraryTopBar(state = state, onEvent = onEvent, onOpenSettings = onOpenSettings)
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddRecipe,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.library_add_recipe)) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> Unit

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
                        top = 8.dp,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    var filterMenuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(stringResource(R.string.library_title)) },
        actions = {
            IconButton(onClick = { onEvent(LibraryEvent.SearchActiveChanged(true)) }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.library_search),
                )
            }

            Box {
                IconButton(onClick = { sortMenuOpen = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.library_sort),
                    )
                }
                SortMenu(
                    expanded = sortMenuOpen,
                    current = state.sort,
                    onDismiss = { sortMenuOpen = false },
                    onSelect = {
                        onEvent(LibraryEvent.SortChanged(it))
                        sortMenuOpen = false
                    },
                )
            }

            Box {
                IconButton(onClick = { filterMenuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.library_filter),
                        tint = if (state.filter.isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                FilterMenu(
                    expanded = filterMenuOpen,
                    state = state,
                    onDismiss = { filterMenuOpen = false },
                    onEvent = onEvent,
                )
            }

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // Opening search should put the cursor in the field; making the user tap twice reads
    // as the button not having worked.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.library_search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.library_search_close),
                )
            }
        },
    )
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    current: RecipeSort,
    onDismiss: () -> Unit,
    onSelect: (RecipeSort) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        RecipeSort.entries.forEach { sort ->
            DropdownMenuItem(
                text = { Text(sort.label()) },
                onClick = { onSelect(sort) },
                leadingIcon = { RadioButton(selected = sort == current, onClick = null) },
            )
        }
    }
}

@Composable
private fun FilterMenu(
    expanded: Boolean,
    state: LibraryUiState,
    onDismiss: () -> Unit,
    onEvent: (LibraryEvent) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        CheckableItem(
            label = stringResource(R.string.filter_favorites),
            checked = state.filter.favoritesOnly,
            onClick = { onEvent(LibraryEvent.ToggleFavoritesFilter) },
        )
        CheckableItem(
            label = stringResource(R.string.filter_never_cooked),
            checked = state.filter.neverCooked,
            onClick = { onEvent(LibraryEvent.ToggleNeverCookedFilter) },
        )
        CheckableItem(
            label = stringResource(R.string.filter_recently_cooked),
            checked = state.filter.cookedSinceEpochMillis != null,
            onClick = { onEvent(LibraryEvent.ToggleRecentlyCookedFilter) },
        )

        if (state.availableTags.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = stringResource(R.string.filter_tags),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            state.availableTags.forEach { tag ->
                CheckableItem(
                    label = tag.name,
                    checked = tag.id in state.filter.tagIds,
                    onClick = { onEvent(LibraryEvent.ToggleTagFilter(tag.id)) },
                )
            }
        }

        if (state.filter.isActive) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_filters_clear)) },
                onClick = {
                    onEvent(LibraryEvent.ClearFilters)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun CheckableItem(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun RecipeCard(
    recipe: RecipeSummary,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            recipe.imagePath?.let { path ->
                AsyncImage(
                    model = path,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }

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

                val metadata = buildList {
                    recipe.totalTimeMinutes?.let {
                        add(stringResource(R.string.common_minutes_short, it))
                    }
                    if (recipe.cookCount > 0) {
                        add(
                            pluralStringResource(
                                R.plurals.detail_cook_count,
                                recipe.cookCount,
                                recipe.cookCount,
                            ),
                        )
                    }
                }
                if (metadata.isNotEmpty()) {
                    Text(
                        text = metadata.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                recipe.rating?.let { rating ->
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        repeat(rating) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
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
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
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
    ReceptariTheme(dynamicColor = false) {
        LibraryScreen(
            state = LibraryUiState(
                recipes = listOf(
                    RecipeSummary(
                        id = "1",
                        title = "Pollastre amb salsa",
                        totalTimeMinutes = 45,
                        rating = 4,
                        cookCount = 3,
                    ),
                    RecipeSummary(id = "2", title = "Crema catalana", totalTimeMinutes = 30, isFavorite = true),
                ),
                isLoading = false,
            ),
            onEvent = {},
            onOpenRecipe = {},
            onAddRecipe = {},
            onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun LibraryScreenEmptyPreview() {
    ReceptariTheme(dynamicColor = false) {
        LibraryScreen(
            state = LibraryUiState(isLoading = false),
            onEvent = {},
            onOpenRecipe = {},
            onAddRecipe = {},
            onOpenSettings = {},
        )
    }
}
