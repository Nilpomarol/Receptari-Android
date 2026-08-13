package cat.receptari.app.ui.transfer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WifiTethering
import cat.receptari.app.core.designsystem.PaperConfirmBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.OrnamentalDivider
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.transfer.NearbyDevice
import cat.receptari.app.domain.transfer.NearbyTransferFailure
import cat.receptari.app.domain.transfer.NearbyTransferMode
import cat.receptari.app.domain.transfer.NearbyTransferPhase
import cat.receptari.app.domain.transfer.NearbyTransferState
import cat.receptari.app.domain.transfer.RecipeTransferArchive
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NearbyTransferActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            ReceptariTheme {
                NearbyTransferRoute(onClose = ::finish)
            }
        }
    }

    companion object {
        const val EXTRA_MODE = "nearbyTransferMode"
        const val EXTRA_ARCHIVE_PATH = "nearbyTransferArchivePath"

        fun sendIntent(context: Context, archive: RecipeTransferArchive): Intent =
            Intent(context, NearbyTransferActivity::class.java).apply {
                putExtra(EXTRA_MODE, NearbyTransferMode.SEND.name)
                putExtra(EXTRA_ARCHIVE_PATH, archive.file.absolutePath)
            }

        fun receiveIntent(context: Context): Intent =
            Intent(context, NearbyTransferActivity::class.java).apply {
                putExtra(EXTRA_MODE, NearbyTransferMode.RECEIVE.name)
            }
    }
}

@Composable
fun NearbyTransferRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NearbyTransferViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.all { it }) {
            viewModel.start()
        } else {
            permissionDenied = true
        }
    }

    LaunchedEffect(Unit) {
        val missing = nearbyPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) viewModel.start() else permissionLauncher.launch(missing.toTypedArray())
    }

    NearbyTransferScreen(
        mode = viewModel.mode,
        state = state,
        permissionDenied = permissionDenied,
        onConnect = viewModel::connect,
        onConfirmConnection = viewModel::confirmConnection,
        onRejectConnection = viewModel::rejectConnection,
        onClose = {
            viewModel.cancel()
            onClose()
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyTransferScreen(
    mode: NearbyTransferMode,
    state: NearbyTransferState,
    permissionDenied: Boolean,
    onConnect: (NearbyDevice) -> Unit,
    onConfirmConnection: () -> Unit,
    onRejectConnection: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaperScaffold(
        modifier = modifier,
        topBar = {
            PaperTopBar(
                title = {
                    Text(
                        stringResource(
                            if (mode == NearbyTransferMode.SEND) {
                                R.string.nearby_send_title
                            } else {
                                R.string.nearby_receive_title
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, stringResource(R.string.common_close))
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            permissionDenied -> NearbyMessage(
                title = stringResource(R.string.nearby_permission_title),
                body = stringResource(R.string.nearby_permission_body),
                action = stringResource(R.string.common_close),
                onAction = onClose,
                modifier = Modifier.padding(innerPadding),
            )
            state.phase == NearbyTransferPhase.ERROR -> NearbyMessage(
                title = stringResource(R.string.nearby_error_title),
                body = stringResource(state.failure.messageRes()),
                action = stringResource(R.string.common_close),
                onAction = onClose,
                modifier = Modifier.padding(innerPadding),
            )
            state.phase == NearbyTransferPhase.COMPLETE -> NearbyComplete(
                mode = mode,
                state = state,
                onClose = onClose,
                modifier = Modifier.padding(innerPadding),
            )
            mode == NearbyTransferMode.SEND && state.phase == NearbyTransferPhase.SEARCHING ->
                NearbyDevicePicker(
                    devices = state.devices,
                    onConnect = onConnect,
                    modifier = Modifier.padding(innerPadding),
                )
            else -> NearbyProgress(
                mode = mode,
                state = state,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (state.phase == NearbyTransferPhase.CONFIRMING) {
        PaperConfirmBottomSheet(
            title = stringResource(R.string.nearby_confirm_title, state.peerName.orEmpty()),
            onDismissRequest = onRejectConnection,
            body = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.nearby_confirm_body))
                    Text(
                        text = state.authenticationDigits.orEmpty(), // i18n-exempt: security code
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmConnection) {
                    Text(stringResource(R.string.nearby_code_matches))
                }
            },
            dismissButton = {
                TextButton(onClick = onRejectConnection) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun NearbyDevicePicker(
    devices: List<NearbyDevice>,
    onConnect: (NearbyDevice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OrnamentHeading(
            title = stringResource(R.string.nearby_choose_device),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
        Aside(
            text = stringResource(R.string.nearby_sender_help),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        )
        if (devices.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.nearby_searching),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(devices, key = { it.endpointId }) { device ->
                    PaperCard(
                        onClick = { onConnect(device) },
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.PhoneAndroid, contentDescription = null)
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyProgress(
    mode: NearbyTransferMode,
    state: NearbyTransferState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiTethering,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(state.progressMessageRes(mode)),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
        state.peerName?.let { peer ->
            Aside(text = peer, modifier = Modifier.padding(top = 8.dp))
        }
        state.progress?.let { progress ->
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            )
        } ?: CircularProgressIndicator(modifier = Modifier.padding(top = 20.dp))
    }
}

@Composable
private fun NearbyComplete(
    mode: NearbyTransferMode,
    state: NearbyTransferState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OrnamentHeading(
            title = stringResource(
                if (mode == NearbyTransferMode.SEND) {
                    R.string.nearby_sent_title
                } else {
                    R.string.nearby_received_title
                },
            ),
        )
        OrnamentalDivider(modifier = Modifier.padding(vertical = 16.dp))
        state.result?.let { result ->
            Text(
                pluralStringResource(
                    R.plurals.transfer_recipes_imported,
                    result.importedRecipeCount,
                    result.importedRecipeCount,
                ),
            )
            if (result.skippedDuplicateCount > 0) {
                Aside(
                    text = pluralStringResource(
                        R.plurals.transfer_duplicates_skipped,
                        result.skippedDuplicateCount,
                        result.skippedDuplicateCount,
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Button(onClick = onClose, modifier = Modifier.padding(top = 24.dp)) {
            Text(stringResource(R.string.common_close))
        }
    }
}

@Composable
private fun NearbyMessage(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            OrnamentHeading(title = title)
            Aside(text = body, modifier = Modifier.padding(top = 14.dp))
            Button(onClick = onAction, modifier = Modifier.padding(top = 24.dp)) {
                Text(action)
            }
        }
    }
}

private fun nearbyPermissions(): List<String> = listOf(
    Manifest.permission.BLUETOOTH_ADVERTISE,
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.NEARBY_WIFI_DEVICES,
)

private fun NearbyTransferState.progressMessageRes(mode: NearbyTransferMode): Int = when (phase) {
    NearbyTransferPhase.IDLE -> R.string.nearby_starting
    NearbyTransferPhase.SEARCHING -> R.string.nearby_searching
    NearbyTransferPhase.ADVERTISING -> R.string.nearby_receiver_waiting
    NearbyTransferPhase.CONNECTING -> R.string.nearby_connecting
    NearbyTransferPhase.CONFIRMING -> R.string.nearby_connecting
    NearbyTransferPhase.WAITING -> if (mode == NearbyTransferMode.SEND) {
        R.string.nearby_waiting_for_import
    } else {
        R.string.nearby_waiting_for_recipes
    }
    NearbyTransferPhase.TRANSFERRING -> R.string.nearby_transferring
    NearbyTransferPhase.IMPORTING -> R.string.nearby_importing
    NearbyTransferPhase.COMPLETE -> R.string.nearby_received_title
    NearbyTransferPhase.ERROR -> R.string.nearby_error_title
}

private fun NearbyTransferFailure?.messageRes(): Int = when (this) {
    NearbyTransferFailure.START -> R.string.nearby_error_start
    NearbyTransferFailure.CONNECTION -> R.string.nearby_error_connection
    NearbyTransferFailure.TRANSFER -> R.string.nearby_error_transfer
    NearbyTransferFailure.IMPORT -> R.string.nearby_error_import
    NearbyTransferFailure.DISCONNECTED -> R.string.nearby_error_disconnected
    null -> R.string.nearby_error_connection
}
