# Algo Atlas

Data structures and algorithms notes, built to be read on a phone.

Each topic carries an explanation, an **animated visualisation** you can step
through frame by frame, implementations in **Kotlin, Go and JavaScript**, and
the two or three interview questions that topic actually gets asked through.

Built with **Compose Multiplatform** — one Kotlin codebase targeting Android,
iOS, desktop and web. Six topics so far, across four chapters; the intended
curriculum is roughly forty, ordered basic to advanced.

## Running it

```bash
./gradlew :androidApp:installDebug                  # Android (needs a device or emulator)
./gradlew :composeApp:run                           # Desktop
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # Web
```

Requires JDK 17+. The Android SDK is needed only for the Android target, and
Xcode only for iOS — a cold Kotlin/Native build takes upwards of ten minutes,
so `:composeApp:compileKotlinDesktop` is the quick way to check that common
code still compiles.

Android is the surface that gets the most attention, because that is where
these notes actually get read.

## Repository layout

```
composeApp/          the shared application — nearly everything lives here
  src/commonMain/    all the content, logic and UI
  src/androidMain/   two expect/actual implementations, plus transition anims
  src/iosMain/       two expect/actual implementations, plus the UIViewController
  src/desktopMain/   two expect/actual implementations, plus the window entry point
  src/wasmJsMain/    two expect/actual implementations, plus the browser entry point
androidApp/          a thin Android host: one Activity that calls App()
docs/ROADMAP.md      where this goes after the DSA core
.claude/skills/      add-topic, plus the android/skills reference set
```

The Android host module is genuinely thin — an `Activity` that enables
edge-to-edge and calls `App()`. Every platform entry point is like this, so
there is only one place where the app is actually built.

## How it is put together

The short version is below; [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) has
the full account, including the decision record and known gaps. There is a
rendered version at [`docs/architecture.html`](docs/architecture.html).

```
composeApp/src/commonMain/kotlin/dev/mks/algoatlas/
├── model/      Topic, Scene, Frame — everything the UI renders is plain data
├── content/    the notes themselves, one file per topic
├── viz/        frame generators: run the algorithm once, record each step
└── ui/
    ├── home/   two tabs behind a floating bottom bar
    ├── viz/    renderers that play frames back
    ├── code/   syntax highlighting for the three languages
    └── theme/  Material scheme, plus the semantic visualisation palette
```

### Visualisations are data, not animation code

This is the idea the rest of the design follows from. A generator runs the
**real algorithm** and records an immutable frame at each meaningful step; the
UI only plays those frames back.

```kotlin
fun binarySearchScene(values: List<Int> = …, target: Int = 23): Scene {
    val frames = mutableListOf<SeqFrame>()
    // …run the actual search, appending a frame per comparison…
    return Scene.Cells(frames)
}
```

Because a scene is just a `List<Frame>`, scrubbing backwards is exact rather
than a re-simulation, playback speed is a property of the player rather than of
the algorithm, and the frames are ordinary values you could assert on in a test.
Adding a topic means writing a generator, never a renderer.

There are five renderers — sequences, bars, linked chains, graphs and matrices —
which between them cover most of the curriculum. Frames are attached to a topic
as a lambda (`scene = { binarySearchScene() }`) so they are built only for the
topic actually on screen.

### The content is the data model

A `Topic` is a plain data class: prose paragraphs, key points, a complexity
table, a `Map<Lang, String>` of code samples, questions, references, and an
optional scene. Chapters group topics, and `content/Catalog.kt` is the single
registry — everything else derives from it. `AllQuestions` feeds the Practice
tab, so a new topic's questions appear with no extra wiring, and `searchTopics`
backs the search sheet.

Content is compiled Kotlin rather than parsed files. That is pleasant at this
size — type-safe, refactorable, no parsing — and the roadmap plans the move to
bundled Markdown around fifty topics, when compile times and app-release
overhead start to bite.

### No architecture, deliberately

There is no ViewModel, no dependency injection, no repository layer and no
navigation library. State is `remember { mutableStateOf(...) }` hoisted into
`App.kt`, flowing down as parameters and back up as lambdas. Navigation is a
nullable topic id plus `AnimatedContent`, with a 720dp breakpoint that switches
between a two-pane layout and a single-pane stack.

This is not minimalism for its own sake. There is no network, no database, no
async work and no mutable domain state — the entire content set is compile-time
constants. A ViewModel here would be a box with nothing in it. That calculus
changes when bookmarks and progress arrive, since persistence brings suspending
calls and state that outlives composition; until then, adding architecture would
only add indirection.

### Platform differences are two functions

Each platform source set implements exactly two `expect` declarations:

| | Android | iOS | Desktop | Web |
|---|---|---|---|---|
| `rememberUrlOpener` | Custom Tabs | `SFSafariViewController` | system browser | new tab |
| `PlatformBackHandler` | `BackHandler` | not wired yet | no system back | not wired yet |

Android is the only target where back means anything today; the declaration
lives in common code anyway so the navigation logic does not fork per platform.

External links open in the platform's in-app browser rather than an embedded
WebView, styled to match the app — same push and pop animation as the in-app
navigation, the app's surface colour on the toolbar, and a back arrow instead of
a close cross. Embedding was rejected because sites the notes cite most refuse
framing outright, and because it would mean four implementations of something
the platform already does better.

### Colour means something

Three sources, kept separate on purpose:

- `MaterialTheme.colorScheme` — chrome, surfaces, text
- `LocalVizPalette` — algorithm state (`Tone.ACTIVE` is being examined,
  `GOOD` is settled, `BAD` is discarded…), difficulty and level chips
- `LocalCodePalette` — syntax highlighting

The visualisation colours are held outside the Material scheme because they are
semantic rather than decorative, and their meanings are fixed across every topic
so that a reader learns them once.

## Adding a topic

Run the `/add-topic` skill in `.claude/skills/`, or follow it by hand. A topic
is one file in `content/`, optionally a frame generator in `viz/`, and a line in
`content/Catalog.kt`.

## Development

```bash
./gradlew ktlintCheck    # lint all modules and source sets
./gradlew ktlintFormat   # auto-fix
```

Several ktlint rules are switched off in `.editorconfig` where they fought
deliberate layout — scene definitions group related arguments on one line, and
short `if`/`else` expressions stay expressions. Lint is here to catch mistakes,
not to relitigate formatting.

There are no tests yet. The frame generators are pure functions returning data,
so they are the obvious place to start.

## Influence

The writing approach follows [basecs](https://medium.com/basecs) by Vaidehi
Joshi ([full index](https://github.com/vaidehijoshi/basecs-series)) — open on
the problem rather than the definition, explain why something was invented
before what it does, and always say where the name came from. The prose here is
original; the instincts are borrowed, and credited on every topic.

## Where it is going

[`docs/ROADMAP.md`](docs/ROADMAP.md) covers the plan: finish the DSA core, then
widen to systems, AI/ML and agentic coding, with a `Track` level above chapters
and content moved to bundled Markdown before the count grows. The rule that
holds throughout is that no topic ships without a visualisation or a concrete
worked example.
