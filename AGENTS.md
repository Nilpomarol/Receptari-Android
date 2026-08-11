# AGENTS.md — Receptari

Instructions for AI coding agents (Claude Code, Codex, and any other) working in this
repository. This file is the single source of truth; `CLAUDE.md` imports it.

---

## 1. What this project is

**Receptari** is a private, Android-only personal recipe book. It stores recipes in a
consistent structured format regardless of where they came from: manual entry, a website,
an image, or unstructured pasted text.

It is **not** a social network, a public recipe platform, or a discovery service. Do not
add features in that direction.

The authoritative product spec is [`Docs/PRD.md`](Docs/PRD.md). When a request conflicts
with the PRD, say so before implementing.

---

## 2. Locked technical decisions

These were decided deliberately. Do not silently change them; see
[`Docs/DECISIONS.md`](Docs/DECISIONS.md) for the full rationale of each.

| Area | Decision |
|---|---|
| Platform | Android only. Kotlin + Jetpack Compose + Material 3 |
| Application ID | `cat.receptari.app` |
| Min / target SDK | `minSdk 33`, `targetSdk 36`, `compileSdk 37` |
| DI | Hilt |
| Persistence | Room (KSP), local-only. **UUID string primary keys**, `updatedAt` on every row |
| Preferences | DataStore (Preferences) |
| AI | Claude API called **directly from the device**, with a user-supplied API key |
| Default AI model | `claude-sonnet-5` |
| Theming | Fixed printed-book design system in `core/designsystem`. **No Material You / dynamic colour** (ADR-009) |
| Typography | Bundled OFL fonts: Pinyon Script (wordmark only), Playfair Display (headings), Lora (body) |
| UI language | Catalan is the default (`values/`), with `values-es/` and `values-en/` overlays |
| Translation targets | Catalan, Spanish, English |
| Sync / accounts | None. Local-only. Schema stays sync-*ready*, nothing more |
| Modules | Single `:app` Gradle module, layered by package |

---

## 3. Non-negotiable product rules

Break any of these and the feature is wrong, regardless of how clean the code is.

1. **`Ingredient.originalText` is never null and never lost.** It holds the line as the
   user approved it — the source's own wording, with exactly one sanctioned transformation:
   `UnitNormalizer` rewrites the leading `quantity [unit]` span before the line reaches the
   editable preview (ADR-008). Nothing else may touch it. No parse, import, translation, or
   edit may drop a line, reword an ingredient name, or delete a qualifier it does not
   understand (PRD §3.2, §14).
   - Normalisation is **deterministic Kotlin, never the model.** Do not ask the extractor
     to convert, tidy, or shorten anything — its prompt says copy verbatim, and that is what
     makes the parser's fixture corpus meaningful.
   - **No volume-to-mass conversion, ever.** A cup of flour is 120 g, of butter 227 g. There
     is no correct answer without the ingredient's density, and a confident wrong number is
     worse than a foreign-looking right one.
2. **Scaling servings never mutates stored data.** The serving multiplier is a
   display-time pure function. `Recipe.baseServings` and stored ingredient quantities are
   immutable under scaling (PRD §3.3, §14).
3. **Ingredients that cannot be structured must still be storable.** `"Salt to taste"`,
   `"a handful of parsley"`, `"1–2 onions"` are all valid ingredients. Parsing produces
   nullable fields, never an error and never a dropped row.
4. **No automatic import saves without user review.** Every import path is
   `Source → Extract → Structure → Preview/Edit → Save`. The preview is editable
   (PRD §7).
5. **Imported recipes are ordinary recipes after saving.** No second-class "imported"
   mode, no read-only fields (PRD §13).
6. **Cooked history is an event log, not a counter.** `times cooked` and `last cooked` are
   derived from `CookEvent` rows (PRD §4).
7. **No user-facing string is hardcoded.** Everything goes through `strings.xml`. See §6.

---

## 4. Architecture in one screen

Single `:app` module, package root `cat.receptari.app`, layered:

```
domain/     Pure Kotlin. No Android imports, no Room, no Compose.
            Models, repository interfaces, use cases, IngredientParser, ServingScaler.
data/       Room entities/DAOs, Claude API client, image file storage,
            repository implementations. Maps data models <-> domain models.
ui/         Compose screens, ViewModels, navigation.
core/       Design system (theme, shared components) and small shared utilities.
di/         Hilt modules.
```

Dependency direction is strictly `ui -> domain <- data`. `domain` depends on nothing.
If you find yourself importing `android.*` into `domain/`, the design is wrong.

Full detail: [`Docs/ARCHITECTURE.md`](Docs/ARCHITECTURE.md).

---

## 5. Code conventions

Full version in [`Docs/CONVENTIONS.md`](Docs/CONVENTIONS.md). The parts you will get wrong
if you skip it:

- **Screen composables split in two.** `FooRoute(viewModel)` collects state and handles
  navigation/side effects; `FooScreen(state, onEvent)` is a pure, previewable,
  testable composable that takes no ViewModel.
- **State via `StateFlow<FooUiState>`**, one immutable data class per screen, updated with
  `MutableStateFlow.update {}`. One-shot events via `Channel(Channel.BUFFERED)` exposed as
  `receiveAsFlow()` — never as `StateFlow`.
- **`Modifier` is the first optional parameter** of every public composable and is applied
  to the root layout node, unmodified.
- **Build screens from `core/designsystem`, not from raw Material.** `PaperScaffold` rather
  than `Scaffold` (a bare `Scaffold` paints over the paper texture), `PaperTopBar`,
  `PaperCard`, `FilterPill`, `StarRating`, `OrnamentHeading`, `paperFieldColors()`. Extra
  tokens — page edge, rule, gold, sealing-wax red — come from `ReceptariTheme.palette`;
  everything else is a normal `MaterialTheme.colorScheme` role. See ADR-009.
- **No `CoroutineScope` stored in domain/data classes.** Suspend functions and `Flow` only;
  scope belongs to the ViewModel.
- Kotlin official code style, 4-space indent, trailing commas on, explicit visibility for
  public API in `domain/`.

---

## 6. Localization rules (this project is Catalan-first)

- Default resources in `res/values/strings.xml` are **Catalan**.
- Overlays: `res/values-es/strings.xml`, `res/values-en/strings.xml`.
- Every new string must be added to `values/` in the same change. Missing overlays are
  acceptable and will fall back; missing default strings are a build error.
- Use plurals (`<plurals>`) for anything countable — servings, steps, minutes, ratings.
- Never concatenate translated fragments. Use positional format args (`%1$s`, `%2$d`).
- `res/xml/locales_config.xml` lists supported locales so the system per-app language
  picker works.
- Lint rule `MissingDefaultResource` is configured as an **error**. Note that Android's
  `HardcodedText` check only inspects XML layouts and is useless in a Compose-only app —
  the equivalent here is the `checkNoHardcodedUiText` Gradle task
  (`gradle/hardcoded-ui-text.gradle.kts`), which fails the build on string literals passed
  to `text =`, `Text(`, or `contentDescription =`. It runs as part of `check` and in CI.
  A literal that genuinely must stay hardcoded carries a trailing `// i18n-exempt`
  comment. Do not add exemptions to silence ordinary strings.

Adding a new UI language later must require only a new `values-XX/` folder and one line in
`locales_config.xml`. Do not introduce anything that breaks that property.

---

## 7. AI integration rules

Full detail: [`Docs/AI_INTEGRATION.md`](Docs/AI_INTEGRATION.md).

- All model access goes through the `AiClient` interface in `domain/`. Nothing outside
  `data/remote/` knows the vendor, the model id, or the wire format.
- The API key is user-supplied, stored encrypted (Android Keystore-backed AES-GCM over
  DataStore), and **never logged, never included in error messages, never sent anywhere
  except `api.anthropic.com`**.
- AI output is **assistance, not truth** (PRD §14). Every AI response lands in an editable
  preview. Never persist a model response directly to the database.
- Website import tries in order: JSON-LD / schema.org Recipe → microdata → AI fallback on
  extracted text. Do not call the model when structured metadata already answered the
  question.
- Translation operates on the **structured recipe fields**, not raw HTML, and excludes
  quantities, units, URLs, and source information from the payload (PRD §11).

---

## 8. Testing expectations

- `ServingScaler` and `IngredientParser` are the two highest-risk components in the
  codebase. They are pure Kotlin and must be developed **test-first** against a fixture
  corpus in `src/test/resources/`, including fractions, ranges (`1–2`), unicode fractions
  (`½`), and unparseable inputs.
- ViewModels: unit-tested with a fake repository and `kotlinx-coroutines-test`.
- `FooScreen` composables: Compose UI tests / screenshot tests where they carry real logic.
- Room: DAO tests with an in-memory database; every schema change ships a migration **and**
  a migration test. `exportSchema = true`, schemas committed to git.
- Do not report a task as done if `./gradlew build` has not been run and passed.

---

## 9. Working agreements for agents

- **Read before writing.** `Docs/PRD.md` for behaviour, `Docs/DATA_MODEL.md` for schema,
  `Docs/DECISIONS.md` before proposing a stack change.
- **Ask when a decision is genuinely product-level** (does the user want X or Y?). Make
  ordinary engineering calls yourself.
- **Stay in scope.** PRD §16 lists features explicitly excluded from v1: shopping lists,
  meal planning, nutrition, pantry, recommendations, sharing. Do not build them, do not
  add "hooks" for them.
- **One feature per branch.** Branch names: `feat/…`, `fix/…`, `chore/…`, `docs/…`.
- **Conventional commits** (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).
- **Never commit or push unless asked.**
- **Keep the docs current.** A change that alters the schema, a locked decision, or the
  roadmap updates the corresponding file in `Docs/` in the same change.
- If the repository state contradicts this file, the repository wins — and tell the user so
  this file can be fixed.

---

## 10. Commands

The project scaffold does not exist yet; these become valid at the end of Roadmap Phase 1.

```bash
./gradlew assembleDebug            # build the debug APK
./gradlew testDebugUnitTest        # JVM unit tests
./gradlew checkNoHardcodedUiText   # localization guardrail
./gradlew lintDebug                # Android lint
./gradlew connectedAndroidTest     # instrumented tests (needs a device/emulator)
./gradlew build                    # everything above except connected tests
```

On Windows use `gradlew.bat` from PowerShell, or `./gradlew` from Git Bash.

The Gradle wrapper needs a JDK 17+; the system default here is Java 8, so set
`JAVA_HOME` to Android Studio's bundled runtime first:

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew build
```
