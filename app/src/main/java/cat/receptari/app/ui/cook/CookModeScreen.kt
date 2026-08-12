package cat.receptari.app.ui.cook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.LocalActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import cat.receptari.app.core.designsystem.SectionLabel
import cat.receptari.app.core.designsystem.paperGrain
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.timer.InstructionDurationParser
import cat.receptari.app.ui.timer.ActiveTimerDock
import cat.receptari.app.ui.timer.TimerSetupSheet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun CookModeRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CookModeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val timerStartedMessage = stringResource(R.string.timer_started)
    val exactAlarmDeniedMessage = stringResource(R.string.timer_exact_alarm_denied)
    val notificationDeniedMessage = stringResource(R.string.timer_notification_permission_denied)
    var showExactAlarmPermissionDialog by remember { mutableStateOf(false) }
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            pendingNotificationAction?.invoke()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(notificationDeniedMessage)
            }
        }
        pendingNotificationAction = null
    }
    val dispatchTimerAction: (() -> Unit) -> Unit = { action ->
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingNotificationAction = action
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    KeepScreenAwake()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.retryPendingTimerAction()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                CookModeEffect.RequestExactAlarmPermission -> {
                    showExactAlarmPermissionDialog = true
                }
                CookModeEffect.ExactAlarmPermissionDenied -> {
                    snackbarHostState.showSnackbar(exactAlarmDeniedMessage)
                }
                CookModeEffect.TimerStarted -> {
                    snackbarHostState.showSnackbar(timerStartedMessage)
                }
                CookModeEffect.CookingFinished -> onNavigateBack()
            }
        }
    }

    CookModeScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onPauseTimer = viewModel::pauseTimer,
        onResumeTimer = { timerId ->
            dispatchTimerAction { viewModel.resumeTimer(timerId) }
        },
        onAddTimerMinute = viewModel::addTimerMinute,
        onCancelTimer = viewModel::cancelTimer,
        onFinishCooking = viewModel::finishCooking,
        onStartTimer = { stepId, label, minutes ->
            dispatchTimerAction { viewModel.startTimer(stepId, label, minutes) }
        },
        modifier = modifier,
    )

    if (showExactAlarmPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showExactAlarmPermissionDialog = false
                viewModel.cancelPendingTimerAction()
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
                androidx.compose.material3.TextButton(
                    onClick = {
                        showExactAlarmPermissionDialog = false
                        viewModel.cancelPendingTimerAction()
                    },
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun KeepScreenAwake() {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
fun CookModeScreen(
    state: CookModeUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onPauseTimer: (String) -> Unit,
    onResumeTimer: (String) -> Unit,
    onAddTimerMinute: (String) -> Unit,
    onCancelTimer: (String) -> Unit,
    onFinishCooking: () -> Unit,
    onStartTimer: (String, String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recipe = state.recipe
    val steps = remember(recipe) { recipe?.cookSteps().orEmpty() }
    var currentIndex by rememberSaveable(recipe?.id) { mutableIntStateOf(0) }
    var showIngredients by remember { mutableStateOf(false) }
    var timerStep by remember { mutableStateOf<CookStep?>(null) }
    val current = steps.getOrNull(currentIndex)
    val stepScrollState = rememberScrollState()

    LaunchedEffect(current?.id) {
        stepScrollState.scrollTo(0)
    }

    PaperScaffold(
        modifier = modifier,
        topBar = {
            PaperTopBar(
                title = {
                    Text(
                        text = recipe?.title.orEmpty(),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cook_mode_exit),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .paperGrain(),
            ) {
                CookNavigation(
                    currentIndex = currentIndex,
                    totalSteps = steps.size,
                    onPrevious = { currentIndex = (currentIndex - 1).coerceAtLeast(0) },
                    onNext = { currentIndex = (currentIndex + 1).coerceAtMost(steps.lastIndex) },
                    onFinish = onFinishCooking,
                    modifier = if (state.timers.isEmpty()) {
                        Modifier.navigationBarsPadding()
                    } else {
                        Modifier
                    },
                )
                if (state.timers.isNotEmpty()) {
                    ActiveTimerDock(
                        timers = state.timers,
                        remainingSeconds = state.timerRemainingSeconds,
                        onPause = onPauseTimer,
                        onResume = onResumeTimer,
                        onAddMinute = onAddTimerMinute,
                        onCancel = onCancelTimer,
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding))
            recipe == null || current == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.cook_mode_no_steps))
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        R.string.cook_mode_progress,
                        currentIndex + 1,
                        steps.size,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                current.sectionName?.let { sectionName ->
                    SectionLabel(
                        text = sectionName,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                PaperCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(stepScrollState),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = (currentIndex + 1).toString(), // i18n-exempt: step numeral
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = current.text,
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 18.dp),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { showIngredients = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
                        Text(
                            text = stringResource(R.string.detail_ingredients),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = { timerStep = current },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Timer, contentDescription = null)
                        Text(
                            text = stringResource(R.string.timer_set),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }

    if (showIngredients && recipe != null) {
        IngredientsSheet(recipe = recipe, onDismiss = { showIngredients = false })
    }

    timerStep?.let { step ->
        val timerLabel = stringResource(
            R.string.timer_step_label,
            step.number,
            recipe?.title.orEmpty(),
        )
        TimerSetupSheet(
            title = timerLabel,
            initialMinutes = InstructionDurationParser.parseMinutes(step.text),
            onDismiss = { timerStep = null },
            onStart = { minutes ->
                onStartTimer(
                    step.id,
                    timerLabel,
                    minutes,
                )
                timerStep = null
            },
        )
    }
}

@Composable
private fun CookNavigation(
    currentIndex: Int,
    totalSteps: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLastStep = totalSteps > 0 && currentIndex == totalSteps - 1

    Column(modifier = modifier.fillMaxWidth()) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = onPrevious, enabled = currentIndex > 0) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(
                    text = stringResource(R.string.cook_mode_previous),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            Button(
                onClick = if (isLastStep) onFinish else onNext,
                enabled = totalSteps > 0,
            ) {
                Text(
                    stringResource(
                        if (isLastStep) R.string.cook_mode_finish else R.string.cook_mode_next,
                    ),
                )
                if (!isLastStep) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientsSheet(recipe: Recipe, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .paperGrain()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OrnamentHeading(
                    title = stringResource(R.string.detail_ingredients),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            recipe.ingredientSections.forEach { section ->
                section.name?.let { name ->
                    item(key = "section-${section.id}") {
                        SectionLabel(
                            text = name,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                items(section.ingredients, key = { it.id }) { ingredient ->
                    Text(
                        text = ingredient.originalText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

private data class CookStep(
    val id: String,
    val number: Int,
    val sectionName: String?,
    val text: String,
)

private fun Recipe.cookSteps(): List<CookStep> {
    var number = 0
    return instructionSections.flatMap { section ->
        section.steps.map { step ->
            number += 1
            CookStep(
                id = step.id,
                number = number,
                sectionName = section.name,
                text = step.text,
            )
        }
    }
}
