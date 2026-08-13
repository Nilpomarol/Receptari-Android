# Personal Recipe Book — Requirements

## 1. Overview

A private recipe book application for personal, family, and close-friend use.

The application stores recipes in a consistent structured format regardless of their original source.

Recipes can be:

- Created manually.
- Imported from a website.
- Imported from an image, screenshot, scanned book page, or similar source.
- Imported from unstructured text.
- Translated into the preferred application language.
- Received as a private structured copy from another Receptari installation.

The application is not intended to be a public recipe platform, social network, or recipe discovery service.

---

## 2. Core Principles

The application should be:

- Simple.
- Fast to use.
- Private.
- Easy to maintain.
- Focused on storing and using personal recipes.
- Flexible enough to import recipes from different sources.
- Structured internally even when the original recipe is unstructured.

Avoid unnecessary complexity or features unrelated to maintaining a personal recipe collection.

---

# 3. Recipe

Each recipe must support the following data.

## 3.1 Basic Information

- Title.
- Main image.
- Preparation time.
- Cooking time.
- Total time.
- Base number of servings.
- Favorite status.
- Personal rating.
- Personal notes.
- Tags.
- Original source.
- Original URL when applicable.
- Original language when applicable.

Only the title is strictly required for manually created recipes.

---

## 3.2 Ingredients

A recipe contains one or more ingredient sections.

Example:

**Sauce**
- 200 ml cream
- 1 tbsp butter
- 2 garlic cloves

**Chicken**
- 500 g chicken
- Salt to taste

Each ingredient should internally support:

- Quantity.
- Unit.
- Ingredient name.
- Optional note.
- Original text.

Example:

`2 tbsp olive oil`

should preferably be stored as:

- Quantity: `2`
- Unit: `tbsp`
- Ingredient: `olive oil`
- Original text: `2 tbsp olive oil`

Ingredients that cannot be fully structured must still be supported.

Examples:

- Salt to taste.
- A handful of parsley.
- 1–2 onions.

The original ingredient text must never be lost during parsing.

---

## 3.3 Ingredient Scaling

The user must be able to change the number of servings.

Ingredient quantities should automatically scale relative to the original number of servings.

Example:

Original recipe:

`4 servings — 400 g chicken`

Changing to:

`6 servings`

should display:

`600 g chicken`

Ingredients without numeric quantities should remain unchanged.

The stored base recipe must not be modified when changing the temporary serving multiplier.

---

## 3.4 Instructions

A recipe contains an ordered list of steps.

Each step contains:

- Step number/order.
- Instruction text.

Instructions may optionally be grouped into sections.

Example:

**Prepare sauce**

1. Chop the onion.
2. Cook until soft.

**Cook chicken**

3. Add chicken.
4. Cook for 10 minutes.

## 3.5 Cooking Timer

While following a recipe, the user can start multiple independent cooking timers.

- A timer can be started manually or from an instruction step.
- When a supported duration is present in a step, the app may prefill it using deterministic
  local parsing; the user always confirms or changes the duration before starting.
- The timer remains active when the recipe is scrolled, the app is backgrounded, or its
  process is recreated.
- Active timers live in a safe-area-aware horizontal dock so they remain accessible without
  covering the recipe. The user can pause, resume, add one minute, or cancel each timer.
- Every timer has its own ongoing system notification with its remaining time and controls.
- Finishing a timer alerts the user but never marks the recipe as cooked automatically.
- Timer state is transient application state. It does not modify the recipe, its base times,
  its ingredients, or its cooked-history event log.

## 3.6 Focused Cook Mode

The recipe detail screen offers an optional focused mode for following instructions while
cooking.

- The mode presents one instruction at a time using large, high-contrast text, with clear
  previous and next controls and the current position in the recipe.
- Section names remain visible so grouped instructions keep their context.
- Ingredients remain available in a temporary sheet without leaving the current step.
- A timer can be started directly from the current instruction, and all active timers remain
  accessible in the safe-area-aware timer dock.
- The device screen stays awake for the entire time focused cook mode is visible. Normal
  system screen-sleep behaviour resumes immediately after leaving it.
- Entering, navigating, or closing cook mode never edits the recipe or records a cooked-history
  event. The explicit final-step Finish action records one event and then leaves cook mode.

---

# 4. Recipe History

The application should track usage of recipes.

For each recipe:

- Number of times cooked.
- Last cooked date.
- Optional rating.
- Optional notes.

The user must be able to mark a recipe as cooked.

This is preferred over a simple completed/not-completed status because recipes can be prepared repeatedly.

---

# 5. Recipe Organization

Recipes must support:

- Tags.
- Folders.
- Favorites.
- Sorting.
- Filtering.

Possible sorting options:

- Alphabetical.
- Recently added.
- Recently cooked.
- Most cooked.
- Highest rated.
- Cooking time.

Possible filters:

- Favorite.
- Tags.
- Folder.
- Ingredients.
- Never cooked.
- Recently cooked.

Tags remain user-defined and should not require a complex category system.

Examples:

- Pasta
- Chicken
- Dessert
- Quick
- Christmas
- Grandma
- Asian

## Folders

A second, simpler way to file a recipe, alongside tags:

- A recipe belongs to **at most one folder** (never several).
- Folders are a **flat list** — no subfolders, no nesting.
- Folders are user-created, renamed, and deleted; a folder's own screen carries those actions
  rather than a separate management screen.
- Each folder carries a colour and an icon, both chosen from a small curated set (a fixed set
  of bookish ink colours and a fixed pictogram set) — never a free colour picker or an uploaded
  image, consistent with the fixed design system (ADR-009).
- Deleting a folder never deletes its recipes — they simply become unfiled.

Folders answer "which one book is this recipe in", where tags answer "what is true about
this recipe" — the two are complementary, not competing.

---

# 6. Search

The application must provide a global recipe search.

Search should match:

- Recipe title.
- Ingredients.
- Tags.
- Notes.

Search should tolerate partial matches.

Example:

Searching:

`chicken`

should find recipes where chicken appears either in the title, ingredients, tags, or notes.

---

# 7. Recipe Import

The application must provide a central **Import Recipe** flow.

Available options:

1. From website.
2. From image.
3. From text.
4. Create manually.

All extraction-based imports must generate an editable preview before the recipe is saved.

Flow:

`Source → Extract → Structure → Preview/Edit → Save`

Automatic imports must never silently overwrite or save incorrect information without giving the user an opportunity to review it.

## 7.1 Receptari-to-Receptari Transfer

The user can send one recipe, any selected subset, or the whole library as a private,
versioned Receptari package. The library supports selection mode and selecting every recipe
currently visible after search or filtering.
The package contains already-approved structured data rather than extraction output, so it
is the deliberate exception to the per-recipe preview requirement:

`Select recipes → Send nearby → Both confirm code → Validate package → Save valid recipes`

The sender chooses between a direct nearby Receptari connection and Android's normal sharing
options. For a direct transfer, the recipient opens **Add a recipe → Receive from a nearby
Receptari** first. Both devices must show and confirm the same authentication code before any
recipe data moves. A `.receptari-share` file remains available as a fallback.

- One acceptance imports the complete valid batch automatically; receiving 50 recipes must
  not require 50 review screens.
- The package includes recipe content, source attribution, images, tags, every still-valid
  cached translation overlay, and the active display language.
- It excludes favorites, ratings, personal notes, folder membership, cooked history, API
  keys, and active timers.
- Imported recipes receive fresh UUIDs. Translation field references are remapped to the
  new ids and their source fingerprints are recalculated.
- Likely duplicates are skipped rather than overwriting an existing recipe.
- The entire package is structurally and size validated before any recipe is saved.
- A transfer is a copy, not synchronization. Later edits on either device are independent.
- The sending device reports success only after the receiving device has validated and imported
  the package.

---

# 8. Website to Recipe

The user can provide a recipe URL.

The application should attempt to extract:

- Title.
- Image.
- Times.
- Servings.
- Ingredients.
- Instructions.
- Source information.

Preferred extraction order:

1. Structured recipe metadata such as Schema.org Recipe / JSON-LD.
2. Relevant webpage content.
3. AI-assisted extraction as fallback.

The original URL must be stored with the recipe.

The import system must tolerate websites where some fields are unavailable.

Website import is not expected to work perfectly with every website.

Possible unsupported cases include:

- Paywalls.
- Authentication requirements.
- Anti-bot protection.
- Broken or incomplete markup.
- Highly dynamic websites.

Manual correction must always remain possible.

---

# 9. Image to Recipe

The user can import one or more images containing a recipe.

Supported sources may include:

- Cookbook pages.
- Magazine pages.
- Screenshots.
- Printed recipes.
- Handwritten recipes where readable.

The system should extract and structure:

- Title.
- Times.
- Servings.
- Ingredients.
- Instructions.

The resulting recipe must be shown in an editable preview before being saved.

The system should support multiple images when a recipe spans several pages.

The application does not need to reliably generate a recipe merely from a photograph of the finished dish.

---

# 10. Text to Recipe

The user can paste unstructured recipe text.

Example:

```text
Chicken curry
Serves 4

500g chicken
1 onion
2 garlic cloves

Cook the onion.
Add chicken.
Cook for 15 minutes.
```

The application should convert this into the standard recipe structure.

It should identify, when possible:

- Title.
- Servings.
- Times.
- Ingredient sections.
- Individual ingredients.
- Steps.

The user must be able to review and correct the result before saving.

---

# 11. Translation

Recipes can be translated into the user's preferred language.

Translation should operate on structured recipe content instead of translating the complete original webpage.

Translate:

- Title.
- Ingredient names.
- Ingredient notes.
- Instruction text.
- Ingredient and instruction section names.

Do not unnecessarily modify:

- Quantities.
- Measurements.
- URLs.
- Source information.
- Tags, which are a library-wide taxonomy rather than recipe prose.

The original language and original content should be retained whenever practical.

Selecting a language applies its display version directly on the recipe detail screen; the
translation flow does not route through the edit form. The canonical source title,
ingredient lines, step text, and section names always remain intact, so the switch is
reversible. If a translation needs correction, the user edits the active display version
through the ordinary Edit action afterward.

Catalan, Spanish, and English display versions are cached locally per recipe, so switching
back to an available language does not spend another API call. The picker identifies the
original language, available translations, and the currently displayed language. Editing
canonical source content makes older cached versions stale and forces a fresh translation.

---

# 12. Images

Each recipe can have a main image.

Images may come from:

- Manual image selection.
- Camera.
- Imported website.
- Imported recipe page.

The user must be able to:

- Add an image.
- Replace an image.
- Remove an image.

A recipe must remain valid without an image.

---

# 13. Editing

Every stored recipe must be editable.

The user must be able to change:

- Title.
- Image.
- Times.
- Servings.
- Ingredients.
- Ingredient sections.
- Steps.
- Step order.
- Tags.
- Rating.
- Notes.
- Source information.

Imported content must behave exactly like manually created content after it is saved.

---

# 14. Data Integrity

Automatic extraction and AI processing are considered assistance, not authoritative data.

The system must:

- Preserve original text where useful.
- Avoid deleting information it cannot understand.
- Allow manual correction.
- Show extracted content before saving. A validated structured package from another
  Receptari installation follows §7.1 instead.
- Avoid changing the original base quantities when scaling servings.

---

# 15. Privacy

The application is intended for private use.

There is no requirement for:

- Public profiles.
- Public recipe pages.
- Followers.
- Likes.
- Comments.
- Public recipe discovery.
- Social feeds.

If multiple family members use the application, access should remain restricted to explicitly authorized users.

Private copy transfer does not create accounts, public links, or a shared cloud library.
The recipient explicitly accepts each incoming package.

---

# 16. Out of Scope for Initial Version

The following features are intentionally excluded from the initial scope:

- Social network features.
- Public recipe publishing.
- Recipe marketplace.
- Automatic nutrition calculation.
- Calorie tracking.
- Pantry inventory.
- Restaurant integration.
- Grocery-store integration.
- AI recipe generation.
- Recipe recommendations.
- Meal planning.
- Advanced household permissions.
- Nested or multi-level folder hierarchies, and a recipe belonging to more than one folder
  at once — folders stay a flat list with single membership (§5). Beyond that, complex
  categories or taxonomies remain out of scope.

These features may be considered later but must not complicate the initial implementation.

---

# 17. Potential Future Features

Natural future extensions include:

## Shopping List

Generate a shopping list from one or more recipes using the structured ingredient data.

## Ingredient-Based Discovery

Allow the user to enter ingredients already available at home and find matching recipes.

## Meal Planning

Assign recipes to specific days.

## Family Sharing

Synchronize the same private recipe collection between selected family members.

## Import From Additional Sources

Potential support for:

- PDFs.
- Social media posts.
- Videos.
- Shared links from other applications.

These should be treated as future extensions rather than core requirements.

---

# 18. MVP

The minimum useful version should include:

- Recipe library.
- Manual recipe creation.
- Recipe editing.
- Structured ingredients.
- Recipe steps.
- Images.
- Serving multiplier.
- Tags.
- Favorites.
- Personal notes.
- Cooked history.
- Search.
- Sorting and filtering.
- Website import.
- Image import.
- Text import.
- Import preview and correction.
- Translation.
- Private Receptari-to-Receptari recipe transfer.

The MVP should prioritize reliability and simplicity over the number of supported import sources.

---

# 19. Main User Flow

The primary application experience should be:

### Browse

`Recipe Library → Search / Filter → Recipe`

### Import

`Add Recipe → Website / Image / Text / Manual → Preview → Edit if required → Save`

### Send

`Recipe / Settings → Send a copy → Recipient accepts → Validate → Save batch`

### Cook

`Recipe → Select servings → Follow ingredients and steps → Mark as cooked`

These three flows represent the core purpose of the application.
