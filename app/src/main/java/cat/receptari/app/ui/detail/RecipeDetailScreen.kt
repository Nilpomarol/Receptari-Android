package cat.receptari.app.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import cat.receptari.app.core.designsystem.IconFact
import cat.receptari.app.core.designsystem.MetaLine
import cat.receptari.app.core.designsystem.SegmentedTabs
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Surface
import cat.receptari.app.core.designsystem.pageFrame
import cat.receptari.app.core.designsystem.paperGrain
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.ui.folders.FolderEditorSheet
import cat.receptari.app.ui.folders.toColor
import cat.receptari.app.ui.folders.toImageVector
import cat.receptari.app.core.designsystem.PaperConfirmBottomSheet
import cat.receptari.app.core.designsystem.PaperModalBottomSheet
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontStyle
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
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import cat.receptari.app.core.designsystem.SectionLabel
import cat.receptari.app.core.designsystem.StarRating
import cat.receptari.app.core.designsystem.ingredientLine
import cat.receptari.app.core.designsystem.pageFrame
import cat.receptari.app.core.designsystem.paperFieldColors
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.ai.RecipeLanguage
import cat.receptari.app.domain.ai.AiError
import cat.receptari.app.domain.model.IngredientSection
import cat.receptari.app.domain.model.InstructionSection
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.Step
import cat.receptari.app.domain.scaling.ServingScaler
import cat.receptari.app.domain.timer.InstructionDurationParser
import cat.receptari.app.ui.timer.ActiveTimerDock
import cat.receptari.app.ui.timer.TimerSetupSheet
import cat.receptari.app.ui.common.aiMessageRes
import cat.receptari.app.domain.transfer.RecipeTransferArchive
import cat.receptari.app.ui.transfer.TransferMethodSheet
import cat.receptari.app.ui.transfer.sendRecipeTransferNearby
import cat.receptari.app.ui.transfer.shareRecipeTransfer
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailRoute(
    onNavigateBack: () -> Unit,
    onEditRecipe: (String) -> Unit,
    onStartCooking: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val cookedMessage = stringResource(R.string.detail_marked_cooked)
    val undoLabel = stringResource(R.string.common_undo)
    val timerStartedMessage = stringResource(R.string.timer_started)
    val exactAlarmDeniedMessage = stringResource(R.string.timer_exact_alarm_denied)
    val notificationDeniedMessage = stringResource(R.string.timer_notification_permission_denied)
    val translationAppliedMessage = stringResource(R.string.translation_applied)
    val shareFailedMessage = stringResource(R.string.transfer_share_failed)
    val settingsActionLabel = stringResource(R.string.settings_title)
    val aiErrorMessages = mapOf(
        R.string.ai_error_no_key to stringResource(R.string.ai_error_no_key),
        R.string.ai_error_invalid_key to stringResource(R.string.ai_error_invalid_key),
        R.string.ai_error_rate_limited to stringResource(R.string.ai_error_rate_limited),
        R.string.ai_error_quota to stringResource(R.string.ai_error_quota),
        R.string.ai_error_offline to stringResource(R.string.ai_error_offline),
        R.string.ai_error_refused to stringResource(R.string.ai_error_refused),
        R.string.ai_error_unreadable to stringResource(R.string.ai_error_unreadable),
        R.string.ai_error_unexpected to stringResource(R.string.ai_error_unexpected),
    )
    var showExactAlarmPermissionDialog by remember { mutableStateOf(false) }
    var pendingNotificationEvent by remember { mutableStateOf<RecipeDetailEvent?>(null) }
    var transferArchive by remember { mutableStateOf<RecipeTransferArchive?>(null) }
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
                is RecipeDetailEffect.MarkedCooked -> {
                    val result = snackbarHostState.showSnackbar(cookedMessage, actionLabel = undoLabel)
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(RecipeDetailEvent.UndoMarkCooked(effect.eventId))
                    }
                }
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
                RecipeDetailEffect.TranslationApplied -> {
                    snackbarHostState.showSnackbar(translationAppliedMessage)
                }
                is RecipeDetailEffect.TranslationFailed -> {
                    val missingKey = effect.error is AiError.NoApiKey
                    val result = snackbarHostState.showSnackbar(
                        message = aiErrorMessages.getValue(effect.error.aiMessageRes()),
                        actionLabel = if (missingKey) {
                            settingsActionLabel
                        } else {
                            null
                        },
                    )
                    if (missingKey && result == SnackbarResult.ActionPerformed) onOpenSettings()
                }
                is RecipeDetailEffect.ShareReady -> transferArchive = effect.archive
                RecipeDetailEffect.ShareFailed -> snackbarHostState.showSnackbar(shareFailedMessage)
            }
        }
    }

    RecipeDetailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = dispatchEvent,
        onNavigateBack = onNavigateBack,
        onEditRecipe = { state.recipe?.id?.let(onEditRecipe) },
        onStartCooking = { state.recipe?.id?.let(onStartCooking) },
        modifier = modifier,
    )

    transferArchive?.let { archive ->
        TransferMethodSheet(
            onSendNearby = {
                transferArchive = null
                context.sendRecipeTransferNearby(archive)
            },
            onShareWithOtherApps = {
                transferArchive = null
                context.shareRecipeTransfer(archive)
            },
            onDismiss = { transferArchive = null },
        )
    }

    if (showExactAlarmPermissionDialog) {
        PaperConfirmBottomSheet(
            title = stringResource(R.string.timer_exact_alarm_title),
            subtitle = stringResource(R.string.timer_exact_alarm_body),
            onDismissRequest = {
                showExactAlarmPermissionDialog = false
                viewModel.onEvent(RecipeDetailEvent.CancelPendingTimerAction)
            },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    state: RecipeDetailUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (RecipeDetailEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onEditRecipe: () -> Unit,
    onStartCooking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var timerSetup by remember { mutableStateOf<TimerSetupRequest?>(null) }
    var showTranslationPicker by remember { mutableStateOf(false) }

    PaperScaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
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
                if (state.recipe != null) {
                    CookBar(onStartCooking = onStartCooking)
                }
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
                        val translateDescription = stringResource(R.string.translation_action)
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
                        RecipeOverflowMenu(
                            busy = state.isSharing || state.isTranslating,
                            busyDescription = translateDescription,
                            onTranslate = { showTranslationPicker = true },
                            onShare = { onEvent(RecipeDetailEvent.Share) },
                            onDelete = { confirmDelete = true },
                        )
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
                onStartCooking = onStartCooking,
                contentPadding = innerPadding,
            )
        }
    }

    if (confirmDelete) {
        PaperConfirmBottomSheet(
            title = stringResource(R.string.detail_delete_confirm_title),
            subtitle = stringResource(R.string.detail_delete_confirm_body),
            onDismissRequest = { confirmDelete = false },
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

    if (showTranslationPicker) {
        TranslationPickerDialog(
            currentLanguage = state.recipe?.displayLanguage ?: state.recipe?.originalLanguage,
            originalLanguage = state.recipe?.originalLanguage,
            translatedLanguages = state.translatedLanguages,
            onDismiss = { showTranslationPicker = false },
            onSelect = { language ->
                showTranslationPicker = false
                onEvent(RecipeDetailEvent.Translate(language))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationPickerDialog(
    currentLanguage: String?,
    originalLanguage: String?,
    translatedLanguages: Set<RecipeLanguage>,
    onDismiss: () -> Unit,
    onSelect: (RecipeLanguage) -> Unit,
) {
    PaperModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OrnamentHeading(title = stringResource(R.string.translation_choose_language))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RecipeLanguage.entries.forEach { language ->
                    val isCurrent = language.matches(currentLanguage)
                    val isOriginal = language.matches(originalLanguage)
                    TextButton(
                        onClick = { onSelect(language) },
                        enabled = !isCurrent,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(
                                    when (language) {
                                        RecipeLanguage.CATALAN -> R.string.settings_language_ca
                                        RecipeLanguage.SPANISH -> R.string.settings_language_es
                                        RecipeLanguage.ENGLISH -> R.string.settings_language_en
                                    },
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start,
                            )
                            if (isOriginal) {
                                LanguageIndicator(stringResource(R.string.translation_original))
                            }
                            if (language in translatedLanguages) {
                                LanguageIndicator(stringResource(R.string.translation_available))
                            }
                            if (isCurrent) {
                                LanguageIndicator(stringResource(R.string.translation_current))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageIndicator(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

private fun RecipeLanguage.matches(languageTag: String?): Boolean =
    languageTag?.startsWith(prefix = this.languageTag, ignoreCase = true) == true

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun RecipeContent(
    state: RecipeDetailUiState,
    onEvent: (RecipeDetailEvent) -> Unit,
    onSetTimer: (TimerSetupRequest) -> Unit,
    onStartCooking: () -> Unit,
    contentPadding: PaddingValues,
) {
    val recipe = state.recipe ?: return

    var showFolderSheet by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(IngredientsTab) }
    // Ticking off ingredients is a scratch mark for this session at the stove, not data.
    val checkedIngredients = remember(recipe.id) { mutableStateListOf<String>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 40.dp,
        ),
    ) {
        item(key = "hero") {
            RecipeHero(
                imagePath = state.imagePath,
                title = recipe.title,
                rating = recipe.rating,
                onSetRating = { onEvent(RecipeDetailEvent.SetRating(it)) },
            )
        }

        item(key = "facts") {
            RecipeFacts(recipe = recipe)
        }

        stickyHeader(key = "tabs") {
            SegmentedTabs(
                options = listOf(
                    stringResource(R.string.detail_ingredients),
                    stringResource(R.string.detail_instructions),
                ),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .paperGrain(intensity = 1f)
                    .padding(horizontal = PagePadding, vertical = 10.dp),
            )
        }

        if (selectedTab == IngredientsTab) {
            item(key = "ingredients") {
                IngredientsPanel(
                    state = state,
                    onEvent = onEvent,
                    checked = checkedIngredients,
                    onToggle = { id ->
                        if (!checkedIngredients.remove(id)) checkedIngredients.add(id)
                    },
                )
            }
        } else {
        item(key = "instructions-heading") {
            InstructionsHeader(
                onSetTimer = {
                    onSetTimer(
                        TimerSetupRequest(
                            stepId = null,
                            label = recipe.title,
                            initialMinutes = null,
                        ),
                    )
                },
            )
        }

        // Step numbers run continuously across sections (PRD §3.4): "Prepare sauce" 1–2,
        // "Cook chicken" 3–4. The threaded rule only joins steps within one section, so a
        // new stage reads as a clean break rather than one unbroken column.
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
                            top = 16.dp,
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
                        // The timer only belongs on steps that actually name a duration.
                        onSetTimer = if (InstructionDurationParser.containsDuration(step.text)) {
                            {
                                onSetTimer(
                                    TimerSetupRequest(
                                        stepId = step.id,
                                        label = label,
                                        initialMinutes =
                                            InstructionDurationParser.parseMinutes(step.text),
                                    ),
                                )
                            }
                        } else {
                            null
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
                        icon = Icons.Outlined.EditNote,
                        modifier = Modifier.padding(start = PagePadding, end = PagePadding, top = 28.dp, bottom = 10.dp),
                    )
                    NotesCard(notes = notes)
                }
            }
        }
        }

        (recipe.sourceName ?: recipe.sourceUrl)?.let { source ->
            item(key = "source") {
                Aside(
                    text = stringResource(R.string.detail_source_line, source),
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding, vertical = 20.dp),
                )
            }
        }

        // Filing and history come last: they matter when putting the book in order, not
        // while reading a recipe to cook it.
        item(key = "meta") {
            RecipeMeta(
                recipe = recipe,
                onOpenFolderSheet = { showFolderSheet = true },
                onOpenTagSheet = { showTagSheet = true },
            )
        }

        item(key = "cooked") {
            OutlinedButton(
                onClick = { onEvent(RecipeDetailEvent.MarkCooked) },
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PagePadding, vertical = 8.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text(
                    text = stringResource(R.string.detail_mark_cooked),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
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

/**
 * The recipe's title page: a full-bleed plate that melts into the paper, then the title set
 * in the display face between two engraved corner sprigs.
 *
 * The photo is edge-to-edge with a scrim fading to the page colour, so it reads as a
 * photograph tipped onto the sheet rather than a card floating above it. With no photo, the
 * cartouche stands on its own — the flourishes and rule are enough to make a title page.
 */
@Composable
private fun RecipeHero(
    imagePath: String?,
    title: String,
    rating: Int?,
    onSetRating: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A one-time settle as the page opens: the title rises a few dp and fades in.
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val reveal by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 480),
        label = "hero-reveal",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = imagePath,
                contentDescription = stringResource(R.string.common_recipe_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(MaterialTheme.shapes.large)
                    .graphicsLayer { alpha = 0.35f + 0.65f * reveal },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = reveal
                    translationY = (1f - reveal) * 18.dp.toPx()
                }
                .padding(start = PagePadding, end = PagePadding, top = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
            )

            RatingStars(
                rating = rating,
                onSetRating = onSetRating,
                // The touch targets are wider than the stars; pull the row back so the
                // first star lines up with the title's left edge.
                modifier = Modifier.offset(x = (-9).dp),
            )
        }
    }
}

/** The five gold rating stars, tappable to set or clear the recipe's rating. */
@Composable
private fun RatingStars(
    rating: Int?,
    onSetRating: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..5).forEach { star ->
            val isSelected = rating != null && star <= rating
            IconButton(
                onClick = { onSetRating(if (rating == star) null else star) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = stringResource(R.string.common_rating_value, star),
                    tint = if (isSelected) {
                        ReceptariTheme.palette.gold
                    } else {
                        ReceptariTheme.palette.gold.copy(alpha = 0.35f)
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/**
 * The figures a cook glances at before starting: total time and yield with their pictograms,
 * then the prep / cook split in the aside voice when the recipe gives one.
 */
@Composable
private fun RecipeFacts(recipe: Recipe, modifier: Modifier = Modifier) {
    val total = recipe.totalTimeMinutes ?: recipe.derivedTotalMinutes
    val servings = recipe.baseServings?.takeIf { it > 0 }
    val split = listOfNotNull(
        recipe.prepTimeMinutes?.let {
            stringResource(R.string.detail_time_part, stringResource(R.string.detail_prep_time), formatDuration(it))
        },
        recipe.cookTimeMinutes?.let {
            stringResource(R.string.detail_time_part, stringResource(R.string.detail_cook_time), formatDuration(it))
        },
    )
    if (total == null && servings == null && split.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PagePadding, end = PagePadding, bottom = 6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            total?.let { IconFact(icon = Icons.Outlined.Schedule, text = formatDuration(it)) }
            servings?.let {
                IconFact(
                    icon = Icons.Outlined.People,
                    text = pluralStringResource(R.plurals.common_servings, it, it),
                )
            }
        }
        MetaLine(parts = split, modifier = Modifier.padding(top = 6.dp))
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

/** Rating, cook history and the folder / tag stamps — the marginalia of the title page. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeMeta(
    recipe: Recipe,
    onOpenFolderSheet: () -> Unit,
    onOpenTagSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PagePadding, end = PagePadding, top = 4.dp, bottom = 12.dp),
    ) {
        Aside(
            textAlign = TextAlign.Start,
            text = if (recipe.cookCount == 0) {
                stringResource(R.string.detail_never_cooked)
            } else {
                pluralStringResource(R.plurals.detail_cook_count, recipe.cookCount, recipe.cookCount)
            },
            modifier = Modifier.padding(top = 2.dp),
        )

        // Folder & Tags metadata chips
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Folder chip
            MetaChip(
                onClick = onOpenFolderSheet,
                selected = recipe.folder != null,
                leading = {
                    Icon(
                        imageVector = recipe.folder?.icon?.toImageVector() ?: Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = recipe.folder?.color?.toColor() ?: MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                },
            ) {
                Text(
                    text = recipe.folder?.name ?: stringResource(R.string.detail_add_folder),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Recipe tags
            recipe.tags.forEach { tag ->
                MetaChip(onClick = onOpenTagSheet, selected = false) {
                    Text(
                        text = "#${tag.name}", // i18n-exempt: tag prefix and name
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Add tag pill button
            MetaChip(
                onClick = onOpenTagSheet,
                selected = false,
                dashed = true,
                leading = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                },
            ) {
                Text(
                    text = stringResource(R.string.detail_add_tag),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** One folder / tag stamp, sharing a single ruled-pill shell so the row stays even. */
@Composable
private fun MetaChip(
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val palette = ReceptariTheme.palette
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = when {
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            dashed -> Color.Transparent
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = 1.dp,
            color = when {
                selected -> MaterialTheme.colorScheme.primary
                dashed -> palette.rule.copy(alpha = 0.6f)
                else -> palette.rule
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leading?.invoke()
            content()
        }
    }
}

/** The ingredients column, set on its own raised sheet so it reads apart from the method. */
@Composable
private fun IngredientsPanel(
    state: RecipeDetailUiState,
    onEvent: (RecipeDetailEvent) -> Unit,
    checked: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recipe = state.recipe ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PagePadding, end = PagePadding, top = 8.dp),
    ) {
        IngredientsHeader(
            isScalable = recipe.isScalable,
            state = state,
            onEvent = onEvent,
        )

        if (state.isScaled && state.servings != null) {
            Aside(
                text = stringResource(R.string.detail_scaled_notice, state.servings),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
        }

        Column(modifier = Modifier.padding(top = 6.dp)) {
            run {
                state.ingredientSections.forEachIndexed { index, section ->
                    section.name?.let { name ->
                        SectionLabel(
                            text = name,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(
                                top = if (index == 0) 0.dp else 12.dp,
                                bottom = 4.dp,
                            ),
                        )
                    }
                    section.ingredients.forEach { ingredient ->
                        IngredientRow(
                            text = ingredientLine(ingredient),
                            checked = ingredient.id in checked,
                            onToggle = { onToggle(ingredient.id) },
                        )
                    }
                }
            }
        }
    }
}

/** The method heading, with a plain kitchen timer beside it. */
@Composable
private fun InstructionsHeader(
    onSetTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OrnamentHeading(
        title = stringResource(R.string.detail_instructions),
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        modifier = modifier.padding(start = PagePadding, end = PagePadding, top = 8.dp, bottom = 8.dp),
        trailing = {
            IconButton(onClick = onSetTimer, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = stringResource(R.string.timer_set),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

/**
 * The one action a recipe page exists for, pinned to the foot of the page so it is there
 * whether the reader is at the ingredients or halfway down the method.
 */
@Composable
private fun CookBar(onStartCooking: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .paperGrain(intensity = 1f)
            .navigationBarsPadding(),
    ) {
        Button(
            onClick = onStartCooking,
            shape = CircleShape,
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PagePadding, vertical = 10.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text(
                text = stringResource(R.string.cook_mode_start),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

/** Translate, send and delete: real actions, but not ones a cook reaches for mid-recipe. */
@Composable
private fun RecipeOverflowMenu(
    busy: Boolean,
    busyDescription: String,
    onTranslate: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = !busy) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(22.dp)
                        .semantics { contentDescription = busyDescription },
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.common_more_options),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.translation_action)) },
                leadingIcon = { Icon(Icons.Outlined.Translate, contentDescription = null) },
                onClick = {
                    expanded = false
                    onTranslate()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.detail_share)) },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = {
                    expanded = false
                    onShare()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.detail_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

/** Notes, opened with an illuminated initial in the manner of a printed aside. */
@Composable
private fun NotesCard(notes: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding),
    ) {
        val trimmed = notes.trimStart()
        if (trimmed.length > 1) {
            Row {
                Text(
                    text = trimmed.take(1),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontStyle = FontStyle.Italic,
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Text(
                    text = trimmed.drop(1),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 6.dp),
                )
            }
        } else {
            Text(text = notes, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * One ingredient with a box to tick off while gathering them.
 *
 * The text is whatever [cat.receptari.app.domain.scaling.ScaledIngredient.displayText]
 * produced — the source's own line. Nothing here re-typesets it.
 */
@Composable
private fun IngredientRow(
    text: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = ReceptariTheme.palette.rule,
            ),
            modifier = Modifier.padding(end = 12.dp, top = 8.dp, bottom = 8.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

/**
 * One step, its number set large in the margin in rubric red, the way a printed method is
 * numbered.
 */
@Composable
private fun StepRow(
    number: Int,
    text: String,
    onSetTimer: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PagePadding, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(StepBadge)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(), // i18n-exempt: a numeral, not a phrase
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, top = 2.dp),
        )
        if (onSetTimer != null) {
            IconButton(
                onClick = onSetTimer,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = stringResource(R.string.timer_for_step, number),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            // Keep the text column the same width on every step so the right margin
            // does not ripple between timed and untimed steps.
            Spacer(modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun IngredientsHeader(
    isScalable: Boolean,
    state: RecipeDetailUiState,
    onEvent: (RecipeDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    OrnamentHeading(
        title = stringResource(R.string.detail_ingredients),
        icon = Icons.Outlined.ShoppingBasket,
        modifier = modifier.padding(top = 8.dp),
        trailing = {
        if (isScalable) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (state.isScaled) {
                    IconButton(
                        onClick = { onEvent(RecipeDetailEvent.ResetServings) },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                            contentDescription = stringResource(R.string.detail_servings_reset),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (state.isScaled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (state.isScaled) MaterialTheme.colorScheme.primary else ReceptariTheme.palette.rule,
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .paperGrain()
                            .height(36.dp)
                            .padding(horizontal = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable(onClick = { onEvent(RecipeDetailEvent.DecreaseServings) }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = stringResource(R.string.detail_servings_decrease),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 32.dp)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${state.servings ?: 0}", // i18n-exempt: numeral
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                ),
                                color = if (state.isScaled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                textAlign = TextAlign.Center,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable(onClick = { onEvent(RecipeDetailEvent.IncreaseServings) }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.detail_servings_increase),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    })
}

private val PagePadding = 20.dp

/** The wax disc that carries a step's numeral. */
private val StepBadge = 28.dp

private const val IngredientsTab = 0

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
            onStartCooking = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickFolderSheet(
    currentFolder: Folder?,
    availableFolders: List<Folder>,
    onDismiss: () -> Unit,
    onSelectFolder: (Folder?) -> Unit,
    onCreateFolder: (String, FolderColor, FolderIcon) -> Unit,
) {
    var creatingFolder by remember { mutableStateOf(false) }

    if (creatingFolder) {
        var draftName by remember { mutableStateOf("") }
        var draftColor by remember(availableFolders.size) {
            mutableStateOf(FolderColor.entries[availableFolders.size % FolderColor.entries.size])
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
                if (draftName.isNotBlank()) {
                    onCreateFolder(draftName, draftColor, draftIcon)
                    creatingFolder = false
                }
            },
            onDismiss = { creatingFolder = false },
        )
    } else {
        PaperModalBottomSheet(
            onDismissRequest = onDismiss,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                OrnamentHeading(title = stringResource(R.string.detail_quick_folder_title))
                Button(
                    onClick = { creatingFolder = true },
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

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "no-folder") {
                        NoFolderTile(
                            isSelected = currentFolder == null,
                            onClick = {
                                onSelectFolder(null)
                                onDismiss()
                            },
                        )
                    }
                    items(availableFolders, key = { it.id }) { folder ->
                        FolderSelectorTile(
                            folder = folder,
                            isSelected = folder.id == currentFolder?.id,
                            onClick = {
                                onSelectFolder(folder)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderSelectorTile(
    folder: Folder,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = ReceptariTheme.palette
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .pageFrame(
                shape = MaterialTheme.shapes.large,
                color = if (isSelected) MaterialTheme.colorScheme.primary else palette.rule,
            ),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(
            modifier = Modifier
                .paperGrain()
                .clickable(onClick = onClick)
                .padding(14.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = folder.icon.toImageVector(),
                        contentDescription = null,
                        tint = folder.color.toColor(),
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun NoFolderTile(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = ReceptariTheme.palette
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .pageFrame(
                shape = MaterialTheme.shapes.large,
                color = if (isSelected) MaterialTheme.colorScheme.primary else palette.rule,
            ),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(
            modifier = Modifier
                .paperGrain()
                .clickable(onClick = onClick)
                .padding(14.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.edit_folder_none),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun QuickTagSheet(
    currentTags: List<String>,
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val currentLower = remember(currentTags) {
        currentTags.mapTo(mutableSetOf()) { it.lowercase() }
    }
    // One grid of every tag rather than a "your tags" list beside a "other tags" list: a
    // chip is on when the recipe carries it and off when it does not, and tapping flips it.
    // Assigned tags sort to the front so the recipe's own tags read first.
    val allTags = remember(availableTags, currentTags, draft) {
        val union = (currentTags + availableTags).distinctBy { it.lowercase() }
        val filtered = if (draft.isBlank()) {
            union
        } else {
            union.filter { it.contains(draft.trim(), ignoreCase = true) }
        }
        filtered.sortedWith(compareBy({ it.lowercase() !in currentLower }, { it.lowercase() }))
    }
    val canCreate = draft.isNotBlank() && draft.trim().lowercase() !in currentLower

    PaperModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OrnamentHeading(title = stringResource(R.string.detail_quick_tags_title))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    colors = paperFieldColors(),
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.edit_tag_add_hint)) },
                    singleLine = true,
                    shape = CircleShape,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = if (draft.isNotBlank()) {
                        {
                            IconButton(onClick = { draft = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (canCreate) {
                                onAddTag(draft.trim())
                                draft = ""
                            }
                        },
                    ),
                    modifier = Modifier.weight(1f),
                )

                Button(
                    enabled = canCreate,
                    onClick = {
                        if (canCreate) {
                            onAddTag(draft.trim())
                            draft = ""
                        }
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.edit_tag_add),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            if (allTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    allTags.forEach { tag ->
                        val assigned = tag.lowercase() in currentLower
                        FilterPill(
                            label = tag,
                            selected = assigned,
                            onClick = { if (assigned) onRemoveTag(tag) else onAddTag(tag) },
                            icon = if (assigned) {
                                Icons.Default.Check
                            } else {
                                Icons.AutoMirrored.Outlined.Label
                            },
                        )
                    }
                }
            }
        }
    }
}

