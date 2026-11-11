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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Timer
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.IconDisc
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.Rubric
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperConfirmBottomSheet
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import cat.receptari.app.core.designsystem.paperFieldColors
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.CookingTime
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
    var errorMessage by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is RecipeEditEffect.Saved -> onSaved(effect.recipeId)
                RecipeEditEffect.SaveFailed -> errorMessage = R.string.edit_save_failed
                RecipeEditEffect.ImageFailed -> errorMessage = R.string.edit_image_failed
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

    errorMessage?.let { message ->
        PaperConfirmBottomSheet(
            title = stringResource(message),
            onDismissRequest = { errorMessage = null },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text(stringResource(R.string.common_done))
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

/**
 * The recipe form holds the recipe's own content, in the order the recipe page prints it:
 * title and photo, servings and times, ingredients, steps, then source and notes folded
 * away. Rating, folder and tags are the reader's, and live on the recipe page.
 */
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
                actions = {
                    // In the top bar rather than pinned to the bottom: a bottom bar sits on
                    // the keyboard and eats the rows being typed into.
                    if (!state.isLoading) {
                        Button(
                            onClick = { onEvent(RecipeEditEvent.Save) },
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(40.dp),
                        ) {
                            Text(stringResource(R.string.edit_save))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) return@PaperScaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(
                start = PagePadding,
                end = PagePadding,
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
            ),
            // Each group is its own card laid on the page, so the parts of the recipe read as
            // distinct sheets rather than as one long run of rules.
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "header") {
                TitleCard(
                    state = state,
                    onEvent = onEvent,
                    onPickImage = onPickImage,
                    onTakePhoto = onTakePhoto,
                )
            }

            item(key = "ingredients") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OrnamentHeading(
                        title = stringResource(R.string.edit_section_ingredients),
                        icon = Icons.Outlined.ShoppingBasket,
                    )
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

            item(key = "instructions") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OrnamentHeading(
                        title = stringResource(R.string.edit_section_instructions),
                        icon = Icons.Outlined.FormatListNumbered,
                    )
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

            item(key = "more") {
                Column {
                    Hairline()
                    ExpandableField(
                        icon = Icons.Outlined.Link,
                        label = stringResource(R.string.detail_source),
                        summary = state.sourceName.ifBlank { state.sourceUrl.urlHost() },
                        initiallyExpanded = state.sourceName.isNotBlank() || state.sourceUrl.isNotBlank(),
                    ) {
                        OutlinedTextField(
                            colors = paperFieldColors(),
                            value = state.sourceName,
                            onValueChange = { onEvent(RecipeEditEvent.SourceNameChanged(it)) },
                            label = { Text(stringResource(R.string.edit_field_source)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            colors = paperFieldColors(),
                            value = state.sourceUrl,
                            onValueChange = { onEvent(RecipeEditEvent.SourceUrlChanged(it)) },
                            label = { Text(stringResource(R.string.edit_field_source_url)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                autoCorrectEnabled = false,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Hairline()
                    // Imported notes start open: extracted text is reviewed before it is saved.
                    ExpandableField(
                        icon = Icons.Outlined.EditNote,
                        label = stringResource(R.string.detail_notes),
                        summary = state.notes.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(),
                        initiallyExpanded = state.notes.isNotBlank(),
                    ) {
                        // Same as the notes sheet on the recipe page: one notes field, one look.
                        OutlinedTextField(
                            colors = paperFieldColors(),
                            value = state.notes,
                            onValueChange = { onEvent(RecipeEditEvent.NotesChanged(it)) },
                            placeholder = { Text(stringResource(R.string.detail_notes_hint)) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                            shape = MaterialTheme.shapes.medium,
                            minLines = 3,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Hairline()
                }
            }
        }
    }
}

/**
 * The recipe's title page, as one card: the cover across its top, then the title, servings
 * and times. The two lists below get cards of their own; source and notes stay on the page,
 * so three sheets carry the form without boxing every part of it.
 */
@Composable
private fun TitleCard(
    state: RecipeEditUiState,
    onEvent: (RecipeEditEvent) -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    PaperCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            CoverBanner(
                imagePath = state.imageDisplayPath,
                onPickImage = onPickImage,
                onTakePhoto = onTakePhoto,
                onRemove = { onEvent(RecipeEditEvent.ImageRemoved) },
            )
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column {
                    Rubric(
                        text = stringResource(R.string.edit_field_title),
                        color = if (state.showTitleError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    )
                    RuledTextField(
                        value = state.title,
                        // Wraps for long titles, but a title is still one line of text.
                        onValueChange = { onEvent(RecipeEditEvent.TitleChanged(it.replace('\n', ' '))) },
                        placeholder = stringResource(R.string.edit_title_placeholder),
                        textStyle = MaterialTheme.typography.headlineMedium,
                        maxLines = 3,
                        isError = state.showTitleError,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    if (state.showTitleError) {
                        Text(
                            text = stringResource(R.string.edit_field_title_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                FactsBlock(state = state, onEvent = onEvent)
            }
        }
    }
}

/** Servings and the three times, as filled tiles that read as values to set at a glance. */
@Composable
private fun FactsBlock(
    state: RecipeEditUiState,
    onEvent: (RecipeEditEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ServingsStepper(
            value = state.servings,
            onValueChange = { onEvent(RecipeEditEvent.ServingsChanged(it)) },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TimeTile(
                icon = Icons.Outlined.Timer,
                label = stringResource(R.string.edit_prep_time_short),
                value = state.prepTime,
                onValueChange = { onEvent(RecipeEditEvent.PrepTimeChanged(it)) },
                modifier = Modifier.weight(1f),
            )
            TimeTile(
                icon = Icons.Outlined.LocalFireDepartment,
                label = stringResource(R.string.edit_cook_time_short),
                value = state.cookTime,
                onValueChange = { onEvent(RecipeEditEvent.CookTimeChanged(it)) },
                modifier = Modifier.weight(1f),
            )
            // Left empty, the total is prep + cook; it is only typed when it differs, as
            // when a dough rests or a stew marinates.
            TimeTile(
                icon = Icons.Outlined.Schedule,
                label = stringResource(R.string.edit_total_time_short),
                value = state.totalTime,
                onValueChange = { onEvent(RecipeEditEvent.TotalTimeChanged(it)) },
                placeholder = CookingTime.derivedTotal(
                    state.prepTime.toIntOrNull(),
                    state.cookTime.toIntOrNull(),
                )?.toString() ?: ZeroPlaceholder,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CoverBanner(
    imagePath: String?,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val description = stringResource(
        if (imagePath == null) R.string.edit_cover_hint else R.string.edit_cover_change,
    )

    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (imagePath == null) 128.dp else 200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(role = Role.Button, onClickLabel = description) { menuOpen = true },
            contentAlignment = Alignment.Center,
        ) {
            if (imagePath != null) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = stringResource(R.string.common_recipe_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                IconDisc(
                    icon = Icons.Outlined.PhotoCamera,
                    size = 36.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconDisc(icon = Icons.Outlined.AddPhotoAlternate)
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_image_from_gallery)) },
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onPickImage()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_image_from_camera)) },
                leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onTakePhoto()
                },
            )
            if (imagePath != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_image_remove)) },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onRemove()
                    },
                )
            }
        }
    }
}

/** A folded row for what most recipes leave empty: a label, a one-line summary, a chevron. */
@Composable
private fun ExpandableField(
    icon: ImageVector,
    label: String,
    summary: String,
    initiallyExpanded: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconDisc(icon = icon, size = 36.dp)
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (!expanded) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp),
                content = content,
            )
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sections.forEach { section ->
            val startNumber = stepNumber
            if (kind == SectionKind.Instructions) stepNumber += section.lines.size

            // One card per group, so "Per a la salsa" and "Per a la massa" read as two sheets.
            PaperCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            ) {
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
        }

        // Most recipes have one group; adding another is a quiet link, not a button.
        TextButton(
            onClick = { onEvent(RecipeEditEvent.SectionAdded(kind)) },
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.edit_add_section),
                modifier = Modifier.padding(start = 6.dp),
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

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showSectionName) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RuledTextField(
                    value = section.name,
                    onValueChange = {
                        onEvent(RecipeEditEvent.SectionNameChanged(kind, section.id, it))
                    },
                    placeholder = stringResource(sectionNameHintRes),
                    textStyle = MaterialTheme.typography.titleMedium,
                    textColor = MaterialTheme.colorScheme.primary,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
                // Where the cursor goes when this line disappears: up, or down if it was first.
                val neighbourId = section.lines.getOrNull(index - 1)?.id
                    ?: section.lines.getOrNull(index + 1)?.id

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
                    canRemove = section.lines.size > 1 || line.text.isNotEmpty(),
                    isDragging = isDragging,
                    dragTranslation = if (isDragging) dragDelta else 0f,
                    focusRequester = requester,
                    handleModifier = handleModifier,
                    onHeightMeasured = { rowHeights[line.id] = it },
                    onTextChange = { text ->
                        val parts = splitEnteredText(text)
                        if (parts == null || parts.size == 1) {
                            val single = parts?.single() ?: text
                            onEvent(RecipeEditEvent.LineChanged(kind, section.id, line.id, single))
                        } else {
                            val newLines = parts.drop(1).map { FormLine(text = it) }
                            onEvent(
                                RecipeEditEvent.LineSplit(kind, section.id, line.id, parts.first(), newLines),
                            )
                            onRequestFocus(newLines.last().id)
                        }
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
                        neighbourId?.let(onRequestFocus)
                    },
                )
            }
        }

        TextButton(
            onClick = { onAddLineAndFocus(section.id, null) },
            modifier = Modifier.align(Alignment.Start),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
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
    canRemove: Boolean,
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
    var focused by remember { mutableStateOf(false) }
    val keyboardOptions = if (keyboardEntry) {
        KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next,
        )
    } else {
        KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    }
    val reorderHandle = stringResource(R.string.edit_reorder_handle)
    val moveUpLabel = stringResource(R.string.edit_move_up)
    val moveDownLabel = stringResource(R.string.edit_move_down)
    val removeLabel = stringResource(removeLineRes)

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
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            } else {
                Diamond(color = MaterialTheme.colorScheme.primary)
            }
        }

        RuledTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = stringResource(hintRes),
            singleLine = keyboardEntry,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(onNext = { onImeNext() }),
            onFocusChanged = { focused = it },
            fieldModifier = Modifier
                .focusRequester(focusRequester)
                // Reordering and removal for screen readers, without a menu on every line.
                .semantics {
                    customActions = buildList {
                        if (canMoveUp) add(CustomAccessibilityAction(moveUpLabel) { onMoveUp(); true })
                        if (canMoveDown) add(CustomAccessibilityAction(moveDownLabel) { onMoveDown(); true })
                        if (canRemove) add(CustomAccessibilityAction(removeLabel) { onRemove(); true })
                    }
                }
                .onPreviewKeyEvent { event ->
                    // A hardware keyboard's backspace on an empty line folds it back into the
                    // previous one, the way a list editor does.
                    if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.Backspace &&
                        text.isEmpty()
                    ) {
                        onBackspaceWhenEmpty()
                        true
                    } else {
                        false
                    }
                },
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
        )

        // Remove shows only on the line being written, so a long list stays quiet.
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (focused && canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = removeLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Text written on a rule rather than in a box: a hairline that inks wax red when focused.
 * Reads as writing on paper and hands the full width back to the words. [fieldModifier] goes
 * on the text field itself, for focus requesters and key handling.
 */
@Composable
private fun RuledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocusChanged: (Boolean) -> Unit = {},
    fieldModifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val palette = ReceptariTheme.palette
    val accent = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            maxLines = maxLines,
            textStyle = textStyle.copy(color = textColor),
            cursorBrush = SolidColor(accent),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(vertical = 8.dp)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
            modifier = fieldModifier
                .fillMaxWidth()
                .onFocusChanged {
                    focused = it.isFocused
                    onFocusChanged(it.isFocused)
                },
        )
        Hairline(
            color = if (focused || isError) accent else palette.rule,
            alpha = if (focused || isError) 1f else 0.5f,
        )
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
    ) {
        IconDisc(icon = Icons.Outlined.People, size = 36.dp)
        Text(
            text = stringResource(R.string.edit_field_servings),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
        ) {
            IconButton(
                onClick = { onValueChange((count - 1).coerceAtLeast(1).toString()) },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.detail_servings_decrease),
                    modifier = Modifier.size(18.dp),
                )
            }

            Text(
                text = value.ifEmpty { "1" },
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = LiningFigures),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(40.dp),
            )

            // A plain disc rather than IconButton: IconButton pads itself out to 48dp, which
            // pushed the red fill past the pill it sits in.
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(role = Role.Button) {
                        onValueChange((count + 1).coerceAtMost(99).toString())
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.detail_servings_increase),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * A time as a filled tile: its pictogram and name, then the minutes. The whole tile is the
 * text field, so a tap anywhere on it starts typing.
 */
@Composable
private fun TimeTile(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ZeroPlaceholder,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.medium
    val primary = MaterialTheme.colorScheme.primary

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontFeatureSettings = LiningFigures,
        ),
        cursorBrush = SolidColor(primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = { innerTextField ->
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (focused) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    // The inner field fills what it is given, so it gets the room left after
                    // the unit rather than squeezing the unit out.
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = LiningFigures),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        innerTextField()
                    }
                    Text(
                        text = stringResource(R.string.edit_minutes_unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 3.dp),
                    )
                }
            }
        },
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (focused) primary else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused },
    )
}

@Composable
private fun Diamond(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(7.dp)) {
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

private fun String.urlHost(): String =
    takeIf { it.isNotBlank() }?.let { Uri.parse(it.trim()).host ?: it.trim() }.orEmpty()

private val PagePadding = 16.dp
private val CoverSize = 88.dp
/** Lining figures: Playfair's default old-style digits drop below the line beside icons. */
private const val LiningFigures = "lnum"
private const val ZeroPlaceholder = "0" // i18n-exempt: locale-neutral numeral placeholder

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
