package cat.receptari.app.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.BuildConfig
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.theme.ReceptariTheme

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var selected by remember { mutableStateOf(context.currentAppLanguage()) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(resources.getString(message.messageRes))
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
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
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
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            ApiKeySection(state = uiState, onEvent = onEvent)

            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    if (uiState.showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsEvent.DismissRemoveDialog) },
            title = { Text(stringResource(R.string.settings_api_key_remove_title)) },
            text = { Text(stringResource(R.string.settings_api_key_remove_body)) },
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
        Text(
            text = stringResource(R.string.settings_api_key),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 12.dp),
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

@Preview
@Composable
private fun SettingsScreenPreview() {
    ReceptariTheme(dynamicColor = false) {
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
    ReceptariTheme(dynamicColor = false) {
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
