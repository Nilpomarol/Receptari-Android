package cat.receptari.app.ui.tags

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.Rubric
import cat.receptari.app.core.designsystem.Sprig
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.domain.model.TagSummary
import cat.receptari.app.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagsUiState(
    val tags: List<TagSummary> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagRepository: TagRepository,
) : ViewModel() {

    val uiState: StateFlow<TagsUiState> = tagRepository.observeAllWithCounts()
        .map { TagsUiState(tags = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagsUiState())

    fun rename(id: String, newName: String) {
        viewModelScope.launch { tagRepository.rename(id, newName) }
    }

    fun delete(id: String) {
        viewModelScope.launch { tagRepository.delete(id) }
    }
}

@Composable
fun TagsRoute(
    onNavigateBack: () -> Unit,
    onOpenTag: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TagsScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onOpenTag = onOpenTag,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

/**
 * Every tag in the library on one card. Tapping a tag opens its recipes; each line carries a
 * pencil, which turns the line into a field to rename (or merge into another tag), and a bin,
 * which asks on the same line before taking the tag off every recipe.
 */
@Composable
fun TagsScreen(
    state: TagsUiState,
    onNavigateBack: () -> Unit,
    onOpenTag: (String) -> Unit,
    onRename: (id: String, newName: String) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // At most one line is being edited or asked about at a time.
    var editingId by remember { mutableStateOf<String?>(null) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = editingId != null || deletingId != null) {
        editingId = null
        deletingId = null
    }

    PaperScaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Before the scroll, so the keyboard shrinks the viewport and the field being
                // edited is scrolled up above it rather than hidden under it.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(
                    start = PageMargin,
                    end = PageMargin,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.offset(x = (-12).dp).padding(top = 4.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.tags_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            if (!state.isLoading) {
                Rubric(
                    text = pluralStringResource(R.plurals.tags_count, state.tags.size, state.tags.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            when {
                state.isLoading -> Unit

                state.tags.isEmpty() -> EmptyState(modifier = Modifier.padding(top = 56.dp))

                else -> PaperCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentPadding = PaddingValues(start = 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    Column {
                        state.tags.forEachIndexed { index, summary ->
                            val id = summary.tag.id
                            if (index > 0) Hairline(modifier = Modifier.padding(end = 12.dp))
                            when (id) {
                                editingId -> TagEditor(
                                    summary = summary,
                                    others = state.tags,
                                    onRename = { onRename(id, it) },
                                    onClose = { editingId = null },
                                )

                                deletingId -> DeleteConfirmation(
                                    summary = summary,
                                    onConfirm = {
                                        onDelete(id)
                                        deletingId = null
                                    },
                                    onCancel = { deletingId = null },
                                )

                                else -> TagLine(
                                    summary = summary,
                                    onOpen = { onOpenTag(id) },
                                    onEdit = {
                                        deletingId = null
                                        editingId = id
                                    },
                                    onDelete = {
                                        editingId = null
                                        deletingId = id
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "#Postres" over its recipe count, then a pencil and a bin. The name opens the tag's recipes. */
@Composable
private fun TagLine(
    summary: TagSummary,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(vertical = 10.dp),
        ) {
            Text(
                text = hashName(summary.tag.name),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Aside(
                text = pluralStringResource(R.plurals.folders_recipe_count, summary.recipeCount, summary.recipeCount),
                textAlign = TextAlign.Start,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.tags_edit_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.common_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The line of the tag being renamed. Done or the tick saves, the cross cancels; typing another
 * tag's name says the two will merge.
 */
@Composable
private fun TagEditor(
    summary: TagSummary,
    others: List<TagSummary>,
    onRename: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember {
        mutableStateOf(TextFieldValue(summary.tag.name, selection = TextRange(summary.tag.name.length)))
    }
    val name = draft.text
    val clash = others.firstOrNull { it.tag.id != summary.tag.id && it.tag.normalizedName == Tag.normalize(name) }
    val save = {
        if (name.isNotBlank() && name.trim() != summary.tag.name) onRename(name)
        onClose()
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    // The text field alone would only scroll its own text above the keyboard; this keeps the
    // whole line (its rule and any merge notice) in view as the keyboard rises.
    val inView = remember { BringIntoViewRequester() }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom, clash != null) { inView.bringIntoView() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(inView)
            .padding(top = 6.dp, bottom = 10.dp, end = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#", // i18n-exempt: tag mark, as the recipe page prints it
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.common_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = save) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(if (clash != null) R.string.tags_merge else R.string.edit_save),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        // The rule under the name is what marks the line as a field.
        Hairline(color = MaterialTheme.colorScheme.primary, alpha = 1f)
        if (clash != null) {
            Aside(
                text = stringResource(R.string.tags_merge_notice, clash.tag.name),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Deleting reaches every recipe that carries the tag, so the line asks first. */
@Composable
private fun DeleteConfirmation(
    summary: TagSummary,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
        Aside(
            text = pluralStringResource(
                R.plurals.tags_delete_confirm_body,
                summary.recipeCount,
                summary.tag.name,
                summary.recipeCount,
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(end = 12.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(R.string.common_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** A tag's name as the recipe page prints it: a wax-red hash, then the name. */
@Composable
internal fun hashName(name: String) = buildAnnotatedString {
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("#") } // i18n-exempt: tag mark
    append(name)
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Sprig(modifier = Modifier.size(width = 64.dp, height = 32.dp))
        Text(
            text = stringResource(R.string.tags_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Aside(text = stringResource(R.string.tags_empty_body), modifier = Modifier.padding(top = 10.dp))
    }
}

private val PageMargin = 20.dp
private val RowHeight = 56.dp

@Preview
@Composable
private fun TagsScreenPreview() {
    ReceptariTheme {
        TagsScreen(
            state = TagsUiState(
                tags = listOf(
                    TagSummary(Tag(id = "1", name = "Àpat ràpid"), recipeCount = 3),
                    TagSummary(Tag(id = "2", name = "Postres"), recipeCount = 7),
                    TagSummary(Tag(id = "3", name = "Sopa"), recipeCount = 1),
                ),
                isLoading = false,
            ),
            onNavigateBack = {},
            onOpenTag = {},
            onRename = { _, _ -> },
            onDelete = {},
        )
    }
}
