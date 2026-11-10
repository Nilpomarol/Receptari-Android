package cat.receptari.app.ui.cook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.FilledIconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
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
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import cat.receptari.app.core.designsystem.PaperConfirmBottomSheet
import cat.receptari.app.core.designsystem.PaperModalBottomSheet
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
import cat.receptari.app.core.designsystem.OrnamentHeading
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

@OptIn(ExperimentalMaterial3Api::class)
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
        PaperConfirmBottomSheet(
            title = stringResource(R.string.timer_exact_alarm_title),
            subtitle = stringResource(R.string.timer_exact_alarm_body),
            onDismissRequest = {
                showExactAlarmPermissionDialog = false
                viewModel.cancelPendingTimerAction()
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
                    .paperGrain(intensity = 1f),
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
            ) {
                Text(
                    text = stringResource(
                        R.string.cook_mode_progress,
                        currentIndex + 1,
                        steps.size,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StepDots(
                    total = steps.size,
                    current = currentIndex,
                    modifier = Modifier.padding(top = 10.dp),
                )
                current.sectionName?.let { sectionName ->
                    SectionLabel(
                        text = sectionName,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                // The step sits high and flush left, as large as the screen allows: the dots
                // above already say which one it is.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 16.dp)
                        .verticalScroll(stepScrollState),
                ) {
                    Text(
                        text = current.text,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                // The timer only belongs on steps that actually name a duration.
                InstructionDurationParser.parseMinutes(current.text)
                    ?.takeIf { InstructionDurationParser.containsDuration(current.text) }
                    ?.let { minutes ->
                        val label = stringResource(R.string.timer_step_label, current.number, recipe.title)
                        StepTimerCard(
                            minutes = minutes,
                            onAdjust = { timerStep = current },
                            onStart = { onStartTimer(current.id, label, minutes) },
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                OutlinedButton(
                    onClick = { showIngredients = true },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Icon(Icons.Outlined.ShoppingBasket, contentDescription = null)
                    Text(
                        text = stringResource(R.string.detail_ingredients),
                        modifier = Modifier.padding(start = 8.dp),
                    )
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = currentIndex > 0,
                shape = CircleShape,
                border = BorderStroke(
                    1.dp,
                    if (currentIndex > 0) MaterialTheme.colorScheme.primary else ReceptariTheme.palette.rule,
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(
                    text = stringResource(R.string.cook_mode_previous),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            Button(
                onClick = if (isLastStep) onFinish else onNext,
                enabled = totalSteps > 0,
                shape = CircleShape,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.weight(1f),
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

/**
 * Where the cook is in the method: finished steps ticked, the current one numbered in wax,
 * the rest as empty rings, all joined by a thread. Long methods drop the numerals so the
 * row still fits on one line.
 */
@Composable
private fun StepDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    val compact = total > MaxNumberedDots
    val dot = if (compact) 10.dp else 24.dp
    val wax = MaterialTheme.colorScheme.primary
    val rule = ReceptariTheme.palette.rule
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.5.dp)
                        .background(if (index <= current) wax else rule),
                )
            }
            Box(
                modifier = Modifier
                    .size(dot)
                    .clip(CircleShape)
                    .background(if (index <= current) wax else Color.Transparent)
                    .border(1.5.dp, if (index <= current) wax else rule, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (!compact && index < current) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                } else if (!compact && index == current) {
                    Text(
                        text = (index + 1).toString(), // i18n-exempt: step numeral
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

/**
 * The step's own kitchen timer, ready with the duration the step names: one tap starts it,
 * tapping the card opens the sheet to change the minutes first.
 */
@Composable
private fun StepTimerCard(
    minutes: Int,
    onAdjust: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaperCard(onClick = onAdjust, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.timer_set),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.cook_mode_timer_clock, minutes),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            FilledIconButton(
                onClick = onStart,
                modifier = Modifier.size(60.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.timer_start),
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientsSheet(recipe: Recipe, onDismiss: () -> Unit) {
    PaperModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
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
                        text = ingredient.displayText ?: ingredient.originalText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

private const val MaxNumberedDots = 10

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
