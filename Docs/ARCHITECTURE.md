# Architecture

Target state. The scaffold is built in Roadmap Phase 1; this document is what it is being
built towards.

---

## 1. Shape

Single Gradle module `:app`, application ID and package root `cat.receptari.app`, layered by package
with a strict dependency direction:

```
        ui  ──────►  domain  ◄──────  data
                       ▲
                    (depends on nothing)
```

`domain` is pure Kotlin: no `android.*`, no Room, no Compose, no Hilt annotations beyond
constructor injection. It is unit-testable on the JVM with no Robolectric and no emulator.

Why one module: the feature surface is broad but shallow, and a solo project pays the
multi-module tax (build config, `api`/`implementation` discipline, navigation plumbing)
without getting much back. The package boundary is drawn as if modules existed, so
`:core:domain` can be extracted later as a mechanical move.

---

## 2. Package layout

```
cat.receptari.app
├── ReceptariApplication.kt        @HiltAndroidApp
├── MainActivity.kt                single activity, hosts the NavHost
│
├── di/                            Hilt modules (DatabaseModule, NetworkModule,
│                                  RepositoryModule, DispatcherModule)
│
├── core/
│   ├── designsystem/              theme/, component/ — ReceptariTheme, shared composables
│   └── util/                      Result wrappers, dispatcher qualifiers, formatters
│
├── domain/
│   ├── model/                     Recipe, IngredientSection, Ingredient, InstructionSection,
│   │                              Step, Tag, CookEvent, RecipeSummary, ParsedIngredient
│   ├── repository/                RecipeRepository, TagRepository, CookHistoryRepository,
│   │                              SettingsRepository  (interfaces only)
│   ├── ai/                        AiClient interface, ExtractionRequest/Result, TranslationRequest
│   ├── parser/                    IngredientParser, UnitLexicon, QuantityParser
│   ├── scaling/                   ServingScaler, QuantityFormatter
│   └── usecase/                   ImportFromUrl, ImportFromImages, ImportFromText,
│                                  TranslateRecipe, MarkAsCooked, SearchRecipes, …
│
├── data/
│   ├── local/
│   │   ├── entity/                Room @Entity classes (+ cross-refs, FTS table)
│   │   ├── dao/                   RecipeDao, TagDao, CookEventDao, SearchDao
│   │   ├── converter/             Instant/enum TypeConverters
│   │   ├── ReceptariDatabase.kt
│   │   └── mapper/                entity <-> domain model mapping
│   ├── remote/
│   │   ├── claude/                ClaudeAiClient (implements domain AiClient), DTOs, prompts
│   │   └── web/                   HtmlFetcher, JsonLdRecipeParser, MicrodataRecipeParser
│   ├── image/                     ImageStore — copy/compress/delete files in app storage
│   ├── settings/                  DataStore + Keystore-backed encrypted API key storage
│   ├── backup/                    versioned archive validation + startup restore swap/rollback
│   ├── timer/                     transient timer DataStore, AlarmManager, notifications
│   └── repository/                *RepositoryImpl
│
└── ui/
    ├── navigation/                ReceptariNavHost, route definitions
    ├── library/                   recipe list, search, filter, sort
    ├── detail/                    recipe view, serving multiplier, mark-as-cooked
    ├── edit/                      create/edit form — reused as the import preview
    ├── importer/                  source picker + per-source input screens
    └── settings/                  API key, language, backup/restore, about
```

---

## 3. Patterns

### 3.1 Screen structure

Every screen is two composables:

```kotlin
@Composable
fun RecipeDetailRoute(
    onNavigateBack: () -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // effect collection, snackbars, navigation
    RecipeDetailScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun RecipeDetailScreen(
    state: RecipeDetailUiState,
    onEvent: (RecipeDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) { /* pure UI */ }
```

`…Screen` never sees a ViewModel, so it is previewable and screenshot-testable. `…Route` is
the only place allowed to touch navigation and side effects.

### 3.2 State and events

- One immutable `data class FooUiState` per screen, exposed as `StateFlow`, produced with
  `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`.
- Updates via `MutableStateFlow.update { it.copy(...) }`.
- User intents arrive as a sealed `FooEvent` through a single `onEvent` lambda.
- One-shot effects (snackbar, navigate-after-save) go through
  `Channel(Channel.BUFFERED)` exposed as `receiveAsFlow()`. Never a `StateFlow`, which
  would replay on rotation.

### 3.3 Repositories

Interfaces in `domain/repository`, implementations in `data/repository`. Reads return
`Flow<T>` backed by Room; writes are `suspend`. Repositories map entities to domain models
so Room types never escape `data/`.

No repository or use case stores a `CoroutineScope`. Structured concurrency comes from the
caller.

The cooking timer follows the same boundary with one Android-specific implementation under
`data/timer`. Its active timer collection lives in a dedicated Preferences DataStore rather
than Room: it is device-session state, not recipe data and not cooked history. The domain model
and repository contract remain Android-free. Every timer has its own `AlarmManager` deadline
and notification identity; the UI merely derives each displayed countdown from its stored
deadline.

---

## 4. The import pipeline

All four import sources share one pipeline, differing only in the extraction step:

```
Source ──► Extract ──► Structure ──► Preview / Edit ──► Save
```

| Source | Extract | Structure |
|---|---|---|
| Website | fetch HTML → JSON-LD → microdata → cleaned text | mapper, or `AiClient` fallback |
| Image | user picks 1..n images | `AiClient` vision |
| Text | user pastes text | `AiClient` |
| Manual | — | — (straight to the edit screen) |

The **Structure** step always produces the same type: a `DraftRecipe` — the domain recipe
model with every field nullable and every ingredient carrying its `originalText`. The
**Preview/Edit** step is the ordinary recipe edit screen bound to a `DraftRecipe`. Only on
Save does it become a `Recipe` row.

This means: the edit screen is written once and gets reused four times, and no import path
can ever bypass review, because saving is only reachable from the edit screen.

Failure handling: extraction failures downgrade rather than abort. A website that yields
only a title still opens the editor with a title filled in and the URL stored.

Translation is the deliberate exception to the import review boundary. `TranslateRecipe`
builds an id-keyed payload
from the canonical source recipe, validates that the response has exactly the requested
ids, and merges a translated display copy. Ingredient quantities and units never enter the
AI payload: the domain renderer reconstructs them with localized unit labels and connectors.
The detail ViewModel saves that reversible overlay immediately; the source remains intact,
and the normal Edit action can correct the active language afterward.

The repository transaction that saves a translated display also upserts a compact
`recipe_translations` overlay for that language. `TranslateRecipe` reuses a matching cached
overlay before calling the model; a fingerprint of canonical source text and stable field ids
prevents stale translations from being applied. The active display remains denormalized in
the normal recipe aggregate, keeping every existing read path language-agnostic.

---

## 5. Serving scaling

`ServingScaler` is a pure function in `domain/scaling`:

```kotlin
fun scale(quantity: Quantity?, factor: Double): Quantity?
```

- `factor = targetServings / baseServings`; when `baseServings` is null, no scaling is
  offered at all.
- `null` quantity (e.g. `"salt to taste"`) returns `null` — the ingredient renders from
  `originalText` unchanged.
- Ranges (`1–2 onions`) scale both bounds.
- `QuantityFormatter` renders results as human quantities: common fractions (`½`, `¾`,
  `1 ⅓`) for small values, sensible rounding otherwise. Never `0.6666666667`.
- Nothing here touches the database. The multiplier lives in `RecipeDetailUiState`.

---

## 6. Search

Room FTS4 virtual table `recipe_fts` holding, per recipe: title, notes, a concatenation of
ingredient names, and a concatenation of tag names. Written by the repository whenever a
recipe is saved, in the same transaction.

Query strategy: prefix-match tokens (`chicken*`) so partial matches work as PRD §6 requires.
Filters (favorite, tags, never-cooked, recently-cooked) and sorting are applied as SQL on
the joined result, not in Kotlin.

---

## 7. Images

Stored as files in app-internal storage (`filesDir/images/<recipeId>.jpg`), never as BLOBs
in the database. `Recipe.imagePath` holds a relative path. `ImageStore` owns copy,
downscale/compress on import, and delete-on-recipe-delete. Coil renders them.

Gallery selection uses Android Photo Picker without storage permission. Camera capture uses
the installed camera app through `TakePicture` and a narrowly scoped FileProvider URI under
`cacheDir/camera`; Receptari therefore does not request `CAMERA`. The capture is a scratch
file only: it is removed on success or cancellation, and abandoned files are cleared when
the editor returns. The editor's normal image lifecycle decides whether the downscaled
internal copy survives Save or is removed on discard.

---

## 8. Threading

- Room and file IO on `Dispatchers.IO`, injected via a `@Dispatcher` qualifier so tests can
  substitute.
- Parsing and scaling are pure and cheap — main thread is fine.
- Network + AI calls are `suspend`, cancelled with the ViewModel scope.

---

## 9. What is deliberately absent

No use-case layer indirection for trivial CRUD (the ViewModel calls the repository
directly); use cases exist only where there is real orchestration — import, translation,
search. No `Result`-wrapping of every call. No feature flags. No analytics. No crash
reporting in v1. No abstractions for the sync that ADR-003 explicitly deferred.
