<h1 align="center">DuskRead</h1>

<p align="center">
  <strong>One reading list. One voice reading it back. One timer keeping you on it —
  on your phone, your desktop, or in a browser tab.</strong>
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

- **One codebase, every screen you actually reach for.** Android, iOS,
  desktop and web, all from a single Compose Multiplatform tree — built
  phone-first (everything sits in the lower third of the screen, one
  thumb), not phone-only.
- **Never lose a link again.** Paste one in or share it from a browser and
  it's usable instantly; the real title fills in a second later on its own.
- **Read with your ears, hands-free.** A synced
  [readback](https://github.com/MKS-01/readback) library plays right from
  the floating bar, so listening never means leaving what you're doing.
- **A timer that actually interrupts you.** Fifteen, twenty-five, thirty,
  or a length you type in yourself — and when it's done, it says so: a
  vibration and a real notification, not a number quietly hitting zero
  somewhere you weren't looking.
- **Colour when you want it, dusk when you don't.** One tap drains the
  whole app to greyscale for the reads that don't need the terracotta —
  and the launcher icon and splash follow, not just the screen.

Home is one screen on purpose: today's pick from Saved, today's Readback,
the timer, and what's new from the blogs you follow — nothing to dig for.

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
