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
