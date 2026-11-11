package cat.receptari.app.ui.library

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import cat.receptari.app.domain.model.RecipeListLayout
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.mutableIntStateOf
import cat.receptari.app.core.designsystem.CollapsingMasthead
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.selection.toggleable
import cat.receptari.app.core.designsystem.Rubric
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.History
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.Sprig
import cat.receptari.app.core.designsystem.FilterPill
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.MetaLine
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperModalBottomSheet
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import cat.receptari.app.core.designsystem.SectionLabel
import cat.receptari.app.core.designsystem.StarRating
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.RecipeSort
import cat.receptari.app.domain.model.RecipeSummary
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.domain.transfer.RecipeTransferArchive
import cat.receptari.app.ui.transfer.TransferMethodSheet
import cat.receptari.app.ui.transfer.sendRecipeTransferNearby
import cat.receptari.app.ui.transfer.shareRecipeTransfer
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LibraryRoute(
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onOpenTags: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val shareFailedMessage = stringResource(R.string.transfer_share_failed)
    var transferArchive by remember { mutableStateOf<RecipeTransferArchive?>(null) }

    BackHandler(enabled = state.isSelectionMode) {
        viewModel.onEvent(LibraryEvent.ClearSelection)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is LibraryEffect.ShareReady -> transferArchive = effect.archive
                LibraryEffect.ShareFailed -> snackbarHostState.showSnackbar(shareFailedMessage)
            }
        }
    }

    LibraryScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenRecipe = onOpenRecipe,
        onAddRecipe = onAddRecipe,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        onOpenTags = onOpenTags,
    )

    transferArchive?.let { archive ->
        TransferMethodSheet(
            onSendNearby = {
                transferArchive = null
                context.sendRecipeTransferNearby(archive)
                viewModel.onEvent(LibraryEvent.ClearSelection)
            },
            onShareWithOtherApps = {
                transferArchive = null
                context.shareRecipeTransfer(archive)
                viewModel.onEvent(LibraryEvent.ClearSelection)
            },
            onDismiss = { transferArchive = null },
        )
    }
}

/**
 * The library is the book's front page: the masthead, a search pill, one line saying what is
 * shown, and the recipes as cards. Everything that narrows or reorders the list lives in a
 * single sheet behind "Filtra". The Favourites tab is this same page with the favourites
 * filter pinned on and a plainer heading.
 *
 * On the front page the masthead folds into a running head as the list scrolls: emblem and
 * wordmark glide to the top-left of a compact bar ([CollapsingMasthead]). Favourites keeps a
 * plain pinned title.
 */
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onOpenTags: (() -> Unit)? = null,
) {
    var filtersOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var mastheadHeightPx by remember { mutableIntStateOf(0) }
    val showMasthead = !state.isSelectionMode && !state.isFavoritesPage
    val shareAction: @Composable RowScope.() -> Unit = {
        if (state.recipes.isNotEmpty()) {
            IconButton(onClick = { onEvent(LibraryEvent.StartSelection) }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.library_send_recipes),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    PaperScaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            when {
                state.isSelectionMode -> SelectionTopBar(state = state, onEvent = onEvent)
                state.isFavoritesPage -> PageTitle(title = stringResource(R.string.filter_favorites))
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    // The front page's masthead overlays the list and reserves its own room.
                    top = if (showMasthead) 0.dp else innerPadding.calculateTopPadding(),
                    // Clears the floating add button, so the last row can scroll above it.
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                if (state.isSelectionMode) {
                    item(key = "selection-help", contentType = "aside") {
                        Aside(
                            text = stringResource(R.string.library_selection_help),
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PageMargin, vertical = 12.dp),
                        )
                    }
                } else {
                    if (showMasthead) {
                        item(key = "masthead", contentType = "masthead") {
                            Spacer(
                                modifier = Modifier.height(
                                    with(LocalDensity.current) { mastheadHeightPx.toDp() },
                                ),
                            )
                        }
                    }
                    if (!state.isLibraryEmpty) {
                        libraryControls(state, onEvent, onOpenFilters = { filtersOpen = true })
                    }
                }

                when {
                    state.isLoading -> item(key = "loading", contentType = "loading") {
                        LoadingState()
                    }

                    state.isLibraryEmpty && state.isFavoritesPage -> item(key = "empty") {
                        EmptyState(
                            title = stringResource(R.string.favorites_empty_title),
                            body = stringResource(R.string.favorites_empty_body),
                            modifier = Modifier.fillParentMaxHeight(EmptyStateHeight),
                        )
                    }

                    state.isLibraryEmpty -> item(key = "empty") {
                        EmptyState(
                            title = stringResource(R.string.library_empty_title),
                            body = stringResource(R.string.library_empty_body),
                            modifier = Modifier.fillParentMaxHeight(EmptyStateHeight),
                            action = {
                                Button(onClick = onAddRecipe, shape = CircleShape) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Text(
                                        text = stringResource(R.string.library_add_recipe),
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            },
                        )
                    }

                    state.recipes.isEmpty() -> noResultsItem(state, onEvent)

                    else -> recipeItems(
                        recipes = state.recipes,
                        layout = state.listLayout,
                        selectionMode = state.isSelectionMode,
                        selectedIds = state.selectedRecipeIds,
                        onClick = { recipe ->
                            if (state.isSelectionMode) {
                                onEvent(LibraryEvent.ToggleSelection(recipe.id))
                            } else {
                                onOpenRecipe(recipe.id)
                            }
                        },
                        onToggleFavorite = { recipe ->
                            onEvent(LibraryEvent.ToggleFavorite(recipe.id, recipe.isFavorite))
                        },
                        itemModifier = Modifier.padding(horizontal = PageMargin),
                    )
                }
            }

            if (showMasthead) {
                CollapsingMasthead(
                    title = stringResource(R.string.library_title),
                    tagline = stringResource(R.string.library_tagline),
                    scrolledPx = {
                        if (listState.firstVisibleItemIndex == 0) {
                            listState.firstVisibleItemScrollOffset
                        } else {
                            Int.MAX_VALUE
                        }
                    },
                    onExpandedHeight = { if (it != mastheadHeightPx) mastheadHeightPx = it },
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    actions = shareAction,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }

    if (filtersOpen) {
        FilterSheet(
            state = state,
            onEvent = onEvent,
            onDismiss = { filtersOpen = false },
            onManageTags = onOpenTags,
        )
    }
}

/** A tab's own heading, for pages that are not the front page. */
@Composable
private fun PageTitle(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = PageMargin, end = PageMargin, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
        Sprig(
            modifier = Modifier
                .padding(start = 12.dp)
                .size(width = 64.dp, height = 32.dp),
        )
    }
}

@Composable
private fun SelectionTopBar(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    PaperTopBar(
        modifier = modifier,
        title = {
            Text(
                pluralStringResource(
                    R.plurals.library_selected_count,
                    state.selectedRecipeIds.size,
                    state.selectedRecipeIds.size,
                ),
            )
        },
        navigationIcon = {
            IconButton(onClick = { onEvent(LibraryEvent.ClearSelection) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.common_cancel),
                )
            }
        },
        actions = {
            IconButton(onClick = { onEvent(LibraryEvent.SelectAllVisible) }) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = stringResource(R.string.library_select_all_visible),
                )
            }
            IconButton(
                onClick = { onEvent(LibraryEvent.ShareSelection) },
                enabled = state.selectedRecipeIds.isNotEmpty() && !state.isSharingSelection,
            ) {
                if (state.isSharingSelection) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.library_send_selected),
                    )
                }
            }
        },
    )
}

/**
 * Search as a soft pill on the page, always there, as a book's index always is. Filters open
 * from its end, with a wax badge counting the ones that are on.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    filterCount: Int,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, ReceptariTheme.palette.rule.copy(alpha = 0.6f)),
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
                    .padding(start = 10.dp, top = 12.dp, bottom = 10.dp),
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
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = 1.dp, height = 22.dp)
                    .background(ReceptariTheme.palette.rule.copy(alpha = 0.6f)),
            )
            IconButton(onClick = onOpenFilters) {
                BadgedBox(
                    badge = {
                        if (filterCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) { Text(text = filterCount.toString()) }
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = if (filterCount > 0) {
                            stringResource(R.string.library_filter_active, filterCount)
                        } else {
                            stringResource(R.string.library_filter)
                        },
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** What the list shows, and how it is laid out. */
@Composable
private fun ListHeader(
    state: LibraryUiState,
    onSelectLayout: (RecipeListLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                R.string.library_count_and_sort,
                pluralStringResource(R.plurals.library_recipe_count, state.recipes.size, state.recipes.size),
                state.sort.label(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        LayoutToggle(
            selected = state.listLayout,
            onSelect = onSelectLayout,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Grid or index: a two-segment pill, the current layout inked in wax red. */
@Composable
private fun LayoutToggle(
    selected: RecipeListLayout,
    onSelect: (RecipeListLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The wax-red pill slides under the icons rather than each segment filling in on its own.
    val thumbX by animateDpAsState(
        targetValue = if (selected == RecipeListLayout.Grid) 0.dp else SegmentWidth,
        label = "layoutThumb",
    )
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, ReceptariTheme.palette.rule.copy(alpha = 0.7f), CircleShape)
            .selectableGroup()
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbX)
                .size(width = SegmentWidth, height = 30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Row {
            LayoutSegment(
                icon = Icons.Outlined.GridView,
                label = stringResource(R.string.library_layout_grid),
                selected = selected == RecipeListLayout.Grid,
                onClick = { onSelect(RecipeListLayout.Grid) },
            )
            LayoutSegment(
                icon = Icons.AutoMirrored.Outlined.ViewList,
                label = stringResource(R.string.library_layout_index),
                selected = selected == RecipeListLayout.Index,
                onClick = { onSelect(RecipeListLayout.Index) },
            )
        }
    }
}

private val SegmentWidth = 38.dp

@Composable
private fun LayoutSegment(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = SegmentWidth, height = 30.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "layoutSegmentIcon",
            ).value,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * The filters currently narrowing the list, each removable in place. Knowing what is on is
 * the most useful thing when the list comes back short, so it stays visible outside the sheet.
 */
@Composable
private fun ActiveFilters(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PageMargin),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val filter = state.filter
        if (filter.favoritesOnly && !state.isFavoritesPage) {
            RemovableFilter(stringResource(R.string.filter_favorites)) {
                onEvent(LibraryEvent.ToggleFavoritesFilter)
            }
        }
        if (filter.neverCooked) {
            RemovableFilter(stringResource(R.string.filter_never_cooked)) {
                onEvent(LibraryEvent.ToggleNeverCookedFilter)
            }
        }
        if (filter.cookedSinceEpochMillis != null) {
            RemovableFilter(stringResource(R.string.filter_recently_cooked)) {
                onEvent(LibraryEvent.ToggleRecentlyCookedFilter)
            }
        }
        state.availableTags.filter { it.id in filter.tagIds }.forEach { tag ->
            RemovableFilter(tag.name) { onEvent(LibraryEvent.ToggleTagFilter(tag.id)) }
        }
    }
}

@Composable
private fun RemovableFilter(label: String, onRemove: () -> Unit) {
    FilterPill(label = label, selected = true, onClick = onRemove, icon = Icons.Default.Close)
}

/**
 * The search field, the count-and-layout line and the removable filters, as one item: the
 * controls over a list of recipes, shared by the library and by a folder's page.
 */
internal fun LazyListScope.libraryControls(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onOpenFilters: () -> Unit,
) {
    item(key = "controls", contentType = "controls") {
        Column {
            SearchField(
                query = state.searchQuery,
                onQueryChange = { onEvent(LibraryEvent.SearchQueryChanged(it)) },
                filterCount = state.userFilterCount,
                onOpenFilters = onOpenFilters,
                modifier = Modifier.padding(horizontal = PageMargin),
            )
            ListHeader(
                state = state,
                onSelectLayout = { onEvent(LibraryEvent.ListLayoutSelected(it)) },
                modifier = Modifier.padding(horizontal = PageMargin),
            )
            if (state.userFilterCount > 0) {
                ActiveFilters(state = state, onEvent = onEvent)
            }
        }
    }
}

/** Nothing matched the search or the filters: say so, and offer to clear the filters. */
internal fun LazyListScope.noResultsItem(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
) {
    item(key = "no-results") {
                EmptyState(
                    title = stringResource(R.string.library_no_results_title),
                    body = stringResource(R.string.library_no_results_body),
                    modifier = Modifier.fillParentMaxHeight(EmptyStateHeight),
                    action = if (state.userFilterCount > 0) {
                        {
                            TextButton(onClick = { onEvent(LibraryEvent.ClearFilters) }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                                Text(
                                    text = stringResource(R.string.library_clear_filters),
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    } else {
                        null
                    },
                )
    
    }
}

/**
 * Sort, state filters and tags together: everything that changes the list, in one place.
 *
 * The title and the way out stay put while the choices scroll between them. Every choice
 * applies at once, and the button at the foot says what the list now holds, so the reader
 * sees the effect of a filter without closing the sheet to find out. Each kind of choice has
 * its own control: sort is a menu, the state filters are switches on ruled lines, and tags
 * are a checklist.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FilterSheet(
    state: LibraryUiState,
    onEvent: (LibraryEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onManageTags: (() -> Unit)? = null,
) {
    PaperModalBottomSheet(modifier = modifier, onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            OrnamentHeading(
                title = stringResource(R.string.library_filter_title),
                trailing = if (state.userFilterCount > 0) {
                    {
                        TextButton(onClick = { onEvent(LibraryEvent.ClearFilters) }) {
                            Text(stringResource(R.string.library_filters_clear))
                        }
                    }
                } else {
                    null
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                FilterGroup(label = stringResource(R.string.library_sort)) {
                    SortPicker(
                        selected = state.sort,
                        onSelect = { onEvent(LibraryEvent.SortChanged(it)) },
                    )
                }

                FilterGroup(label = stringResource(R.string.library_filter_show)) {
                    if (!state.isFavoritesPage) {
                        SwitchRow(
                            label = stringResource(R.string.filter_favorites),
                            icon = Icons.Default.FavoriteBorder,
                            checked = state.filter.favoritesOnly,
                            onToggle = { onEvent(LibraryEvent.ToggleFavoritesFilter) },
                        )
                    }
                    SwitchRow(
                        label = stringResource(R.string.filter_never_cooked),
                        icon = Icons.Outlined.Restaurant,
                        checked = state.filter.neverCooked,
                        onToggle = { onEvent(LibraryEvent.ToggleNeverCookedFilter) },
                    )
                    SwitchRow(
                        label = stringResource(R.string.filter_recently_cooked),
                        icon = Icons.Outlined.History,
                        checked = state.filter.cookedSinceEpochMillis != null,
                        onToggle = { onEvent(LibraryEvent.ToggleRecentlyCookedFilter) },
                    )
                }

                if (state.availableTags.isNotEmpty()) {
                    FilterGroup(label = stringResource(R.string.filter_tags)) {
                        FlowRow(
                            maxItemsInEachRow = 2,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            state.availableTags.sortedBy { it.name.lowercase() }.forEach { tag ->
                                TagCheck(
                                    label = tag.name,
                                    checked = tag.id in state.filter.tagIds,
                                    onToggle = { onEvent(LibraryEvent.ToggleTagFilter(tag.id)) },
                                )
                            }
                        }
                        onManageTags?.let { manage ->
                            TextButton(onClick = { onDismiss(); manage() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Tag,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = stringResource(R.string.tags_manage),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            Hairline()
            Button(
                onClick = onDismiss,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(52.dp),
            ) {
                Text(
                    text = if (state.recipes.isEmpty()) {
                        stringResource(R.string.library_filter_apply_none)
                    } else {
                        pluralStringResource(R.plurals.library_filter_apply, state.recipes.size, state.recipes.size)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** A rubric over a group of controls. */
@Composable
private fun FilterGroup(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        Rubric(text = label, modifier = Modifier.padding(bottom = 4.dp))
        content()
    }
}

/** The current sort, written out on a ruled line, opening a menu of the others. */
@Composable
private fun SortPicker(
    selected: RecipeSort,
    onSelect: (RecipeSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().clickable(role = Role.DropdownList) { open = true }) {
            Row(
                modifier = Modifier.heightIn(min = 52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selected.label(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Hairline()
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            RecipeSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                    trailingIcon = {
                        if (option == selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}

/** One on/off filter: an icon, its name, and a wax-red switch, on a ruled line. */
@Composable
private fun SwitchRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rule = ReceptariTheme.palette.rule
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
                .heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            )
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = rule,
                    uncheckedTrackColor = Color.Transparent,
                    uncheckedBorderColor = rule,
                ),
            )
        }
        Hairline()
    }
}

/** One tag in the checklist; two share a line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowScope.TagCheck(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 44.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = ReceptariTheme.palette.rule,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/**
 * One recipe as a line of the book's index: a rounded-square photo, the title in the display
 * face, and under it the rating, time and yield. Rows are separated by a hairline, not boxed.
 */
@Composable
private fun RecipeRow(
    recipe: RecipeSummary,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecipePlate(imagePath = recipe.imagePath, size = PlateSize)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Zero or negative servings would be a broken import, not "for nobody".
                val servings = recipe.baseServings?.takeIf { it > 0 }
                val minutes = recipe.effectiveTimeMinutes
                val parts = buildList {
                    minutes?.let { add(stringResource(R.string.common_minutes_short, it)) }
                    servings?.let { add(pluralStringResource(R.plurals.common_servings, it, it)) }
                }
                if (recipe.rating != null || parts.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        recipe.rating?.let { rating ->
                            StarRating(
                                rating = rating,
                                contentDescription = stringResource(R.string.common_rating_value, rating),
                                starSize = 12.dp,
                            )
                        }
                        MetaLine(parts = parts)
                    }
                }
            }

            if (selectionMode) {
                Icon(
                    imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(start = 12.dp),
                )
            } else {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.padding(start = 2.dp)) {
                    Icon(
                        imageVector = if (recipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(
                            if (recipe.isFavorite) R.string.detail_favorite_remove else R.string.detail_favorite_add,
                        ),
                        tint = if (recipe.isFavorite) {
                            ReceptariTheme.palette.heart
                        } else {
                            ReceptariTheme.palette.rule
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Hairline()
    }
}

private val PlateSize = 60.dp

/**
 * The recipes of a list in the reader's chosen layout: grid lines of [RecipeTileRow], or
 * index lines of [RecipeRow]. [itemModifier] goes on every line (page margins, say).
 */
internal fun LazyListScope.recipeItems(
    recipes: List<RecipeSummary>,
    layout: RecipeListLayout,
    onClick: (RecipeSummary) -> Unit,
    onToggleFavorite: (RecipeSummary) -> Unit,
    itemModifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
) {
    when (layout) {
        RecipeListLayout.Grid -> items(
            items = recipes.chunked(TileColumns),
            key = { it.first().id },
            contentType = { "recipe-tiles" },
        ) { line ->
            RecipeTileRow(
                recipes = line,
                onClick = onClick,
                onToggleFavorite = onToggleFavorite,
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                modifier = itemModifier,
            )
        }

        RecipeListLayout.Index -> items(recipes, key = { it.id }, contentType = { "recipe-row" }) { recipe ->
            RecipeRow(
                recipe = recipe,
                onClick = { onClick(recipe) },
                onToggleFavorite = { onToggleFavorite(recipe) },
                selectionMode = selectionMode,
                selected = recipe.id in selectedIds,
                modifier = itemModifier,
            )
        }
    }
}

/**
 * One line of the two-column photo grid: up to two [RecipeTile]s side by side, the second
 * slot left empty on an odd last line so the tile keeps its width.
 */
@Composable
private fun RecipeTileRow(
    recipes: List<RecipeSummary>,
    onClick: (RecipeSummary) -> Unit,
    onToggleFavorite: (RecipeSummary) -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = TileRowGap),
        horizontalArrangement = Arrangement.spacedBy(TileGap),
    ) {
        recipes.forEach { recipe ->
            RecipeTile(
                recipe = recipe,
                onClick = { onClick(recipe) },
                onToggleFavorite = { onToggleFavorite(recipe) },
                selectionMode = selectionMode,
                selected = recipe.id in selectedIds,
                modifier = Modifier.weight(1f),
            )
        }
        repeat(TileColumns - recipes.size) { Spacer(modifier = Modifier.weight(1f)) }
    }
}

/**
 * One recipe as a grid tile: a landscape photograph carrying a bare heart and the time and
 * rating, and one line of title under it in the display face.
 */
@Composable
private fun RecipeTile(
    recipe: RecipeSummary,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false,
) {
    Column(
        modifier = modifier.clickable(
            interactionSource = null,
            indication = null,
            onClick = onClick,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(TilePhotoRatio)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick),
        ) {
            if (recipe.imagePath == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, ReceptariTheme.palette.rule.copy(alpha = 0.6f), MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    Sprig(modifier = Modifier.size(width = 60.dp, height = 32.dp))
                }
            } else {
                AsyncImage(
                    model = recipe.imagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // On a photograph, a faint scrim top and bottom keeps the bare heart and the facts
            // legible without boxing them; on the empty plate they take the ink colours.
            val onPhoto = recipe.imagePath != null
            if (onPhoto) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.28f),
                                0.35f to Color.Transparent,
                                0.6f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.5f),
                            ),
                        ),
                )
            }

            TileFacts(
                recipe = recipe,
                onPhoto = onPhoto,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )

            if (selectionMode) {
                Icon(
                    imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f), CircleShape),
                )
            } else {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = if (recipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(
                            if (recipe.isFavorite) R.string.detail_favorite_remove else R.string.detail_favorite_add,
                        ),
                        tint = when {
                            onPhoto -> Color.White // filled when liked, outlined when not
                            recipe.isFavorite -> ReceptariTheme.palette.heart
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Text(
            text = recipe.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * The tile's facts, printed straight on the photo's bottom-left corner: the time (or the
 * yield, when there is no time) and the rating. Nothing is drawn when there is neither.
 */
@Composable
private fun TileFacts(recipe: RecipeSummary, onPhoto: Boolean, modifier: Modifier = Modifier) {
    val minutes = recipe.effectiveTimeMinutes
    // Zero or negative servings would be a broken import, not "for nobody".
    val servings = recipe.baseServings?.takeIf { it > 0 }
    val fact = when {
        minutes != null -> stringResource(R.string.common_minutes_short, minutes)
        servings != null -> pluralStringResource(R.plurals.common_servings, servings, servings)
        else -> null
    }
    if (fact == null && recipe.rating == null) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        fact?.let {
            MetaLine(
                parts = listOf(it),
                color = if (onPhoto) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        recipe.rating?.let { rating ->
            StarRating(
                rating = rating,
                contentDescription = stringResource(R.string.common_rating_value, rating),
                starSize = 10.dp,
            )
        }
    }
}

private const val TileColumns = 2
private val TileGap = 8.dp
private val TileRowGap = 10.dp
private const val TilePhotoRatio = 4f / 3f

/** The photograph in a rounded square, like a grid tile — or, without one, an empty plate with a sprig. */
@Composable
private fun RecipePlate(imagePath: String?, modifier: Modifier = Modifier, size: Dp = PlateSize) {
    val shape = MaterialTheme.shapes.medium
    val frame = modifier
        .size(size)
        .clip(shape)

    if (imagePath == null) {
        Box(
            modifier = frame
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, ReceptariTheme.palette.rule.copy(alpha = 0.6f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Sprig(modifier = Modifier.size(width = size * 0.6f, height = size * 0.32f))
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

/** Empty plates and ruled bars where the recipes will land. */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = PageMargin, vertical = 4.dp),
    ) {
        repeat(SkeletonRowCount) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(PlateSize)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBar(widthFraction = 0.7f, height = 14.dp)
                    SkeletonBar(widthFraction = 0.45f, height = 10.dp)
                }
            }
            Hairline()
        }
    }
}

@Composable
private fun SkeletonBar(widthFraction: Float, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .background(ReceptariTheme.palette.rule.copy(alpha = 0.28f)),
    )
}

@Composable
private fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Aside(
            text = body,
            modifier = Modifier.padding(top = 10.dp),
        )
        action?.let {
            Spacer(modifier = Modifier.height(20.dp))
            it()
        }
    }
}

private val PageMargin = 20.dp

private const val EmptyStateHeight = 0.6f
private const val SkeletonRowCount = 5

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
                    RecipeSummary(
                        id = "2",
                        title = "Crema catalana",
                        cookTimeMinutes = 25,
                        baseServings = 6,
                        rating = 5,
                    ),
                    RecipeSummary(id = "4", title = "Escudella"),
                ),
                availableTags = listOf(Tag(id = "t1", name = "Postres")),
                isLoading = false,
            ),
            onEvent = {},
            onOpenRecipe = {},
            onAddRecipe = {},
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
        )
    }
}
