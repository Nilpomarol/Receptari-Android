# AI Integration

Covers PRD §8–§11: website import, image import, text import, and translation.

**Governing principle (PRD §14): AI output is assistance, not authoritative data.** No model
response is ever written straight to the database. Every one lands in an editable preview.

---

## 1. Shape

```
domain/ai/
    AiClient                  interface — the only AI abstraction the app knows
    DraftRecipe               nullable-everything recipe, the output of every extractor
    ExtractionRequest/Result
    TranslationRequest/Result

data/remote/claude/
    ClaudeAiClient            implements AiClient
    dto/                      wire types (kotlinx.serialization)
    prompt/                   system prompts + JSON schemas, one per task
```

Nothing outside `data/remote/claude/` knows the vendor, the model id, or the wire format.
ADR-002 chose device-direct calls; moving to a backend proxy later is a change inside this
package only.

```kotlin
interface AiClient {
    suspend fun extractFromText(text: String): Result<DraftRecipe>
    suspend fun extractFromImages(images: List<ByteArray>): Result<DraftRecipe>
    suspend fun extractFromWebContent(url: String, content: String): Result<DraftRecipe>
    suspend fun translate(request: TranslationRequest): Result<TranslationResult>
}
```

---

## 2. API key handling

Non-negotiable rules:

- The key is **user-supplied**, entered in Settings. The app ships no key.
- Stored encrypted at rest: AES-GCM with an Android Keystore-backed key, ciphertext in
  DataStore. Never in `SharedPreferences` in plaintext, never in `local.properties`, never
  in the APK.
- **Never logged.** Not in debug builds, not in OkHttp logging interceptors (the
  `x-api-key` header must be redacted), not in crash traces, not in error messages shown to
  the user.
- Sent to `api.anthropic.com` and nowhere else.
- Settings offers "test key" (a minimal request) and "remove key".
- With no key configured, the manual-creation and website-JSON-LD paths still work fully.
  The UI must degrade, not break.

Run `/security-review` before merging any change that touches this.

---

## 3. Website import (PRD §8)

Escalating strategy — **do not call the model when cheaper steps already answered**:

1. **JSON-LD / schema.org `Recipe`** — parse `<script type="application/ld+json">`. Covers
   the large majority of recipe sites. Handle `@graph` arrays and `Recipe` nested in
   `@type` lists.
2. **Microdata / RDFa** `itemtype="…/Recipe"`.
3. **AI fallback** — strip scripts, styles, nav, and comments from the HTML, send the
   remaining text to `extractFromWebContent`.

`sourceUrl` is always stored, whichever step succeeded. Partial results are a success:
a page yielding only a title opens the editor with the title and URL filled in.

Expected failures (PRD §8) — surface a clear message and offer manual entry, never a crash:
paywalls, auth walls, anti-bot protection, broken markup, JS-only rendering.

HTML fetching/parsing uses Jsoup in `data/remote/web/`. Send a normal user agent; respect
redirects; cap response size and set a timeout.

---

## 4. Image import (PRD §9)

- Accepts 1..n images, ordered — a recipe spanning several cookbook pages is one recipe.
- Downscale and JPEG-compress before upload (target long edge ~1600px) to control tokens
  and latency.
- Send all pages in a single request so the model sees the whole recipe at once.
- Handwriting is best-effort; low-confidence output is still shown for correction rather
  than rejected.
- Not expected to work from a photo of the finished dish.

---

## 5. Text import (PRD §10)

Plain paste → `extractFromText`. Simplest path, no network fetch, no images — build it
first when starting the import phase.

---

## 6. Extraction contract

All three extraction methods return the same `DraftRecipe`, with these rules baked into the
prompt and re-enforced in code after parsing:

- **Every ingredient carries `originalText` exactly as it appeared.** If the model returns
  a structured ingredient without it, reconstruct from the raw source or fail that
  ingredient into `originalText` only. Never drop the row.
- Fields the source does not contain are `null`. **Do not invent** times, servings, or
  ingredients.
- Do not normalize, convert, or "improve" units and quantities during extraction.
- Preserve ingredient and step **sections** when the source has them.
- Return the detected `originalLanguage`.

Request structured JSON output and validate it before mapping. A malformed response is a
recoverable error: show the raw text and let the user correct it manually.

---

## 7. Translation (PRD §11)

Operates on the **structured recipe**, not on raw HTML or a whole page.

| Translate | Leave untouched |
|---|---|
| title | quantities |
| ingredient names | units |
| ingredient notes | URLs |
| step text | source name |
| tags, only when meaningful | `originalText` fields |

Targets: Catalan, Spanish, English (ADR-004). Per ADR-005, translation writes in place and
preserves the original in `originalTitle`, `Step.originalText`, and
`Ingredient.originalText`.

Send only the translatable fields as a structured payload, keyed by id, and merge the
response back by id. Do not send the whole recipe object.

---

## 8. Cost, latency, and failure

- Model choice, request shape, streaming, and caching: **read the `claude-api` skill before
  writing this code.** Do not work from memory.
- Long-running imports run in a foreground-safe way with visible progress; the user can
  cancel, and cancelling cancels the HTTP call.
- Retry once on transient network failure; never auto-retry on a 4xx.
- Rate limit and quota errors get a specific, actionable message (it's the user's own key
  and their own bill).
