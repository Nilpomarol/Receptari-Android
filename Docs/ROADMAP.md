# Roadmap

Ordering principle, taken from PRD §18: *"prioritize reliability and simplicity over the
number of supported import sources."* The app must be a genuinely useful manual recipe book
before any AI code is written.

Status legend: ☐ not started · ◐ in progress · ☑ done

---

## Phase 0 — Decisions and agent setup ☑

- ☑ Product spec (`Docs/PRD.md`)
- ☑ Stack, AI placement, storage, and localization decided (`Docs/DECISIONS.md`)
- ☑ Agent instructions for Claude Code and Codex (`AGENTS.md`, `CLAUDE.md`)
- ☑ Architecture, data model, conventions, AI integration documented

---

## Phase 1 — Project scaffold ☑

Deliverable: an app that builds, installs, shows an empty themed screen, and has CI green.

- ☑ Gradle project, `:app` module, `cat.receptari.app`, `minSdk 33` / `targetSdk 36`
- ☑ Version catalog (`gradle/libs.versions.toml`), AGP 9.2.1, Kotlin 2.2.10, KSP
- ☑ Hilt, Compose (explicitly pinned, no BOM), Material 3, Navigation Compose, Room,
  DataStore, Coil, kotlinx-serialization, Jsoup, OkHttp
- ☑ Package skeleton per `Docs/ARCHITECTURE.md` §2
- ☑ `ReceptariTheme` — colour scheme, typography, dynamic colour, dark mode
- ☑ Catalan `values/strings.xml`, `values-es/`, `values-en/`, `locales_config.xml`
- ☑ Localization guardrail: `MissingDefaultResource` as a lint error plus the
  `checkNoHardcodedUiText` task (Compose needs its own — see `Docs/CONVENTIONS.md` §4)
- ☑ `.gitignore`, GitHub Actions (assemble + test + guardrail + lint on push)
- ☑ `NavHost` with Library / Import / Settings stubbed, plus a working language picker
- ☑ Verify on a real device: install, empty screen renders, language switching works

**Exit criteria:** `./gradlew build` passes; app installs; language switching works. ✅

`./gradlew build` passes locally (debug + release with R8, unit tests, lint, guardrail).
Verified on a Pixel 10 Pro XL (Android 17): installs, renders, and switches Catalan ↔
Spanish ↔ English at runtime. CI is green on GitHub Actions, which also confirms the two
things that can only fail on Linux: `gradlew` is committed executable, and the runner
provisions the Android 37 platform itself.

### Environment notes

- The system default JDK is Java 8; Gradle needs 17+. Set
  `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
- AGP 9 applies Kotlin itself — do **not** also apply `org.jetbrains.kotlin.android` in
  `app/build.gradle.kts`, it fails with a duplicate `kotlin` extension.
- KSP registers generated sources through `kotlin.sourceSets`, which AGP 9 rejects by
  default; `android.disallowKotlinSourceSets=false` in `gradle.properties` is required for
  Room and Hilt.
- Hilt's Gradle plugin needs **2.60.1+** for AGP 9 (2.57.2 fails with "Android
  BaseExtension not found").
- **Resolved:** `compileSdk` is now 37 and the AndroidX pins are lifted. The Android 37
  platform was not installed and `cmdline-tools/latest` is empty, so there is no
  `sdkmanager` on this machine — Gradle installed the platform itself on the first build
  after the bump, using the already-accepted licence in `Sdk/licenses/`. If another SDK
  package is ever needed, raising the version in `app/build.gradle.kts` and building is the
  path of least resistance.
- `targetSdk` stays at **36** deliberately. It is what opts the app into new runtime
  behaviour and wants testing on its own; `compileSdk` only decides which APIs compile.

---

## Phase 2 — Domain and data core ☑

Deliverable: no UI, but the hard logic exists and is tested.

- ☑ `domain/model` — full aggregate types
- ☑ Room entities, DAOs, `ReceptariDatabase` v1, `exportSchema = true` (schema committed)
- ☑ Entity ↔ domain mappers (`position` columns live only in the data layer)
- ☑ `RecipeRepository` + impl (CRUD with `@Transaction` aggregate reads)
- ☑ `TagRepository`, `CookHistoryRepository`
- ☑ **`ServingScaler` + `QuantityFormatter`, test-first**
- ☑ **`IngredientParser` + `UnitLexicon` (ca/es/en), test-first with fixture corpus**
- ☑ `ImageStore` (`FileImageStore` — downscales on the way in, files not BLOBs)
- ☑ DAO tests, migration test harness
- ☑ Hilt wiring: database, daos, repositories, dispatchers, injectable `Clock`

**Exit criteria:** scaler and parser fixture suites pass, including ranges, unicode
fractions, and unstructurable ingredients that survive round-trip with `originalText`
intact. ✅

42 unit tests and 14 instrumented tests pass; `./gradlew build` and
`connectedDebugAndroidTest` both green on a Pixel 10 Pro XL.

### Decisions taken during the phase

- **Fraction glyphs only below 10.** `1 ⅓ cups` is how a recipe is written; `133 ⅓ g` is
  not. Above that threshold `QuantityFormatter` uses decimals capped at two places.
- **A malformed fraction yields no quantity at all.** `1/0` is not silently read as `1` —
  a wrong number is worse than an absent one.
- **Children are replaced wholesale on save**, not diffed. An edit can reorder, merge and
  split sections arbitrarily; cascade-delete-then-insert cannot get that subtly wrong.
- **Tag identity is resolved inside `RecipeRepositoryImpl.save`** by normalized name, so
  the editor can hand over tags built from typed text without creating duplicates.
- **The FTS index is deleted explicitly on recipe delete** — see `Docs/DATA_MODEL.md`.
- `RecipeQuery`/filter/sort models were deliberately *not* written yet; they belong with
  the Phase 3 search UI rather than sitting unused.

---

## Phase 3 — MVP, manual recipe book ◐

Deliverable: **an app worth using every day**, with no AI and no network.

1. ☑ **Library** — list, empty state, recipe card (image, title, time, rating, favourite)
2. ☑ **Detail** — image, times, ingredients by section, steps with continuous numbering
3. ☑ **Create / edit** — title, image, times, servings, ingredient sections and rows,
   step sections and rows, reordering, tags, rating, notes, source
4. ☑ **Serving multiplier** on detail — display-time only, non-numeric ingredients unchanged
5. ☑ **Tags, favourites, notes**
6. ☑ **Mark as cooked** + history (times cooked, last cooked)
7. ☑ **Search** — FTS over title, ingredients, tags, notes, prefix matching
8. ☑ **Sort and filter** — the six sorts and five filters of PRD §5
9. ◐ **Images** — photo picker, replace, remove. **Camera capture not built yet**
10. ☑ **Settings** — app language, about

- ☑ Verify the whole flow on a real device

**Exit criteria:** you can add a real recipe by hand, find it, scale it, cook from it, and
mark it cooked. Dogfood for a week before starting Phase 4.

`./gradlew build` passes (60 unit tests, lint, guardrail) and 14 instrumented tests pass.
Verified on a Pixel 10 Pro XL with seeded sample data: library, detail with three ingredient
sections, continuous step numbering across sections, scaling, and the editor round-trip.

### Sample data

`SeedSampleRecipes` (androidTest, `@ManualOnly`) fills the on-device database with six
varied recipes. It never runs in the suite — the instrumentation runner is configured with
`notAnnotation=cat.receptari.app.ManualOnly`. Run it explicitly:

```
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e annotation cat.receptari.app.ManualOnly \
  cat.receptari.app.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Do **not** use `./gradlew connectedAndroidTest` for this: it uninstalls both APKs when it
finishes, taking the seeded database with it.

### Notes

- **Ingredients are edited as text, parsed on save.** The editor shows each ingredient's
  `originalText` and runs `IngredientParser` when saving, so what the user typed is what
  gets stored and structure is derived from it — never the other way round.
- **Ingredient lines render from the source text, not rebuilt from parsed fields.**
  Rebuilding produced broken Catalan — "3 grans all" instead of "3 grans d'all" — because
  the parser strips the connector to isolate the name. Scaling substitutes only the leading
  quantity, so connectors, elisions and plurals survive. See `ScaledIngredient.displayText`,
  and ADR-005 for the consequence this has for Phase 5 translation.
- **Scaled quantities are rounded to suit what is measured** (`QuantityFormatter`):
  countable items round to whole above 5 (no 10.67 egg yolks), grams and millilitres round
  to what a kitchen scale shows (266.67 g → 265 g), spoons and cups keep their fractions.
- **Search input is sanitized before reaching FTS.** Quotes, hyphens, parens and colons are
  FTS4 operators; passing them through makes SQLite throw on ordinary typing.
- The editor deliberately does not write `isFavorite` or cooked history — those are owned
  by the detail screen and the event log, and an edit must not clobber them.

---

## Phase 4 — Import ☑

Deliverable: PRD §7–§10. Build the shared pipeline once
(`Source → Extract → Structure → Preview/Edit → Save`), reusing the Phase 3 edit screen as
the preview — so no import path can bypass review.

1. ☑ Import source picker + `DraftRecipe` + pipeline skeleton
2. ☑ Settings: API key entry, encrypted storage, "test key", "remove key"
3. ☑ `AiClient` interface + `ClaudeAiClient`
4. ☑ **Text import** — simplest, no fetching, validates the whole pipeline
5. ☑ **Website import** — JSON-LD → microdata → AI fallback; store `sourceUrl`
6. ☑ **Image import** — multi-image, downscaling, vision extraction
7. ☑ Failure handling: typed `AiError`/`ImportError`, one message per failure the user can act on

`./gradlew build` passes (112 unit tests, lint, guardrail).

### Verified against the live API

On a Pixel 10 Pro XL with a real key: **one text import and two image imports**, all
successful end to end. That covers the parts that could only be proven in flight — the
request shape, `output_config` structured outputs against `RecipeSchema`, `ExtractionDto`
decoding, the base64 vision path through `JpegDownscaler`, the draft handoff, and the
editor prefill.

**Website import has not been run against a real page yet.** It is the one path with
substantial logic the other two do not share (`OkHttpWebPageSource` plus the JSON-LD and
microdata short-circuit in `SchemaOrgRecipeParser`), so its 14 unit tests are fixture-based
only. Worth an hour against a handful of real recipe sites before it is trusted.

The failure paths — rejected key, offline, exhausted quota — are also unexercised. They are
straightforward mappings, but they are untested mappings.

A `/security-review` of the whole change found no HIGH or MEDIUM issues. The Keystore
AES-256-GCM implementation, the key's confinement to `api.anthropic.com`, and the redaction
of the key out of error messages were each checked specifically.

### How the pieces fit

```
ImportScreen ──┬── TextImportViewModel ─────── AiClient.extractFromText
               ├── WebsiteImportViewModel ──── ImportRecipeFromWebsite
               │                                 ├─ WebPageSource (JSON-LD → microdata)
               │                                 └─ AiClient.extractFromWebContent (fallback)
               └── ImageImportViewModel ─────── AiClient.extractFromImages
                                    │
                          ImportDraftHandoff (in-memory, consumed once)
                                    │
                            RecipeEditViewModel  ← the same editor as manual entry
```

### Notes

- **The model returns ingredient *lines*, not structured fields.** `DraftRecipe` carries
  text; `IngredientParser` structures it on save, exactly as for a hand-typed recipe. That is
  what makes `originalText` trustworthy end to end, and it means extraction quality is
  covered by the parser's fixture corpus rather than by whatever the model felt like
  returning.
- **Structured outputs, not prompt-and-hope.** `RecipeSchema` constrains the response, so
  there is no prose to strip and no code-fence handling.
- **The draft never touches the database.** `ImportDraftHandoff` hands it to the editor in
  memory and clears itself on read, so an unreviewed model response cannot be resurrected or
  persisted (PRD §14).
- **Structured metadata short-circuits the model.** A page publishing schema.org Recipe is
  parsed directly — free, instant, exact. `SchemaOrgRecipeParser` has 14 tests covering the
  shapes that actually occur: `@graph` wrappers, `@type` arrays, `HowToStep`, `HowToSection`,
  instructions as one `<br>`-separated blob, and HTML inside ingredient text.
- **Photos are downscaled to 1568 px before sending.** The API resizes anything larger
  server-side anyway, so this costs no quality and saves the user's data.
- **The key is never in an error message.** `AiError.Unexpected` carries a diagnostic string
  that is redacted against the key before it leaves the client, and the UI shows a generic
  message rather than the detail.

**Exit criteria:** a failed or partial import always lands the user in the editor with
whatever was recovered, never in an error dead end.

---

## Phase 5 — Translation ☐

- ☐ `translate()` on `AiClient`, structured-field payload
- ☐ Preserve `originalTitle`, `Step.originalText`, `originalLanguage` (ADR-005)
- ☐ Translate action on recipe detail, with target-language picker (ca / es / en)
- ☐ Never send or alter quantities, units, URLs, source info

---

## Phase 5.5 — Design system ☑

Replaced Material You with a fixed printed-book aesthetic (ADR-009). Done out of order,
before translation, because every screen built after this point inherits it for free and
retrofitting later would mean touching all of them twice.

- ☑ `core/designsystem`: `PaperScaffold`, `PaperTopBar`, `PaperCard`, `FilterPill`,
  `StarRating`, `OrnamentHeading`, `OrnamentalDivider`, `Wordmark`, `paperFieldColors()`
- ☑ Fixed parchment/lamplight palette, `ReceptariPalette` for the non-Material tokens
- ☑ Three bundled OFL faces; Lora and Playfair as variable fonts
- ☑ Generated paper grain and vignette (`Modifier.paperBackground()`), no shipped texture
- ☑ Library rebuilt: script masthead, always-visible search, filter pills instead of a
  dropdown, framed recipe plates
- ☑ Recipe detail rebuilt: centred title page, ornamental section headings, ruled step
  numerals
- ☑ Import, editor and settings moved onto the same components

**Not verified on a device.** The build is green — 112 unit tests, lint, and
`checkNoHardcodedUiText` — but nothing here has been seen rendered. Dark mode in
particular has only ever existed as hex values.

---

## Phase 6 — Polish before calling it v1 ☐

- ☐ Backup / restore (export database + images, import back)
- ☐ Accessibility pass: TalkBack, touch targets, contrast, font scaling
- ☐ Dark theme audit
- ☐ Empty, loading, and error states for every screen
- ☐ Cook mode niceties: keep screen on, larger step text
- ☐ Release build config, signing, ProGuard/R8 rules

---

## Explicitly not on this roadmap

Per PRD §16: social features, public publishing, nutrition and calories, pantry inventory,
grocery integration, AI recipe generation, recommendations, meal planning, household
permissions, complex taxonomies.

Per PRD §17, plausible *later*, but not now and with no hooks left for them: shopping
lists, ingredient-based discovery, meal planning, family sync, PDF/social/video import.

---

## Risk notes

- **`IngredientParser` is the highest-risk component.** Scaling, ingredient search, and any
  future shopping list all depend on it, and PRD §3.2 requires it to degrade gracefully
  rather than fail. It gets its own fixture corpus and is never buried inside import code.
- **Import is the most likely place for the project to stall** — it is the most interesting
  part and the least predictable. Phase 3 shipping first means a stall there leaves a
  working app rather than an unfinished one.
