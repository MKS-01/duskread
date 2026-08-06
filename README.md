# Stacks

Algorithms explained properly, read on a phone — a reader for whatever
you're studying elsewhere sits alongside the notes, so it's one app for the
whole study session.

Not just classic DSA: sorting, searching and graph algorithms are the
current core, and cryptographic and ML/AI algorithms are on the way in —
same treatment throughout, one concept, one page, read in ten minutes.

Each topic carries an explanation that opens on the problem rather than the
definition, an **animated visualisation** you can step through frame by
frame, and implementations in **Kotlin and Go**. The Reader tab picks up a
synced [readback](https://github.com/MKS-01/readback) audio library where you
left off.

Built with **Compose Multiplatform** — one Kotlin codebase targeting Android,
iOS, desktop and web. Thirty topics so far.

## Running it

```bash
./gradlew :androidApp:installDebug                  # Android (device or emulator)
./gradlew :composeApp:run                           # Desktop
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # Web
```

iOS needs an Xcode host, generated rather than committed:

```bash
cd iosApp && xcodegen generate && cd ..
open iosApp/iosApp.xcodeproj
```

Requires JDK 17+. `:composeApp:compileKotlinDesktop` is the fast way to check
common code compiles without a full platform build.

## How it's put together

A generator runs the **real algorithm** and records an immutable frame at
each meaningful step; the UI only ever plays frames back. Scrubbing is exact,
not a re-simulation, and adding a topic means writing a generator, never a
renderer. Full account in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

A `Topic` is a plain data class — prose, key points, a complexity table, code
samples, questions, an optional scene — parsed from bundled Markdown files in
`composeApp/src/commonMain/composeResources/files/topics/`, grouped into
chapters by `content/Catalog.kt`. No ViewModel, no DI, no navigation library:
state is hoisted into `App.kt`, and there's no mutable domain state to
justify the indirection yet.

## Adding a topic

Run the `/add-topic` skill in `.claude/skills/`, or follow it by hand. A
topic is one Markdown file in `composeApp/src/commonMain/composeResources/files/topics/`,
optionally a frame generator in `viz/`, and a line in `content/Catalog.kt`.

## Development

```bash
./gradlew ktlintCheck    # lint all modules and source sets
./gradlew ktlintFormat   # auto-fix
```

No tests yet — the frame generators are pure functions returning data, the
obvious place to start.

## Influence

The writing approach follows [basecs](https://medium.com/basecs) by Vaidehi
Joshi — open on the problem, explain why the thing was invented before what
it does, always say where the name came from. Prose is original; credited on
every topic.

## Where it's going

[`docs/ROADMAP.md`](docs/ROADMAP.md) has the plan. One rule holds throughout:
no topic ships without a visualisation or a concrete worked example.
