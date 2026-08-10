package cat.receptari.app.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cat.receptari.app.BuildConfig
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.theme.ReceptariTheme

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(context.currentAppLanguage()) }

    SettingsScreen(
        selectedLanguage = selected,
        versionName = BuildConfig.VERSION_NAME,
        onLanguageSelected = { language ->
            selected = language
            context.applyAppLanguage(language)
        },
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedLanguage: AppLanguage,
    versionName: String,
    onLanguageSelected: (AppLanguage) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
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
                .padding(innerPadding),
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

            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
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
            selectedLanguage = AppLanguage.Catalan,
            versionName = "0.1.0",
            onLanguageSelected = {},
            onNavigateBack = {},
        )
    }
}
