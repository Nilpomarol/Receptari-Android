package cat.receptari.app.ui.settings

import android.app.LocaleManager
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.LocaleList
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import cat.receptari.app.core.designsystem.PaperConfirmBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.BuildConfig
import cat.receptari.app.MainActivity
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.OrnamentalDivider
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import cat.receptari.app.core.designsystem.paperFieldColors
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.backup.BackupArchive
import cat.receptari.app.domain.transfer.RecipeTransferArchive
import cat.receptari.app.ui.transfer.TransferMethodSheet
import cat.receptari.app.ui.transfer.sendRecipeTransferNearby
import cat.receptari.app.ui.transfer.shareRecipeTransfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import kotlin.system.exitProcess

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(context.currentAppLanguage()) }
    var pendingArchive by remember { mutableStateOf<BackupArchive?>(null) }
    var transferArchive by remember { mutableStateOf<RecipeTransferArchive?>(null) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val createBackupDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.receptari.backup"),
    ) { uri ->
        val archive = pendingArchive ?: return@rememberLauncherForActivityResult
        pendingArchive = null
        if (uri == null) {
            viewModel.onBackupSaveCancelled(archive)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        archive.file.inputStream().use { input -> input.copyTo(output) }
                    } ?: error("No output stream")
                }.isSuccess
            }
            viewModel.onBackupSaved(archive, saved)
        }
    }
    val openBackupDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val localFile = withContext(Dispatchers.IO) {
                runCatching {
                    File(context.cacheDir, "restore-${UUID.randomUUID()}.receptari").also { target ->
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            target.outputStream().use { output -> input.copyTo(output) }
                        } ?: error("No input stream")
                    }
                }.getOrNull()
            }
            viewModel.onRestoreFileSelected(localFile)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(resources.getString(message.messageRes))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.SaveBackup -> {
                    pendingArchive = effect.archive
                    createBackupDocument.launch(effect.archive.suggestedFileName)
                }
                SettingsEffect.OpenBackup -> openBackupDocument.launch(
                    arrayOf("application/vnd.receptari.backup", "application/zip", "application/octet-stream"),
                )
                SettingsEffect.RestartAfterRestore -> context.restartForRestore()
                is SettingsEffect.ShareRecipes -> transferArchive = effect.archive
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        selectedLanguage = selected,
        versionName = BuildConfig.VERSION_NAME,
        onEvent = viewModel::onEvent,
        onLanguageSelected = { language ->
            selected = language
            context.applyAppLanguage(language)
        },
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    selectedLanguage: AppLanguage,
    versionName: String,
    onEvent: (SettingsEvent) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    PaperScaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PaperTopBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            OrnamentHeading(
                title = stringResource(R.string.settings_language),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )

            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    LanguageRow(
                        language = language,
                        selected = language == selectedLanguage,
                        onSelect = { onLanguageSelected(language) },
                    )
                }
            }

            OrnamentalDivider(Modifier.padding(vertical = 12.dp))

            ApiKeySection(state = uiState, onEvent = onEvent)

            OrnamentalDivider(Modifier.padding(vertical = 12.dp))

            SharingSection(state = uiState, onEvent = onEvent)

            OrnamentalDivider(Modifier.padding(vertical = 12.dp))

            BackupSection(state = uiState, onEvent = onEvent)

            Aside(
                text = stringResource(R.string.settings_version, versionName),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        }
    }

    if (uiState.showRemoveDialog) {
        PaperConfirmBottomSheet(
            title = stringResource(R.string.settings_api_key_remove_title),
            subtitle = stringResource(R.string.settings_api_key_remove_body),
            onDismissRequest = { onEvent(SettingsEvent.DismissRemoveDialog) },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsEvent.ConfirmRemoveKey) }) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SettingsEvent.DismissRemoveDialog) }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    uiState.restorePreview?.let { preview ->
        val date = remember(preview.createdAt) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date.from(preview.createdAt))
        }
        PaperConfirmBottomSheet(
            title = stringResource(R.string.settings_restore_confirm_title),
            onDismissRequest = { if (!uiState.isBackupBusy) onEvent(SettingsEvent.DismissRestore) },
            body = {
                Column {
                    Text(stringResource(R.string.settings_restore_confirm_body))
                    Text(
                        text = stringResource(R.string.settings_restore_created, date),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(pluralStringResource(R.plurals.settings_restore_recipe_count, preview.recipeCount, preview.recipeCount))
                    Text(pluralStringResource(R.plurals.settings_restore_image_count, preview.imageCount, preview.imageCount))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(SettingsEvent.ConfirmRestore) },
                    enabled = !uiState.isBackupBusy,
                ) { Text(stringResource(R.string.settings_restore_confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(SettingsEvent.DismissRestore) },
                    enabled = !uiState.isBackupBusy,
                ) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun SharingSection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        OrnamentHeading(
            title = stringResource(R.string.settings_sharing_title),
            modifier = Modifier.padding(bottom = 14.dp),
        )
        Text(
            text = stringResource(R.string.settings_sharing_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Aside(
            text = stringResource(R.string.settings_sharing_flow),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        OutlinedButton(
            onClick = { onEvent(SettingsEvent.ShareLibrary) },
            enabled = !state.isTransferBusy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            if (state.isTransferBusy) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Share, contentDescription = null)
            }
            Text(
                text = stringResource(
                    if (state.isTransferBusy) {
                        R.string.transfer_preparing
                    } else {
                        R.string.settings_share_library
                    },
                ),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun BackupSection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        OrnamentHeading(
            title = stringResource(R.string.settings_backup_title),
            modifier = Modifier.padding(bottom = 14.dp),
        )
        Text(
            text = stringResource(R.string.settings_backup_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val lastBackup = state.lastBackupAt
        val lastBackupText = if (lastBackup == null) {
            stringResource(R.string.settings_backup_last_never)
        } else {
            val formatted = remember(lastBackup) {
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date.from(lastBackup))
            }
            stringResource(R.string.settings_backup_last, formatted)
        }
        Text(
            text = lastBackupText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp),
        )
        OutlinedButton(
            onClick = { onEvent(SettingsEvent.CreateBackup) },
            enabled = !state.isBackupBusy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            Icon(Icons.Default.SaveAlt, contentDescription = null)
            Text(
                text = stringResource(R.string.settings_backup_export),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        OutlinedButton(
            onClick = { onEvent(SettingsEvent.ChooseRestore) },
            enabled = !state.isBackupBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Restore, contentDescription = null)
            Text(
                text = stringResource(R.string.settings_restore_choose),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (state.isBackupBusy) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.settings_backup_working),
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}

/**
 * Two states, not one: a key is either configured (test / replace / remove) or being entered.
 * Showing an input box pre-filled with the stored key would mean decrypting it onto the
 * screen for no reason — replacing a key means typing a new one.
 */
@Composable
private fun ApiKeySection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        OrnamentHeading(
            title = stringResource(R.string.settings_api_key),
            modifier = Modifier.padding(bottom = 14.dp),
        )
        Text(
            text = stringResource(R.string.settings_api_key_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.hasKey && !state.isEditingKey) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.settings_api_key_configured),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { onEvent(SettingsEvent.TestKey) },
                    enabled = !state.isTesting,
                ) {
                    Text(stringResource(R.string.settings_api_key_test))
                }
                TextButton(onClick = { onEvent(SettingsEvent.EditKey) }) {
                    Text(stringResource(R.string.settings_api_key_replace))
                }
                TextButton(onClick = { onEvent(SettingsEvent.RequestRemoveKey) }) {
                    Text(stringResource(R.string.settings_api_key_remove))
                }
                if (state.isTesting) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        } else {
            OutlinedTextField(
                colors = paperFieldColors(),
                value = state.keyInput,
                onValueChange = { onEvent(SettingsEvent.KeyChanged(it)) },
                label = { Text(stringResource(R.string.settings_api_key_label)) },
                singleLine = true,
                // Password transformation plus KeyboardType.Password keeps the key out of
                // the keyboard's learned-word dictionary and off the screen by default.
                visualTransformation = if (state.keyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                ),
                trailingIcon = {
                    IconButton(onClick = { onEvent(SettingsEvent.ToggleKeyVisible) }) {
                        Icon(
                            imageVector = if (state.keyVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = stringResource(
                                if (state.keyVisible) {
                                    R.string.settings_api_key_hide
                                } else {
                                    R.string.settings_api_key_show
                                },
                            ),
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            Row {
                TextButton(
                    onClick = { onEvent(SettingsEvent.SaveKey) },
                    enabled = state.canSave,
                ) {
                    Text(stringResource(R.string.settings_api_key_save))
                }
                if (state.isEditingKey) {
                    TextButton(onClick = { onEvent(SettingsEvent.CancelEditKey) }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // onClick = null: the whole row owns the click, so the button must not duplicate it.
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(language.labelRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

private fun Context.localeManager(): LocaleManager =
    getSystemService(LocaleManager::class.java)

private fun Context.currentAppLanguage(): AppLanguage =
    AppLanguage.fromLanguageTag(localeManager().applicationLocales.toLanguageTags())

/**
 * minSdk 33 means the platform per-app language API is available directly — no AppCompat
 * backport path to maintain (ADR-006).
 */
private fun Context.applyAppLanguage(language: AppLanguage) {
    localeManager().applicationLocales = LocaleList.forLanguageTags(language.languageTag)
}

private fun Context.restartForRestore() {
    val intent = Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    val pendingIntent = PendingIntent.getActivity(
        this,
        41_907,
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val alarmManager = getSystemService(AlarmManager::class.java)
    val triggerAt = SystemClock.elapsedRealtime() + 500L
    if (alarmManager.canScheduleExactAlarms()) {
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
    } else {
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
    }
    (this as? Activity)?.finishAffinity()
    exitProcess(0)
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    ReceptariTheme() {
        SettingsScreen(
            uiState = SettingsUiState(),
            selectedLanguage = AppLanguage.Catalan,
            versionName = "0.1.0",
            onEvent = {},
            onLanguageSelected = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun SettingsScreenWithKeyPreview() {
    ReceptariTheme() {
        SettingsScreen(
            uiState = SettingsUiState(hasKey = true),
            selectedLanguage = AppLanguage.Catalan,
            versionName = "0.1.0",
            onEvent = {},
            onLanguageSelected = {},
            onNavigateBack = {},
        )
    }
}
