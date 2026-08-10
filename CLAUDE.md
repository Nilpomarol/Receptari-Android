# CLAUDE.md

This project is developed with both Claude Code and Codex. To keep the two in sync, all
shared guidance lives in a single file that both tools read.

@AGENTS.md

---

## Claude Code specifics

Everything above applies. The notes below are Claude Code features that Codex does not
have, so they are kept out of the shared file.

### Skills worth reaching for

This is a Kotlin + Jetpack Compose project, so the Compose and Kotlin skills are usually
the right call before writing UI or state code:

- `chrisbanes-skills:compose-state-holder-ui-split` — before writing any screen. The
  `FooRoute` / `FooScreen` split in AGENTS.md §5 comes from here.
- `chrisbanes-skills:compose-state-hoisting` — when deciding whether state belongs in
  `remember`, a parameter, a state holder, or the ViewModel.
- `chrisbanes-skills:kotlin-flow-state-event-modeling` — for `StateFlow` / one-shot event
  design in ViewModels.
- `chrisbanes-skills:compose-side-effects` — `LaunchedEffect`, snackbars, navigation.
- `chrisbanes-skills:compose-modifier-and-layout-style` — modifier parameter conventions.
- `chrisbanes-skills:kotlin-coroutines-structured-concurrency` — for repository and
  import-pipeline code.
- `chrisbanes-skills:compose-ui-testing-patterns` — when writing UI tests.
- `claude-api` — **required reading before touching anything under `data/remote/`.** Do
  not write Claude API request code, pick a model id, or reason about pricing/caching from
  memory.
- `compose-expert:compose-expert` — broader Compose questions not covered above.

### Slash commands

- `/code-review` — review the working diff before committing.
- `/security-review` — run before any change that touches the API key, key storage, or
  network layer.

### Permissions

`.claude/settings.json` pre-allows the Gradle and read-only git commands this project
needs, so routine builds do not prompt. If you hit a prompt for a command that is clearly
routine and safe, mention it rather than working around it.
