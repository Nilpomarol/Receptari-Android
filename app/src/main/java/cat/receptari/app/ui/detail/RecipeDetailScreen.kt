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
import cat.receptari.app.core.designsystem.CornerFlourish
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
                        IconButton(
                            onClick = { onEvent(RecipeDetailEvent.Share) },
                            enabled = !state.isSharing,
                        ) {
                            if (state.isSharing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(R.string.detail_share),
                                )
                            }
                        }
                        IconButton(
                            onClick = { showTranslationPicker = true },
                            enabled = !state.isTranslating,
                        ) {
                            if (state.isTranslating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .semantics {
                                            contentDescription = translateDescription
                                        },
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Translate,
                                    contentDescription = translateDescription,
                                )
                            }
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

@OptIn(ExperimentalLayoutApi::class)
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

        item(key = "specs") {
            SpecsBand(recipe = recipe)
        }

        item(key = "meta") {
            RecipeMeta(
                recipe = recipe,
                onOpenFolderSheet = { showFolderSheet = true },
                onOpenTagSheet = { showTagSheet = true },
            )
        }

        item(key = "ingredients") {
            IngredientsPanel(state = state, onEvent = onEvent)
        }

        item(key = "instructions-heading") {
            InstructionsHeader(
                onStartCooking = onStartCooking,
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
            val lastIndex = section.steps.lastIndex
            section.steps.forEachIndexed { index, step ->
                stepNumber += 1
                val number = stepNumber
                item(key = "step-${step.id}") {
                    val label = stringResource(R.string.timer_step_label, number, recipe.title)
                    StepRow(
                        number = number,
                        text = step.text,
                        connectTop = index > 0,
                        connectBottom = index < lastIndex,
                        illuminated = number == 1,
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
                        modifier = Modifier.padding(horizontal = PagePadding, vertical = 14.dp),
                    )
                    NotesCard(notes = notes)
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (imagePath != null) {
            val page = MaterialTheme.colorScheme.background
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = stringResource(R.string.common_recipe_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .graphicsLayer { alpha = 0.35f + 0.65f * reveal },
                )
                // The lower edge dissolves into the parchment so the plate has no hard seam.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, page)),
                        ),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = reveal
                    translationY = (1f - reveal) * 18.dp.toPx()
                }
                // With a photo, the block rides up into the faded lower edge so the title
                // sits close under the plate instead of leaving a band of bare paper.
                .then(if (imagePath != null) Modifier.offset(y = (-18).dp) else Modifier)
                .padding(
                    start = PagePadding,
                    end = PagePadding,
                    top = if (imagePath != null) 0.dp else 24.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Two engraved sprigs pointing inward from the corners frame the title.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CornerFlourish()
                Spacer(modifier = Modifier.weight(1f))
                CornerFlourish(mirrored = true)
            }

            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            RatingStars(
                rating = rating,
                onSetRating = onSetRating,
                modifier = Modifier.padding(top = 10.dp),
            )

            OrnamentalDivider(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .widthIn(max = 200.dp),
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
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = stringResource(R.string.common_rating_value, star),
                    tint = if (isSelected) {
                        ReceptariTheme.palette.gold
                    } else {
                        ReceptariTheme.palette.gold.copy(alpha = 0.35f)
                    },
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

/**
 * The figures a cook glances at before starting: prep, cook, total and yield, set as a
 * printed spec table between two hairlines. Cells that have no figure are simply absent, so
 * a recipe with only a total time still reads as a deliberate row rather than a gap.
 */
@Composable
private fun SpecsBand(recipe: Recipe, modifier: Modifier = Modifier) {
    val cells = buildList {
        recipe.prepTimeMinutes?.let {
            add(stringResource(R.string.detail_prep_time) to formatDuration(it))
        }
        recipe.cookTimeMinutes?.let {
            add(stringResource(R.string.detail_cook_time) to formatDuration(it))
        }
        (recipe.totalTimeMinutes ?: recipe.derivedTotalMinutes)?.let {
            add(stringResource(R.string.detail_total_time) to formatDuration(it))
        }
        recipe.baseServings?.takeIf { it > 0 }?.let {
            add(stringResource(R.string.detail_servings) to it.toString())
        }
    }
    if (cells.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PagePadding, end = PagePadding, top = 14.dp),
    ) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            cells.forEachIndexed { index, (label, value) ->
                if (index > 0) SpecDivider()
                SpecCell(label = label, value = value, modifier = Modifier.weight(1f))
            }
        }
        Hairline()
    }
}

@Composable
private fun SpecCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun SpecDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(ReceptariTheme.palette.rule.copy(alpha = 0.5f)),
    )
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
            .padding(start = PagePadding, end = PagePadding, top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Aside(
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
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
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

        OrnamentalDivider(modifier = Modifier.padding(top = 14.dp))
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

        PaperCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column {
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
                        IngredientRow(text = ingredientLine(ingredient))
                    }
                }
            }
        }
    }
}

/** The method heading and its two starting actions: begin cooking, or set a plain timer. */
@Composable
private fun InstructionsHeader(
    onStartCooking: () -> Unit,
    onSetTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OrnamentHeading(
            title = stringResource(R.string.detail_instructions),
            modifier = Modifier.padding(start = PagePadding, end = PagePadding, top = 20.dp, bottom = 14.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PagePadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onStartCooking,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Outlined.Restaurant, contentDescription = null)
                Text(
                    text = stringResource(R.string.cook_mode_start),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(
                onClick = onSetTimer,
                shape = CircleShape,
                border = BorderStroke(1.dp, ReceptariTheme.palette.rule),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Outlined.Timer, contentDescription = null)
                Text(
                    text = stringResource(R.string.timer_set),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** Notes, opened with an illuminated initial in the manner of a printed aside. */
@Composable
private fun NotesCard(notes: String, modifier: Modifier = Modifier) {
    PaperCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding),
        contentPadding = PaddingValues(16.dp),
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
            .padding(vertical = 5.dp),
    ) {
        Diamond(modifier = Modifier.padding(top = 10.dp, end = 12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * One step, opened by its number in a ruled roundel and threaded onto a faint vertical rule
 * that joins consecutive steps into a single column — the printed method's spine.
 *
 * The very first step is *illuminated*: a larger, gold-framed roundel, the printed-book
 * answer to a manuscript's decorated initial.
 */
@Composable
private fun StepRow(
    number: Int,
    text: String,
    connectTop: Boolean,
    connectBottom: Boolean,
    illuminated: Boolean,
    onSetTimer: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = ReceptariTheme.palette
    val page = MaterialTheme.colorScheme.background
    val roundelSize = if (illuminated) 34.dp else 28.dp
    val frameColor = if (illuminated) palette.gold else palette.rule
    val numberColor = if (illuminated) palette.gold else MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding, vertical = 7.dp),
    ) {
        Box(
            modifier = Modifier
                .width(GutterWidth)
                .fillMaxHeight(),
        ) {
            // The threaded rule runs behind the roundel; the opaque disc masks it at the
            // centre so it reads as passing through, not across.
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(GutterWidth),
            ) {
                val centerX = size.width / 2f
                val roundelCenterY = (StepRoundelTop + roundelSize / 2).toPx()
                val top = if (connectTop) 0f else roundelCenterY
                val bottom = if (connectBottom) size.height else roundelCenterY
                if (bottom > top) {
                    drawLine(
                        color = palette.rule.copy(alpha = 0.5f),
                        start = Offset(centerX, top),
                        end = Offset(centerX, bottom),
                        strokeWidth = StepRuleStroke,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = StepRoundelTop)
                    .size(roundelSize)
                    .background(page, CircleShape)
                    .pageFrame(CircleShape, frameColor, inset = 0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(), // i18n-exempt: a numeral, not a phrase
                    style = if (illuminated) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    color = numberColor,
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 2.dp),
        )
        if (onSetTimer != null) {
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.detail_ingredients),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

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
                    shape = RoundedCornerShape(6.dp),
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
                            .height(32.dp)
                            .padding(horizontal = 2.dp),
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

private val PagePadding = 20.dp

/** The leading column that carries a step's roundel and the rule threaded through it. */
private val GutterWidth = 40.dp
private val StepRoundelTop = 1.dp
private const val StepRuleStroke = 1.5f

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

