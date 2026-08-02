# Stacks

DSA notes built to be read on a phone, alongside a focus timer and a reader
for whatever you're studying from elsewhere — one app for the whole study
session, not just the notes part of it.

Each topic carries an explanation, an **animated visualisation** you can step
through frame by frame, implementations in **Kotlin, Go and JavaScript**, and
the two or three interview questions that topic actually gets asked through.
A Pomodoro timer runs alongside it, and the Reader tab picks up a synced
[readback](https://github.com/MKS-01/readback) audio library where you left
off.

Built with **Compose Multiplatform** — one Kotlin codebase targeting Android,
iOS, desktop and web. Nine topics so far, across five chapters; the intended
curriculum is roughly forty, ordered basic to advanced.

## Running it

```bash
./gradlew :androidApp:installDebug                  # Android (needs a device or emulator)
./gradlew :composeApp:run                           # Desktop
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # Web
```

iOS has no Gradle one-liner because Compose Multiplatform only produces a
framework for that target — it needs an Xcode host app to embed it in. That
host lives in `iosApp/`, generated from `iosApp/project.yml` with
[XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`)
rather than committed, so it never drifts and never needs manual pbxproj
merges:

```bash
cd iosApp && xcodegen generate && cd ..
open iosApp/iosApp.xcodeproj   # then Run in Xcode, or:
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

The Xcode build has a pre-build script that runs
`:composeApp:embedAndSignAppleFrameworkForXcode`, so Gradle still owns the
actual Kotlin/Native compile — Xcode just triggers it and links the result.

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
iosApp/              a thin iOS host: SwiftUI wrapping MainViewController();
                     iosApp.xcodeproj is generated, not committed
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
composeApp/src/commonMain/kotlin/dev/mks/stacks/
├── model/      Topic, Scene, Frame — everything the UI renders is plain data
├── content/    the notes themselves, one file per topic
├── viz/        frame generators: run the algorithm once, record each step
└── ui/
    ├── home/   Home, Library and Reader behind a floating bottom bar, search growing out of it
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
registry — everything else derives from it. `AllQuestions` feeds the Library
tab's questions filter, so a new topic's questions appear with no extra
wiring, and `searchTopics` backs the search screen.

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

This is not minimalism for its own sake. The content itself has no network, no
database and no mutable domain state — the entire topic set is compile-time
constants, and a ViewModel over it would be a box with nothing in it. The
Trending card is the one place with real async work (a network fetch, a
cache), and it still just holds its own `remember { mutableStateOf(...) }`
rather than reaching for one — one `LaunchedEffect` doesn't earn a layer. That
calculus changes when bookmarks and progress arrive, since persistence brings
suspending calls and state that outlives composition across the whole app;
until then, adding architecture would only add indirection.

### Platform differences are three functions

Each platform source set implements exactly two `expect` declarations:

| | Android | iOS | Desktop | Web |
|---|---|---|---|---|
| `rememberUrlOpener` | Custom Tabs | `SFSafariViewController` | system browser | new tab |
| `PlatformBackHandler` | `BackHandler` | not wired yet | no system back | not wired yet |
| `rememberKeyValueStore` | `SharedPreferences` | `NSUserDefaults` | properties file | `localStorage` |

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

Each dependency earns its place: `activity-compose` for the back handler,
`androidx.browser` for Custom Tabs, `core-splashscreen` for the launch window,
and `haze` for the backdrop blur behind the floating bar — no Compose API
blurs what sits *behind* a composable across all four targets. The dashboard's
Trending card is the one exception to "no network required": it needed an
HTTP client and an image loader multiplatform Compose doesn't ship, so it
pulls in Ktor (per-platform engine: OkHttp, Darwin, or the JS engine) and
Coil 3, both chosen because they already support all four targets rather than
needing an `expect`/`actual` client of our own.

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
