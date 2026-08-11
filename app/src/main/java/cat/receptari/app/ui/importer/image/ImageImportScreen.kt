package cat.receptari.app.ui.importer.image

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.ui.importer.ImportEffect
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun ImageImportRoute(
    onNavigateBack: () -> Unit,
    onDraftReady: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageImportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
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

    // Multiple selection, because a recipe spanning two cookbook pages is the normal case,
    // not the exception.
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PAGES),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val images = uris.mapNotNull { uri ->
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }
        if (images.isNotEmpty()) viewModel.onEvent(ImageImportEvent.PagesAdded(images))
    }

    // Same shape as the editor's capture: only the file name survives the camera app taking
    // over the screen, and the file is resolved again on the way back.
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { captured ->
        val fileName = pendingCapture
        pendingCapture = null
        if (!captured || fileName == null) return@rememberLauncherForActivityResult

        val file = context.captureFile(fileName)
        if (file.exists()) {
            viewModel.onEvent(ImageImportEvent.PagesAdded(listOf(file.readBytes())))
            file.delete()
        }
    }

    ImageImportScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onAddFromGallery = {
            pickImages.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onTakePhoto = {
            val fileName = "import-${System.currentTimeMillis()}.jpg"
            pendingCapture = fileName
            takePhoto.launch(context.captureUri(fileName))
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageImportScreen(
    uiState: ImageImportUiState,
    onEvent: (ImageImportEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onAddFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    PaperScaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PaperTopBar(
                title = { Text(stringResource(R.string.import_image_title)) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.import_image_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onAddFromGallery, enabled = !uiState.isExtracting) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Text(
                        text = stringResource(R.string.import_image_from_gallery),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                FilledTonalButton(onClick = onTakePhoto, enabled = !uiState.isExtracting) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Text(
                        text = stringResource(R.string.import_image_from_camera),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(uiState.pages, key = { it.id }) { page ->
                    PageThumbnail(
                        page = page,
                        enabled = !uiState.isExtracting,
                        onRemove = { onEvent(ImageImportEvent.PageRemoved(page.id)) },
                    )
                }
            }

            Button(
                onClick = { onEvent(ImageImportEvent.Extract) },
                enabled = uiState.canExtract,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isExtracting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = stringResource(R.string.import_extracting),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                } else {
                    Text(stringResource(R.string.import_action_extract))
                }
            }

            Text(
                text = stringResource(R.string.import_review_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun PageThumbnail(
    page: ImportPage,
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.aspectRatio(1f)) {
        AsyncImage(
            model = page.bytes,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
        )
        FilledTonalIconButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.import_image_remove_page),
            )
        }
    }
}

/** Matches the editor's capture path, `file_paths.xml`, and the manifest authority. */
private fun Context.captureFile(fileName: String): File =
    File(File(cacheDir, "camera").apply { mkdirs() }, fileName)

private fun Context.captureUri(fileName: String): Uri =
    FileProvider.getUriForFile(this, "$packageName.fileprovider", captureFile(fileName))

/** Well under the API's per-request image limit, and more pages than any recipe needs. */
private const val MAX_PAGES = 10

@Preview
@Composable
private fun ImageImportScreenPreview() {
    ReceptariTheme() {
        ImageImportScreen(
            uiState = ImageImportUiState(),
            onEvent = {},
            onNavigateBack = {},
            onAddFromGallery = {},
            onTakePhoto = {},
        )
    }
}
