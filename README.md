<h1 align="center">Blogmark</h1>

<p align="center">
  <strong>Save a link. Follow a blog. Hear it read back. Stay focused.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose_Multiplatform-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose Multiplatform">
  <img src="https://img.shields.io/badge/Android_·_iOS_·_Desktop_·_Web-000000?style=flat-square" alt="Android, iOS, Desktop, Web">
  <img src="https://img.shields.io/badge/MIT-22c55e?style=flat-square" alt="MIT License">
</p>

<p align="center">
  <img src="docs/media/home-dashboard.png" alt="Home dashboard: focus timer, a pick from Saved, today's Readback, followed feeds" width="220">
  <img src="docs/media/saved-tab.png" alt="Saved tab: paste-a-link box and the reading list" width="220">
  <img src="docs/media/readback-tab.png" alt="Readback tab: past reads with summaries and audio" width="220">
</p>

---

- **Save** — paste a link, or share one in from a browser. Usable instantly,
  fills in its real title a second later.
- **Follow** — point it at a blog's RSS/Atom feed; new posts show up on Home,
  one tap from becoming a saved link.
- **Readback** — a synced [readback](https://github.com/MKS-01/readback)
  library, playable from the floating bar without leaving your tab.
- **Focus** — a Pomodoro timer with a full-screen big-clock mode.

Everything lives in the lower third of the screen — phone-first, one thumb.

## Running it

```bash
./gradlew :androidApp:installDebug                  # Android
./gradlew :composeApp:run                           # Desktop
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # Web
```

iOS needs an Xcode host, generated rather than committed:

```bash
cd iosApp && xcodegen generate && cd ..
open iosApp/iosApp.xcodeproj
```

Requires JDK 17+.

## Stack

One Kotlin codebase, four targets — **Compose Multiplatform**. No
ViewModel, no DI, no database: state is hoisted into `App.kt`, saved links
and feeds live in a flat key/value store. Readback integration is
read-only — this app never writes to `library.db`, only reads what a
separate sync step puts on the device.

```bash
./gradlew ktlintCheck    # lint
./gradlew ktlintFormat   # auto-fix
```

No tests yet.
