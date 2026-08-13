package cat.receptari.app.ui.edit

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import cat.receptari.app.core.designsystem.PaperConfirmBottomSheet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import cat.receptari.app.core.designsystem.pageFrame
import cat.receptari.app.core.designsystem.paperFieldColors
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.Folder
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditRoute(
    onNavigateBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val captureScratch = remember(context) {
        CameraCaptureScratch(File(context.cacheDir, CAMERA_DIRECTORY))
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showCameraUnavailableDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is RecipeEditEffect.Saved -> onSaved(effect.recipeId)
            }
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            viewModel.onEvent(RecipeEditEvent.ImagePicked(stream.readBytes()))
        }
    }

    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        captureScratch.clearAbandoned(pendingCapture)
    }
    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { captured ->
        val fileName = pendingCapture
        pendingCapture = null
        fileName
            ?.let { captureScratch.consume(it, captured) }
            ?.let { bytes -> viewModel.onEvent(RecipeEditEvent.ImagePicked(bytes)) }
    }

    val leaveEditor = {
        if (hasUnsavedChanges) showDiscardDialog = true else onNavigateBack()
    }

    BackHandler(enabled = hasUnsavedChanges) { showDiscardDialog = true }

    RecipeEditScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = leaveEditor,
        onPickImage = {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onTakePhoto = {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (cameraIntent.resolveActivity(context.packageManager) == null) {
                showCameraUnavailableDialog = true
            } else {
                val fileName = captureScratch.createFileName(System.currentTimeMillis())
                pendingCapture = fileName
                try {
                    takePhoto.launch(context.captureUri(captureScratch.file(fileName)))
                } catch (_: ActivityNotFoundException) {
                    captureScratch.discard(fileName)
                    pendingCapture = null
                    showCameraUnavailableDialog = true
                }
            }
        },
        modifier = modifier,
    )

    if (showDiscardDialog) {
        PaperConfirmBottomSheet(
            title = stringResource(R.string.edit_discard_title),
            subtitle = stringResource(R.string.edit_discard_body),
            onDismissRequest = { showDiscardDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardChanges()
                        onNavigateBack()
                    },
                ) {
                    Text(stringResource(R.string.edit_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showCameraUnavailableDialog) {
        PaperConfirmBottomSheet(
            title = stringResource(R.string.edit_camera_unavailable_title),
            subtitle = stringResource(R.string.edit_camera_unavailable_body),
            onDismissRequest = { showCameraUnavailableDialog = false },
            confirmButton = {
                TextButton(onClick = { showCameraUnavailableDialog = false }) {
                    Text(stringResource(R.string.common_done))
                }
            },
        )
    }
}

private fun Context.captureUri(file: File): Uri =
    FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

private const val CAMERA_DIRECTORY = "camera"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    state: RecipeEditUiState,
    onEvent: (RecipeEditEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaperScaffold(
        modifier = modifier,
        topBar = {
            PaperTopBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.edit_title_new else R.string.edit_title_existing,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { onEvent(RecipeEditEvent.Save) },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text(stringResource(R.string.edit_save))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEvent(RecipeEditEvent.Save) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = {
                    Text(
                        text = stringResource(R.string.edit_save_recipe),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { innerPadding ->
        if (state.isLoading) return@PaperScaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Hero Title & Image Card
            item(key = "title-image-card") {
                PaperCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ImageField(
                            state = state,
                            onEvent = onEvent,
                            onPickImage = onPickImage,
                            onTakePhoto = onTakePhoto,
                        )

                        OutlinedTextField(
                            colors = paperFieldColors(),
                            value = state.title,
                            onValueChange = { onEvent(RecipeEditEvent.TitleChanged(it)) },
                            label = { Text(stringResource(R.string.edit_field_title)) },
                            isError = state.showTitleError,
                            supportingText = if (state.showTitleError) {
                                { Text(stringResource(R.string.edit_field_title_required)) }
                            } else {
                                null
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // 2. Times & Servings Card
            item(key = "times-servings-card") {
                PaperCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OrnamentHeading(title = stringResource(R.string.edit_section_times))

                        // Servings Stepper
                        ServingsStepper(
                            value = state.servings,
                            onValueChange = { onEvent(RecipeEditEvent.ServingsChanged(it)) },
                        )

                        // Times inputs with quick +5/-5 chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TimeStepperField(
                                label = stringResource(R.string.edit_prep_time_short),
                                value = state.prepTime,
                                onValueChange = { onEvent(RecipeEditEvent.PrepTimeChanged(it)) },
                                modifier = Modifier.weight(1f),
                            )
                            TimeStepperField(
                                label = stringResource(R.string.edit_cook_time_short),
                                value = state.cookTime,
                                onValueChange = { onEvent(RecipeEditEvent.CookTimeChanged(it)) },
                                modifier = Modifier.weight(1f),
                            )
                            TimeStepperField(
                                label = stringResource(R.string.edit_total_time_short),
                                value = state.totalTime,
                                onValueChange = { onEvent(RecipeEditEvent.TotalTimeChanged(it)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // 3. Ingredients Section Card
            item(key = "ingredients-card") {
                PaperCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OrnamentHeading(title = stringResource(R.string.edit_section_ingredients))

                        SectionListEditor(
                            kind = SectionKind.Ingredients,
                            sections = state.ingredientSections,
                            lineHintRes = R.string.edit_ingredient_hint,
                            addLineRes = R.string.edit_add_ingredient,
                            removeLineRes = R.string.edit_remove_ingredient,
                            onEvent = onEvent,
                        )
                    }
                }
            }

            // 4. Instructions Section Card
            item(key = "instructions-card") {
                PaperCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OrnamentHeading(title = stringResource(R.string.edit_section_instructions))

                        SectionListEditor(
                            kind = SectionKind.Instructions,
                            sections = state.instructionSections,
                            lineHintRes = R.string.edit_step_hint,
                            addLineRes = R.string.edit_add_step,
                            removeLineRes = R.string.edit_remove_step,
                            onEvent = onEvent,
                        )
                    }
                }
            }

            // 5. Details & Classifications (Rating, Folder, Tags)
            item(key = "details-card") {
                PaperCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OrnamentHeading(title = stringResource(R.string.edit_section_details))

                        RatingField(rating = state.rating, onEvent = onEvent)
                        FolderField(
                            folder = state.folder,
                            availableFolders = state.availableFolders,
                            onEvent = onEvent,
                        )
                        TagsField(tags = state.tags, onEvent = onEvent)
                    }
                }
            }

            // 6. Notes & Source Card
            item(key = "source-notes-card") {
                PaperCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OrnamentHeading(title = stringResource(R.string.edit_section_source_notes))

                        OutlinedTextField(
                            colors = paperFieldColors(),
                            value = state.notes,
                            onValueChange = { onEvent(RecipeEditEvent.NotesChanged(it)) },
                            label = { Text(stringResource(R.string.edit_field_notes)) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            colors = paperFieldColors(),
                            value = state.sourceName,
                            onValueChange = { onEvent(RecipeEditEvent.SourceNameChanged(it)) },
                            label = { Text(stringResource(R.string.edit_field_source)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            colors = paperFieldColors(),
                            value = state.sourceUrl,
                            onValueChange = { onEvent(RecipeEditEvent.SourceUrlChanged(it)) },
                            label = { Text(stringResource(R.string.edit_field_source_url)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionListEditor(
    kind: SectionKind,
    sections: List<FormSection>,
    lineHintRes: Int,
    addLineRes: Int,
    removeLineRes: Int,
    onEvent: (RecipeEditEvent) -> Unit,
) {
    var globalStepIndex = 0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        colors = paperFieldColors(),
                        value = section.name,
                        onValueChange = {
                            onEvent(RecipeEditEvent.SectionNameChanged(kind, section.id, it))
                        },
                        label = { Text(stringResource(R.string.edit_section_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    if (sections.size > 1) {
                        IconButton(
                            onClick = { onEvent(RecipeEditEvent.SectionRemoved(kind, section.id)) },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.edit_remove_section),
                            )
                        }
                    }
                }

                section.lines.forEach { line ->
                    if (kind == SectionKind.Instructions) globalStepIndex++

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (kind == SectionKind.Ingredients) {
                            Diamond(modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .pageFrame(CircleShape, ReceptariTheme.palette.rule, inset = 0.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = globalStepIndex.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        OutlinedTextField(
                            colors = paperFieldColors(),
                            value = line.text,
                            onValueChange = {
                                onEvent(RecipeEditEvent.LineChanged(kind, section.id, line.id, it))
                            },
                            placeholder = { Text(stringResource(lineHintRes)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f),
                        )

                        Column {
                            IconButton(
                                onClick = {
                                    onEvent(RecipeEditEvent.LineMoved(kind, section.id, line.id, -1))
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.edit_move_up),
                                )
                            }
                            IconButton(
                                onClick = {
                                    onEvent(RecipeEditEvent.LineMoved(kind, section.id, line.id, +1))
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.edit_move_down),
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onEvent(RecipeEditEvent.LineRemoved(kind, section.id, line.id))
                            },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(removeLineRes),
                            )
                        }
                    }
                }

                TextButton(
                    onClick = { onEvent(RecipeEditEvent.LineAdded(kind, section.id)) },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(
                        text = stringResource(addLineRes),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { onEvent(RecipeEditEvent.SectionAdded(kind)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(
                text = stringResource(R.string.edit_add_section),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageField(
    state: RecipeEditUiState,
    onEvent: (RecipeEditEvent) -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.imageDisplayPath?.let { path ->
            AsyncImage(
                model = path,
                contentDescription = stringResource(R.string.common_recipe_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pageFrame(MaterialTheme.shapes.medium, ReceptariTheme.palette.rule)
                    .padding(bottom = 8.dp),
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(onClick = onPickImage) {
                Icon(Icons.Default.Image, contentDescription = null)
                Text(
                    text = stringResource(R.string.edit_image_from_gallery),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            OutlinedButton(onClick = onTakePhoto) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Text(
                    text = stringResource(R.string.edit_image_from_camera),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            if (state.imagePath != null) {
                TextButton(onClick = { onEvent(RecipeEditEvent.ImageRemoved) }) {
                    Text(stringResource(R.string.edit_image_remove))
                }
            }
        }
    }
}

@Composable
private fun ServingsStepper(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val count = value.toIntOrNull() ?: 1

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.edit_field_servings),
            style = MaterialTheme.typography.titleMedium,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedIconButton(
                onClick = { onValueChange((count - 1).coerceAtLeast(1).toString()) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.detail_servings_decrease),
                    modifier = Modifier.size(18.dp),
                )
            }

            Text(
                text = value.ifEmpty { "1" },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            OutlinedIconButton(
                onClick = { onValueChange((count + 1).coerceAtMost(99).toString()) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.detail_servings_increase),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun TimeStepperField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        colors = paperFieldColors(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun RatingField(rating: Int?, onEvent: (RecipeEditEvent) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.edit_field_rating),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { star ->
                IconButton(
                    onClick = {
                        onEvent(RecipeEditEvent.RatingChanged(if (rating == star) null else star))
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (rating != null && star <= rating) {
                            Icons.Default.Star
                        } else {
                            Icons.Default.StarBorder
                        },
                        contentDescription = null,
                        tint = ReceptariTheme.palette.gold,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagsField(tags: List<String>, onEvent: (RecipeEditEvent) -> Unit) {
    var draft by remember { mutableStateOf("") }

    Column {
        Text(
            text = stringResource(R.string.edit_field_tags),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                colors = paperFieldColors(),
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(stringResource(R.string.edit_tag_add_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onEvent(RecipeEditEvent.TagAdded(draft))
                        draft = ""
                    }
                },
            ) {
                Text(stringResource(R.string.edit_tag_add))
            }
        }

        FlowRow(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tags.forEach { tag ->
                FilterChip(
                    selected = true,
                    onClick = { onEvent(RecipeEditEvent.TagRemoved(tag)) },
                    label = { Text(tag) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.edit_tag_remove, tag),
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun FolderField(
    folder: Folder?,
    availableFolders: List<Folder>,
    onEvent: (RecipeEditEvent) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    Column {
        Text(
            text = stringResource(R.string.edit_field_folder),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(onClick = { expanded = true }) {
                Text(folder?.name ?: stringResource(R.string.edit_folder_none))
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_folder_none)) },
                    onClick = {
                        onEvent(RecipeEditEvent.FolderSelected(null))
                        expanded = false
                    },
                )
                availableFolders.forEach { candidate ->
                    DropdownMenuItem(
                        text = { Text(candidate.name) },
                        onClick = {
                            onEvent(RecipeEditEvent.FolderSelected(candidate))
                            expanded = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_folder_add)) },
                    onClick = {
                        expanded = false
                        creating = true
                    },
                )
            }
        }

        if (creating) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    colors = paperFieldColors(),
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.edit_folder_add_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onEvent(RecipeEditEvent.FolderCreateRequested(draft))
                            draft = ""
                            creating = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.edit_folder_add))
                }
            }
        }
    }
}

@Composable
private fun Diamond(modifier: Modifier = Modifier) {
    val color = ReceptariTheme.palette.rule
    Canvas(modifier = modifier.size(6.dp)) {
        val half = size.minDimension / 2f
        drawPath(
            path = Path().apply {
                moveTo(half, 0f)
                lineTo(size.width, half)
                lineTo(half, size.height)
                lineTo(0f, half)
                close()
            },
            color = color,
        )
    }
}

private val PagePadding = 16.dp

@Preview
@Composable
private fun RecipeEditScreenPreview() {
    ReceptariTheme {
        RecipeEditScreen(
            state = RecipeEditUiState(
                title = "Pollastre amb salsa", // i18n-exempt: preview sample data
                servings = "4", // i18n-exempt: preview sample data
                tags = listOf("Sopar", "Ràpid"), // i18n-exempt: preview sample data
                ingredientSections = listOf(
                    FormSection(
                        name = "Salsa", // i18n-exempt: preview sample data
                        lines = listOf(
                            FormLine(text = "200 ml de nata"), // i18n-exempt: preview sample data
                            FormLine(text = "Sal al gust"), // i18n-exempt: preview sample data
                        ),
                    ),
                ),
            ),
            onEvent = {},
            onNavigateBack = {},
            onPickImage = {},
            onTakePhoto = {},
        )
    }
}
