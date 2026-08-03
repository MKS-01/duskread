# Move content off compiled Kotlin, drop JavaScript as a reference language

Plan for the Phase 2 content-architecture item in [`ROADMAP.md`](ROADMAP.md) —
written 2026-08, not yet started. Pick this up as a standalone session.

## Context

`content/Topics.kt` holds every `Topic` as a compiled Kotlin object literal
and has grown to ~6000 lines for 30 topics (~10KB/topic, mostly the three code
samples). `docs/ROADMAP.md` already names this as a Phase 2 blocker — every
content edit needs a full app rebuild, compile time grows with prose, and
non-code contributions are awkward. The user wants to address this now,
before adding more topics, and — while touching every topic's content anyway
— wants to also drop JavaScript as one of the three reference languages,
keeping only Kotlin and Go.

Outcome: topic content (prose, code, questions, etc.) lives in bundled
Markdown files with a small YAML-like front matter, parsed at startup;
frame generators stay in Kotlin (per the roadmap); every topic carries
Kotlin + Go code only, not JavaScript.

Confirmed via research (three parallel Explore passes) — key facts this plan
relies on:

- `Catalog.kt`'s `Chapters`/`AllTopics`/`TopicsById`/`AllQuestions`/
  `searchTopics` are all ordinary runtime collection operations
  (`flatMap`, `associateBy`, `filter`) — **none of it depends on `Topic`
  being a compile-time constant.** Only requirement: these need to become
  populated from a loaded value rather than eagerly-initialized top-level
  `val`s.
- `Topic.scene` is a direct `() -> Scene` Kotlin lambda today (e.g.
  `scene = { binarySearchScene() }`), for 9 of the 30 topics. Markdown can't
  hold a function reference, so front matter needs a string key
  (`scene: binarySearchScene`) resolved through a small `Map<String, () -> Scene>`
  registry built once in Kotlin.
- No YAML, Markdown, or `kotlinx.serialization` dependency exists anywhere in
  the repo today. No `compose.resources` (`composeResources/`, `Res.readBytes`)
  set up either, though the Compose Multiplatform plugin (1.11.0, already
  applied) supports it — it's just unused.
- Dropping JS is clean: `CodeBlock.kt` already builds its tabs generically
  from whatever `Lang` keys exist in `Topic.code` — no JS-specific branch
  there. `Highlighter.kt`'s JS-specific code is ~10 isolated lines (a
  `JsKeywords` set and one `when` branch), not interleaved with the shared
  tokenizer. `App.kt`'s default selected language is already Kotlin. All 30
  topics currently have a `Lang.JAVASCRIPT to """...""""` entry to drop.

## Format

A deliberately small, hand-parseable micro-format — not full YAML, not full
Markdown — since we fully control every file that will ever be written in it
(by hand or by the `/add-topic` skill). Front matter is flat scalars/lists
only (no nesting); everything structurally richer (paragraphs, tables,
code, repeated question blocks) is a `##`-headed body section with its own
simple per-section syntax:

```
---
id: binary-search
title: Binary Search
tagline: Halve the search space on every comparison.
level: basic
scene: binarySearchScene
related: arrays, merge-sort
---

## Quick Summary
- Halve the search space on every comparison — O(log n) instead of O(n)...

## Read More
Basecs — computer science fundamentals, explained properly | https://medium.com/basecs

## Intuition
Paragraph one, using **bold**, *italic*, `code` — same inline markup() already renders.

Paragraph two.

## Origin
Origin story prose.

## Key Points
- The input must be **sorted**...

## Complexity
Search | O(log n) | O(1) | Iterative form.

## Pitfalls
- Using (lo+hi)/2 on large ranges...

## Steps
1. Set lo = 0 and hi = n - 1...

## Code: Kotlin
```kotlin
fun binarySearch(...) { ... }
```

## Code: Go
```go
func BinarySearch(...) { ... }
```

## Questions
### Binary Search
id: 704
difficulty: easy
askedAt: Warm-up at almost every company
The template itself. Worth writing from memory...

## References
```

`References` only needs topic-specific extra links — the two fixed basecs
links (`Refs.basecs()` today) get appended automatically by the loader after
parsing, so they aren't repeated in 30 files. `Complexity` and `References`
reuse the same `label | value | value | note` pipe-delimited row scheme.
`Questions` is repeated `### Title` blocks with `key: value` lines followed by
free-text `idea`.

## Implementation

**1. Parser** — new `content/TopicMarkdown.kt` (commonMain): a hand-rolled
`fun parseTopic(raw: String): Topic`. No external dependency (no YAML/Markdown
lib, no kotlinx.serialization) — the format is small and fully ours, so a
purpose-built parser is less total complexity than configuring and trusting a
generic multiplatform library for a shape we're not using generically anyway.

**2. Scene registry** — new `content/SceneRegistry.kt`:
```kotlin
val SceneRegistry: Map<String, () -> Scene> = mapOf(
    "arrayScene" to { arrayScene() },
    "binarySearchScene" to { binarySearchScene() },
    // ...the other 7 currently-wired scenes
)
```
Front matter's `scene:` key resolves through this; missing/unmatched key ⇒
`null` scene, same as today for the 21 topics that don't have one yet.

**3. Bundled files** — `composeApp/src/commonMain/composeResources/files/topics/<id>.md`,
one per topic, read via the generated `Res.readBytes(...)` API (works across
Android/iOS/Desktop/wasmJs identically). Confirm exact Gradle wiring when
implementing — the plugin is already applied, but the `composeResources`
source set doesn't exist yet in this repo.

**4. Catalog rewrite** — `content/Catalog.kt`: keep chapter *structure*
(which topic ids belong to which chapter, in what order) as a small explicit
Kotlin manifest — that's navigation metadata that rarely changes and benefits
from staying compile-checked, unlike per-topic prose:
```kotlin
private val ChapterManifest = listOf(
    ChapterSpec("foundations", "Foundations", "...", listOf("arrays", "linked-lists", ...)),
    // ...
)
suspend fun loadCatalog(): List<Chapter> { /* Res.readBytes each topic file, parseTopic, group by manifest */ }
```
`AllTopics`, `TopicsById`, `AllQuestions`, `searchTopics` stay as they are
today, just fed from the loaded `List<Chapter>` instead of a literal one.

**5. Startup wiring** — `App.kt`, following this app's existing convention
(state hoisted into `App.kt`, no ViewModel): load once via `LaunchedEffect`,
show the existing UI once loaded.
```kotlin
var catalog by remember { mutableStateOf<List<Chapter>?>(null) }
LaunchedEffect(Unit) { catalog = loadCatalog() }
if (catalog == null) { /* small loading state */ } else { /* existing app, catalog threaded down */ }
```
Parsing ~30 small files once at startup is the same amount of data already
held in memory today (compiled `Topic` constants) — no eager per-topic lazy
loading is being introduced now. **Deferred, not built**: if the topic count
grows enough that startup parse time becomes measurable (the ROADMAP's own
"fifty topics" marker is the natural point to revisit), split into an eager
lightweight index (id/title/tagline/level/questions — enough for Library and
Search) plus a lazy per-topic parse on open. Not worth the complexity at 30-40
topics.

**6. Drop JavaScript**, done alongside the migration (each topic naturally
loses its JS block as it's converted, rather than a separate pass):
   - `model/Model.kt`: remove `JAVASCRIPT` from the `Lang` enum.
   - `ui/code/Highlighter.kt`: remove the `JsKeywords` set and its `when`
     branch; reword the "Kotlin, Go and JavaScript" doc comment.
   - `CodeBlock.kt`: no change needed (already generic over `Lang.entries`).
   - `README.md`, `CLAUDE.md`, `.claude/skills/add-topic/SKILL.md`: update
     "Kotlin/Go/JavaScript" → "Kotlin and Go" wording, "3-language" → "two-
     language", "all three code tabs" → "both code tabs".

**7. Rewrite the `/add-topic` skill's step 1** to describe writing the new
Markdown format directly (file path, front matter, section syntax) instead of
"create a `.kt` file holding a `Topic` val" — this is what future topic
authoring (by hand or by Claude) actually produces going forward.

**8. Migrate content**: prove the format on one topic first (Binary Search —
it exercises `scene`, `quickSummary`, `readMore`, `origin`, everything),
verify it renders identically in the app, then convert the remaining 29
mechanically. Delete `content/Topics.kt` only once all 30 are confirmed
migrated and rendering correctly.

## Verification

- Add a small test for the parser itself (`content/TopicMarkdownTest.kt` —
  this would be the first test in the project; CLAUDE.md already flags frame
  generators, and this parser, as "the obvious place to start" since both are
  pure functions over data). Round-trip the migrated Binary Search file and
  assert every field matches what the old compiled `Topic` held.
- `./gradlew :composeApp:compileKotlinDesktop` after each structural step.
- `./gradlew :androidApp:installDebug`, then open Binary Search (proof topic)
  and a handful of others post-bulk-migration on-device — confirm prose,
  complexity table, both code tabs (no JS tab), questions, and the scene (for
  topics that have one) all render exactly as before.
- `./gradlew ktlintCheck` before committing.
- Confirm app cold-start isn't visibly slower with the loading step in place
  (should be imperceptible at 30 topics, but worth a glance).
