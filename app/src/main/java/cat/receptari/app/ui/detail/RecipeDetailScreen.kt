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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

@Composable
private fun TranslationPickerDialog(
    currentLanguage: String?,
    originalLanguage: String?,
    translatedLanguages: Set<RecipeLanguage>,
    onDismiss: () -> Unit,
    onSelect: (RecipeLanguage) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text(stringResource(R.string.translation_choose_language)) },
        text = {
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
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 40.dp,
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

        item(key = "title") { RecipeHeader(recipe = recipe, onEvent = onEvent) }

        item(key = "tags-and-folder") {
            var showFolderSheet by remember { mutableStateOf(false) }
            var showTagSheet by remember { mutableStateOf(false) }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PagePadding, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterPill(
                    label = recipe.folder?.name ?: stringResource(R.string.detail_add_folder),
                    selected = recipe.folder != null,
                    onClick = { showFolderSheet = true },
                    icon = Icons.Outlined.Folder,
                )

                recipe.tags.forEach { tag ->
                    FilterPill(
                        label = tag.name,
                        selected = false,
                        onClick = { showTagSheet = true },
                    )
                }

                FilterPill(
                    label = stringResource(R.string.detail_add_tag),
                    selected = false,
                    onClick = { showTagSheet = true },
                    icon = Icons.Default.Add,
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
                    onCreateFolder = { name ->
                        onEvent(RecipeDetailEvent.FolderCreateRequested(name))
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = onStartCooking) {
                        Icon(Icons.Outlined.Restaurant, contentDescription = null)
                        Text(
                            text = stringResource(R.string.cook_mode_start),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
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
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        Icon(Icons.Outlined.Timer, contentDescription = null)
                        Text(
                            text = stringResource(R.string.timer_set),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
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
private fun RecipeHeader(
    recipe: Recipe,
    onEvent: (RecipeDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
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

        // Interactive rating stars
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (1..5).forEach { star ->
                val isSelected = recipe.rating != null && star <= recipe.rating
                IconButton(
                    onClick = {
                        onEvent(RecipeDetailEvent.SetRating(if (recipe.rating == star) null else star))
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = stringResource(R.string.common_rating_value, star),
                        tint = ReceptariTheme.palette.gold,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Aside(
            text = if (recipe.cookCount == 0) {
                stringResource(R.string.detail_never_cooked)
            } else {
                pluralStringResource(R.plurals.detail_cook_count, recipe.cookCount, recipe.cookCount)
            },
            modifier = Modifier.padding(top = 6.dp),
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
            onStartCooking = {},
        )
    }
}

@Composable
private fun QuickFolderSheet(
    currentFolder: Folder?,
    availableFolders: List<Folder>,
    onDismiss: () -> Unit,
    onSelectFolder: (Folder?) -> Unit,
    onCreateFolder: (String) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { OrnamentHeading(title = stringResource(R.string.detail_quick_folder_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { onSelectFolder(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.edit_folder_none),
                        color = if (currentFolder == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }

                availableFolders.forEach { folder ->
                    val isCurrent = folder.id == currentFolder?.id
                    TextButton(
                        onClick = { onSelectFolder(folder) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = folder.name,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                if (creating) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            colors = paperFieldColors(),
                            value = draft,
                            onValueChange = { draft = it },
                            placeholder = { Text(stringResource(R.string.edit_folder_add_hint)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                if (draft.isNotBlank()) onCreateFolder(draft)
                            },
                        ) {
                            Text(stringResource(R.string.folders_add))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { creating = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(
                            text = stringResource(R.string.edit_folder_add),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
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
    val candidateTags = remember(availableTags, currentTags) {
        (availableTags - currentTags.toSet()).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { OrnamentHeading(title = stringResource(R.string.detail_quick_tags_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        colors = paperFieldColors(),
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text(stringResource(R.string.edit_tag_add_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            if (draft.isNotBlank()) {
                                onAddTag(draft)
                                draft = ""
                            }
                        },
                    ) {
                        Text(stringResource(R.string.edit_tag_add))
                    }
                }

                if (currentTags.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.edit_field_tags),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        currentTags.forEach { tag ->
                            FilterChip(
                                selected = true,
                                onClick = { onRemoveTag(tag) },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.edit_tag_remove, tag),
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }
                }

                if (candidateTags.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.filter_tags),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        candidateTags.forEach { tag ->
                            FilterChip(
                                selected = false,
                                onClick = { onAddTag(tag) },
                                label = { Text(tag) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
}

