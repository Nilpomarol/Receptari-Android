package cat.receptari.app.ui.detail

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.semantics.semantics
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.PaperTopBar
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import cat.receptari.app.core.designsystem.CardRowDivider
import cat.receptari.app.core.designsystem.IconDisc
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cat.receptari.app.core.designsystem.FilterPill
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperConfirmBottomSheet
import cat.receptari.app.core.designsystem.paperFieldColors
import cat.receptari.app.core.designsystem.paperGrain
import cat.receptari.app.core.designsystem.PaperModalBottomSheet
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.ai.AiError
import cat.receptari.app.domain.ai.RecipeLanguage
import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.model.IngredientSection
import cat.receptari.app.domain.model.InstructionSection
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.Step
import cat.receptari.app.domain.scaling.ServingScaler
import cat.receptari.app.domain.transfer.RecipeTransferArchive
import cat.receptari.app.R
import cat.receptari.app.ui.common.aiMessageRes
import cat.receptari.app.ui.folders.FolderEditorSheet
import cat.receptari.app.ui.folders.toColor
import cat.receptari.app.ui.folders.toImageVector
import cat.receptari.app.ui.timer.ActiveTimerDock
import cat.receptari.app.ui.timer.TimerSetupSheet
import cat.receptari.app.ui.transfer.sendRecipeTransferNearby
import cat.receptari.app.ui.transfer.shareRecipeTransfer
import cat.receptari.app.ui.transfer.TransferMethodSheet
import java.time.Instant
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    val listState = rememberLazyListState()

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
                    CookBar(
                        onStartCooking = onStartCooking,
                        onMarkCooked = { onEvent(RecipeDetailEvent.MarkCooked) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val recipe = state.recipe
        when {
            state.isLoading -> Unit

            recipe == null -> Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.detail_not_found),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
                BarSheet(progress = { 1f })
                DetailTopBar(
                    title = null,
                    titleVisible = false,
                    onPhoto = false,
                    onNavigateBack = onNavigateBack,
                )
            }

            else -> RecipeContent(
                state = state,
                listState = listState,
                bottomPadding = innerPadding.calculateBottomPadding(),
                onEvent = onEvent,
                onSetTimer = { timerSetup = it },
                onNavigateBack = onNavigateBack,
            ) {
                IconButton(
                    onClick = {
                        timerSetup = TimerSetupRequest(stepId = null, label = recipe.title, initialMinutes = null)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = stringResource(R.string.timer_set),
                    )
                }
                IconButton(onClick = { onEvent(RecipeDetailEvent.ToggleFavorite) }) {
                    Icon(
                        imageVector = if (recipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(
                            if (recipe.isFavorite) R.string.detail_favorite_remove else R.string.detail_favorite_add,
                        ),
                        tint = if (recipe.isFavorite) ReceptariTheme.palette.heart else LocalContentColor.current,
                    )
                }
                IconButton(onClick = onEditRecipe) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.detail_edit),
                    )
                }
                RecipeOverflowMenu(
                    busy = state.isSharing || state.isTranslating,
                    onTranslate = { showTranslationPicker = true },
                    onShare = { onEvent(RecipeDetailEvent.Share) },
                    onDelete = { confirmDelete = true },
                )
            }
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

/** Translate, send and delete: real actions, but not ones a cook reaches for mid-recipe. */
@Composable
private fun RecipeOverflowMenu(
    busy: Boolean,
    onTranslate: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = !busy) {
            if (busy) {
                val busyDescription = stringResource(R.string.translation_action)
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
                text = { Text(text = stringResource(R.string.detail_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

/**
 * The one action a recipe page exists for, pinned to the foot of the page under a hairline,
 * with a round tick beside it to log the recipe as cooked.
 */
@Composable
private fun CookBar(
    onStartCooking: () -> Unit,
    onMarkCooked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .paperGrain(intensity = 1f),
    ) {
        Hairline()
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = PagePadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onStartCooking,
                shape = CircleShape,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(
                    text = stringResource(R.string.cook_mode_start),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            OutlinedIconButton(
                onClick = onMarkCooked,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.detail_mark_cooked),
                )
            }
        }
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
internal fun QuickFolderSheet(
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
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                OrnamentHeading(title = stringResource(R.string.detail_quick_folder_title))
                // Ruled like the settings page: one folder to a line, the current one ticked.
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(top = 6.dp),
                ) {
                    item(key = "no-folder") {
                        FolderChoiceRow(
                            icon = Icons.Outlined.FolderOff,
                            name = stringResource(R.string.edit_folder_none),
                            isSelected = currentFolder == null,
                            onClick = {
                                onSelectFolder(null)
                                onDismiss()
                            },
                        )
                    }
                    items(availableFolders, key = { it.id }) { folder ->
                        CardRowDivider()
                        FolderChoiceRow(
                            icon = folder.icon.toImageVector(),
                            name = folder.name,
                            isSelected = folder.id == currentFolder?.id,
                            discContainer = folder.color.toColor().copy(alpha = 0.16f),
                            discContent = folder.color.toColor(),
                            onClick = {
                                onSelectFolder(folder)
                                onDismiss()
                            },
                        )
                    }
                }
                // Pinned under the list so it is never scrolled out of reach.
                Hairline()
                FolderChoiceRow(
                    icon = Icons.Default.Add,
                    name = stringResource(R.string.edit_folder_add),
                    isSelected = false,
                    nameColor = MaterialTheme.colorScheme.primary,
                    onClick = { creatingFolder = true },
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }
}

/** One line of the folder sheet: the folder's disc, its name, and a wax tick when it is the current one. */
@Composable
private fun FolderChoiceRow(
    icon: ImageVector,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    discContainer: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    discContent: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    nameColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconDisc(icon = icon, size = 38.dp, container = discContainer, content = discContent)
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = nameColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun QuickTagSheet(
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

            // One field; the wax plus appears inside it only when there is a new tag to add.
            val addDraft = {
                if (canCreate) {
                    onAddTag(draft.trim())
                    draft = ""
                }
            }
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
                trailingIcon = if (canCreate) {
                    {
                        FilledIconButton(onClick = addDraft, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.edit_tag_add),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addDraft() }),
                modifier = Modifier.fillMaxWidth(),
            )

            if (allTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    allTags.forEach { tag ->
                        val assigned = tag.lowercase() in currentLower
                        FilterPill(
                            label = "#$tag", // i18n-exempt: a tag's own name, as the recipe page prints it
                            selected = assigned,
                            onClick = { if (assigned) onRemoveTag(tag) else onAddTag(tag) },
                            icon = if (assigned) Icons.Default.Check else null,
                        )
                    }
                }
            }
        }
    }
}

