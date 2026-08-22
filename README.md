<h1 align="center">Blogmark</h1>

<p align="center">
  <strong>One phone-first app for a reading session.</strong><br>
  Save a link, follow a blog's feed, and pick up a synced
  <a href="https://github.com/MKS-01/readback">readback</a> audio library —
  all under a Pomodoro timer that keeps the sitting distraction-free.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose_Multiplatform-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose Multiplatform">
  <img src="https://img.shields.io/badge/Android_·_iOS_·_Desktop_·_Web-000000?style=flat-square" alt="Android, iOS, Desktop, Web">
  <img src="https://img.shields.io/badge/MIT-22c55e?style=flat-square" alt="MIT License">
</p>

---

**Three things, one thumb-reach.** Home opens on a dashboard, not a list:
start a focus session, pick up the next unread saved link, catch today's
Readback pick, and see what the blogs you follow just published — everything
reachable in the lower third of the screen, because the top of a 6-inch
display is a stretch mid-session.

- **Saved** — paste a link or share one in from a browser; it's usable the
  instant it lands and fills in its real title and description once the page
  answers. Export the whole list as Markdown to get it back out, no lock-in.
- **Following** — point it at a blog's RSS/Atom feed and its new posts show
  up as unsaved picks on Home, one tap from becoming a saved link.
- **Readback** — a read-only window onto a synced
  [readback](https://github.com/MKS-01/readback) library: past reads, their
  summaries, and their audio, playable right from the floating bar without
  leaving whatever tab you're on.
- **Focus** — a Pomodoro timer with its own full-screen big-clock mode, for
  when the point is to actually stare at the countdown.

<p align="center">
  <img src="docs/media/home-dashboard.png" alt="Blogmark's Home dashboard: a focus timer, a pick from Saved, today's Readback, and followed feeds" width="220">
  <img src="docs/media/saved-tab.png" alt="Saved tab: paste-a-link box, clipboard suggestion, and the reading list" width="220">
  <img src="docs/media/readback-tab.png" alt="Readback tab: past reads with summaries, word counts and durations" width="220">
</p>

Built with **Compose Multiplatform** — one Kotlin codebase targeting Android,
iOS, desktop and web, with `androidMain`/`iosMain`/`desktopMain`/`wasmJsMain`
holding only the handful of platform actuals (audio playback, the HTTP
client, persistence) that can't live in `commonMain`.

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

No ViewModel, no DI, no navigation library: state is plain
`remember { mutableStateOf(...) }` hoisted into `App.kt`, and there's no
mutable domain state yet to justify the indirection. Saved links and feeds
are packed into a `KeyValueStore` blob rather than a database — a reading
list, not an archive, so a full rewrite on every change is cheaper than any
incremental scheme would be to maintain.

Readback integration is strictly read-only: this app never writes to
readback's `library.db`, only reads whatever a separate, user-run sync step
puts on the device.

## Development

```bash
./gradlew ktlintCheck    # lint all modules and source sets
./gradlew ktlintFormat   # auto-fix
```

No tests yet — most of the logic here (parsing a feed, packing a link
record, recognising a pasted URL) is pure functions over strings, the
obvious place to start.
