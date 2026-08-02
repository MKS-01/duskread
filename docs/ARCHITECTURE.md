# Architecture

How Stacks is built, and why. Written 2026-07-28, against six topics and
four chapters.

A rendered version of this document lives at [`architecture.html`](architecture.html).

---

## The shape of it

One Kotlin codebase, four targets, and a deliberately small amount of
machinery:

```
androidApp/          Activity → enableEdgeToEdge() → App()
composeApp/
  commonMain/        everything: content, logic, UI
  androidMain/       3 actuals + transition animations
  iosMain/           3 actuals + UIViewController
  desktopMain/       3 actuals + window entry point
  wasmJsMain/        3 actuals + browser entry point
```

Each platform module is an entry point and three `actual` implementations.
Nothing else forks per platform, which is the whole reason for choosing Compose
Multiplatform.

## Layers

```
┌──────────────────────────────────────────────┐
│ ui/                                          │
│   home/  Learn and Practice tabs, search     │
│   viz/   renderers: play frames back         │
│   code/  syntax highlighting, 3 languages    │
│   theme/ Material scheme + semantic palette  │
└──────────────────────────────────────────────┘
                    ↑ reads
┌──────────────────────────────────────────────┐
│ viz/    frame generators                     │
│         run the real algorithm, record steps │
└──────────────────────────────────────────────┘
                    ↑ produces Scene
┌──────────────────────────────────────────────┐
│ content/  the notes; Catalog.kt is the       │
│           single registry                    │
└──────────────────────────────────────────────┘
                    ↑ instances of
┌──────────────────────────────────────────────┐
│ model/    Topic, Scene, Frame — plain data,  │
│           no behaviour                       │
└──────────────────────────────────────────────┘
```

Dependencies point in one direction only. `model/` knows about nothing;
`ui/` knows about everything. There is no layer that both reads and writes
application state, because there is no mutable application state.

## The central decision: visualisations are data

A generator runs the **real algorithm** and records an immutable frame at each
meaningful step. The UI never runs an algorithm — it plays frames back.

```kotlin
fun binarySearchScene(values: List<Int> = …, target: Int = 23): Scene {
    val frames = mutableListOf<SeqFrame>()
    var lo = 0; var hi = values.lastIndex
    while (lo <= hi) {
        val mid = lo + (hi - lo) / 2
        frames += SeqFrame(values, caption = "…", marks = …, span = …)
        // …narrow the range, record another frame…
    }
    return Scene.Cells(frames)
}
```

What this buys, in order of importance:

1. **Scrubbing backwards is exact.** Frame 7 is the same object however you
   arrived at it. A re-simulating player would have to replay from zero, and
   any non-determinism would show.
2. **Adding a topic never touches the UI.** Five renderers already cover
   sequences, bars, chains, graphs and matrices. A new topic is content plus a
   generator.
3. **The teaching lives with the algorithm.** Captions are written at the point
   the step happens, next to the code that performs it.
4. **Frames are testable values.** A generator is a pure function returning a
   list — assertable without a device or a composition.

The cost is memory: every frame holds a full snapshot. At the scale that
matters here — under fifty frames of a dozen elements — that is nothing, and
scenes are built lazily (`scene = { binarySearchScene() }`) so only the topic on
screen has any.

## State and navigation

There is no ViewModel, no dependency injection, no repository and no navigation
library.

- State is `remember { mutableStateOf(...) }` hoisted into `App.kt`
- Navigation is a nullable topic id plus `AnimatedContent`
- A 720dp breakpoint switches between two-pane and single-pane; `selectedId`
  and `lang` are hoisted above `BoxWithConstraints`, so crossing the breakpoint
  preserves them
- `PlatformBackHandler` intercepts the Android back gesture from common code

**Why this is a decision and not an omission.** There is no network, no
database, no async work and no mutable domain state — the whole content set is
compile-time constants. A repository would wrap a `val`; a ViewModel would hold
a `String?`. Measured against Google's four architectural principles, the app
already satisfies separation of concerns, drive-UI-from-data-models, single
source of truth (`Catalog.kt`) and unidirectional data flow. What it omits is
the machinery, not the principles.

**That has now partly changed.** Onboarding introduced the first state that is
genuinely mutable and outlives a composition — a name and an "intro seen"
flag — so it also introduced the first thing that needed an owner:
`data/UserPrefs.kt`. It is a plain class, not a ViewModel: reads come from an
in-memory snapshot taken at construction, writes go straight through to the
store, and there is no async work, no lifecycle to survive beyond the process
and nothing to inject. Bookmarks, progress and spaced repetition all belong on
this same holder.

A repository interface still has nothing to abstract; it earns its place when
the Markdown content move gives it a second source. Hilt never will, being
Android-only.

### Persistence: a key-value store, not a database

Everything this app must remember is a few kilobytes — a name, a flag, later a
set of read topic ids and a due date per question. At 200 topics that is still
well under 50 KB, which fits in memory many times over. There is nothing to
query and nothing to join, so a database would be machinery without a job.

`data/KeyValueStore.kt` is one `expect` with four small `actual`s:
`SharedPreferences`, `NSUserDefaults`, a properties file, and `localStorage`.

Reads are **synchronous on purpose**. An async load means a frame where the app
does not yet know whether to show the intro, so a returning reader would see it
flash. At this size, synchronous is both simpler and better behaved.

Room was ruled out for a second reason beyond size: it publishes no Wasm
artifact, so adopting it would quietly drop the web target. If per-review
history ever lands — which does grow unbounded — SQLDelight is the one that
covers every target this project builds for.

## Content as code

A `Topic` is a data class; a topic file is one `val`. `content/Catalog.kt` is
the single registry, and everything derives from it — `AllTopics`,
`AllQuestions` (which feeds the Practice tab with no extra wiring), `topicById`,
`searchTopics`.

Compiled Kotlin is right at this size: type-safe, refactorable, no parser, no
schema drift. It stops being right around fifty topics, when every prose typo
becomes an app release and compile time grows with paragraphs. The roadmap
plans bundled Markdown with YAML front matter at that point, keeping the same
`Topic` model and leaving generators in Kotlin, where they belong.

## The platform layer

Three `expect` declarations, and nothing else:

| | Android | iOS | Desktop | Web |
|---|---|---|---|---|
| `rememberUrlOpener` | Custom Tabs | `SFSafariViewController` | system browser | new tab |
| `PlatformBackHandler` | `BackHandler` | not wired yet | no system back | not wired yet |
| `rememberKeyValueStore` | `SharedPreferences` | `NSUserDefaults` | properties file | `localStorage` |

`rememberUrlOpener` is a composable because Android needs the local `Context`,
and because both mobile implementations tint their chrome from the current
Material colours.

An embedded WebView was rejected: the sites the notes cite most refuse framing
via `X-Frame-Options`, logged-in content would lose its session, and it would
mean four implementations — one heavyweight (JCEF, ~100 MB) and one impossible
(Wasm cannot iframe most origins). The platform's in-app browser is purpose-built
for this and keeps the browser's cookies, password manager and share sheet.

## Colour carries meaning

Three sources, kept separate deliberately:

- `MaterialTheme.colorScheme` — chrome, surfaces, text
- `LocalVizPalette` — algorithm state, difficulty and level chips
- `LocalCodePalette` — syntax highlighting

`VizPalette` sits outside the Material scheme because its colours are
**semantic, not decorative**. `Tone.ACTIVE` means "being examined now" in every
topic; `GOOD` means settled, `BAD` discarded, `WARN` special (a pivot, a
collision), `INFO` in scope, `IDLE` untouched. A reader learns them once on
binary search and they still hold on Dijkstra. Mapping them onto Material roles
would let a redesign of the surface palette silently change what a colour means.

## Chrome: one bar, one search screen

Home carries a single floating pill at the bottom holding both tabs and search.
It is **icons only** — with two destinations there is nothing to disambiguate,
and the labels were spending width on the one axis a phone cannot spare. The
active tab is a filled disc behind the icon.

The pill **blurs what scrolls beneath it** rather than sitting on an opaque
slab, via [`haze`](https://github.com/chrisbanes/haze). That is the project's
only dependency added for appearance, and it is here because no Compose API
does backdrop blur across all four targets — `Modifier.blur` blurs a
composable's own content, not what is behind it. The scrim gradient that used
to fade content under the bar is gone: hiding that content would defeat the
effect.

Search is a **screen**, not a sheet over the list. The field floats at the
bottom, where the thumb already is, and the keyboard pushes it up rather than
covering it. There is no separate back button beside it — an unenclosed icon
next to an enclosed field reads as two unrelated controls, so the cross inside
the pill clears the query, or closes the screen when the query is empty.

Its empty state is a slow carousel of algorithm families, each **drawn
running**: bars settling into order, a search range halving, a graph lighting
up in rings. Tapping one searches it, so the animation is the suggestion rather
than decoration around it. Each illustration is a pure function of one looping
`0..1` value — the frame-generator idea again, continuous instead of stepped.

A query with no matches never dead-ends: it offers the web and Wikipedia first
(through the same Custom Tab as references), then topics we do have.

## Icons

`ui/theme/AtlasIcons.kt` holds the icon set as vector paths rather than pulling
from Material. Two reasons: they are **stroked** at a consistent weight where
Material's are filled, which sits better against the thin type and hairline
borders used everywhere else; and they can mean something specific — the Learn
icon is the stepped motif from the launcher mark, so the app's one visual idea
appears in three places instead of none.

Practice is a target rather than a lightning bolt: its questions are aimed at
one specific insight, which is not what "fast" says.

## Known gaps

- **Session state is still not saved.** The name and the intro flag now persist,
  but everything else is `remember` rather than `rememberSaveable`, so process
  death still loses the selected topic, tab, language and theme override. The
  store to fix it now exists; the call sites have not been changed.
- **No tests.** `commonTest` is wired but empty. The frame generators are pure
  functions and are the obvious place to start.
- **Search results list top-down**, away from the field at the bottom, so the
  best match sits furthest from where you are typing. Bottom-anchoring them is
  what would make the bottom-mounted field strictly better rather than a
  trade-off.
- **The carousel centres in the full column height**, so it drifts low without
  the keyboard and shifts when the keyboard opens.
- **No CI.** Nothing checks the targets that are not built locally.

## Decision record

| Decision | Rationale | Revisit when |
|---|---|---|
| Compose Multiplatform, not a web app | Read on an Android phone; Kotlin end to end means it gets maintained | — |
| Visualisations as recorded frames | Exact scrubbing; adding topics never touches the UI | — |
| No ViewModel, DI or nav library | No async, no I/O, no mutable domain state | Bookmarks and progress land |
| Content as compiled Kotlin | Type-safe and refactorable at this size | ~50 topics |
| Custom Tabs over embedded WebView | Sites refuse framing; four implementations otherwise | Offline article capture |
| Semantic viz palette outside Material | Colour meanings must survive a redesign | — |
| Hand-rolled syntax highlighter | No `shiki` equivalent in Kotlin | A real Kotlin grammar library appears |
| Backdrop blur via `haze` | No Compose API blurs what is *behind* a composable on all four targets | A first-party backdrop API lands |
| Hand-drawn icon set | Stroked weight matches the type; the Learn icon carries the app's mark | — |
| Search as a screen, field at the bottom | Consistent with having no top chrome anywhere; thumb reach | The two-pane layout, where a mouse changes the argument |

## Where it goes next

[`ROADMAP.md`](ROADMAP.md) has the plan: finish the DSA core, add a `Track`
level above chapters, move content to Markdown, then widen to systems, AI/ML and
agentic coding. The rule that holds throughout is that no topic ships without a
visualisation or a concrete worked example.
