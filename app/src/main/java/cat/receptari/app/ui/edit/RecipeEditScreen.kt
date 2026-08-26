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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.PaperConfirmBottomSheet
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import cat.receptari.app.core.designsystem.pageFrame
import cat.receptari.app.core.designsystem.paperFieldColors
import cat.receptari.app.core.designsystem.paperGrain
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.Folder
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.util.UUID

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
    // Focus plumbing for keyboard-driven entry. Each editable line registers a requester
    // keyed by its stable id; adding a line then simply names the id to move the cursor onto.
    val focusRegistry = remember { mutableStateMapOf<String, FocusRequester>() }
    var pendingFocusId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingFocusId) {
        val id = pendingFocusId ?: return@LaunchedEffect
        // One frame so a just-inserted row can attach its requester before we ask for focus.
        withFrameNanos {}
        runCatching { focusRegistry[id]?.requestFocus() }
        pendingFocusId = null
    }

    val addLineAndFocus: (SectionKind, String, String?) -> Unit = { kind, sectionId, afterLineId ->
        val newId = UUID.randomUUID().toString()
        onEvent(RecipeEditEvent.LineAdded(kind, sectionId, afterLineId = afterLineId, newLineId = newId))
        pendingFocusId = newId
    }

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
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                SaveBar(onSave = { onEvent(RecipeEditEvent.Save) })
            }
        },
    ) { innerPadding ->
        if (state.isLoading) return@PaperScaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Cover photo & title — the title page of the recipe.
            item(key = "cover-card") {
                PaperCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        CoverField(
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
                            textStyle = MaterialTheme.typography.headlineSmall,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // 2. Times & servings.
            item(key = "times-servings-card") {
                PaperCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OrnamentHeading(title = stringResource(R.string.edit_section_times))

                        ServingsStepper(
                            value = state.servings,
                            onValueChange = { onEvent(RecipeEditEvent.ServingsChanged(it)) },
                        )

                        Hairline()

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TimeField(
                                label = stringResource(R.string.edit_prep_time_short),
                                value = state.prepTime,
                                onValueChange = { onEvent(RecipeEditEvent.PrepTimeChanged(it)) },
                                modifier = Modifier.weight(1f),
                            )
                            TimeField(
                                label = stringResource(R.string.edit_cook_time_short),
                                value = state.cookTime,
                                onValueChange = { onEvent(RecipeEditEvent.CookTimeChanged(it)) },
                                modifier = Modifier.weight(1f),
                            )
                            TimeField(
                                label = stringResource(R.string.edit_total_time_short),
                                value = state.totalTime,
                                onValueChange = { onEvent(RecipeEditEvent.TotalTimeChanged(it)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // 3. Ingredients.
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
                            keyboardEntry = true,
                            sectionNameHintRes = R.string.edit_section_ingredients_hint,
                            lineHintRes = R.string.edit_ingredient_hint,
                            addLineRes = R.string.edit_add_ingredient,
                            removeLineRes = R.string.edit_remove_ingredient,
                            focusRegistry = focusRegistry,
                            onAddLineAndFocus = { sectionId, afterId ->
                                addLineAndFocus(SectionKind.Ingredients, sectionId, afterId)
                            },
                            onRequestFocus = { pendingFocusId = it },
                            onEvent = onEvent,
                        )
                    }
                }
            }

            // 4. Instructions.
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
                            keyboardEntry = false,
                            sectionNameHintRes = R.string.edit_section_instructions_hint,
                            lineHintRes = R.string.edit_step_hint,
                            addLineRes = R.string.edit_add_step,
                            removeLineRes = R.string.edit_remove_step,
                            focusRegistry = focusRegistry,
                            onAddLineAndFocus = { sectionId, afterId ->
                                addLineAndFocus(SectionKind.Instructions, sectionId, afterId)
                            },
                            onRequestFocus = { pendingFocusId = it },
                            onEvent = onEvent,
                        )
                    }
                }
            }

            // 5. Rating, folder, tags.
            item(key = "details-card") {
                PaperCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

            // 6. Source & notes.
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
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
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

/** The single primary action, pinned to the bottom so it stays under the thumb. */
@Composable
private fun SaveBar(onSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .paperGrain(intensity = 1f),
    ) {
        Hairline()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
        ) {
            Button(
                onClick = onSave,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.edit_save_recipe),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionListEditor(
    kind: SectionKind,
    sections: List<FormSection>,
    keyboardEntry: Boolean,
    sectionNameHintRes: Int,
    lineHintRes: Int,
    addLineRes: Int,
    removeLineRes: Int,
    focusRegistry: MutableMap<String, FocusRequester>,
    onAddLineAndFocus: (sectionId: String, afterLineId: String?) -> Unit,
    onRequestFocus: (lineId: String) -> Unit,
    onEvent: (RecipeEditEvent) -> Unit,
) {
    // Steps are numbered continuously across every section, so the count carries over.
    var stepNumber = 0

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        sections.forEach { section ->
            val startNumber = stepNumber
            if (kind == SectionKind.Instructions) stepNumber += section.lines.size

            SectionBlock(
                kind = kind,
                section = section,
                showSectionName = sections.size > 1 || section.name.isNotBlank(),
                canRemoveSection = sections.size > 1,
                keyboardEntry = keyboardEntry,
                startStepNumber = startNumber,
                sectionNameHintRes = sectionNameHintRes,
                lineHintRes = lineHintRes,
                addLineRes = addLineRes,
                removeLineRes = removeLineRes,
                focusRegistry = focusRegistry,
                onAddLineAndFocus = onAddLineAndFocus,
                onRequestFocus = onRequestFocus,
                onEvent = onEvent,
            )
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

@Composable
private fun SectionBlock(
    kind: SectionKind,
    section: FormSection,
    showSectionName: Boolean,
    canRemoveSection: Boolean,
    keyboardEntry: Boolean,
    startStepNumber: Int,
    sectionNameHintRes: Int,
    lineHintRes: Int,
    addLineRes: Int,
    removeLineRes: Int,
    focusRegistry: MutableMap<String, FocusRequester>,
    onAddLineAndFocus: (sectionId: String, afterLineId: String?) -> Unit,
    onRequestFocus: (lineId: String) -> Unit,
    onEvent: (RecipeEditEvent) -> Unit,
) {
    // Drag-to-reorder state, scoped to this section. Rows report their measured heights so a
    // drag knows when it has travelled far enough to swap past a neighbour of any size.
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    val rowHeights = remember { mutableStateMapOf<String, Int>() }
    val currentLines by rememberUpdatedState(section.lines)
    val currentOnEvent by rememberUpdatedState(onEvent)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showSectionName) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    colors = paperFieldColors(),
                    value = section.name,
                    onValueChange = {
                        onEvent(RecipeEditEvent.SectionNameChanged(kind, section.id, it))
                    },
                    label = { Text(stringResource(sectionNameHintRes)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                if (canRemoveSection) {
                    IconButton(
                        onClick = { onEvent(RecipeEditEvent.SectionRemoved(kind, section.id)) },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.edit_remove_section),
                        )
                    }
                }
            }
        }

        section.lines.forEachIndexed { index, line ->
            key(line.id) {
                val requester = remember(line.id) { FocusRequester() }
                DisposableEffect(line.id) {
                    focusRegistry[line.id] = requester
                    onDispose { focusRegistry.remove(line.id) }
                }

                val isLast = index == section.lines.lastIndex
                val isDragging = draggingId == line.id

                val handleModifier = Modifier.pointerInput(line.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingId = line.id
                            dragDelta = 0f
                        },
                        onDragEnd = {
                            draggingId = null
                            dragDelta = 0f
                        },
                        onDragCancel = {
                            draggingId = null
                            dragDelta = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragDelta += amount.y
                            val lines = currentLines
                            val at = lines.indexOfFirst { it.id == line.id }
                            if (at > 0 && dragDelta < 0) {
                                val prevHeight = rowHeights[lines[at - 1].id] ?: 0
                                if (prevHeight > 0 && -dragDelta > prevHeight / 2f) {
                                    currentOnEvent(
                                        RecipeEditEvent.LineMoved(kind, section.id, line.id, -1),
                                    )
                                    dragDelta += prevHeight
                                }
                            } else if (at in 0 until lines.lastIndex && dragDelta > 0) {
                                val nextHeight = rowHeights[lines[at + 1].id] ?: 0
                                if (nextHeight > 0 && dragDelta > nextHeight / 2f) {
                                    currentOnEvent(
                                        RecipeEditEvent.LineMoved(kind, section.id, line.id, +1),
                                    )
                                    dragDelta -= nextHeight
                                }
                            }
                        },
                    )
                }

                LineRow(
                    kind = kind,
                    stepNumber = startStepNumber + index + 1,
                    text = line.text,
                    hintRes = lineHintRes,
                    removeLineRes = removeLineRes,
                    keyboardEntry = keyboardEntry,
                    canMoveUp = index > 0,
                    canMoveDown = !isLast,
                    isDragging = isDragging,
                    dragTranslation = if (isDragging) dragDelta else 0f,
                    focusRequester = requester,
                    handleModifier = handleModifier,
                    onHeightMeasured = { rowHeights[line.id] = it },
                    onTextChange = {
                        onEvent(RecipeEditEvent.LineChanged(kind, section.id, line.id, it))
                    },
                    onImeNext = {
                        if (isLast) {
                            onAddLineAndFocus(section.id, line.id)
                        } else {
                            onRequestFocus(section.lines[index + 1].id)
                        }
                    },
                    onBackspaceWhenEmpty = {
                        if (index > 0) {
                            val previousId = section.lines[index - 1].id
                            onEvent(RecipeEditEvent.LineRemoved(kind, section.id, line.id))
                            onRequestFocus(previousId)
                        }
                    },
                    onMoveUp = {
                        onEvent(RecipeEditEvent.LineMoved(kind, section.id, line.id, -1))
                    },
                    onMoveDown = {
                        onEvent(RecipeEditEvent.LineMoved(kind, section.id, line.id, +1))
                    },
                    onRemove = {
                        onEvent(RecipeEditEvent.LineRemoved(kind, section.id, line.id))
                    },
                )
            }
        }

        TextButton(
            onClick = { onAddLineAndFocus(section.id, null) },
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

@Composable
private fun LineRow(
    kind: SectionKind,
    stepNumber: Int,
    text: String,
    hintRes: Int,
    removeLineRes: Int,
    keyboardEntry: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isDragging: Boolean,
    dragTranslation: Float,
    focusRequester: FocusRequester,
    handleModifier: Modifier,
    onHeightMeasured: (Int) -> Unit,
    onTextChange: (String) -> Unit,
    onImeNext: () -> Unit,
    onBackspaceWhenEmpty: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val keyboardOptions = if (keyboardEntry) {
        KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next,
        )
    } else {
        KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    }
    val reorderHandle = stringResource(R.string.edit_reorder_handle)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = dragTranslation
                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
            }
            .onGloballyPositioned { onHeightMeasured(it.size.height) },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The marker doubles as the grab handle: long-press it and drag to reorder. A
        // numbered stamp for steps, a printer's diamond for ingredients.
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(28.dp)
                .then(handleModifier)
                .semantics { contentDescription = reorderHandle },
            contentAlignment = Alignment.Center,
        ) {
            if (kind == SectionKind.Instructions) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .pageFrame(CircleShape, ReceptariTheme.palette.rule, inset = 0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Diamond()
            }
        }

        LineTextField(
            value = text,
            onValueChange = onTextChange,
            hintRes = hintRes,
            singleLine = keyboardEntry,
            keyboardOptions = keyboardOptions,
            focusRequester = focusRequester,
            onImeNext = onImeNext,
            onBackspaceWhenEmpty = onBackspaceWhenEmpty,
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
        )

        Box(modifier = Modifier.padding(top = 4.dp)) {
            LineOverflowMenu(
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                removeLineRes = removeLineRes,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onRemove = onRemove,
            )
        }
    }
}

/**
 * A line of the recipe as a ruled entry rather than a boxed field: text on a hairline that
 * inks olive when focused. Reads as writing on paper and hands the full width back to the
 * words, instead of stacking another outlined box inside the card.
 */
@Composable
private fun LineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hintRes: Int,
    singleLine: Boolean,
    keyboardOptions: KeyboardOptions,
    focusRequester: FocusRequester,
    onImeNext: () -> Unit,
    onBackspaceWhenEmpty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val palette = ReceptariTheme.palette
    val primary = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(onNext = { onImeNext() }),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(vertical = 8.dp)) {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(hintRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    // A hardware keyboard's backspace on an empty line folds it back into the
                    // previous one, the way a list editor does. Soft keyboards fall back to
                    // the explicit remove control.
                    if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.Backspace &&
                        value.isEmpty()
                    ) {
                        onBackspaceWhenEmpty()
                        true
                    } else {
                        false
                    }
                },
        )
        Hairline(
            color = if (focused) primary else palette.rule,
            alpha = if (focused) 1f else 0.5f,
        )
    }
}

/** Accessible fallback for reordering and removal, for when dragging is not an option. */
@Composable
private fun LineOverflowMenu(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    removeLineRes: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.edit_line_options),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_move_up)) },
                enabled = canMoveUp,
                leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) },
                onClick = {
                    onMoveUp()
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_move_down)) },
                enabled = canMoveDown,
                leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                onClick = {
                    onMoveDown()
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(removeLineRes)) },
                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                onClick = {
                    onRemove()
                    expanded = false
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoverField(
    state: RecipeEditUiState,
    onEvent: (RecipeEditEvent) -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    val imagePath = state.imageDisplayPath
    if (imagePath != null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = stringResource(R.string.common_recipe_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .pageFrame(MaterialTheme.shapes.medium, ReceptariTheme.palette.rule),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedButton(onClick = onPickImage) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.edit_cover_change),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                OutlinedButton(onClick = onTakePhoto) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.edit_image_from_camera),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                TextButton(onClick = { onEvent(RecipeEditEvent.ImageRemoved) }) {
                    Text(stringResource(R.string.edit_image_remove))
                }
            }
        }
    } else {
        // Empty state: a framed well that names what goes here, with the two ways to fill it.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .pageFrame(MaterialTheme.shapes.medium, ReceptariTheme.palette.rule)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                tint = ReceptariTheme.palette.rule,
                modifier = Modifier.size(36.dp),
            )
            Aside(
                text = stringResource(R.string.edit_cover_hint),
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedButton(onClick = onPickImage) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.edit_image_from_gallery),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                OutlinedButton(onClick = onTakePhoto) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.edit_image_from_camera),
                        modifier = Modifier.padding(start = 6.dp),
                    )
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
                modifier = Modifier.size(38.dp),
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
                modifier = Modifier.padding(horizontal = 18.dp),
            )

            OutlinedIconButton(
                onClick = { onValueChange((count + 1).coerceAtMost(99).toString()) },
                modifier = Modifier.size(38.dp),
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
private fun TimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val palette = ReceptariTheme.palette
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (value.isEmpty()) {
                                Text(
                                    text = "0", // i18n-exempt: locale-neutral numeral placeholder
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        }
                        Text(
                            text = stringResource(R.string.edit_minutes_unit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
            )
            Hairline(
                color = if (focused) primary else palette.rule,
                alpha = if (focused) 1f else 0.5f,
            )
        }
    }
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
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = if (rating != null && star <= rating) {
                            Icons.Default.Star
                        } else {
                            Icons.Default.StarBorder
                        },
                        contentDescription = null,
                        tint = ReceptariTheme.palette.gold,
                        modifier = Modifier.size(26.dp),
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
    val commit = {
        if (draft.isNotBlank()) {
            onEvent(RecipeEditEvent.TagAdded(draft))
            draft = ""
        }
    }

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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = commit) {
                Text(stringResource(R.string.edit_tag_add))
            }
        }

        if (tags.isNotEmpty()) {
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
                prepTime = "15", // i18n-exempt: preview sample data
                cookTime = "40", // i18n-exempt: preview sample data
                rating = 4,
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
                instructionSections = listOf(
                    FormSection(
                        lines = listOf(
                            FormLine(text = "Salpebra el pollastre."), // i18n-exempt: preview sample data
                            FormLine(text = "Daura'l a foc viu."), // i18n-exempt: preview sample data
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
