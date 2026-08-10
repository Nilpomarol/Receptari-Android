# Receptari

A private personal recipe book for Android.

Recipes are stored in one consistent structured format no matter where they came from —
typed by hand, imported from a website, read from a photo of a cookbook page, or pasted as
plain text — and can be translated into your language, scaled to any number of servings,
searched, tagged, and marked as cooked.

It is deliberately **not** a social network, a public recipe platform, or a discovery
service.

**Status:** scaffold in place — the app builds, runs, and switches language, but has no
recipe features yet. See [`Docs/ROADMAP.md`](Docs/ROADMAP.md).

---

## Documentation

| Document | What it covers |
|---|---|
| [`Docs/PRD.md`](Docs/PRD.md) | Product requirements — the authoritative spec |
| [`Docs/ROADMAP.md`](Docs/ROADMAP.md) | Phased delivery plan and current status |
| [`Docs/DECISIONS.md`](Docs/DECISIONS.md) | Architecture decision records |
| [`Docs/ARCHITECTURE.md`](Docs/ARCHITECTURE.md) | Layering, packages, import pipeline, scaling, search |
| [`Docs/DATA_MODEL.md`](Docs/DATA_MODEL.md) | Room schema and domain models |
| [`Docs/AI_INTEGRATION.md`](Docs/AI_INTEGRATION.md) | Claude API usage, key handling, extraction and translation |
| [`Docs/CONVENTIONS.md`](Docs/CONVENTIONS.md) | Kotlin/Compose style, localization, testing, git |

---

## Stack

Kotlin · Jetpack Compose · Material 3 · Room · Hilt · DataStore · Coroutines/Flow
Android only, `minSdk 33` (Android 13). Local-only storage, no accounts, no backend.
AI features call the Claude API directly with an API key you supply in Settings.

---

## Working with AI coding agents

This repository is developed with both **Claude Code** and **Codex**.

- [`AGENTS.md`](AGENTS.md) holds all shared guidance and is read automatically by Codex.
- [`CLAUDE.md`](CLAUDE.md) imports `AGENTS.md` and adds Claude Code specifics (skills,
  slash commands).
- `.claude/settings.json` pre-allows routine Gradle and read-only git commands.

Keeping the shared rules in one file means the two tools cannot drift apart. Add anything
tool-agnostic to `AGENTS.md`, not to `CLAUDE.md`.

---

## Building

Gradle needs a JDK 17+. The system default here is Java 8, so point `JAVA_HOME` at Android
Studio's bundled runtime:

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew build
```

Debug APK only:

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug
```
