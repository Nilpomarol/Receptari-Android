package cat.receptari.app.ui.transfer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.MainActivity
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.OrnamentalDivider
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.transfer.RecipeTransferError
import dagger.hilt.android.AndroidEntryPoint
import android.graphics.Color.TRANSPARENT

@AndroidEntryPoint
class TransferImportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val transferUri = intent.transferUri()
        setContent {
            ReceptariTheme {
                TransferImportRoute(
                    transferUri = transferUri,
                    onOpenLibrary = {
                        startActivity(
                            Intent(this, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            },
                        )
                        finish()
                    },
                    onClose = ::finish,
                )
            }
        }
    }

    private fun Intent.transferUri(): Uri? = when (action) {
        Intent.ACTION_SEND -> getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        Intent.ACTION_VIEW -> data
        else -> null
    }
}

@Composable
fun TransferImportRoute(
    transferUri: Uri?,
    onOpenLibrary: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransferImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(transferUri) { viewModel.import(transferUri) }
    TransferImportScreen(
        state = state,
        onOpenLibrary = onOpenLibrary,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
fun TransferImportScreen(
    state: TransferImportUiState,
    onOpenLibrary: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaperScaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                TransferImportUiState.Importing -> {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.transfer_importing),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                }
                is TransferImportUiState.Imported -> {
                    OrnamentHeading(title = stringResource(R.string.transfer_import_complete))
                    OrnamentalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        pluralStringResource(
                            R.plurals.transfer_recipes_imported,
                            state.result.importedRecipeCount,
                            state.result.importedRecipeCount,
                        ),
                    )
                    Text(
                        pluralStringResource(
                            R.plurals.transfer_translations_imported,
                            state.result.importedTranslationCount,
                            state.result.importedTranslationCount,
                        ),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    if (state.result.skippedDuplicateCount > 0) {
                        Aside(
                            text = pluralStringResource(
                                R.plurals.transfer_duplicates_skipped,
                                state.result.skippedDuplicateCount,
                                state.result.skippedDuplicateCount,
                            ),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    Button(onClick = onOpenLibrary, modifier = Modifier.padding(top = 24.dp)) {
                        Text(stringResource(R.string.transfer_open_library))
                    }
                }
                is TransferImportUiState.Failed -> {
                    OrnamentHeading(title = stringResource(R.string.transfer_import_failed_title))
                    Aside(
                        text = stringResource(state.error.messageRes()),
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Button(onClick = onClose, modifier = Modifier.padding(top = 24.dp)) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
        }
    }
}

private fun RecipeTransferError.messageRes(): Int = when (this) {
    is RecipeTransferError.EmptySelection -> R.string.transfer_empty
    is RecipeTransferError.InvalidPackage -> R.string.transfer_invalid
    is RecipeTransferError.NewerVersion -> R.string.transfer_newer_version
    is RecipeTransferError.Storage -> R.string.transfer_storage_error
}
