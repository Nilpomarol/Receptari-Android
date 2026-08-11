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

## ADR-005 — Translation is in-place, with original text preserved per field

**Date:** 2026-08-10 · **Status:** Accepted

### Context
PRD §11 requires translating recipes while "retaining the original language and original
content whenever practical". A full translation table (one row per recipe per language)
would satisfy this completely but adds significant complexity to every read path.

### Decision
Translation writes the translated values into the recipe's normal fields. The original is
preserved via `Ingredient.originalText` (already mandatory), a nullable `Step.originalText`,
a nullable `Recipe.originalTitle`, and `Recipe.originalLanguage`. Quantities, units, URLs,
and source information are never sent to the model.

### Rationale
Keeps every read path — list, detail, search, scaling — working on a single set of fields
with no language dimension. Satisfies "retain whenever practical" without a translation
table the MVP does not need.

### Consequences
- A recipe exists in exactly one display language at a time. Re-translating overwrites.
- Reverting to the original is possible field-by-field but is not a v1 feature.
- If multi-language display is ever wanted, a `RecipeTranslation` table supersedes this ADR.
- **Open problem for Phase 5.** Ingredient lines are rendered from `Ingredient.originalText`
  rather than rebuilt from `quantity + unit + name`, because the source text carries the
  connector and plural agreement that Catalan and Spanish need ("2 grans **d'**all", not
  "2 grans all") — see `ScaledIngredient.displayText`. Translation breaks that assumption:
  once `name` is Catalan and `originalText` is still English, showing `originalText` shows
  the untranslated line. Translation must therefore either rewrite `originalText` into the
  target language (and preserve the pre-translation text elsewhere), or Phase 5 must add a
  structured renderer with localized unit labels and a connector rule. Decide before
  building the translate action, not after.

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

- **No dynamic colour.** One hand-picked palette, plus a dark variant. `ReceptariTheme` no
  longer takes a `dynamicColor` parameter, because there is nothing sensible for it to do.
- **Light is paper, dark is lamplight.** The dark theme is warm walnut and aged cream — the
  same book read at night — rather than the neutral near-black Material would derive.
- **Three bundled fonts** (SIL OFL, licences in `Docs/licenses/`): *Pinyon Script* for the
  wordmark only, *Playfair Display* for titles and headings, *Lora* for everything read
  rather than glanced at. Lora and Playfair ship as variable fonts, so each weight is the
  same file with a different `wght` axis.
- **Paper is generated, not shipped.** `Modifier.paperBackground()` draws a colour, a 96 px
  repeating grain tile, and an off-centre vignette.
- **Tokens Material has no role for** — page edge, rule, gold, sealing-wax red — live in
  `ReceptariPalette`, reached through `ReceptariTheme.palette`. Everything that does map
  onto a Material role stays a Material role.

### Rationale
Dynamic colour and this aesthetic are mutually exclusive: the whole point of a wallpaper-
derived scheme is that the app does not control its own colours, and the whole point here is
that it does. Keeping both would mean the parchment and the ink survive only until someone
sets a blue wallpaper.

A photographic paper texture was the obvious alternative to generating one. It was rejected
because it needs a light and a dark variant, costs about a megabyte, and tiles visibly down
a long scroll. Generated grain costs one 96×96 bitmap per theme and cannot seam.

Three faces is one more than a careful designer would usually allow. The script face earns
its place only because it is confined to the wordmark; the moment it appears twice, the
whole thing reads as a pastiche.

### Consequences
- The app ignores Material You. On a device themed to match its wallpaper, Receptari will
  be the one app that does not join in. Deliberate.
- ~890 KB of fonts in the APK. Acceptable for a private app; the first thing to revisit if
  size ever matters.
- Screens must use `PaperScaffold`, not `Scaffold`. A bare `Scaffold` paints
  `colorScheme.background` over the grain and the vignette and looks *almost* right, which
  is the worst kind of wrong.
- `values/colors.xml` and `values-night/colors.xml` hold the pre-first-frame window
  background and must be kept in step with `Parchment` and `Leather` in `Color.kt`.
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
