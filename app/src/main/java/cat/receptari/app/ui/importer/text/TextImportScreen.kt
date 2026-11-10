package cat.receptari.app.ui.importer.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.paperFieldColors
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.ui.importer.ImportEffect
import cat.receptari.app.ui.importer.ImportForm
import cat.receptari.app.ui.importer.rememberPasteAction

@Composable
fun TextImportRoute(
    onNavigateBack: () -> Unit,
    onDraftReady: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TextImportViewModel = hiltViewModel(),
) {
    val resources = LocalResources.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ImportEffect.DraftReady -> onDraftReady()
                is ImportEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
            }
        }
    }

    TextImportScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun TextImportScreen(
    uiState: TextImportUiState,
    onEvent: (TextImportEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val paste = rememberPasteAction { onEvent(TextImportEvent.TextChanged(it)) }

    ImportForm(
        icon = Icons.AutoMirrored.Outlined.Notes,
        title = stringResource(R.string.import_text_title),
        explanation = stringResource(R.string.import_text_explanation),
        isExtracting = uiState.isExtracting,
        canExtract = uiState.canExtract,
        onExtract = { onEvent(TextImportEvent.Extract) },
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            OutlinedTextField(
                colors = paperFieldColors(),
                value = uiState.text,
                onValueChange = { onEvent(TextImportEvent.TextChanged(it)) },
                label = { Text(stringResource(R.string.import_text_label)) },
                shape = MaterialTheme.shapes.medium,
                enabled = !uiState.isExtracting,
                modifier = Modifier.fillMaxSize(),
            )
            // On an empty page the quickest way in is the clipboard; the pill leaves once
            // there is text.
            if (uiState.text.isEmpty() && !uiState.isExtracting) {
                OutlinedButton(
                    onClick = paste,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .height(44.dp),
                ) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.settings_api_key_paste),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

/** Preview fixture, not UI copy — kept out of the composable so it reads as sample data. */
private const val SAMPLE_TEXT = "Truita de patates\n\n4 ous\n2 patates"

@Preview
@Composable
private fun TextImportScreenPreview() {
    ReceptariTheme {
        TextImportScreen(
            uiState = TextImportUiState(text = SAMPLE_TEXT),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}
