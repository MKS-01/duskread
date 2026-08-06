---
name: add-topic
description: Add a new DSA topic to Stacks — note content, an animated scene, two-language code, and interview questions. Use when the user asks to add, write, or draft a topic/chapter for the algo notes app (e.g. "add quicksort", "add a topic on tries", "write up Dijkstra").
---

# Adding a topic to Stacks

A topic is plain data. Nothing here touches the UI — the renderers already
handle every scene type, so adding a topic means writing content and a frame
generator, never a composable.

## 1. Write the content file

Create `composeApp/src/commonMain/composeResources/files/topics/<id>.md` — the
file's `<id>` (kebab-case, must be unique) is the topic's `id`. Copy the shape
from `files/topics/hash-tables.md` — it is the most complete example. The
format is a small hand-parsed one (`content/TopicMarkdown.kt`), not full
YAML or Markdown: flat `key: value` front matter, then `##`-headed body
sections, each with its own tiny syntax:

```
---
id: binary-search
title: Binary Search
tagline: Halve the search space on every comparison.
level: basic
scene: binarySearchScene
related: arrays, merge-sort
---

## Note
- Halve the search space on every comparison...
- The input must be **sorted**...
- Iterative form runs in O(log n) time, O(1) space...
- Using (lo+hi)/2 on large ranges can overflow...

## Read More
basecs — computer science fundamentals, explained properly | https://medium.com/basecs | Vaidehi Joshi · Medium

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
The insight that unlocks it, written from memory.

## References
```

Required front matter: `id`, `title`, `tagline`, `level`. Required sections:
`Note`, `Code: Kotlin`, `Code: Go`, `Questions`. Strongly encouraged:
`Read More`, `related` in front matter. `scene:` is a string key resolved
through `content/SceneRegistry.kt` — set once you've written the scene in
step 3, or omit for a topic with no visualisation yet.

**The note is the whole page, not a teaser.** As of 2026-08 this app stopped
trying to be a second place to read the deep explanation — that's what the
`Read More` link is for. `Note` is 4-6 bullets: what the thing is, the one or
two facts worth remembering, the trade-off, maybe a complexity figure inline
if it's genuinely load-bearing. There is no separate Intuition, Origin, Key
Points, Complexity table, Pitfalls, or Steps section anymore — fold anything
essential into the bullets, and if it doesn't fit in a bullet, it belongs in
the linked article, not here.

`## References` links use `label | url | source` (final field optional),
same shape as `Read More`. Don't repeat the two fixed basecs links under
`## References` — the loader appends them to every topic automatically; only
add topic-specific extras there.

## 2. Write the note

- **Open on what matters, not a definition.** "Halves the search space on
  every comparison" beats "Binary search is an algorithm that finds a target
  in a sorted array."
- **Name the trade-off** where there is one, in one bullet.
- **Be precise where interviews are pedantic** — amortised vs worst case,
  average vs guaranteed — if it fits a bullet; otherwise leave it for the
  linked article.
- If you write an origin fact at all (inventor, year), **verify it** — if you
  cannot confirm a claim, leave it out rather than guess. It's fine, and
  often better, to have no origin bullet at all now that there's no dedicated
  Origin section to hold one.
- `markup()` supports `**bold**`, `*italic*` and `` `code` `` in note text.

## 3. Write the scene

Add a generator to `viz/`, returning a `Scene`. As of 2026-08 the default is a
**single static frame** — a labelled wireframe of the concept, not a played-
back animation — `Scene.X(listOf(frame))`, no loop, no per-step recording.
Reserve a multi-frame, step-by-step scene (record a frame at each meaningful
step of the real algorithm, as `BinarySearchScene.kt` or `MergeSortScene.kt`
do) for topics where the *process* is the point — sorting, searching,
traversal — where scrubbing through steps is how the reader actually
understands it. For a concept better shown as one labelled diagram
(architecture, a static distribution, a small fixed structure), one frame is
correct and a multi-step animation would be manufacturing motion nothing
needs. `GradientDescentScene.kt` is the reference for the single-frame shape.

Pick the scene type by what the reader needs to see:

| Type | Use for | Example |
|---|---|---|
| `Scene.Cells` | indices, pointers, windows, ranges | `BinarySearchScene.kt` |
| `Scene.Bars` | relative magnitude — sorting, distributions | `MergeSortScene.kt` |
| `Scene.Chain` | linked nodes with arrows, or a linear pipeline | `linkedListScene` |
| `Scene.Graph` | nodes and edges; set `tree = true` for trees | `BfsScene.kt` |
| `Scene.Matrix` | DP tables, grids, weight matrices | `DpScene.kt` |

Tone meanings are fixed and consistent across every topic — do not repurpose
them: `ACTIVE` being examined now, `INFO` in scope, `GOOD` settled/accepted,
`BAD` discarded/rejected, `WARN` special (pivot, collision), `IDLE` untouched.

Captions carry the teaching. Each one should say *why*, not narrate the
obvious — "16 < 23, so every element at or left of mid is too small" rather
than "comparing index 4". Use `aux` for 1-2 illustrative values (that's the
house convention even for multi-frame scenes — check `viz/` if unsure).

Keep multi-frame scenes under ~50 frames; pick small inputs that still show
the interesting case.

## 4. Questions

Two or three per topic, from LeetCode or commonly asked interviews. Give the
**insight that unlocks it**, never a full solution — and name the trap where
there is one ("merging one at a time degrades to O(Nk)"). Set `askedAt` when
you know it. `url` is derived from `id` automatically, so only set `id` if the
LeetCode slug matches the title.

## 5. Register it

Add the topic's id to a chapter's `topicIds` in the `ChapterManifest` in
`content/Catalog.kt`. Chapters are ordered basic → advanced and topics within
them likewise. Add a new `ChapterSpec` if it does not fit an existing one.

Cross-link with `related` in **both** directions — a new topic should be
reachable from the ones it builds on.

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
and that both code tabs highlight. Long values or many elements are the
usual layout breakers.

Do not build iOS or Wasm unless specifically asked — a cold Kotlin/Native
build takes over ten minutes. `:composeApp:compileKotlinDesktop` is a fast
sanity check that common code is sound.
