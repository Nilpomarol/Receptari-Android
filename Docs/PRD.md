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

All automatic imports must generate an editable preview before the recipe is saved.

Flow:

`Source → Extract → Structure → Preview/Edit → Save`

Automatic imports must never silently overwrite or save incorrect information without giving the user an opportunity to review it.

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
- Tags only when appropriate.

Do not unnecessarily modify:

- Quantities.
- Measurements.
- URLs.
- Source information.

The original language and original content should be retained whenever practical.

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
- Show imported content before saving.
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
- Complex categories or taxonomies.

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

The MVP should prioritize reliability and simplicity over the number of supported import sources.

---

# 19. Main User Flow

The primary application experience should be:

### Browse

`Recipe Library → Search / Filter → Recipe`

### Import

`Add Recipe → Website / Image / Text / Manual → Preview → Edit if required → Save`

### Cook

`Recipe → Select servings → Follow ingredients and steps → Mark as cooked`

These three flows represent the core purpose of the application.