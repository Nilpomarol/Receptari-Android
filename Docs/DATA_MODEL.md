# Data Model

Target Room schema. Every table uses a **string UUID primary key** (ADR-003) and every
mutable row carries `updatedAt` as epoch milliseconds.

---

## 1. Entity overview

```
Recipe ──1:n──► IngredientSection ──1:n──► Ingredient
   │
   ├──1:n──► InstructionSection ──1:n──► Step
   │
   ├──n:m──► Tag            (via RecipeTagCrossRef)
   │
   ├──n:1──► Folder          (nullable, at most one)
   │
   └──1:n──► CookEvent

RecipeFts   (FTS4, derived — rebuilt on save)
```

---

## 2. Tables

### `recipes`

| Column | Type | Notes |
|---|---|---|
| `id` | `String` PK | UUID |
| `title` | `String` | the only required field (PRD §3.1) |
| `originalTitle` | `String?` | preserved when translated (ADR-005) |
| `imagePath` | `String?` | relative path under `filesDir/images/` |
| `prepTimeMinutes` | `Int?` | |
| `cookTimeMinutes` | `Int?` | |
| `totalTimeMinutes` | `Int?` | stored, not derived — sources often give only a total |
| `baseServings` | `Int?` | null ⇒ the serving multiplier UI is hidden |
| `isFavorite` | `Boolean` | default `false` |
| `rating` | `Int?` | 1..5, null = unrated |
| `notes` | `String?` | personal notes |
| `sourceName` | `String?` | e.g. "Cuinar amb la iaia", "NYT Cooking" |
| `sourceUrl` | `String?` | required for website imports (PRD §8) |
| `originalLanguage` | `String?` | BCP-47, e.g. `en`, `es`, `ca` |
| `folderId` | `String?` FK → `folders.id` | `ON DELETE SET NULL` — deleting a folder unfiles its recipes, never deletes them |
| `createdAt` | `Long` | epoch ms |
| `updatedAt` | `Long` | epoch ms |

Indexes: `title`, `isFavorite`, `updatedAt`, `folderId`.

### `ingredient_sections`

| Column | Type | Notes |
|---|---|---|
| `id` | `String` PK | |
| `recipeId` | `String` FK → `recipes.id` | `ON DELETE CASCADE`, indexed |
| `name` | `String?` | null = the unnamed default section |
| `position` | `Int` | 0-based ordering |

Every recipe has at least one section. A recipe with no groupings has exactly one section
with `name = null` — the UI hides the header in that case.

### `ingredients`

| Column | Type | Notes |
|---|---|---|
| `id` | `String` PK | |
| `sectionId` | `String` FK → `ingredient_sections.id` | `ON DELETE CASCADE`, indexed |
| `position` | `Int` | |
| `quantity` | `Double?` | lower bound of a range, or the single value |
| `quantityMax` | `Double?` | upper bound; non-null only for ranges (`1–2 onions`) |
| `unit` | `String?` | normalized token (`g`, `ml`, `tbsp`, `cullerada`…) |
| `name` | `String?` | ingredient name with unit and quantity stripped |
| `note` | `String?` | e.g. `finely chopped`, `al gust` |
| `originalText` | `String` **NOT NULL** | verbatim source text — never lost (PRD §3.2) |

**`originalText` is the integrity guarantee of this project.** All four of `quantity`,
`unit`, `name`, and `note` may be null simultaneously; the row is still valid and renders
from `originalText`.

### `instruction_sections`

Same shape as `ingredient_sections`: `id`, `recipeId`, `name?`, `position`.

### `steps`

| Column | Type | Notes |
|---|---|---|
| `id` | `String` PK | |
| `sectionId` | `String` FK → `instruction_sections.id` | `ON DELETE CASCADE`, indexed |
| `position` | `Int` | step order within the section |
| `text` | `String` | |
| `originalText` | `String?` | preserved when translated (ADR-005) |

Displayed step numbering is continuous across sections (PRD §3.4: *Prepare sauce* 1–2,
*Cook chicken* 3–4), computed at render time from `position` — not stored.

### `tags`

| Column | Type | Notes |
|---|---|---|
| `id` | `String` PK | |
| `name` | `String` | display form, e.g. `Postres` |
| `normalizedName` | `String` UNIQUE | lowercased, accent-stripped — dedupe key |

User-defined and flat. No hierarchy, no colours, no categories (PRD §5).

### `recipe_tag_cross_ref`

Composite PK `(recipeId, tagId)`, both indexed, both `ON DELETE CASCADE`.

### `folders`

| Column | Type | Notes |
|---|---|---|
| `id` | `String` PK | |
| `name` | `String` | display form, e.g. `Postres de Nadal` |
| `normalizedName` | `String` UNIQUE | lowercased, accent-stripped — dedupe key |
| `color` | `String` | `FolderColor` enum name — one of a fixed eight-ink palette, not a free colour picker |
| `icon` | `String` | `FolderIcon` enum name — a curated pictogram set, not a free image |

User-created, flat — no nesting (PRD §5). A recipe references at most one folder via
`recipes.folderId`; this is a plain FK, not a join table, because membership is single, not
many-to-many like tags. Colour and icon are stored as enum names rather than raw values so a
future rename of a palette entry doesn't require a data migration; the mapper falls back to
the defaults (`OLIVE` / `FOLDER`) on an unrecognised value.

### `cook_events`

| Column | Type | Notes |
|---|---|---|
| `id` | `String` PK | |
| `recipeId` | `String` FK → `recipes.id` | `ON DELETE CASCADE`, indexed |
| `cookedAt` | `Long` | epoch ms |
| `note` | `String?` | optional per-occasion note |

**Times cooked and last cooked are derived from this table**, never stored as counters on
`recipes` (PRD §4). A recipe can be cooked many times; each is a row.

```sql
SELECT COUNT(*), MAX(cookedAt) FROM cook_events WHERE recipeId = :id
```

### `recipe_fts`

FTS4 virtual table: `recipeId`, `title`, `notes`, `ingredientNames`, `tagNames`.
Written by `RecipeDao.saveAggregate` in the same transaction as any recipe write. Not a
source of truth — droppable and rebuildable.

**SQLite does not allow foreign keys on virtual tables**, so the cascade that clears
sections, ingredients, steps, tag links and cook events does *not* reach this table.
`RecipeDao.deleteRecipe` therefore deletes the index row explicitly, inside a transaction.
Forgetting this leaves search returning ids of recipes that no longer exist —
`RecipeDaoTest.deletingARecipeTakesItsChildrenWithIt` guards it.

Ingredients contribute their parsed `name` when there is one and their `originalText`
otherwise, so an unstructurable line like "Sal al gust" is still findable.

---

## 3. Query shapes the schema must serve well

| PRD ref | Query |
|---|---|
| §5 sort | alphabetical, recently added (`createdAt`), recently cooked (`MAX(cookedAt)`), most cooked (`COUNT(cook_events)`), highest rated, cooking time |
| §5 filter | favourite, by tag, by folder (`recipes.folderId = :id`), by ingredient, never cooked (`NOT EXISTS` on `cook_events`), recently cooked |
| §6 search | prefix match across title / ingredient names / tags / notes via `recipe_fts` |

The list screen reads a `RecipeSummary` projection (id, title, imagePath, totalTime,
isFavorite, rating, cookCount, lastCookedAt) — never the full object graph.

---

## 4. Domain vs entity models

`domain/model` types are what the app reasons about; `data/local/entity` types are what
Room stores. They are separate classes with explicit mappers.

The key difference: the domain `Recipe` is a full aggregate —

```kotlin
data class Recipe(
    val id: String,
    val title: String,
    …
    val ingredientSections: List<IngredientSection>,
    val instructionSections: List<InstructionSection>,
    val tags: List<Tag>,
    val cookCount: Int,
    val lastCookedAt: Instant?,
)
```

— assembled by the repository with a Room `@Transaction` query. The UI never sees foreign
keys or `position` columns; ordering is expressed by list order, and the mapper writes
`position` back on save.

---

## 5. Migrations

- `exportSchema = true`; JSON schemas committed under `app/schemas/`.
- Every schema change ships an explicit `Migration` **and** a migration test. No
  `fallbackToDestructiveMigration()` outside debug builds.
- Version 1 is whatever ships at the end of Roadmap Phase 2; there are no users before then,
  so get it right rather than migrating early.
- `Migration(1, 2)` adds folders: `CREATE TABLE folders(...)`, a unique index on its
  `normalizedName`, `ALTER TABLE recipes ADD COLUMN folderId TEXT REFERENCES folders(id)`,
  and an index on `recipes.folderId`. Covered by `MigrationTest.migrate1To2`.
- `Migration(2, 3)` adds folder appearance: `ALTER TABLE folders ADD COLUMN color TEXT NOT
  NULL DEFAULT 'OLIVE'` and `ADD COLUMN icon TEXT NOT NULL DEFAULT 'FOLDER'`, so every folder
  created before this version gets the same default a new folder starts with. Covered by
  `MigrationTest.migrate2To3`.
