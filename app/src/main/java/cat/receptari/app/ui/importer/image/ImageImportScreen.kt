package cat.receptari.app.ui.importer.image

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.IconDisc
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.ui.importer.ImportEffect
import cat.receptari.app.ui.importer.ImportSheet
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun ImageImportSheetRoute(
    onDismiss: () -> Unit,
    onDraftReady: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageImportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // The view model outlives the sheet, so closing it for any reason clears it for next time.
    fun close(then: () -> Unit) {
        viewModel.onEvent(ImageImportEvent.Reset)
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

    ImageImportSheet(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onDismiss = { close(onDismiss) },
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

@Composable
fun ImageImportSheet(
    uiState: ImageImportUiState,
    onEvent: (ImageImportEvent) -> Unit,
    onDismiss: () -> Unit,
    onAddFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    ImportSheet(
        icon = Icons.Outlined.PhotoCamera,
        title = stringResource(R.string.import_image_title),
        explanation = stringResource(R.string.import_image_explanation),
        isExtracting = uiState.isExtracting,
        canExtract = uiState.canExtract,
        onExtract = { onEvent(ImageImportEvent.Extract) },
        onDismiss = onDismiss,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SourceTile(
                icon = Icons.Outlined.PhotoLibrary,
                label = stringResource(R.string.import_image_from_gallery),
                onClick = onAddFromGallery.takeUnless { uiState.isExtracting },
                modifier = Modifier.weight(1f),
            )
            SourceTile(
                icon = Icons.Outlined.PhotoCamera,
                label = stringResource(R.string.import_image_from_camera),
                onClick = onTakePhoto.takeUnless { uiState.isExtracting },
                modifier = Modifier.weight(1f),
            )
        }

        if (uiState.pages.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                itemsIndexed(uiState.pages, key = { _, page -> page.id }) { index, page ->
                    PageThumbnail(
                        page = page,
                        number = index + 1,
                        enabled = !uiState.isExtracting,
                        onRemove = { onEvent(ImageImportEvent.PageRemoved(page.id)) },
                        modifier = Modifier.size(88.dp),
                    )
                }
            }
        }
    }
}

/** One way of adding pages, as a soft card; a null [onClick] means it is not available now. */
@Composable
private fun SourceTile(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    PaperCard(
        onClick = onClick,
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconDisc(icon = icon)
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** A page of the recipe, in order: its photo, a wax-red number, and a way to take it out. */
@Composable
private fun PageThumbnail(
    page: ImportPage,
    number: Int,
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Box(modifier = modifier.aspectRatio(1f)) {
        AsyncImage(
            model = page.bytes,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .border(1.dp, ReceptariTheme.palette.rule.copy(alpha = 0.55f), shape),
        )
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .wrapContentSize(Alignment.Center),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f))
                .clickable(enabled = enabled, role = Role.Button, onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.import_image_remove_page),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp),
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
private fun ImageImportSheetPreview() {
    ReceptariTheme() {
        ImageImportSheet(
            uiState = ImageImportUiState(),
            onEvent = {},
            onDismiss = {},
            onAddFromGallery = {},
            onTakePhoto = {},
        )
    }
}
