package cat.receptari.app.ui.detail

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.CardRow
import cat.receptari.app.core.designsystem.CardRowDivider
import cat.receptari.app.core.designsystem.Emblem
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.IconDisc
import cat.receptari.app.core.designsystem.MetaLine
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.Rubric
import cat.receptari.app.core.designsystem.SegmentedTabs
import cat.receptari.app.core.designsystem.Sprig
import cat.receptari.app.core.designsystem.ingredientLine
import cat.receptari.app.core.designsystem.paperGrain
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.scaling.ScaledIngredientSection
import cat.receptari.app.domain.timer.InstructionDurationParser
import cat.receptari.app.ui.folders.toColor
import cat.receptari.app.ui.folders.toImageVector
import coil3.compose.AsyncImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

/*
 * The recipe page, top to bottom:
 *
 *   the photo as the page's header, edge to edge under the status bar, with the folder,
 *   the title and one quiet line (stars · time · servings) printed on it
 *   Ingredients | Preparació, pinned under the top bar once it reaches it
 *   the open tab: plain ruled ingredient lines, or the numbered method
 *   notes, a card about the recipe (folder, tags, source, history), and a colophon
 *
 * The top bar floats over the photo in cream and folds into a paper bar, carrying the title,
 * as the photo scrolls away. Without a photo the same head is printed on paper under the bar.
 * Everything that tracks scrolling is read in the layout or draw phase; only two booleans
 * (title in the bar, icons on the photo) recompose.
 */

internal val PagePadding = 20.dp
internal val DetailBarHeight = 56.dp

private val HeroPhotoHeight = 250.dp
private val SheetRise = 20.dp
private val BarSheetFade = 56.dp

/** The photo less a rounded-top sheet of [SheetRise] at its foot. */
private val PageRiseShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val rise = with(density) { SheetRise.toPx() }
        val photo = Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }
        val sheet = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, size.height - rise, size.width, size.height + rise),
                    cornerRadius = CornerRadius(rise),
                ),
            )
        }
        return Outline.Generic(Path().apply { op(photo, sheet, PathOperation.Difference) })
    }
}
private val PaperFold = 72.dp
private val TabsSlot = 60.dp
private val StepDisc = 32.dp

private const val TabsKey = "tabs"
private const val TabsIndex = 1
private const val IngredientsTab = 0

internal data class TimerSetupRequest(
    val stepId: String?,
    val label: String,
    val initialMinutes: Int?,
)

@Composable
internal fun RecipeContent(
    state: RecipeDetailUiState,
    listState: LazyListState,
    bottomPadding: Dp,
    onEvent: (RecipeDetailEvent) -> Unit,
    onSetTimer: (TimerSetupRequest) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val recipe = state.recipe ?: return

    var showFolderSheet by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(IngredientsTab) }
    // Ticking off ingredients is a scratch mark for this session at the stove, not data.
    val checked = remember(recipe.id) { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val barPx = with(density) { (statusTop + DetailBarHeight).roundToPx() }
    val hasPhoto = state.imagePath != null
    var heroPx by remember { mutableIntStateOf(0) }
    // How far the page scrolls before the bar is fully paper: the photo's height less the
    // bar's, or a short fade when the head is printed on paper.
    val foldPx = if (hasPhoto) {
        (heroPx - barPx).coerceAtLeast(1)
    } else {
        with(density) { PaperFold.roundToPx() }
    }
    val progress: () -> Float = {
        if (listState.firstVisibleItemIndex > 0) {
            1f
        } else {
            (listState.firstVisibleItemScrollOffset / foldPx.toFloat()).coerceIn(0f, 1f)
        }
    }
    // Over a photo the bar's paper waits until the page's sheet is almost under it, then comes
    // in quickly: a half-clear paper band over the photo reads as milk, not as paper.
    val sheetFadePx = with(density) { BarSheetFade.roundToPx() }
    val sheetAlpha: () -> Float = {
        val start = (1f - sheetFadePx / foldPx.toFloat()).coerceAtLeast(0f)
        if (hasPhoto) ((progress() - start) / (1f - start)).coerceIn(0f, 1f) else progress()
    }
    val titleInBar by remember(foldPx) { derivedStateOf { progress() >= 0.98f } }
    val onPhoto by remember(foldPx, hasPhoto) { derivedStateOf { hasPhoto && sheetAlpha() < 0.5f } }

    // Where the tabs' slot in the list sits; the real tabs ride on it and stop under the bar.
    val tabsSlotPx = with(density) { TabsSlot.roundToPx() }
    val tabsTop: () -> Int = {
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == TabsKey }?.offset
            ?: if (listState.firstVisibleItemIndex > TabsIndex) Int.MIN_VALUE else Int.MAX_VALUE / 4
    }

    val tabsPinned: () -> Boolean = { tabsTop() <= barPx }
    val onSelectTab: (Int) -> Unit = { index ->
        if (index != selectedTab) {
            selectedTab = index
            // Pinned and deep in the other tab: open the new one at its top.
            if (tabsPinned()) {
                scope.launch { listState.scrollToItem(TabsIndex, scrollOffset = -barPx) }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding + 24.dp),
        ) {
            item(key = "hero") {
                val heroModifier = Modifier.onSizeChanged { heroPx = it.height }
                if (state.imagePath != null) {
                    PhotoHero(
                        // The printed head fades before it can slide under the bar's icons.
                        headAlpha = { 1f - ((progress() - 0.15f) / 0.3f).coerceIn(0f, 1f) },
                        recipe = recipe,
                        imagePath = state.imagePath,
                        servings = state.servings,
                        onSetRating = { onEvent(RecipeDetailEvent.SetRating(it)) },
                        onOpenFolderSheet = { showFolderSheet = true },
                        modifier = heroModifier,
                    )
                } else {
                    PaperHero(
                        recipe = recipe,
                        servings = state.servings,
                        onSetRating = { onEvent(RecipeDetailEvent.SetRating(it)) },
                        onOpenFolderSheet = { showFolderSheet = true },
                        modifier = heroModifier,
                    )
                }
            }

            // The tabs live in the list so they ride its overscroll stretch; a twin takes over
            // under the bar once they pin.
            item(key = TabsKey) {
                RecipeTabs(
                    selectedIndex = selectedTab,
                    onSelect = onSelectTab,
                    modifier = Modifier.graphicsLayer { alpha = if (tabsPinned()) 0f else 1f },
                )
            }

            if (selectedTab == IngredientsTab) {
                ingredientItems(state, onEvent, checked) { id ->
                    if (!checked.remove(id)) checked.add(id)
                }
            } else {
                methodItems(recipe, onSetTimer)
            }

            recipe.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                item(key = "notes") { NotesCard(notes = notes) }
            }

            item(key = "about") {
                AboutCard(
                    recipe = recipe,
                    onOpenFolderSheet = { showFolderSheet = true },
                    onOpenTagSheet = { showTagSheet = true },
                )
            }

            item(key = "colophon") { Colophon() }
        }

        // One sheet of paper behind the bar and, once they pin, the tabs: no seam between them.
        BarSheet(
            progress = sheetAlpha,
            extraPx = { if (tabsPinned()) tabsSlotPx else 0 },
        )

        // Rides on the list's tabs (so taps land the same) and shows only once pinned.
        RecipeTabs(
            selectedIndex = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier
                .offset { IntOffset(0, maxOf(barPx, tabsTop())) }
                .graphicsLayer { alpha = if (tabsPinned()) 1f else 0f },
        )

        DetailTopBar(
            title = recipe.title,
            titleVisible = titleInBar,
            onPhoto = onPhoto,
            onNavigateBack = onNavigateBack,
            actions = actions,
        )
    }

    if (showFolderSheet) {
        QuickFolderSheet(
            currentFolder = recipe.folder,
            availableFolders = state.availableFolders,
            onDismiss = { showFolderSheet = false },
            onSelectFolder = { folder ->
                onEvent(RecipeDetailEvent.SetFolder(folder))
                showFolderSheet = false
            },
            onCreateFolder = { name, color, icon ->
                onEvent(RecipeDetailEvent.FolderCreateRequested(name, color, icon))
                showFolderSheet = false
            },
        )
    }

    if (showTagSheet) {
        QuickTagSheet(
            currentTags = recipe.tags.map { it.name },
            availableTags = state.availableTags.map { it.name },
            onDismiss = { showTagSheet = false },
            onAddTag = { tag -> onEvent(RecipeDetailEvent.AddTag(tag)) },
            onRemoveTag = { tag -> onEvent(RecipeDetailEvent.RemoveTag(tag)) },
        )
    }
}

// --- Top bar -------------------------------------------------------------------------

/**
 * The bar's icons and title, with no paper of its own: [BarSheet] is laid behind it. Over the
 * photo its icons are cream; as the photo scrolls away they turn to ink and the title takes
 * the middle.
 */
@Composable
internal fun DetailTopBar(
    title: String?,
    titleVisible: Boolean,
    onPhoto: Boolean,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val ink by animateColorAsState(
        targetValue = if (onPhoto) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "bar-ink",
    )
    val titleAlpha by animateFloatAsState(if (titleVisible) 1f else 0f, label = "bar-title")
    // The app pins dark status-bar icons for paper (ADR-009); over the photo they turn light.
    val view = LocalView.current
    DisposableEffect(onPhoto) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = !onPhoto
        onDispose { controller?.isAppearanceLightStatusBars = true }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalContentColor provides ink) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(DetailBarHeight)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                    )
                }
                Text(
                    text = title.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .graphicsLayer { alpha = titleAlpha },
                )
                actions()
            }
        }
    }
}

/**
 * The paper behind [DetailTopBar], faded in by [progress] and ruled at its foot. It reaches
 * [extraPx] below the bar so pinned content shares the bar's sheet. Drawn from the screen's
 * top edge, its grain lines up with the page's.
 */
@Composable
internal fun BarSheet(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    extraPx: () -> Int = { 0 },
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val height = (statusTop + DetailBarHeight).roundToPx() + extraPx()
                val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .graphicsLayer { alpha = progress() }
            .background(MaterialTheme.colorScheme.background)
            .paperGrain(intensity = 1f),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        // Ruled only once the paper is whole, never as a line across a fading band.
        Hairline(modifier = Modifier.graphicsLayer { alpha = if (progress() >= 1f) 1f else 0f })
    }
}

// --- Head ----------------------------------------------------------------------------

/**
 * The photo as the page's header, edge to edge under the status bar. A scrim at the top keeps
 * the bar's icons legible; one at the foot carries the folder, the title and the meta line.
 */
@Composable
private fun PhotoHero(
    headAlpha: () -> Float,
    recipe: Recipe,
    imagePath: String,
    servings: Int?,
    onSetRating: (Int?) -> Unit,
    onOpenFolderSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusTop + HeroPhotoHeight),
    ) {
        // The page rises over the photo's foot as a sheet with rounded shoulders. The photo is
        // cut away rather than paper painted on it, so the page's own grain runs on unbroken.
        Box(modifier = Modifier.fillMaxSize().clip(PageRiseShape)) {
            AsyncImage(
                model = imagePath,
                contentDescription = stringResource(R.string.common_recipe_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.18f to Color.Black.copy(alpha = 0.3f),
                        0.38f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.68f),
                    ),
                ),
            )
        }
        HeadText(
            recipe = recipe,
            servings = servings,
            ink = MaterialTheme.colorScheme.onPrimary,
            onSetRating = onSetRating,
            onOpenFolderSheet = onOpenFolderSheet,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .graphicsLayer { alpha = headAlpha() }
                .padding(start = PagePadding, end = PagePadding, bottom = SheetRise + 8.dp),
        )
    }
}

/** The same head printed on paper, under the bar, for a recipe without a photo. */
@Composable
private fun PaperHero(
    recipe: Recipe,
    servings: Int?,
    onSetRating: (Int?) -> Unit,
    onOpenFolderSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    HeadText(
        recipe = recipe,
        servings = servings,
        ink = MaterialTheme.colorScheme.onSurface,
        onSetRating = onSetRating,
        onOpenFolderSheet = onOpenFolderSheet,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PagePadding, end = PagePadding, top = statusTop + DetailBarHeight, bottom = 4.dp),
    )
}

/** Folder rubric (tap to refile), the title, and the quiet line: stars · time · servings. */
@Composable
private fun HeadText(
    recipe: Recipe,
    servings: Int?,
    ink: Color,
    onSetRating: (Int?) -> Unit,
    onOpenFolderSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val time = recipe.totalTimeMinutes ?: recipe.derivedTotalMinutes
    val parts = listOfNotNull(
        time?.let { formatDuration(it) },
        servings?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.common_servings, it, it) },
    )
    Column(modifier = modifier) {
        recipe.folder?.let { folder ->
            Rubric(
                text = folder.name,
                color = ink.copy(alpha = 0.85f),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onOpenFolderSheet)
                    .padding(vertical = 2.dp),
            )
        }
        Text(text = recipe.title, style = MaterialTheme.typography.headlineMedium, color = ink)
        Row(
            modifier = Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RatingStars(
                rating = recipe.rating,
                onSetRating = onSetRating,
                // Pull the touch targets back so the first star lines up with the title.
                modifier = Modifier.offset(x = (-6).dp),
            )
            if (parts.isNotEmpty()) {
                Aside(
                    text = "·", // i18n-exempt: list separator, not text
                    color = ink.copy(alpha = 0.9f),
                    modifier = Modifier.padding(end = 6.dp),
                )
                MetaLine(parts = parts, color = ink.copy(alpha = 0.9f))
            }
        }
    }
}

/** Five small gold stars; tapping one sets the rating, tapping the current one clears it. */
@Composable
private fun RatingStars(
    rating: Int?,
    onSetRating: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gold = ReceptariTheme.palette.gold
    Row(modifier = modifier) {
        (1..5).forEach { star ->
            val filled = rating != null && star <= rating
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onSetRating(if (rating == star) null else star) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (filled) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = stringResource(R.string.common_rating_value, star),
                    tint = if (filled) gold else gold.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** The two tab names on the wax switch; the bar's [BarSheet] grows behind them once pinned. */
@Composable
private fun RecipeTabs(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().height(TabsSlot)) {
        SegmentedTabs(
            options = listOf(
                stringResource(R.string.detail_ingredients),
                stringResource(R.string.detail_instructions),
            ),
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = PagePadding),
        )
    }
}

@Composable
private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0 -> stringResource(R.string.detail_duration_minutes, remainder)
        remainder == 0 -> stringResource(R.string.detail_duration_hours, hours)
        else -> stringResource(R.string.detail_duration_hours_minutes, hours, remainder)
    }
}

// --- Ingredients ---------------------------------------------------------------------

private fun LazyListScope.ingredientItems(
    state: RecipeDetailUiState,
    onEvent: (RecipeDetailEvent) -> Unit,
    checked: List<String>,
    onToggle: (String) -> Unit,
) {
    state.servings?.takeIf { it > 0 }?.let { servings ->
        item(key = "servings") {
            ServingsLine(
                servings = servings,
                isScaled = state.isScaled,
                isScalable = state.recipe?.isScalable == true,
                onEvent = onEvent,
            )
        }
    }
    state.ingredientSections.forEach { section ->
        item(key = "isec-${section.id}") {
            IngredientGroup(section = section, checked = checked, onToggle = onToggle)
        }
    }
}

/** How many it serves, on one slim line with the small stepper that scales the quantities below. */
@Composable
private fun ServingsLine(
    servings: Int,
    isScaled: Boolean,
    isScalable: Boolean,
    onEvent: (RecipeDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = if (isScaled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PagePadding + 4.dp, end = PagePadding, top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.People,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = pluralStringResource(R.plurals.common_servings, servings, servings),
            style = MaterialTheme.typography.bodyMedium,
            color = ink,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        if (isScaled) {
            IconButton(onClick = { onEvent(RecipeDetailEvent.ResetServings) }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                    contentDescription = stringResource(R.string.detail_servings_reset),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        if (isScalable) {
            StepperButton(
                icon = Icons.Default.Remove,
                description = stringResource(R.string.detail_servings_decrease),
                onClick = { onEvent(RecipeDetailEvent.DecreaseServings) },
                modifier = Modifier.padding(start = 6.dp),
            )
            StepperButton(
                icon = Icons.Default.Add,
                description = stringResource(R.string.detail_servings_increase),
                onClick = { onEvent(RecipeDetailEvent.IncreaseServings) },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** One ingredient group: its name as a rubric, then its lines ruled on the page. */
@Composable
private fun IngredientGroup(
    section: ScaledIngredientSection,
    checked: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = PagePadding)) {
        section.name?.let { name ->
            Rubric(text = name, modifier = Modifier.padding(top = 14.dp, bottom = 0.dp))
        }
        section.ingredients.forEach { ingredient ->
            IngredientRow(
                // The source's own line, scaled — never re-typeset here.
                text = ingredientLine(ingredient),
                checked = ingredient.id in checked,
                onToggle = { onToggle(ingredient.id) },
            )
            Hairline(alpha = 0.35f)
        }
    }
}

/** A round tick and the line; ticking strikes the line through and fades it. */
@Composable
private fun IngredientRow(
    text: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wax = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(vertical = 7.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(if (checked) wax else Color.Transparent)
                .border(1.5.dp, if (checked) wax else ReceptariTheme.palette.rule, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
    }
}

// --- Method --------------------------------------------------------------------------

private fun LazyListScope.methodItems(
    recipe: Recipe,
    onSetTimer: (TimerSetupRequest) -> Unit,
) {
    item(key = "timer-any") {
        TimerChip(
            text = stringResource(R.string.timer_set),
            onClick = { onSetTimer(TimerSetupRequest(stepId = null, label = recipe.title, initialMinutes = null)) },
            modifier = Modifier.padding(start = PagePadding, top = 8.dp, bottom = 8.dp),
        )
    }

    // Step numbers run on across sections (PRD §3.4); each section opens with its rubric.
    var number = 0
    recipe.instructionSections.forEach { section ->
        section.name?.let { name ->
            item(key = "ssec-${section.id}") {
                Rubric(
                    text = name,
                    modifier = Modifier.padding(start = PagePadding, end = PagePadding, top = 18.dp, bottom = 6.dp),
                )
            }
        }
        section.steps.forEachIndexed { index, step ->
            number += 1
            val stepNumber = number
            val isLast = index == section.steps.lastIndex
            item(key = "step-${step.id}") {
                val label = stringResource(R.string.timer_step_label, stepNumber, recipe.title)
                // A timer only belongs on a step that names a duration.
                val minutes = if (InstructionDurationParser.containsDuration(step.text)) {
                    InstructionDurationParser.parseMinutes(step.text) ?: 0
                } else {
                    null
                }
                StepRow(
                    number = stepNumber,
                    text = step.text,
                    showRule = !isLast,
                    timerMinutes = minutes,
                    onSetTimer = {
                        onSetTimer(
                            TimerSetupRequest(stepId = step.id, label = label, initialMinutes = minutes?.takeIf { it > 0 }),
                        )
                    },
                )
            }
        }
    }
}

/**
 * One step: its numeral pressed into a wax disc in the margin, the text beside it, and a
 * timer chip when the step names a duration ([timerMinutes] 0 when it names one we cannot read).
 */
@Composable
private fun StepRow(
    number: Int,
    text: String,
    showRule: Boolean,
    timerMinutes: Int?,
    onSetTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = PagePadding)) {
        Row(modifier = Modifier.padding(vertical = 14.dp)) {
            Box(
                modifier = Modifier
                    .size(StepDisc)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(), // i18n-exempt: a numeral, not a phrase
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp, top = 4.dp)) {
                Text(text = text, style = MaterialTheme.typography.bodyLarge)
                timerMinutes?.let { minutes ->
                    TimerChip(
                        text = if (minutes > 0) formatDuration(minutes) else stringResource(R.string.timer_set),
                        description = stringResource(R.string.timer_for_step, number),
                        onClick = onSetTimer,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
        if (showRule) Hairline(modifier = Modifier.padding(start = StepDisc + 16.dp), alpha = 0.35f)
    }
}

/** A wax-outlined pill with a timer glyph: what starts a timer on this page. */
@Composable
private fun TimerChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// --- Foot of the page ----------------------------------------------------------------

/** The cook's own notes, under a heading, on a card of their own. */
@Composable
private fun NotesCard(notes: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = PagePadding).padding(top = 28.dp)) {
        OrnamentHeading(
            title = stringResource(R.string.detail_notes),
            icon = Icons.Outlined.EditNote,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        PaperCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
            )
        }
    }
}

/**
 * Where the recipe sits in the book and where it came from, as the settings page's rows:
 * folder and tags lead to their sheets, the source opens its page, and the history is stated.
 */
@Composable
private fun AboutCard(
    recipe: Recipe,
    onOpenFolderSheet: () -> Unit,
    onOpenTagSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val folder = recipe.folder
    val tags = recipe.tags.joinToString("  ") { "#${it.name}" } // i18n-exempt: tag names
    val source = recipe.sourceName ?: recipe.sourceUrl
    val sourceUrl = recipe.sourceUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

    PaperCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding)
            .padding(top = 28.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column {
            CardRow(
                icon = folder?.icon?.toImageVector() ?: Icons.Outlined.Folder,
                title = folder?.name ?: stringResource(R.string.detail_add_folder),
                onClick = onOpenFolderSheet,
                discContainer = folder?.color?.toColor()?.copy(alpha = 0.16f)
                    ?: MaterialTheme.colorScheme.primaryContainer,
                discContent = folder?.color?.toColor() ?: MaterialTheme.colorScheme.onPrimaryContainer,
            )
            CardRowDivider()
            CardRow(
                icon = Icons.AutoMirrored.Outlined.Label,
                title = tags.ifEmpty { stringResource(R.string.detail_add_tag) },
                onClick = onOpenTagSheet,
            )
            source?.let {
                CardRowDivider()
                CardRow(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = stringResource(R.string.detail_source_line, it),
                    onClick = if (sourceUrl != null) ({ runCatching { uriHandler.openUri(sourceUrl) } }) else null,
                )
            }
            CardRowDivider()
            CardRow(
                icon = Icons.Outlined.History,
                title = if (recipe.cookCount == 0) {
                    stringResource(R.string.detail_never_cooked)
                } else {
                    pluralStringResource(R.plurals.detail_cook_count, recipe.cookCount, recipe.cookCount)
                },
                subtitle = recipe.lastCookedAt?.let { stringResource(R.string.detail_last_cooked, formatDate(it)) },
            )
        }
    }
}

@Composable
private fun formatDate(instant: Instant): String = remember(instant) {
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(instant)
}

/** The printer's mark that closes the page: the book's emblem between two sprigs. */
@Composable
private fun Colophon(modifier: Modifier = Modifier, sprig: Dp = 44.dp) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 36.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Sprig(mirrored = true, modifier = Modifier.size(width = sprig, height = 26.dp))
        Emblem(modifier = Modifier.size(34.dp).padding(horizontal = 2.dp))
        Sprig(modifier = Modifier.size(width = sprig, height = 26.dp))
    }
}
