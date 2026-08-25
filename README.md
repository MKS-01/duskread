<h1 align="center">DuskRead</h1>

<p align="center">
  <strong>Save what you want to read, and now summarised on-device, no cloud
  required. One codebase, your phone, your desktop, or a browser tab.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-1a1a1a?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose_Multiplatform-1a1a1a?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose Multiplatform">
  <img src="https://img.shields.io/badge/Android_·_iOS_·_Desktop_·_Web-1a1a1a?style=flat-square" alt="Android, iOS, Desktop, Web">
  <img src="https://img.shields.io/badge/On--device_AI-Android_only-b45f3c?style=flat-square" alt="On-device AI, Android only">
  <img src="https://img.shields.io/badge/MIT-1a1a1a?style=flat-square" alt="MIT License">
</p>

<p align="center"><sub>Monochrome by default. One accent, spent on purpose.</sub></p>

---

## What it does

- **Never lose a link again.** Paste one in, share it from a browser, or
  follow a blog's RSS feed — it's usable instantly, and read back to you
  hands-free through a synced [readback](https://github.com/MKS-01/readback)
  library.
- **Ask for a summary, get one without a round trip to a server.** Swipe a
  saved link or open an article and [Gemini Nano, running on-device through
  Android AICore](https://developer.android.com/ai), reads it for you.
  Nothing about the article leaves the phone. Android only for now — every
  other target just doesn't show the control.
- **A timer that interrupts you, and a theme that gets out of the way.**
  A real notification and a vibration when time's up, not a number quietly
  hitting zero — and one tap drains the app to "Ink", a near-black,
  colourless scheme, with a single terracotta accent one more tap away.

Home is one screen on purpose: today's pick from Saved, today's Readback,
the timer, and what's new from the blogs you follow — nothing to dig for.

## On-device AI

Summaries run through [ML Kit GenAI](https://developer.android.com/ai),
which routes to **Gemini Nano** via AICore on supported hardware. The app
asks for article summarisation only — one to three bullets, folded into a
paragraph — and lets the system decide the register; there's no prompt to
write and nothing to configure beyond how long a summary you want.

This is Android-only today, because AICore is Android-only today. iOS,
desktop and web compile the same summary UI against a stub that reports
itself unavailable, so the rest of the app never has to know the difference
— the summary control just quietly isn't there.

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
separate sync step puts on the device. On-device summaries are ML Kit
GenAI on Android; every other target sees the same UI wired to a no-op.

```bash
./gradlew ktlintCheck    # lint
./gradlew ktlintFormat   # auto-fix
```

No tests yet.
