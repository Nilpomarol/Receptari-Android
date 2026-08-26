package cat.receptari.app.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.receptari.app.domain.model.Folder
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon
import cat.receptari.app.domain.model.Ingredient
import cat.receptari.app.domain.model.IngredientSection
import cat.receptari.app.domain.model.InstructionSection
import cat.receptari.app.domain.model.Recipe
import cat.receptari.app.domain.model.Step
import cat.receptari.app.domain.model.Tag
import cat.receptari.app.domain.parser.IngredientParser
import cat.receptari.app.domain.repository.FolderRepository
import cat.receptari.app.domain.repository.ImageStore
import cat.receptari.app.domain.repository.RecipeRepository
import cat.receptari.app.ui.importer.ImportDraftHandoff
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** One editable line — an ingredient or a step. Identity is stable so text fields keep focus. */
data class FormLine(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val originalText: String? = null,
)

data class FormSection(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val lines: List<FormLine> = listOf(FormLine()),
    val originalName: String? = null,
)

data class RecipeEditUiState(
    val id: String = UUID.randomUUID().toString(),
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val title: String = "",
    val imagePath: String? = null,
    val imageDisplayPath: String? = null,
    val prepTime: String = "",
    val cookTime: String = "",
    val totalTime: String = "",
    val servings: String = "",
    val rating: Int? = null,
    val notes: String = "",
    val sourceName: String = "",
    val sourceUrl: String = "",
    val tags: List<String> = emptyList(),
    val folder: Folder? = null,
    val availableFolders: List<Folder> = emptyList(),
    val ingredientSections: List<FormSection> = listOf(FormSection()),
    val instructionSections: List<FormSection> = listOf(FormSection()),
    val showTitleError: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank()
}

sealed interface RecipeEditEvent {
    data class TitleChanged(val value: String) : RecipeEditEvent
    data class PrepTimeChanged(val value: String) : RecipeEditEvent
    data class CookTimeChanged(val value: String) : RecipeEditEvent
    data class TotalTimeChanged(val value: String) : RecipeEditEvent
    data class ServingsChanged(val value: String) : RecipeEditEvent
    data class NotesChanged(val value: String) : RecipeEditEvent
    data class SourceNameChanged(val value: String) : RecipeEditEvent
    data class SourceUrlChanged(val value: String) : RecipeEditEvent
    data class RatingChanged(val value: Int?) : RecipeEditEvent

    data class TagAdded(val name: String) : RecipeEditEvent
    data class TagRemoved(val name: String) : RecipeEditEvent

    data class FolderSelected(val folder: Folder?) : RecipeEditEvent
    data class FolderCreateRequested(val name: String) : RecipeEditEvent

    data class ImagePicked(val bytes: ByteArray) : RecipeEditEvent {
        override fun equals(other: Any?) = this === other
        override fun hashCode() = System.identityHashCode(this)
    }
    data object ImageRemoved : RecipeEditEvent

    data class SectionNameChanged(val kind: SectionKind, val sectionId: String, val value: String) : RecipeEditEvent
    data class SectionAdded(val kind: SectionKind) : RecipeEditEvent
    data class SectionRemoved(val kind: SectionKind, val sectionId: String) : RecipeEditEvent

    data class LineChanged(val kind: SectionKind, val sectionId: String, val lineId: String, val value: String) : RecipeEditEvent

    /**
     * Adds an empty line. With [afterLineId] null it appends; otherwise it inserts directly
     * below that line — the shape keyboard-driven entry needs, where pressing "next" on a
     * line grows a fresh one right under it. [newLineId] is supplied by the caller so the UI
     * can move focus onto the line it just created.
     */
    data class LineAdded(
        val kind: SectionKind,
        val sectionId: String,
        val afterLineId: String? = null,
        val newLineId: String = UUID.randomUUID().toString(),
    ) : RecipeEditEvent
    data class LineRemoved(val kind: SectionKind, val sectionId: String, val lineId: String) : RecipeEditEvent
    data class LineMoved(val kind: SectionKind, val sectionId: String, val lineId: String, val delta: Int) : RecipeEditEvent

    data object Save : RecipeEditEvent
}

enum class SectionKind { Ingredients, Instructions }

sealed interface RecipeEditEffect {
    data class Saved(val recipeId: String) : RecipeEditEffect
}

@HiltViewModel
class RecipeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val folderRepository: FolderRepository,
    private val imageStore: ImageStore,
    private val clock: Clock,
    importDraftHandoff: ImportDraftHandoff,
) : ViewModel() {

    private val editingRecipeId: String? = savedStateHandle["recipeId"]

    private val _uiState = MutableStateFlow(RecipeEditUiState(isLoading = editingRecipeId != null))
    val uiState: StateFlow<RecipeEditUiState> = _uiState.asStateFlow()

    /**
     * The form as it was last loaded or saved. Compared against the live state so leaving
     * the editor can warn before throwing work away.
     */
    private var savedSnapshot: RecipeEditUiState = _uiState.value

    /**
     * Images written to storage while editing. An image has to be written before it can be
     * shown, but which of them survives is only decided on save or discard — so they are
     * tracked here and cleaned up once the outcome is known.
     */
    private val imagesWrittenThisSession = mutableListOf<String>()

    val hasUnsavedChanges: StateFlow<Boolean> = _uiState
        .map { it.differsFrom(savedSnapshot) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    private val effectChannel = Channel<RecipeEditEffect>(Channel.BUFFERED)
    val effects: Flow<RecipeEditEffect> = effectChannel.receiveAsFlow()

    /** Preserved so an edit does not reset when the recipe was originally created. */
    private var createdAt: Instant = Instant.now(clock)
    private var existingTagIds: Map<String, String> = emptyMap()
    private var originalTitle: String? = null
    private var originalLanguage: String? = null
    private var displayLanguage: String? = null

    init {
        viewModelScope.launch {
            folderRepository.observeAll().collect { folders ->
                _uiState.update { it.copy(availableFolders = folders) }
            }
        }

        if (editingRecipeId != null) {
            load(editingRecipeId)
        } else {
            // An import lands here as an ordinary new recipe: same form, same validation,
            // same save path. Nothing is read-only and nothing marks it as imported
            // (PRD §13). `savedSnapshot` stays the empty form on purpose, so backing out of
            // a filled-in import warns rather than silently discarding the extraction.
            importDraftHandoff.consume()?.let { draft ->
                _uiState.value = draft.toFormState()
                    .copy(availableFolders = _uiState.value.availableFolders)
                // Recorded now because the source language is only knowable at import time,
                // and Phase 5 translation needs to know what it is translating from.
                originalLanguage = draft.originalLanguage
            }
        }
    }

    private fun load(id: String) = viewModelScope.launch {
        val recipe = recipeRepository.getRecipe(id) ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return@launch
        }

        loadRecipe(recipe)
    }

    private fun loadRecipe(recipe: Recipe, updateSavedSnapshot: Boolean = true) {
        createdAt = recipe.createdAt
        originalTitle = recipe.originalTitle
        originalLanguage = recipe.originalLanguage
        displayLanguage = recipe.displayLanguage
        existingTagIds = recipe.tags.associate { it.name to it.id }

        _uiState.value = recipe.toEditState(_uiState.value.availableFolders)
        if (updateSavedSnapshot) savedSnapshot = _uiState.value
    }

    private fun Recipe.toEditState(availableFolders: List<Folder>): RecipeEditUiState {
        val preserveSourceText = displayLanguage != null
        return RecipeEditUiState(
            id = id,
            isNew = false,
            isLoading = false,
            title = title,
            imagePath = imagePath,
            imageDisplayPath = imagePath?.let(imageStore::absolutePathOf),
            prepTime = prepTimeMinutes?.toString().orEmpty(),
            cookTime = cookTimeMinutes?.toString().orEmpty(),
            totalTime = totalTimeMinutes?.toString().orEmpty(),
            servings = baseServings?.toString().orEmpty(),
            rating = rating,
            notes = notes.orEmpty(),
            sourceName = sourceName.orEmpty(),
            sourceUrl = sourceUrl.orEmpty(),
            tags = tags.map { it.name },
            folder = folder,
            availableFolders = availableFolders,
            // The editor works on the text the user (or the import) actually wrote. Parsing
            // happens on save, so `originalText` is whatever is on screen — never a
            // re-rendered approximation of it.
            ingredientSections = ingredientSections.map { section ->
                FormSection(
                    id = section.id,
                    name = section.name.orEmpty(),
                    lines = section.ingredients
                        .map {
                            FormLine(
                                id = it.id,
                                text = it.displayText ?: it.originalText,
                                originalText = it.originalText.takeIf { preserveSourceText },
                            )
                        }
                        .ifEmpty { listOf(FormLine()) },
                    originalName = section.originalName,
                )
            }.ifEmpty { listOf(FormSection()) },
            instructionSections = instructionSections.map { section ->
                FormSection(
                    id = section.id,
                    name = section.name.orEmpty(),
                    lines = section.steps
                        .map {
                            FormLine(
                                id = it.id,
                                text = it.text,
                                originalText = it.originalText.takeIf { preserveSourceText },
                            )
                        }
                        .ifEmpty { listOf(FormLine()) },
                    originalName = section.originalName,
                )
            }.ifEmpty { listOf(FormSection()) },
        )
    }

    /**
     * Called when the user chooses to throw away their edits.
     *
     * Removes the images written during this edit and leaves the recipe's own image alone —
     * discarding must not damage what is already saved. `NonCancellable` because this runs
     * as the screen is being torn down and `viewModelScope` is about to be cancelled.
     */
    fun discardChanges() {
        val orphans = imagesWrittenThisSession.filterNot { it == savedSnapshot.imagePath }
        imagesWrittenThisSession.clear()
        if (orphans.isEmpty()) return

        viewModelScope.launch(NonCancellable) {
            orphans.forEach { imageStore.delete(it) }
        }
    }

    fun onEvent(event: RecipeEditEvent) {
        when (event) {
            is RecipeEditEvent.TitleChanged ->
                _uiState.update { it.copy(title = event.value, showTitleError = false) }

            is RecipeEditEvent.PrepTimeChanged ->
                _uiState.update { it.copy(prepTime = event.value.digitsOnly()) }

            is RecipeEditEvent.CookTimeChanged ->
                _uiState.update { it.copy(cookTime = event.value.digitsOnly()) }

            is RecipeEditEvent.TotalTimeChanged ->
                _uiState.update { it.copy(totalTime = event.value.digitsOnly()) }

            is RecipeEditEvent.ServingsChanged ->
                _uiState.update { it.copy(servings = event.value.digitsOnly()) }

            is RecipeEditEvent.NotesChanged -> _uiState.update { it.copy(notes = event.value) }
            is RecipeEditEvent.SourceNameChanged -> _uiState.update { it.copy(sourceName = event.value) }
            is RecipeEditEvent.SourceUrlChanged -> _uiState.update { it.copy(sourceUrl = event.value) }
            is RecipeEditEvent.RatingChanged -> _uiState.update { it.copy(rating = event.value) }

            is RecipeEditEvent.TagAdded -> _uiState.update { state ->
                val name = event.name.trim()
                if (name.isEmpty() || state.tags.any { it.equals(name, ignoreCase = true) }) {
                    state
                } else {
                    state.copy(tags = state.tags + name)
                }
            }

            is RecipeEditEvent.TagRemoved ->
                _uiState.update { it.copy(tags = it.tags - event.name) }

            is RecipeEditEvent.FolderSelected ->
                _uiState.update { it.copy(folder = event.folder) }

            is RecipeEditEvent.FolderCreateRequested -> viewModelScope.launch {
                val name = event.name.trim()
                if (name.isEmpty()) return@launch
                // Colour and icon are picked afterwards from the folder's own screen; a
                // quick add from the editor should not ask for more than a name.
                val folder = folderRepository.create(name, FolderColor.OLIVE, FolderIcon.FOLDER)
                _uiState.update { it.copy(folder = folder) }
            }

            is RecipeEditEvent.ImagePicked -> viewModelScope.launch {
                val path = imageStore.save(_uiState.value.id, event.bytes)
                imagesWrittenThisSession += path
                _uiState.update {
                    it.copy(imagePath = path, imageDisplayPath = imageStore.absolutePathOf(path))
                }
            }

            // Deliberately does not delete anything. The file belongs to the saved recipe
            // until the edit is saved; deleting here would leave a saved recipe pointing at
            // a missing file the moment the user backed out.
            RecipeEditEvent.ImageRemoved ->
                _uiState.update { it.copy(imagePath = null, imageDisplayPath = null) }

            is RecipeEditEvent.SectionNameChanged -> updateSections(event.kind) { sections ->
                sections.map { if (it.id == event.sectionId) it.copy(name = event.value) else it }
            }

            is RecipeEditEvent.SectionAdded -> updateSections(event.kind) { it + FormSection() }

            is RecipeEditEvent.SectionRemoved -> updateSections(event.kind) { sections ->
                // Always leave one section standing: a recipe with no section at all has
                // nowhere to type.
                if (sections.size <= 1) sections else sections.filterNot { it.id == event.sectionId }
            }

            is RecipeEditEvent.LineChanged -> updateLines(event.kind, event.sectionId) { lines ->
                lines.map { if (it.id == event.lineId) it.copy(text = event.value) else it }
            }

            is RecipeEditEvent.LineAdded -> updateLines(event.kind, event.sectionId) { lines ->
                val newLine = FormLine(id = event.newLineId)
                val index = lines.indexOfFirst { it.id == event.afterLineId }
                if (index < 0) {
                    lines + newLine
                } else {
                    lines.toMutableList().apply { add(index + 1, newLine) }
                }
            }

            is RecipeEditEvent.LineRemoved -> updateLines(event.kind, event.sectionId) { lines ->
                if (lines.size <= 1) listOf(FormLine()) else lines.filterNot { it.id == event.lineId }
            }

            is RecipeEditEvent.LineMoved -> updateLines(event.kind, event.sectionId) { lines ->
                val index = lines.indexOfFirst { it.id == event.lineId }
                val target = index + event.delta
                if (index < 0 || target !in lines.indices) {
                    lines
                } else {
                    lines.toMutableList().apply { add(target, removeAt(index)) }
                }
            }

            RecipeEditEvent.Save -> save()
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) {
            _uiState.update { it.copy(showTitleError = true) }
            return
        }

        viewModelScope.launch {
            val now = Instant.now(clock)

            val recipe = Recipe(
                id = state.id,
                title = state.title.trim(),
                originalTitle = originalTitle,
                imagePath = state.imagePath,
                prepTimeMinutes = state.prepTime.toIntOrNull(),
                cookTimeMinutes = state.cookTime.toIntOrNull(),
                totalTimeMinutes = state.totalTime.toIntOrNull(),
                baseServings = state.servings.toIntOrNull(),
                isFavorite = false,
                rating = state.rating,
                notes = state.notes.trim().takeIf { it.isNotEmpty() },
                sourceName = state.sourceName.trim().takeIf { it.isNotEmpty() },
                sourceUrl = state.sourceUrl.trim().takeIf { it.isNotEmpty() },
                originalLanguage = originalLanguage,
                displayLanguage = displayLanguage,
                createdAt = createdAt,
                updatedAt = now,
                ingredientSections = state.ingredientSections.toIngredientSections(),
                instructionSections = state.instructionSections.toInstructionSections(),
                tags = state.tags.map { name ->
                    Tag(id = existingTagIds[name] ?: UUID.randomUUID().toString(), name = name)
                },
                folder = state.folder,
            )

            val merged = if (state.isNew) {
                recipe
            } else {
                // Favourite state and cooked history are owned elsewhere; the editor must
                // not clobber them by writing its own defaults back.
                recipe.copy(isFavorite = recipeRepository.getRecipe(state.id)?.isFavorite ?: false)
            }

            recipeRepository.save(merged)

            // The outcome is settled: every image written during this edit, plus the one
            // the recipe used to reference, is now dead weight unless it is the one saved.
            val keep = merged.imagePath
            (imagesWrittenThisSession + listOfNotNull(savedSnapshot.imagePath))
                .distinct()
                .filterNot { it == keep }
                .forEach { imageStore.delete(it) }
            imagesWrittenThisSession.clear()

            savedSnapshot = _uiState.value
            effectChannel.send(RecipeEditEffect.Saved(merged.id))
        }
    }

    /**
     * Transient UI flags are not edits — a validation error appearing must not make the
     * form look dirty.
     */
    private fun RecipeEditUiState.differsFrom(other: RecipeEditUiState): Boolean =
        copy(showTitleError = false, isLoading = false, availableFolders = emptyList()) !=
            other.copy(showTitleError = false, isLoading = false, availableFolders = emptyList())

    private fun List<FormSection>.toIngredientSections(): List<IngredientSection> =
        mapNotNull { section ->
            val ingredients = section.lines.mapNotNull { line ->
                IngredientParser.parse(line.text)?.let { parsed ->
                    val sourceText = line.originalText ?: parsed.originalText
                    Ingredient(
                        id = line.id,
                        quantity = parsed.quantity,
                        quantityMax = parsed.quantityMax,
                        unit = parsed.unit,
                        name = parsed.name,
                        note = parsed.note,
                        originalText = sourceText,
                        displayText = parsed.originalText.takeIf { sourceText != parsed.originalText },
                    )
                }
            }
            if (ingredients.isEmpty()) {
                null
            } else {
                IngredientSection(
                    id = section.id,
                    name = section.name.trim().takeIf { it.isNotEmpty() },
                    ingredients = ingredients,
                    originalName = section.originalName,
                )
            }
        }

    private fun List<FormSection>.toInstructionSections(): List<InstructionSection> =
        mapNotNull { section ->
            val steps = section.lines
                .map { it.id to it.text.trim() }
                .filter { it.second.isNotEmpty() }
                .map { (id, text) ->
                    val original = section.lines.first { it.id == id }.originalText
                    Step(id = id, text = text, originalText = original)
                }

            if (steps.isEmpty()) {
                null
            } else {
                InstructionSection(
                    id = section.id,
                    name = section.name.trim().takeIf { it.isNotEmpty() },
                    steps = steps,
                    originalName = section.originalName,
                )
            }
        }

    private fun updateSections(kind: SectionKind, transform: (List<FormSection>) -> List<FormSection>) {
        _uiState.update { state ->
            when (kind) {
                SectionKind.Ingredients -> state.copy(ingredientSections = transform(state.ingredientSections))
                SectionKind.Instructions -> state.copy(instructionSections = transform(state.instructionSections))
            }
        }
    }

    private fun updateLines(
        kind: SectionKind,
        sectionId: String,
        transform: (List<FormLine>) -> List<FormLine>,
    ) = updateSections(kind) { sections ->
        sections.map { section ->
            if (section.id == sectionId) section.copy(lines = transform(section.lines)) else section
        }
    }

    private fun String.digitsOnly(): String = filter { it.isDigit() }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
