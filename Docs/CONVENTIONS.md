# Conventions

---

## 1. Kotlin

- Official Kotlin code style, 4-space indent, 100-column soft limit.
- Trailing commas everywhere they are legal.
- Explicit visibility modifiers on public API in `domain/`; `internal` by default elsewhere.
- Prefer `when` expressions over `if/else` chains; rely on sealed-type exhaustiveness rather
  than `else ->` branches, so adding a case becomes a compile error.
- Expression bodies for single-expression functions.
- No `!!`. Handle null explicitly or restructure so the type is non-null.
- `@JvmInline value class` for identifier types where it prevents mix-ups (`RecipeId`,
  `TagId`) — but not for every primitive.

### Coroutines

- Suspend functions and `Flow` in `domain/` and `data/`. **No class stores a
  `CoroutineScope`** outside ViewModels.
- No `runBlocking` in production code.
- `Dispatchers` injected via a qualifier, never referenced directly, so tests substitute.
- Catch specific exceptions around suspend calls. Never catch `Throwable`, and never
  swallow `CancellationException`.

---

## 2. Compose

- **Every screen is two composables** — `FooRoute(viewModel)` and
  `FooScreen(state, onEvent)`. See `Docs/ARCHITECTURE.md` §3.1. `FooScreen` never receives
  a ViewModel.
- `modifier: Modifier = Modifier` is the **first optional parameter** and is applied to the
  root layout node, unaltered.
- Composables that emit UI return `Unit` and are named in `PascalCase`.
- Slot parameters (`content: @Composable () -> Unit`) instead of boolean shape flags once a
  component varies in more than one way.
- No hardcoded `dp` colours or type — everything from `MaterialTheme` /
  `core/designsystem`.
- Defer frequently-changing state reads to layout/draw (lambda-form modifiers) rather than
  reading in composition.
- `collectAsStateWithLifecycle()`, never `collectAsState()`.
- `@Preview` on every `FooScreen` with at least a loaded and an empty state.

---

## 3. State and events

```kotlin
data class LibraryUiState(
    val recipes: List<RecipeSummary> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
)

sealed interface LibraryEvent {
    data class QueryChanged(val query: String) : LibraryEvent
    data class ToggleFavorite(val id: String) : LibraryEvent
}
```

- One immutable state class per screen. No `MutableState` in the state class.
- Update with `MutableStateFlow.update {}`, never `.value =` read-modify-write.
- One-shot effects via `Channel(Channel.BUFFERED).receiveAsFlow()`. Never a `StateFlow`
  and never a `SharedFlow` with replay for navigation or snackbars.
- Avoid sentinel initial values; model loading explicitly.

---

## 4. Localization

Catalan-first (ADR-004). This is the convention most easily broken by accident.

- `res/values/strings.xml` — **Catalan**, the default and the only mandatory set.
- `res/values-es/strings.xml`, `res/values-en/strings.xml` — overlays.
- Every new user-visible string is added to `values/` in the same change that uses it.
- **No hardcoded text in composables.** `stringResource(R.string.…)` always. Enforced by
  the `checkNoHardcodedUiText` Gradle task, not by lint — Android's `HardcodedText` check
  only looks at XML layouts and never fires in a Compose-only app. Escape hatch for a
  genuine literal: a trailing `// i18n-exempt` comment on the line.
- `<plurals>` for anything countable: servings, steps, minutes, times cooked, ratings.
  Catalan and Spanish plural rules differ from English — do not fake it with `%d`.
- Never build sentences by concatenation. Use positional args: `%1$s`, `%2$d`.
- String ids: `screen_element_purpose` (`library_empty_title`,
  `recipe_detail_servings_label`, `import_error_paywall`).
- `res/xml/locales_config.xml` declares supported locales; adding a language must be
  exactly one new folder plus one line here.
- Formatting of numbers, dates, and quantities goes through locale-aware formatters, not
  `String.format` with a hardcoded locale.

---

## 5. Resources

- Colours, dimensions, and type live in `core/designsystem`, not in per-screen files.
- Vector drawables only; no PNG assets except the launcher icon.
- Content descriptions on every non-decorative image; `null` explicitly on decorative ones.

---

## 6. Testing

| What | How |
|---|---|
| `ServingScaler`, `IngredientParser` | **Test-first**, JVM, fixture corpus in `src/test/resources/`. Cover fractions, unicode fractions (`½`), ranges (`1–2`), unit variants in ca/es/en, and unparseable input |
| ViewModels | JVM, fake repositories, `kotlinx-coroutines-test`, `Turbine` for flows |
| DAOs | in-memory Room, instrumented |
| Migrations | one test per migration, using `MigrationTestHelper` |
| `FooScreen` | Compose UI test / screenshot test where it carries real logic |
| Parsers (JSON-LD, microdata) | JVM, against saved real-world HTML fixtures |

Test names describe behaviour: `` `scales ranges on both bounds`() ``.

Do not report work as complete without running `./gradlew build` and seeing it pass.

---

## 7. Git

- Branches: `feat/…`, `fix/…`, `chore/…`, `docs/…`, `refactor/…`.
- Conventional commits: `feat: add serving multiplier to recipe detail`.
- One feature per branch; keep diffs reviewable.
- Never commit `local.properties`, keystores, or an API key.
- Agents never commit or push unless explicitly asked.

---

## 8. Keeping documentation honest

A change that alters the schema, a locked decision, or the roadmap updates the matching
file in `Docs/` in the **same** change:

| Change | Update |
|---|---|
| Room schema | `Docs/DATA_MODEL.md` |
| Stack, storage, AI placement, localization strategy | new ADR in `Docs/DECISIONS.md` |
| Layering, packages, pipeline | `Docs/ARCHITECTURE.md` |
| Phase completed or re-scoped | `Docs/ROADMAP.md` |
| Rules agents must follow | `AGENTS.md` |
