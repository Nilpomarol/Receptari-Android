package cat.receptari.app.ui.detail

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.FilterPill
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.OrnamentalDivider
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import cat.receptari.app.core.designsystem.SectionLabel
import cat.receptari.app.core.designsystem.StarRating
import cat.receptari.app.core.designsystem.ingredientLine
import cat.receptari.app.core.designsystem.pageFrame
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.model.IngredientSection
import cat.receptari.app.domain.model.InstructionSection
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.Step
import cat.receptari.app.domain.scaling.ServingScaler
import cat.receptari.app.domain.timer.InstructionDurationParser
import cat.receptari.app.ui.timer.ActiveTimerDock
import cat.receptari.app.ui.timer.TimerSetupSheet
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun RecipeDetailRoute(
    onNavigateBack: () -> Unit,
    onEditRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val cookedMessage = stringResource(R.string.detail_marked_cooked)
    val timerStartedMessage = stringResource(R.string.timer_started)
    val exactAlarmDeniedMessage = stringResource(R.string.timer_exact_alarm_denied)
    val notificationDeniedMessage = stringResource(R.string.timer_notification_permission_denied)
    var showExactAlarmPermissionDialog by remember { mutableStateOf(false) }
    var pendingNotificationEvent by remember { mutableStateOf<RecipeDetailEvent?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            pendingNotificationEvent?.let(viewModel::onEvent)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(notificationDeniedMessage)
            }
        }
        pendingNotificationEvent = null
    }
    val dispatchEvent: (RecipeDetailEvent) -> Unit = { event ->
        val requiresNotification = event is RecipeDetailEvent.StartTimer ||
            event is RecipeDetailEvent.ResumeTimer
        if (requiresNotification && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingNotificationEvent = event
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.onEvent(event)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(RecipeDetailEvent.RetryPendingTimerAction)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val timerIsRunning = state.timers.any { timer ->
        timer.status == cat.receptari.app.domain.model.CookingTimerStatus.RUNNING
    }
    DisposableEffect(timerIsRunning) {
        val activity = context as? Activity
        if (timerIsRunning) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (timerIsRunning) {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                RecipeDetailEffect.MarkedCooked -> snackbarHostState.showSnackbar(cookedMessage)
                RecipeDetailEffect.Deleted -> onNavigateBack()
                RecipeDetailEffect.RequestExactAlarmPermission -> {
                    showExactAlarmPermissionDialog = true
                }
                RecipeDetailEffect.TimerStarted -> {
                    snackbarHostState.showSnackbar(timerStartedMessage)
                }
                RecipeDetailEffect.ExactAlarmPermissionDenied -> {
                    snackbarHostState.showSnackbar(exactAlarmDeniedMessage)
                }
            }
        }
    }

    RecipeDetailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = dispatchEvent,
        onNavigateBack = onNavigateBack,
        onEditRecipe = { state.recipe?.id?.let(onEditRecipe) },
        modifier = modifier,
    )

    if (showExactAlarmPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showExactAlarmPermissionDialog = false
                viewModel.onEvent(RecipeDetailEvent.CancelPendingTimerAction)
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text(stringResource(R.string.timer_exact_alarm_title)) },
            text = { Text(stringResource(R.string.timer_exact_alarm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showExactAlarmPermissionDialog = false
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    },
                ) {
                    Text(stringResource(R.string.timer_open_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExactAlarmPermissionDialog = false
                        viewModel.onEvent(RecipeDetailEvent.CancelPendingTimerAction)
                    },
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
fun RecipeDetailScreen(
    state: RecipeDetailUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (RecipeDetailEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onEditRecipe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var timerSetup by remember { mutableStateOf<TimerSetupRequest?>(null) }

    PaperScaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.timers.isNotEmpty()) {
                ActiveTimerDock(
                    timers = state.timers,
                    remainingSeconds = state.timerRemainingSeconds,
                    onPause = { onEvent(RecipeDetailEvent.PauseTimer(it)) },
                    onResume = { onEvent(RecipeDetailEvent.ResumeTimer(it)) },
                    onAddMinute = { onEvent(RecipeDetailEvent.AddTimerMinute(it)) },
                    onCancel = { onEvent(RecipeDetailEvent.CancelTimer(it)) },
                )
            }
        },
        topBar = {
            PaperTopBar(
                // No title: the recipe names itself in display type a few dp below, and
                // printing it twice makes the page look like a form.
                showRule = true,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
                actions = {
                    state.recipe?.let { recipe ->
                        IconButton(onClick = { onEvent(RecipeDetailEvent.ToggleFavorite) }) {
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
                        IconButton(onClick = onEditRecipe) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.detail_edit),
                            )
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.detail_delete),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize())

            state.isMissing -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.detail_not_found),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            else -> RecipeContent(
                state = state,
                onEvent = onEvent,
                onSetTimer = { timerSetup = it },
                contentPadding = innerPadding,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text(stringResource(R.string.detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.detail_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onEvent(RecipeDetailEvent.Delete)
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

    timerSetup?.let { request ->
        TimerSetupSheet(
            title = request.label,
            initialMinutes = request.initialMinutes,
            onDismiss = { timerSetup = null },
            onStart = { minutes ->
                timerSetup = null
                onEvent(
                    RecipeDetailEvent.StartTimer(
                        stepId = request.stepId,
                        label = request.label,
                        durationMinutes = minutes,
                    ),
                )
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeContent(
    state: RecipeDetailUiState,
    onEvent: (RecipeDetailEvent) -> Unit,
    onSetTimer: (TimerSetupRequest) -> Unit,
    contentPadding: PaddingValues,
) {
    val recipe = state.recipe ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = 40.dp,
        ),
    ) {
        state.imagePath?.let { path ->
            item(key = "photo") {
                AsyncImage(
                    model = path,
                    contentDescription = stringResource(R.string.common_recipe_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding)
                        .height(210.dp)
                        .pageFrame(MaterialTheme.shapes.large, ReceptariTheme.palette.rule)
                        .clip(MaterialTheme.shapes.large),
                )
            }
        }

        item(key = "title") { RecipeHeader(recipe = recipe) }

        if (recipe.tags.isNotEmpty()) {
            item(key = "tags") {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    recipe.tags.forEach { tag ->
                        // Read-only here: tags are edited in the editor, and a tappable chip
                        // that does nothing is worse than a label.
                        FilterPill(label = tag.name, selected = false)
                    }
                }
            }
        }

        if (recipe.isScalable) {
            item(key = "servings") { ServingsSelector(state = state, onEvent = onEvent) }
        }

        item(key = "ingredients-heading") {
            OrnamentHeading(
                title = stringResource(R.string.detail_ingredients),
                modifier = Modifier.padding(horizontal = PagePadding, vertical = 12.dp),
            )
        }

        state.ingredientSections.forEach { section ->
            section.name?.let { name ->
                item(key = "isec-${section.id}") {
                    SectionLabel(
                        text = name,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(
                            start = PagePadding,
                            end = PagePadding,
                            top = 10.dp,
                            bottom = 4.dp,
                        ),
                    )
                }
            }
            items(section.ingredients, key = { it.id }) { ingredient ->
                IngredientRow(text = ingredientLine(ingredient))
            }
        }

        item(key = "instructions-heading") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OrnamentHeading(
                    title = stringResource(R.string.detail_instructions),
                    modifier = Modifier.padding(horizontal = PagePadding, vertical = 12.dp),
                )
                TextButton(
                    onClick = {
                        onSetTimer(
                            TimerSetupRequest(
                                stepId = null,
                                label = recipe.title,
                                initialMinutes = null,
                            ),
                        )
                    },
                ) {
                    Icon(Icons.Outlined.Timer, contentDescription = null)
                    Text(
                        text = stringResource(R.string.timer_set),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }

        // Step numbers run continuously across sections (PRD §3.4): "Prepare sauce" 1–2,
        // "Cook chicken" 3–4.
        var stepNumber = 0
        recipe.instructionSections.forEach { section ->
            section.name?.let { name ->
                item(key = "ssec-${section.id}") {
                    SectionLabel(
                        text = name,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(
                            start = PagePadding,
                            end = PagePadding,
                            top = 12.dp,
                            bottom = 6.dp,
                        ),
                    )
                }
            }
            section.steps.forEach { step ->
                stepNumber += 1
                val number = stepNumber
                item(key = "step-${step.id}") {
                    val label = stringResource(R.string.timer_step_label, number, recipe.title)
                    StepRow(
                        number = number,
                        text = step.text,
                        onSetTimer = {
                            onSetTimer(
                                TimerSetupRequest(
                                    stepId = step.id,
                                    label = label,
                                    initialMinutes = InstructionDurationParser.parseMinutes(step.text),
                                ),
                            )
                        },
                    )
                }
            }
        }

        recipe.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            item(key = "notes") {
                Column {
                    OrnamentHeading(
                        title = stringResource(R.string.detail_notes),
                        modifier = Modifier.padding(horizontal = PagePadding, vertical = 12.dp),
                    )
                    PaperCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PagePadding),
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Text(text = notes, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        (recipe.sourceName ?: recipe.sourceUrl)?.let { source ->
            item(key = "source") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OrnamentalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    Aside(text = stringResource(R.string.detail_source_line, source))
                }
            }
        }

        item(key = "cooked") {
            Button(
                onClick = { onEvent(RecipeDetailEvent.MarkCooked) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PagePadding, vertical = 8.dp),
            ) {
                Icon(Icons.Outlined.Restaurant, contentDescription = null)
                Text(
                    text = stringResource(R.string.detail_mark_cooked),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}

/** Title, times, rating and history — the recipe's own title page. */
@Composable
private fun RecipeHeader(recipe: Recipe, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = recipe.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        val times = buildList {
            recipe.prepTimeMinutes?.let {
                add(labelledTime(R.string.detail_prep_time, it))
            }
            recipe.cookTimeMinutes?.let {
                add(labelledTime(R.string.detail_cook_time, it))
            }
            // Falls back to the total the parts imply, so a recipe that recorded prep and
            // cook separately still shows what it actually costs without the reader adding
            // up. Null when it would only repeat a figure already on this line.
            (recipe.totalTimeMinutes ?: recipe.derivedTotalMinutes)?.let {
                add(labelledTime(R.string.detail_total_time, it))
            }
        }
        if (times.isNotEmpty()) {
            Text(
                text = times.joinToString(TimeSeparator),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        recipe.rating?.let { rating ->
            StarRating(
                rating = rating,
                contentDescription = stringResource(R.string.common_rating_value, rating),
                starSize = 18.dp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Aside(
            text = if (recipe.cookCount == 0) {
                stringResource(R.string.detail_never_cooked)
            } else {
                pluralStringResource(R.plurals.detail_cook_count, recipe.cookCount, recipe.cookCount)
            },
            modifier = Modifier.padding(top = 8.dp),
        )

        OrnamentalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}

/**
 * One ingredient, marked with a printer's diamond.
 *
 * The text is whatever [cat.receptari.app.domain.scaling.ScaledIngredient.displayText]
 * produced — the source's own line. Nothing here re-typesets it.
 */
@Composable
private fun IngredientRow(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding, vertical = 5.dp),
    ) {
        Diamond(modifier = Modifier.padding(top = 10.dp, end = 12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** One step, opened by its number set in a ruled roundel. */
@Composable
private fun StepRow(
    number: Int,
    text: String,
    onSetTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding, vertical = 7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .pageFrame(CircleShape, ReceptariTheme.palette.rule, inset = 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(), // i18n-exempt: a numeral, not a phrase
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        IconButton(
            onClick = onSetTimer,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = stringResource(R.string.timer_for_step, number),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun ServingsSelector(
    state: RecipeDetailUiState,
    onEvent: (RecipeDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    PaperCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding, vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.detail_servings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )

                OutlinedIconButton(
                    onClick = { onEvent(RecipeDetailEvent.DecreaseServings) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(R.string.detail_servings_decrease),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = state.servings?.toString().orEmpty(), // i18n-exempt: a numeral
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
                OutlinedIconButton(
                    onClick = { onEvent(RecipeDetailEvent.IncreaseServings) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.detail_servings_increase),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (state.isScaled) {
                Hairline(modifier = Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Aside(
                        text = stringResource(R.string.detail_scaled_notice, state.servings ?: 0),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onEvent(RecipeDetailEvent.ResetServings) }) {
                        Text(
                            text = stringResource(R.string.detail_servings_reset),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Diamond(modifier: Modifier = Modifier) {
    val color = ReceptariTheme.palette.rule
    Canvas(modifier = modifier.size(6.dp)) {
        val half = size.minDimension / 2f
        drawPath(
            path = Path().apply {
                moveTo(half, 0f)
                lineTo(size.width, half)
                lineTo(half, size.height)
                lineTo(0f, half)
                close()
            },
            color = color,
        )
    }
}

@Composable
private fun labelledTime(@StringRes labelRes: Int, minutes: Int): String =
    stringResource(R.string.detail_time_pair, stringResource(labelRes), minutes)

private val PagePadding = 20.dp
private const val TimeSeparator = "   ·   " // i18n-exempt: punctuation, identical in every locale

private data class TimerSetupRequest(
    val stepId: String?,
    val label: String,
    val initialMinutes: Int?,
)

@Preview
@Composable
private fun RecipeDetailScreenPreview() {
    val recipe = Recipe(
        id = "1",
        title = "Fricandó amb moixernons",
        baseServings = 4,
        prepTimeMinutes = 20,
        cookTimeMinutes = 80,
        totalTimeMinutes = 100,
        rating = 4,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        sourceName = "Àvia Montserrat",
        notes = "Millor de un dia per l'altre.",
        ingredientSections = listOf(
            IngredientSection(
                id = "s1",
                name = "Per al sofregit",
                ingredients = listOf(
                    Ingredient("i1", 200.0, null, "ml", "nata", null, "200 ml de nata"),
                    Ingredient("i2", null, null, null, null, null, "Sal al gust"),
                ),
            ),
        ),
        instructionSections = listOf(
            InstructionSection(
                id = "is1",
                name = "Prepara la salsa",
                steps = listOf(Step("st1", "Talla la ceba ben fina."), Step("st2", "Cou-la a foc lent.")),
            ),
        ),
        cookCount = 3,
    )

    ReceptariTheme {
        RecipeDetailScreen(
            state = RecipeDetailUiState(
                recipe = recipe,
                ingredientSections = ServingScaler.scaleSections(recipe.ingredientSections, null),
                servings = 4,
                isLoading = false,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onNavigateBack = {},
            onEditRecipe = {},
        )
    }
}
