# Architecture Decision Records

Short, dated records of decisions that are expensive to reverse. Each has a status.
When a decision changes, add a new ADR that supersedes the old one — do not edit history.

---

## ADR-001 — Android-only, Kotlin + Compose + Room + Hilt

**Date:** 2026-08-10 · **Status:** Accepted

### Context
The app could be built as a standard Android app, as Kotlin Multiplatform with Compose
Multiplatform, or with a KMP-friendly pure-Kotlin core inside an Android-only project.

### Decision
Standard Android app. Kotlin, Jetpack Compose, Material 3, Room (KSP), Hilt, DataStore,
Coroutines/Flow. Application ID `cat.receptari.app`. Single `:app` Gradle module, layered by
package. SDK levels: see ADR-006.

### Rationale
This is a solo project with a broad feature surface (four import paths, translation,
scaling, search). KMP adds setup friction and a smaller library ecosystem for a benefit —
iOS/desktop — that is not on the roadmap. The `domain` package is kept free of Android
imports anyway, which is most of what a future KMP lift would need.

### Consequences
- Fastest path to a working app; largest library ecosystem.
- A future iOS version means rewriting the UI layer.
- Single module keeps build config trivial; if build times or layering discipline degrade,
  split `:core:domain` out first — the package boundary is already drawn for it.

---

## ADR-002 — Claude API called directly from the device, user-supplied key

**Date:** 2026-08-10 · **Status:** Accepted

### Context
Website, image, and text import plus translation all need an LLM. The call can originate
from the device with a user-supplied key, or from a backend proxy holding a shared key.

### Decision
The app calls the Claude API directly. The user enters their own API key in Settings. No
backend. Default model `claude-sonnet-5`.

### Rationale
Zero infrastructure to build, host, secure, or pay for. It matches the PRD's privacy
stance literally: recipe content goes to Anthropic and nowhere else, with no intermediary
operated by anyone. Each user's usage is billed to their own account.

### Consequences
- The key lives on the device. Mitigated by Keystore-backed encryption at rest and a hard
  rule that it is never logged or transmitted anywhere but `api.anthropic.com`.
- Each family member needs their own key — acceptable, as there is no sharing in v1.
- The model id and prompts ship in the app; changing them requires an app update.
- All access goes through the `AiClient` interface in `domain/`, so moving to a proxy later
  is a `data/remote/` change only.

---

## ADR-003 — Local-only storage with sync-ready identifiers

**Date:** 2026-08-10 · **Status:** Accepted

### Context
PRD §17 lists family sharing as a future extension. Designing for it now means auth,
a backend, and conflict resolution in the MVP.

### Decision
Room on-device only. No accounts, no network storage, no sync. But: **string UUID primary
keys** on every entity and an `updatedAt` timestamp on every mutable row.

### Rationale
Autoincrement `Long` ids make a later sync layer a painful schema migration across every
table and foreign key. UUIDs cost nothing now and remove that migration entirely. This is
the only concession made to a future that may never arrive.

### Consequences
- Slightly larger indexes and no natural insertion ordering — explicit `position` columns
  are used for ordering instead, which the data model needs anyway.
- Nothing else in the codebase anticipates sync. No sync fields, no tombstones, no
  `isDirty` flags until sync is actually built.
- Backup/restore (export the database + image folder) is the realistic v1 answer to "I got
  a new phone".

---

## ADR-004 — Catalan-first localization, Spanish and English overlays

**Date:** 2026-08-10 · **Status:** Accepted

### Context
The app is primarily for Catalan-speaking users, but must translate imported recipes into
Spanish and English, and adding UI languages later should be cheap.

### Decision
Catalan is the **default** resource set (`res/values/strings.xml`). Spanish and English are
overlays (`values-es/`, `values-en/`). Translation targets for recipe content are Catalan,
Spanish, and English. `res/xml/locales_config.xml` declares supported locales for the
system per-app language picker.

### Rationale
Putting Catalan in the default set guarantees the primary audience never sees an untranslated
string, and makes a missing translation degrade to Catalan rather than to English. The
per-app locale API (AppCompat 1.6+, backported below API 33) gives an in-app language
switcher for free.

### Consequences
- Adding a UI language = one new `values-XX/` folder + one line in `locales_config.xml`.
  Nothing may be introduced that breaks this property.
- Lint `MissingDefaultResource` is an error. Hardcoded Compose text is caught by the
  `checkNoHardcodedUiText` Gradle task instead — Android's `HardcodedText` lint check only
  inspects XML layouts and does nothing in a Compose-only app.
- The ingredient parser's unit lexicon must cover ca/es/en from the start, since recipes
  will be imported in all three.

---

## ADR-005 — One active translation, with language overlays cached locally

**Date:** 2026-08-10 · **Amended:** 2026-08-12 · **Status:** Accepted

### Context
PRD §11 requires translating recipes while "retaining the original language and original
content whenever practical". Keeping only one translated display makes switching languages
repeat the same paid model call, while making the entire recipe read path language-aware is
unnecessary for a three-language, local-only app.

### Decision
Translation writes translated values into the recipe's normal display fields. The source is
preserved via immutable `Ingredient.originalText`, nullable `Step.originalText`, section
`originalName` fields, `Recipe.originalTitle`, and `Recipe.originalLanguage`. Ingredients
gain a nullable `displayText`, and the recipe gains `displayLanguage`. Quantities, units,
URLs, and source information are never sent to the model.

Selecting a language on recipe detail immediately saves that display and caches it as an
id-keyed overlay in `recipe_translations`, keyed by recipe and language. The main aggregate
still owns the one active display version. A fingerprint over canonical source text and
stable ids prevents a cached overlay from being applied after the source changes. The normal
Edit action can correct the active overlay afterward without replacing the canonical source.

### Rationale
Keeps every read path — list, detail, search, scaling, and cook mode — working on a single
set of fields with no language dimension, while avoiding repeat API cost for an available
language. The overlay table is only consulted by the translation use case and is written
transactionally when a language is applied or its active display is edited.

### Consequences
- A recipe has one active display language, but every still-valid Catalan, Spanish,
  and English overlay can be selected again without an API call.
- The picker labels the canonical original, cached translations, and current display so the
  cost and effect of each choice are visible before selection.
- Selecting the canonical source language restores the source locally and also requires no
  API call.
- Editing canonical source content invalidates older overlays by fingerprint; it does not
  silently show a stale translation.
- Tags are not translated. They form a library-wide taxonomy, so translating them per
  recipe would create near-duplicate global tags.
- The model receives only ingredient meaning. Kotlin preserves the numeric quantity,
  localizes known unit labels and plural forms, and supplies `de`, `d'`, or `of` when
  rebuilding `displayText`. This fixes the lossy strip-and-reattach pipeline while keeping
  `Ingredient.originalText` untouched.

---

## ADR-007 — Use the official Anthropic Java SDK, not hand-rolled HTTP

**Date:** 2026-08-11 · **Status:** Accepted

### Context
Kotlin has no dedicated Anthropic SDK; Kotlin projects use `com.anthropic:anthropic-java`.
The alternative was raw HTTP with OkHttp and kotlinx.serialization, both already
dependencies — zero additional bytes, but every request/response type, retry rule, and
error mapping written and maintained by hand.

The open question was whether a JVM SDK built on Jackson and OkHttp would work on Android
at all. It was tested rather than assumed: `com.anthropic:anthropic-java:2.53.0` compiles
and dexes cleanly at `minSdk 33` with no desugaring errors.

### Decision
Use the official SDK.

### Rationale
It is the documented path for Kotlin, and it supplies typed exceptions per HTTP status,
automatic retry with backoff on 429/5xx, and structured-output support. Hand-rolling those
means re-implementing them and keeping them current as the API changes — for a solo
project, that maintenance is the expensive part, not the bytes.

### Consequences
- **The release APK grows from 2.1 MB to 9.24 MB** (post-R8) — Jackson is most of it. For
  a private app that never faces a store size limit this is affordable; if it ever stops
  being affordable, the escape hatch is real, because all model access sits behind the
  `AiClient` interface in `domain/ai` and nothing outside `data/remote/claude/` imports an
  SDK type.
- The SDK's calls are blocking, so every one is wrapped in `withContext(ioDispatcher)`.
- Ships Jackson alongside kotlinx.serialization. Two JSON libraries is not elegant;
  kotlinx.serialization stays the choice for our own code (Jsoup JSON-LD parsing, any
  future export), and Jackson is an implementation detail of the SDK.

---

## ADR-008 — Units are normalised in code before review, never by the model

**Date:** 2026-08-11 · **Status:** Accepted

### Context
Imported recipes arrive in whatever units the source used: `2 tablespoons`, `8 oz`,
`1 1/2 cups`, `350°F`. The goal is a recipe book that reads consistently in European units
with symbols rather than spelled-out words.

Two places could do this. The extractor's prompt could be told to convert, or the app could
do it deterministically after extraction. The prompt is the obvious-looking option and the
wrong one: it costs the same money, cannot be tested, may convert inconsistently between
runs, and does nothing for recipes typed by hand.

This decision also relaxes AGENTS.md §3.1, which previously said the source text was never
altered at all. That absolute wording was a hardening of PRD §14 ("preserve original text
where useful"), adopted after rendering-from-parsed-fields produced broken Catalan. The
grammatical hazard is real; the blanket ban was stricter than the PRD requires.

### Decision
Normalise in `domain/parser/UnitNormalizer`, a pure function applied in two places:

- **On import**, to the lines that fill the editable preview — so the user reviews and
  approves the normalised text, and that is what gets stored.
- **At display time**, in `ScaledIngredient.displayText()`, after scaling — so hand-typed
  recipes read consistently too.

The extractor's prompt is unchanged: it still copies the source verbatim.

### Rationale
Deterministic, free, and testable against the existing fixture corpus. It works on manual
entry as well as imports. And because it runs before an editable preview, the user is the
one who approves the result — which is what PRD §14 actually asks for.

Three limits define what it may do:

- **Only the leading `quantity [unit]` span is replaced.** The connector, name, note and
  punctuation are copied through byte-for-byte. This is the same surgical substitution
  scaling uses, for the same reason: rebuilding a line from parsed fields yields
  "3 grans all" for "3 grans d'all" (see ADR-005).
- **Conversions are exact arithmetic only** — oz→g, lb→g, cup→ml, °F→°C. Volume-to-mass is
  excluded by design; it needs per-ingredient density and would silently produce recipes
  that fail.
- **Abbreviating is not translating.** Spoons are a legitimate European measure, so they are
  shortened within their own language — `cullerades`→`cs`, `cucharadas`→`cda`,
  `tablespoons`→`tbsp` — and never converted to millilitres. Counting words with no
  language-neutral symbol (`gra`, `clove`, `fulla`) are left alone entirely, because
  abbreviating those would mean translating them.

### Consequences
- Scaling must run **before** normalising, not after, or a scaled imperial quantity gets
  paired with a metric symbol ("16 g" for two lots of 8 oz). `displayText()` enforces the
  order and a test covers it.
- The stored `originalText` of an imported recipe may differ from the source page. It is
  still what the user saw and approved, and no information is dropped — but "byte-identical
  to the website" is no longer a property of the database.
- Every abbreviation produced is a spelling `UnitLexicon` already recognises, so normalised
  text round-trips back through the parser.
- Adding a unit means touching `UnitLexicon` (spellings) and `UnitNormalizer` (target), in
  that order.

---

## ADR-009 — A fixed printed-book design system, not Material You

**Date:** 2026-08-11 · **Status:** Accepted

### Context
Until now the app took its colours from `dynamicLightColorScheme`, so it looked like
whatever wallpaper the phone happened to have, in Material's default type. That is a fine
default for an app whose job is to disappear into the system. This one's job is the
opposite: it is a private recipe book, and it should read as a book.

### Decision
A fixed design system in `core/designsystem`, modelled on a printed recipe book:

- **No dynamic colour.** One hand-picked palette. `ReceptariTheme` no longer takes a
  `dynamicColor` parameter, because there is nothing sensible for it to do.
- **Always light.** `darkTheme` survives as a parameter but defaults to `false` rather than
  to `isSystemInDarkTheme()`, and `enableEdgeToEdge` pins the system-bar icons dark to
  match. A printed book is paper; following the phone into dark mode hands the look back to
  a setting the design does not agree with. The lamplight palette — warm walnut and aged
  cream, the same book read at night — is written and correct, waiting for a theme
  preference in Settings to drive it.
- **Three bundled fonts** (SIL OFL, licences in `Docs/licenses/`): *Pinyon Script* for the
  wordmark only, *Playfair Display* for titles and headings, *Lora* for everything read
  rather than glanced at. Lora and Playfair ship as variable fonts, so each weight is the
  same file with a different `wght` axis.
- **Paper is generated, not shipped**, at two scales. A seamless 256 px tile carries the
  *sheet*: fine value-noise grain, pulp fibres, foxing specks — all high-frequency.
  Page-scale marks — cloudy mottling, a vignette, the shadow of the binding down the inner
  edge — are drawn against the composable's own size. `Modifier.paperGrain()` applies the
  tile alone, so cards are paper too rather than flat colour.
- **Tokens Material has no role for** — page edge, rule, gold, sealing-wax red — live in
  `ReceptariPalette`, reached through `ReceptariTheme.palette`. Everything that does map
  onto a Material role stays a Material role.

### Rationale
Dynamic colour and this aesthetic are mutually exclusive: the whole point of a wallpaper-
derived scheme is that the app does not control its own colours, and the whole point here is
that it does. Keeping both would mean the parchment and the ink survive only until someone
sets a blue wallpaper.

A photographic paper texture was the obvious alternative to generating one. It was rejected
because it costs about a megabyte and tiles visibly down a long scroll. Generated grain
costs one 256×256 bitmap and cannot seam.

The split between sheet scale and page scale was not a design flourish but a bug fix. The
first version put the broad mottling into the tile, and the repeat was the first thing you
saw — a tile laid across a phone four times over turns any coarse feature into a motif, and
a motif is wallpaper. Keeping the tile strictly high-frequency and moving the broad
variation to page-relative gradients removes the repeat entirely.

None of this was judged by eye on a device. The texture maths was ported to a throwaway
Python script and rendered to PNG so the layers could actually be looked at and tuned; the
constants in `Paper.kt` are the ones that survived that.

Three faces is one more than a careful designer would usually allow. The script face earns
its place only because it is confined to the wordmark; the moment it appears twice, the
whole thing reads as a pastiche.

### Consequences
- The app ignores Material You *and* the system dark-mode setting. On a device themed to
  match its wallpaper, at night, Receptari will be the one app that does not join in.
  Deliberate, and revisited when the theme preference lands.
- `values-night/` is gone. With the theme pinned light, a dark window background would flash
  parchment-over-walnut on launch for anyone whose phone is in dark mode.
- ~890 KB of fonts in the APK. Acceptable for a private app; the first thing to revisit if
  size ever matters.
- Screens must use `PaperScaffold`, not `Scaffold`. A bare `Scaffold` paints
  `colorScheme.background` over the grain and the vignette and looks *almost* right, which
  is the worst kind of wrong.
- `values/colors.xml` holds the pre-first-frame window background and must be kept in step
  with `Parchment` in `Color.kt`.
- The library's search field is now always visible rather than hidden behind an icon, so
  `LibraryUiState.isSearchActive` and `LibraryEvent.SearchActiveChanged` are gone.

---

## ADR-006 — Modern SDK floor: `minSdk 33`

**Date:** 2026-08-10 · **Status:** Accepted

### Context
This is a private app for the author and their family, all on recent devices. There is no
Play Store audience to maximise reach for, so the usual pressure to support old Android
versions does not apply.

### Decision
`minSdk 33` (Android 13), `targetSdk 36`, `compileSdk 36`.

### Rationale
API 33 is the level where every platform feature this app actually wants becomes native
rather than a compat backport:

- **`LocaleManager` per-app language** — the in-app language switcher central to ADR-004
  works through the system, with no AppCompat backport path to test.
- **Photo Picker and granular `READ_MEDIA_IMAGES`** — image import (PRD §9, §12) needs no
  legacy storage-permission branch.
- **`POST_NOTIFICATIONS`** as the single notification permission model.
- **Predictive back**, themed icons, and Material You without version guards.

Going higher (34/35/36) buys little for this app while excluding usable devices. Going
lower reintroduces exactly the compat branches listed above.

### Consequences
- Devices older than Android 13 (released 2022) cannot install the app. Accepted
  deliberately.
- No `androidx.core.os.LocaleListCompat` backport path, no legacy storage permissions, no
  `AppCompatDelegate` locale plumbing — one code path each.
- Lowering the floor later is a real cost once code assumes these APIs. If a family member
  turns up on Android 12, the realistic drop is to `minSdk 31`, which costs the
  `LocaleManager` and media-permission simplifications above.

---

## ADR-010 — Private copy transfer through versioned packages

**Date:** 2026-08-13 · **Status:** Accepted

### Context
Family members need to move anything from one recipe to a large collection between
Receptari installations. Requiring the extraction preview for every item in a batch would
turn already-approved structured data back into uncertain input and make a 50-recipe
transfer unusable. Full family synchronization still requires accounts, network storage,
conflict resolution, and deletion semantics.

### Decision
Receptari can send one recipe, a selected subset, or the whole library as a versioned
`.receptari-share` package. The primary in-person transport is Google Nearby Connections with
`P2P_POINT_TO_POINT`: the recipient explicitly starts receiving and both people confirm matching
authentication digits before accepting the connection. Android's Sharesheet remains the
file-based fallback. Receiving either way is one explicit acceptance; the app validates the
complete package and then automatically saves all non-duplicate recipes. Direct transfer reports
success to the sender only after the recipient has finished importing.

The package contains canonical recipe content, source attribution, tags, images, every
still-valid translation overlay, and the active display language. It excludes favorites,
ratings, notes, folders, cook events, API keys, and timers. Imported aggregates get fresh
UUIDs, translation field ids are remapped, and source fingerprints are recalculated. No
transfer or sync metadata is stored.

This supersedes ADR-002's consequence that there is "no sharing in v1". It does not
supersede ADR-003: storage remains local-only, with no accounts, backend, network storage,
or synchronization.

### Rationale
Nearby Connections removes the file-handling friction for two people in the same place, while
matching authentication digits make the intended peer visible to both. The Android Sharesheet
still supplies a familiar asynchronous fallback. Neither requires Receptari to operate a server
or know who the recipient is. A structured internal package preserves information and
translations exactly, while a package-level validation boundary makes one-click batch import
safe enough to bypass per-recipe extraction review.

### Consequences
- Transfers are copies. Later edits on the two devices diverge normally.
- Reopening the same package skips likely duplicates instead of overwriting local data.
- The custom archive codec must cap entry counts and sizes, reject undeclared entries and
  path traversal, and reject newer unsupported format versions.
- Direct transfer requires Google Play services and Android's nearby-device permissions. It does
  not require an account, internet service, cloud storage, or location data.
- The nearby session carries the same package bytes as file sharing, so transport and import
  validation remain separate.
