---
name: add-topic
description: Add a new DSA topic to Stacks — note content, an animated scene, 3-language code, and interview questions. Use when the user asks to add, write, or draft a topic/chapter for the algo notes app (e.g. "add quicksort", "add a topic on tries", "write up Dijkstra").
---

# Adding a topic to Stacks

A topic is plain data. Nothing here touches the UI — the renderers already
handle every scene type, so adding a topic means writing content and a frame
generator, never a composable.

## 1. Write the content file

Create `composeApp/src/commonMain/kotlin/dev/mks/stacks/content/<Name>.kt`
holding a single `val` of type `Topic`. Copy the shape from
`content/HashTables.kt` — it is the most complete example.

Required: `id` (kebab-case, must be unique), `title`, `tagline`, `level`,
`intuition`, `keyPoints`, `complexity`, `code`, `questions`.
Strongly encouraged: `origin`, `pitfalls`, `steps`, `related`, `references`.

## 2. Write in the basecs style

The model is Vaidehi Joshi's [basecs](https://medium.com/basecs) series
([index](https://github.com/vaidehijoshi/basecs-series)). Take the approach,
never the words — all prose must be original.

What that means concretely:

- **Open on the problem, not the definition.** Start with why someone needed
  this thing. "Arrays are fast to read and slow to modify, and both facts come
  from the same source" beats "An array is a contiguous block of memory."
- **Motivate before mechanising.** The reader should understand why the
  structure exists before seeing how it works.
- **Always write an `origin`.** Who invented it, roughly when, what problem
  they were staring at, and where the name came from. These are the details
  that make a topic stick. **Verify them** — if you cannot confirm a claim,
  leave it out rather than guess. Dates and attributions must be real.
- **Name the trade-off explicitly.** Every structure buys something and pays
  for it somewhere. Say what, in both directions.
- **Be precise where interviews are pedantic.** Amortised vs worst case,
  average vs guaranteed, "O(1) insert" vs "O(1) once you hold the node".
- Write `intuition` as 3–6 paragraphs, each a complete thought. Conversational
  but never padded. No filler sentences, no "let's dive in".
- `markup()` supports `**bold**`, `*italic*` and `` `code` `` in note text.

## 3. Write the scene

Add a generator to `viz/`, returning a `Scene`. Run the real algorithm and
record a frame at each meaningful step — the UI only plays frames back, so
scrubbing is exact and nothing is recomputed.

Pick the scene type by what the reader needs to see:

| Type | Use for | Example |
|---|---|---|
| `Scene.Cells` | indices, pointers, windows, ranges | `BinarySearchScene.kt` |
| `Scene.Bars` | relative magnitude — sorting | `MergeSortScene.kt` |
| `Scene.Chain` | linked nodes with arrows | `linkedListScene` |
| `Scene.Graph` | nodes and edges; set `tree = true` for trees | `BfsScene.kt` |
| `Scene.Matrix` | DP tables, grids | `DpScene.kt` |

Tone meanings are fixed and consistent across every topic — do not repurpose
them: `ACTIVE` being examined now, `INFO` in scope, `GOOD` settled/accepted,
`BAD` discarded/rejected, `WARN` special (pivot, collision), `IDLE` untouched.

Captions carry the teaching. Each one should say *why* this step happens, not
narrate the obvious — "16 < 23, so every element at or left of mid is too
small" rather than "comparing index 4". Use `aux` for running counters
(comparisons, queue contents, load factor).

Keep scenes under ~50 frames; pick small inputs that still show the interesting
case.

## 4. Questions

Two or three per topic, from LeetCode or commonly asked interviews. Give the
**insight that unlocks it**, never a full solution — and name the trap where
there is one ("merging one at a time degrades to O(Nk)"). Set `askedAt` when
you know it. `url` is derived from `id` automatically, so only set `id` if the
LeetCode slug matches the title.

## 5. Register it

Add the topic to a chapter in `content/Catalog.kt`. Chapters are ordered
basic → advanced and topics within them likewise. Add a new `Chapter` if it
does not fit an existing one.

Cross-link with `related` in **both** directions — a new topic should be
reachable from the ones it builds on.

Add `references = Refs.basecs()` (from `content/References.kt`), plus any
topic-specific links as arguments.

## 6. Where it appears in the UI

Home is two tabs behind a floating bottom bar (`ui/home/`), split by **intent**:

- **Learn** — chapters of `TopicCard`s, curriculum order. Long sessions.
- **Practice** — every `Question` across all topics, filterable by difficulty.
  Fed automatically by `AllQuestions`, so a new topic's questions appear with
  no extra wiring.

Search lives in the bar and expands upward from it, so nothing needs reaching
for at the top of the screen. There is no top app bar anywhere — titles scroll
away with content.

Keep it at two tabs. New features belong as **filters or modes inside them**,
not new destinations: bookmarks as a filter chip in both, companies as a filter
row in Practice, progress as a strip atop Learn. Only a genuinely different
session shape (e.g. scheduled spaced-repetition review) would justify a third.

## 7. Verify

```bash
./gradlew :androidApp:installDebug        # ~5s warm; the normal loop
```

Then check it on a device, because compiling proves nothing about layout:

```bash
adb shell am force-stop dev.mks.stacks
adb shell am start -n dev.mks.stacks/dev.mks.stacks.android.MainActivity
adb exec-out screencap -p > /tmp/check.png
```

Confirm the scene renders, plays, and scrubs; that captions are not clipped;
and that all three code tabs highlight. Long values or many elements are the
usual layout breakers.

Do not build iOS or Wasm unless specifically asked — a cold Kotlin/Native
build takes over ten minutes. `:composeApp:compileKotlinDesktop` is a fast
sanity check that common code is sound.
