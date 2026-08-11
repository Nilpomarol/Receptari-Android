package cat.receptari.app.ui.edit

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import java.io.File

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

    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is RecipeEditEffect.Saved -> onSaved(effect.recipeId)
            }
        }
    }

    // The photo picker needs no runtime permission, which keeps choosing an image a single
    // tap with no permission dialog to explain.
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            viewModel.onEvent(RecipeEditEvent.ImagePicked(stream.readBytes()))
        }
    }

    // The capture target must exist before launching and survive the camera app taking over
    // the screen — including process death — so only its name is held, and the file is
    // resolved again on the way back.
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { captured ->
        val fileName = pendingCapture
        pendingCapture = null
        if (!captured || fileName == null) return@rememberLauncherForActivityResult

        val file = context.captureFile(fileName)
        if (file.exists()) {
            viewModel.onEvent(RecipeEditEvent.ImagePicked(file.readBytes()))
            // ImageStore has its own downscaled copy now; this one is scratch.
            file.delete()
        }
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
            val fileName = "capture-${System.currentTimeMillis()}.jpg"
            pendingCapture = fileName
            takePhoto.launch(context.captureUri(fileName))
        },
        modifier = modifier,
    )

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.edit_discard_title)) },
            text = { Text(stringResource(R.string.edit_discard_body)) },
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
}

private fun Context.captureFile(fileName: String): File =
    File(File(cacheDir, "camera").apply { mkdirs() }, fileName)

/** Must match the `file_paths.xml` cache-path and the manifest authority. */
private fun Context.captureUri(fileName: String): Uri =
    FileProvider.getUriForFile(this, "$packageName.fileprovider", captureFile(fileName))

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
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
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
                    TextButton(onClick = { onEvent(RecipeEditEvent.Save) }) {
                        Text(stringResource(R.string.edit_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 48.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ImageField(
                    state = state,
                    onEvent = onEvent,
                    onPickImage = onPickImage,
                    onTakePhoto = onTakePhoto,
                )
            }

            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NumberField(
                        value = state.prepTime,
                        label = stringResource(R.string.edit_field_prep_time),
                        onValueChange = { onEvent(RecipeEditEvent.PrepTimeChanged(it)) },
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        value = state.cookTime,
                        label = stringResource(R.string.edit_field_cook_time),
                        onValueChange = { onEvent(RecipeEditEvent.CookTimeChanged(it)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NumberField(
                        value = state.totalTime,
                        label = stringResource(R.string.edit_field_total_time),
                        onValueChange = { onEvent(RecipeEditEvent.TotalTimeChanged(it)) },
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        value = state.servings,
                        label = stringResource(R.string.edit_field_servings),
                        onValueChange = { onEvent(RecipeEditEvent.ServingsChanged(it)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item { RatingField(rating = state.rating, onEvent = onEvent) }
            item { TagsField(tags = state.tags, onEvent = onEvent) }

            sectionEditor(
                titleRes = R.string.edit_section_ingredients,
                kind = SectionKind.Ingredients,
                sections = state.ingredientSections,
                lineHintRes = R.string.edit_ingredient_hint,
                addLineRes = R.string.edit_add_ingredient,
                removeLineRes = R.string.edit_remove_ingredient,
                onEvent = onEvent,
            )

            sectionEditor(
                titleRes = R.string.edit_section_instructions,
                kind = SectionKind.Instructions,
                sections = state.instructionSections,
                lineHintRes = R.string.edit_step_hint,
                addLineRes = R.string.edit_add_step,
                removeLineRes = R.string.edit_remove_step,
                onEvent = onEvent,
            )

            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    HorizontalDivider(Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { onEvent(RecipeEditEvent.NotesChanged(it)) },
                        label = { Text(stringResource(R.string.edit_field_notes)) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.sourceName,
                        onValueChange = { onEvent(RecipeEditEvent.SourceNameChanged(it)) },
                        label = { Text(stringResource(R.string.edit_field_source)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
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

private fun androidx.compose.foundation.lazy.LazyListScope.sectionEditor(
    titleRes: Int,
    kind: SectionKind,
    sections: List<FormSection>,
    lineHintRes: Int,
    addLineRes: Int,
    removeLineRes: Int,
    onEvent: (RecipeEditEvent) -> Unit,
) {
    item(key = "header-$kind") {
        Column(Modifier.padding(horizontal = 16.dp)) {
            HorizontalDivider(Modifier.padding(bottom = 8.dp))
            Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleLarge)
        }
    }

    sections.forEach { section ->
        item(key = "$kind-${section.id}") {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
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
                                modifier = Modifier.size(32.dp),
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
                                modifier = Modifier.size(32.dp),
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
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(removeLineRes),
                            )
                        }
                    }
                }

                TextButton(onClick = { onEvent(RecipeEditEvent.LineAdded(kind, section.id)) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(
                        text = stringResource(addLineRes),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }

    item(key = "add-section-$kind") {
        OutlinedButton(
            onClick = { onEvent(RecipeEditEvent.SectionAdded(kind)) },
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(stringResource(R.string.edit_add_section))
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
    Column {
        state.imageDisplayPath?.let { path ->
            AsyncImage(
                model = path,
                contentDescription = stringResource(R.string.common_recipe_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            )
        }

        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onPickImage) {
                Icon(Icons.Default.Image, contentDescription = null)
                Text(
                    text = stringResource(R.string.edit_image_from_gallery),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(onClick = onTakePhoto) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Text(
                    text = stringResource(R.string.edit_image_from_camera),
                    modifier = Modifier.padding(start = 8.dp),
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
private fun RatingField(rating: Int?, onEvent: (RecipeEditEvent) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.edit_field_rating),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row {
            (1..5).forEach { star ->
                IconButton(
                    onClick = {
                        // Tapping the current rating clears it — otherwise a mis-tap is
                        // permanent.
                        onEvent(RecipeEditEvent.RatingChanged(if (rating == star) null else star))
                    },
                ) {
                    Icon(
                        imageVector = if (rating != null && star <= rating) {
                            Icons.Default.Star
                        } else {
                            Icons.Default.StarBorder
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
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

    Column(Modifier.padding(horizontal = 16.dp)) {
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
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(stringResource(R.string.edit_tag_add_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    onEvent(RecipeEditEvent.TagAdded(draft))
                    draft = ""
                },
            ) {
                Text(stringResource(R.string.edit_tag_add))
            }
        }

        // Flows onto more lines rather than running off the edge — a recipe can easily
        // carry four or five tags.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun NumberField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun RecipeEditScreenPreview() {
    ReceptariTheme(dynamicColor = false) {
        RecipeEditScreen(
            state = RecipeEditUiState(
                title = "Pollastre amb salsa",
                servings = "4",
                tags = listOf("Sopar", "Ràpid"),
                ingredientSections = listOf(
                    FormSection(
                        name = "Salsa",
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
