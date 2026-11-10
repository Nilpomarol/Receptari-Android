package cat.receptari.app.ui.importer.website

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.paperFieldColors
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.ui.importer.ImportEffect
import cat.receptari.app.ui.importer.ImportSheet
import cat.receptari.app.ui.importer.rememberPasteAction

@Composable
fun WebsiteImportSheetRoute(
    onDismiss: () -> Unit,
    onDraftReady: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WebsiteImportViewModel = hiltViewModel(),
) {
    val resources = LocalResources.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // The view model outlives the sheet, so closing it for any reason clears it for next time.
    fun close(then: () -> Unit) {
        viewModel.onEvent(WebsiteImportEvent.Reset)
        then()
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ImportEffect.DraftReady -> close(onDraftReady)
                is ImportEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
            }
        }
    }

    WebsiteImportSheet(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onDismiss = { close(onDismiss) },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun WebsiteImportSheet(
    uiState: WebsiteImportUiState,
    onEvent: (WebsiteImportEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val paste = rememberPasteAction { onEvent(WebsiteImportEvent.UrlChanged(it)) }

    ImportSheet(
        icon = Icons.Outlined.Language,
        title = stringResource(R.string.import_website_title),
        explanation = stringResource(R.string.import_website_explanation),
        isExtracting = uiState.isExtracting,
        canExtract = uiState.canExtract,
        onExtract = { onEvent(WebsiteImportEvent.Extract) },
        onDismiss = onDismiss,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        OutlinedTextField(
            colors = paperFieldColors(),
            value = uiState.url,
            onValueChange = { onEvent(WebsiteImportEvent.UrlChanged(it)) },
            label = { Text(stringResource(R.string.import_website_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            enabled = !uiState.isExtracting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                autoCorrectEnabled = false,
            ),
            // Most addresses are copied from the browser, so pasting is one tap; it goes away
            // once there is something to check or correct.
            trailingIcon = if (uiState.url.isEmpty() && !uiState.isExtracting) {
                {
                    IconButton(onClick = paste) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = stringResource(R.string.settings_api_key_paste),
                        )
                    }
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun WebsiteImportSheetPreview() {
    ReceptariTheme {
        WebsiteImportSheet(
            uiState = WebsiteImportUiState(url = SAMPLE_URL),
            onEvent = {},
            onDismiss = {},
        )
    }
}

/** Preview fixture, not UI copy. */
private const val SAMPLE_URL = "cuinacatalana.example/arros-negre"
