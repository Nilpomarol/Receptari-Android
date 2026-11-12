<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="Receptari app icon" width="112">
</p>

<h1 align="center">Receptari</h1>

<p align="center">
  A private cookbook for the recipes you actually use.
  <br>
  Save them from a page, a photo, pasted text, or your own notes — then cook from one tidy recipe.
</p>

<p align="center">
  Android · Kotlin · Jetpack Compose · Catalan / Spanish / English
</p>

> **Status:** Version **1.0** is out. The core recipe book, import, translation, cooking,
> backup, and private transfer flows are implemented. There is no Play Store release;
> install the signed APK directly: [`Receptari-1.0.apk`](app/release/Receptari-1.0.apk)
> (Android 13+).

## Take a look

These are captures of the real app running with fictional sample recipes and
[credited food photographs](Docs/screenshots/PHOTO_CREDITS.md).

| Library | Recipe | Edit |
|:---:|:---:|:---:|
| <img src="Docs/screenshots/library.png" alt="Recipe library with search, filters, and sample recipes" width="240"> | <img src="Docs/screenshots/recipe.png" alt="Recipe detail with ingredient sections and a serving stepper" width="240"> | <img src="Docs/screenshots/editor.png" alt="Recipe editor with structured fields" width="240"> |

| Import | Cook mode |
|:---:|:---:|
| <img src="Docs/screenshots/import.png" alt="Options for bringing a recipe into Receptari" width="240"> | <img src="Docs/screenshots/cook-mode.png" alt="Focused cooking view showing one step at a time" width="240"> |

## What you can do

- **Keep one personal collection.** Create and edit recipes, add photos, organize them with
  folders and tags, find them with search and filters, and record when you cooked them.
- **Bring recipes in.** Import from a website, a photo, or pasted text. Extracted recipes
  open in the normal editor for review before they are saved. Website metadata is used
  directly when available, without an AI call.
- **Cook without rewriting a recipe.** Scale ingredient amounts for a different number of
  servings, follow one step at a time in cook mode, and run independent cooking timers.
- **Read in your language.** The interface supports Catalan, Spanish, and English. Recipe
  translations are kept as reversible display versions alongside the original wording.
- **Keep control of your data.** Recipes live on the device. Export or restore a backup,
  or send selected recipes privately to another Receptari installation.

## Designed to keep the source intact

An ingredient such as `1–2 onions` or `salt to taste` is useful even when it cannot be
fully parsed. Receptari keeps the approved original line and derives structured fields
only where it can. Serving scaling changes what is displayed, never the stored recipe.

AI helps extract and translate recipes, but an extracted import is **never saved without
review**. Import extraction uses Claude Haiku 5.5 and translation uses OpenAI GPT-6 Luna;
you supply your own Claude and OpenAI API keys in Settings, and both are encrypted on the
device. Manual recipes and websites with usable recipe metadata work without either key.
Photos or text are sent to a provider only when you request an AI operation.

## Try it

You need Android Studio, an Android SDK with platform 37, and JDK 21. The app supports
Android 13 and newer (`minSdk 33`). Open the project in Android Studio, let Gradle sync,
and run the `app` configuration on a device or emulator. Set `ANDROID_HOME` or let
Android Studio create `local.properties` so Gradle can find the SDK.

Build and run the checks from a terminal:

```powershell
# Windows PowerShell
.\gradlew.bat build
```

```bash
# macOS / Linux
./gradlew build
```

You can create and use manual recipes immediately. To try AI extraction, enter your own
Anthropic API key in **Settings**; for translation, an OpenAI API key. The project does not
ship with any key.

## Signed release APK

The current release is version `1.0` (`versionCode 2`), committed as
[`app/release/Receptari-1.0.apk`](app/release/Receptari-1.0.apk). Release builds enable R8
and resource shrinking. To sign a release, create a release keystore using Android Studio's
**Build > Generate Signed Bundle/APK**, then copy `keystore.properties.example` to
`keystore.properties` and fill in the keystore path, alias, and passwords. The keystore
path is relative to the repository root, or may be absolute (use forward slashes on Windows).

Run `./gradlew build` (or `.\gradlew.bat build` on Windows). With local signing configured,
the signed APK is `app/build/outputs/apk/release/app-release.apk`. Without
`keystore.properties`, release builds remain unsigned so contributors and CI need no secrets.

Keystores and `keystore.properties` are ignored by Git. Keep a private backup of both:
future updates must use the same signing key and a higher `versionCode`.

## Under the hood

The app is a single Android module organized into `ui`, `domain`, and `data` layers.
Its main tools are Kotlin, Jetpack Compose and Material 3, Room, Hilt, DataStore,
Coroutines/Flow, Jsoup, OkHttp, the Anthropic SDK, and the OpenAI Responses API. The visual design uses a fixed
printed-book palette and bundled open-licensed fonts rather than dynamic system colours.

The test suite covers ingredient parsing and scaling, import and transfer logic, ViewModels,
Room DAOs, and schema migrations. GitHub Actions runs assembly, unit tests, lint, and the
Compose localization guardrail on pushes and pull requests.

This project is developed with Claude Code and Codex. Product rules and architectural
decisions are written down so agent-generated changes can be reviewed against the same
constraints as hand-written code. See [`AGENTS.md`](AGENTS.md) for those rules.

<details>
<summary>Project documentation</summary>

- [`Docs/PRD.md`](Docs/PRD.md) — product requirements
- [`Docs/ROADMAP.md`](Docs/ROADMAP.md) — progress and remaining work
- [`Docs/DECISIONS.md`](Docs/DECISIONS.md) — architecture decisions
- [`Docs/ARCHITECTURE.md`](Docs/ARCHITECTURE.md) — layers and major flows
- [`Docs/DATA_MODEL.md`](Docs/DATA_MODEL.md) — persistence model
- [`Docs/AI_INTEGRATION.md`](Docs/AI_INTEGRATION.md) — extraction, translation, and key handling
- [`Docs/CONVENTIONS.md`](Docs/CONVENTIONS.md) — code and localization conventions

</details>
