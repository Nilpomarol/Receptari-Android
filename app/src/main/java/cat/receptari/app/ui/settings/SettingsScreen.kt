package cat.receptari.app.ui.settings

import android.app.Activity
import android.app.AlarmManager
import android.app.LocaleManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.LocaleList
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.Dp
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.Sprig
import cat.receptari.app.core.designsystem.Wordmark
import cat.receptari.app.core.designsystem.pageFrame
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.BuildConfig
import cat.receptari.app.MainActivity
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.Emblem
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.PaperConfirmBottomSheet
import cat.receptari.app.core.designsystem.PaperModalBottomSheet
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.Rubric
import cat.receptari.app.core.designsystem.paperFieldColors
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.backup.BackupArchive
import cat.receptari.app.domain.transfer.RecipeTransferArchive
import cat.receptari.app.ui.transfer.RECIPE_TRANSFER_MIME_TYPE
import cat.receptari.app.ui.transfer.TransferImportActivity
import cat.receptari.app.ui.transfer.TransferMethodSheet
import cat.receptari.app.ui.transfer.receiveRecipeTransferNearby
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

    // A copy someone sent as a file goes through the same import screen as opening the file
    // from another app would.
    val openSharedCopy = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.startActivity(
            Intent(context, TransferImportActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
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
        onReceiveNearby = context::receiveRecipeTransferNearby,
        onImportSharedCopy = {
            openSharedCopy.launch(
                arrayOf(RECIPE_TRANSFER_MIME_TYPE, "application/zip", "application/octet-stream"),
            )
        },
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


/**
 * Settings in three tiers: a display heading with its wax-red mark for each part, the part's
 * controls on one soft card below it, and what the part means in a footnote under the card.
 * The page closes on a bookplate, the way a printed book names its owner and edition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    selectedLanguage: AppLanguage,
    versionName: String,
    onEvent: (SettingsEvent) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onReceiveNearby: () -> Unit,
    onImportSharedCopy: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    PaperScaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = PageMargin,
                    end = PageMargin,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 20.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Rubric(
                    text = stringResource(R.string.settings_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_language),
                icon = Icons.Outlined.Language,
            ) {
                SettingsCard(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp)) {
                    LanguagePicker(selected = selectedLanguage, onSelect = onLanguageSelected)
                }
            }

            SettingsSection(
                title = stringResource(R.string.settings_api_key),
                icon = Icons.Outlined.VpnKey,
            ) {
                ApiKeyCard(state = uiState, onEvent = onEvent)
            }

            SettingsSection(
                title = stringResource(R.string.settings_sharing_title),
                icon = Icons.Outlined.Share,
                footnote = stringResource(R.string.settings_sharing_explanation),
            ) {
                SettingsCard {
                    ActionRow(
                        icon = Icons.AutoMirrored.Outlined.Send,
                        title = stringResource(R.string.settings_share_library),
                        subtitle = if (uiState.isTransferBusy) stringResource(R.string.transfer_preparing) else null,
                        onClick = { onEvent(SettingsEvent.ShareLibrary) },
                        enabled = !uiState.isTransferBusy,
                        busy = uiState.isTransferBusy,
                    )
                    RowDivider()
                    ActionRow(
                        icon = Icons.Outlined.WifiTethering,
                        title = stringResource(R.string.import_receive_nearby),
                        onClick = onReceiveNearby,
                    )
                    RowDivider()
                    ActionRow(
                        icon = Icons.Outlined.FileOpen,
                        title = stringResource(R.string.import_from_shared_copy),
                        onClick = onImportSharedCopy,
                    )
                }
            }

            BackupSection(state = uiState, onEvent = onEvent)

            Bookplate(versionName = versionName)
        }
    }

    if (uiState.isEditingKey) {
        ApiKeySheet(state = uiState, onEvent = onEvent)
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

/** A part of the settings: its heading, its card, and an optional footnote under the card. */
@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    footnote: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(top = SectionGap)) {
        OrnamentHeading(
            title = title,
            icon = icon,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        content()
        if (footnote != null) {
            Aside(
                text = footnote,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp),
            )
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    PaperCard(modifier = modifier.fillMaxWidth(), contentPadding = contentPadding) {
        Column(content = content)
    }
}

/** A wax-tinted disc with an icon in it: what marks a row as an action of its own kind. */
@Composable
private fun IconDisc(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(size / 2),
        )
    }
}

/** One action on a card: its disc, its name in the display face, a note, and a chevron. */
@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconDisc(icon = icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Aside(
                    text = subtitle,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A hairline between rows of a card, starting where the row's text starts. */
@Composable
private fun RowDivider() {
    Hairline(modifier = Modifier.padding(start = 54.dp))
}

@Composable
private fun BackupSection(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastBackup = state.lastBackupAt
    val status = when {
        state.isBackupBusy -> stringResource(R.string.settings_backup_working)
        lastBackup == null -> stringResource(R.string.settings_backup_last_never)
        else -> {
            val formatted = remember(lastBackup) {
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date.from(lastBackup))
            }
            stringResource(R.string.settings_backup_last, formatted)
        }
    }
    SettingsSection(
        title = stringResource(R.string.settings_backup_title),
        icon = Icons.Outlined.Inventory2,
        footnote = stringResource(R.string.settings_backup_explanation),
        modifier = modifier,
    ) {
        SettingsCard {
            ActionRow(
                icon = Icons.Outlined.SaveAlt,
                title = stringResource(R.string.settings_backup_export),
                subtitle = status,
                onClick = { onEvent(SettingsEvent.CreateBackup) },
                enabled = !state.isBackupBusy,
                busy = state.isBackupBusy,
            )
            RowDivider()
            ActionRow(
                icon = Icons.Outlined.Restore,
                title = stringResource(R.string.settings_restore_choose),
                onClick = { onEvent(SettingsEvent.ChooseRestore) },
                enabled = !state.isBackupBusy,
            )
        }
    }
}

/**
 * The key as a status, never as a box on the page: with no key, a call to connect; with one,
 * a check and what can be done with it. Typing a key happens in [ApiKeySheet]. Showing a box
 * pre-filled with the stored key would mean decrypting it onto the screen for no reason —
 * replacing a key means entering a new one.
 */
@Composable
private fun ApiKeyCard(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(modifier = modifier, contentPadding = PaddingValues(18.dp)) {
        if (state.hasKey) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconDisc(
                    icon = Icons.Default.Check,
                    size = 48.dp,
                    container = ReceptariTheme.palette.botanical.copy(alpha = 0.16f),
                    content = ReceptariTheme.palette.botanical,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_api_key_configured),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Aside(
                        text = stringResource(R.string.settings_api_key_stored),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Hairline(modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onEvent(SettingsEvent.TestKey) }, enabled = !state.isTesting) {
                    if (state.isTesting) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = stringResource(R.string.settings_api_key_test),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                TextButton(onClick = { onEvent(SettingsEvent.EditKey) }) {
                    Text(stringResource(R.string.settings_api_key_replace))
                }
                TextButton(onClick = { onEvent(SettingsEvent.RequestRemoveKey) }) {
                    Text(
                        text = stringResource(R.string.settings_api_key_remove),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconDisc(icon = Icons.Outlined.AutoAwesome, size = 48.dp)
                Text(
                    text = stringResource(R.string.settings_api_key_connect),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
            Text(
                text = stringResource(R.string.settings_api_key_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(
                onClick = { onEvent(SettingsEvent.EditKey) },
                shape = CircleShape,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.settings_api_key_add),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/**
 * Entering a key: most people copy it from the Anthropic console, so pasting is one tap and
 * the field is there to check or correct it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeySheet(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    PaperModalBottomSheet(onDismissRequest = { onEvent(SettingsEvent.CancelEditKey) }) {
        Column(
            modifier = Modifier
                .padding(start = PageMargin, end = PageMargin, top = 4.dp, bottom = 20.dp)
                .imePadding(),
        ) {
            OrnamentHeading(
                title = stringResource(R.string.settings_api_key),
                icon = Icons.Outlined.VpnKey,
            )
            OutlinedTextField(
                colors = paperFieldColors(),
                value = state.keyInput,
                onValueChange = { onEvent(SettingsEvent.KeyChanged(it)) },
                label = { Text(stringResource(R.string.settings_api_key_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
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
                    .padding(top = 18.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val pasted = clipboard.getClipEntry()?.clipData
                                ?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)?.text?.toString()?.trim()
                            if (!pasted.isNullOrEmpty()) onEvent(SettingsEvent.KeyChanged(pasted))
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.settings_api_key_paste),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Button(
                    onClick = { onEvent(SettingsEvent.SaveKey) },
                    enabled = state.canSave,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Text(stringResource(R.string.settings_api_key_save))
                }
            }
        }
    }
}

/** The app's languages as flags, the current one ringed in wax red. */
@Composable
private fun LanguagePicker(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AppLanguage.entries.forEach { language ->
            LanguageTile(
                language = language,
                selected = language == selected,
                onSelect = { onSelect(language) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LanguageTile(
    language: AppLanguage,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ring by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "languageRing",
    )
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .border(2.dp, ring, MaterialTheme.shapes.small)
                .padding(3.dp),
        ) {
            Flag(language = language, modifier = Modifier.size(width = 54.dp, height = 36.dp))
        }
        Text(
            text = stringResource(language.labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * The bookplate pasted inside the cover: a double-ruled plate with the emblem between two
 * sprigs, the wordmark, and the edition.
 */
@Composable
private fun Bookplate(versionName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(top = 40.dp)
            .fillMaxWidth()
            .pageFrame(MaterialTheme.shapes.medium, ReceptariTheme.palette.rule, inset = 4.dp)
            .padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Sprig(mirrored = true, modifier = Modifier.size(width = 46.dp, height = 28.dp))
            Emblem(modifier = Modifier.size(44.dp).padding(horizontal = 2.dp))
            Sprig(modifier = Modifier.size(width = 46.dp, height = 28.dp))
        }
        Wordmark(text = stringResource(R.string.library_title), modifier = Modifier.padding(top = 4.dp))
        Aside(text = stringResource(R.string.settings_version, versionName))
    }
}

/**
 * A language's flag, drawn rather than shipped as an image: there is no emoji for Catalonia's
 * senyera, and three flat flags are a few lines each. "Follow the system" is a globe.
 */
@Composable
private fun Flag(language: AppLanguage, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, ReceptariTheme.palette.rule.copy(alpha = 0.6f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (language == AppLanguage.System) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (language) {
                    AppLanguage.Catalan -> drawSenyera()
                    AppLanguage.Spanish -> drawSpain()
                    AppLanguage.English -> drawUnionJack()
                    AppLanguage.System -> Unit
                }
            }
        }
    }
}

/** Four red bars on gold: nine bands, the first and the last gold. */
private fun DrawScope.drawSenyera() {
    drawRect(Color(0xFFFCDD09))
    val band = size.height / 9f
    for (i in 1..7 step 2) {
        drawRect(Color(0xFFDA121A), topLeft = Offset(0f, band * i), size = Size(size.width, band))
    }
}

/** Red, a double-width gold band, red. */
private fun DrawScope.drawSpain() {
    drawRect(Color(0xFFAA151B))
    drawRect(
        color = Color(0xFFF1BF00),
        topLeft = Offset(0f, size.height * 0.25f),
        size = Size(size.width, size.height * 0.5f),
    )
}

/** The Union Jack: white and red saltires under a white and red cross, on blue. */
private fun DrawScope.drawUnionJack() {
    val w = size.width
    val h = size.height
    val red = Color(0xFFC8102E)
    drawRect(Color(0xFF012169))
    drawLine(Color.White, Offset(0f, 0f), Offset(w, h), strokeWidth = h * 0.2f)
    drawLine(Color.White, Offset(0f, h), Offset(w, 0f), strokeWidth = h * 0.2f)
    drawLine(red, Offset(0f, 0f), Offset(w, h), strokeWidth = h * 0.07f)
    drawLine(red, Offset(0f, h), Offset(w, 0f), strokeWidth = h * 0.07f)
    drawRect(Color.White, topLeft = Offset(w / 2f - h * 0.17f, 0f), size = Size(h * 0.34f, h))
    drawRect(Color.White, topLeft = Offset(0f, h / 2f - h * 0.17f), size = Size(w, h * 0.34f))
    drawRect(red, topLeft = Offset(w / 2f - h * 0.1f, 0f), size = Size(h * 0.2f, h))
    drawRect(red, topLeft = Offset(0f, h / 2f - h * 0.1f), size = Size(w, h * 0.2f))
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
            onReceiveNearby = {},
            onImportSharedCopy = {},
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
            onReceiveNearby = {},
            onImportSharedCopy = {},
        )
    }
}


private val SectionGap = 32.dp
private val PageMargin = 20.dp
