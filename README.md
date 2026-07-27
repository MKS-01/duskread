# Algo Atlas

Data structures and algorithms notes, built to be read on a phone.

Each topic carries an explanation, an **animated visualisation** you can step
through frame by frame, implementations in **Kotlin, Go and JavaScript**, and
the two or three interview questions that topic actually gets asked through.

Built with **Compose Multiplatform** — one Kotlin codebase targeting Android,
iOS, desktop and web.

## Running it

```bash
./gradlew :androidApp:installDebug   # Android (needs a device or emulator)
./gradlew :composeApp:run            # Desktop
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # Web
```

Requires JDK 17+. The Android SDK is needed only for the Android target.

## How it is put together

```
composeApp/src/commonMain/kotlin/dev/mks/algoatlas/
├── model/      Topic, Scene, Frame — everything the UI renders is plain data
├── content/    the notes themselves, one file per topic
├── viz/        frame generators: run the algorithm once, record each step
└── ui/
    ├── home/   two tabs behind a floating bottom bar
    ├── viz/    renderers that play frames back
    └── code/   syntax highlighting for the three languages
```

The important idea is that **visualisations are data, not animation code**. A
generator runs the real algorithm and records an immutable frame at each
meaningful step; the UI only plays those frames back. Scrubbing backwards is
therefore exact rather than a re-simulation, and adding a topic means writing a
generator, never a renderer.

There are five renderers — sequences, bars, linked chains, graphs and matrices
— which between them cover most of the curriculum.

## Adding a topic

Run the `/add-topic` skill in `.claude/skills/`, or follow it by hand. A topic
is one file in `content/`, optionally a frame generator in `viz/`, and a line
in `content/Catalog.kt`.

## Influence

The writing approach follows [basecs](https://medium.com/basecs) by Vaidehi
Joshi ([full index](https://github.com/vaidehijoshi/basecs-series)) — open on
the problem rather than the definition, explain why something was invented
before what it does, and always say where the name came from. The prose here is
original; the instincts are borrowed, and credited on every topic.
