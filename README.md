<p align="center">
  <img src="docs/media/duskread-mark.svg" alt="DuskRead" width="96" height="96">
</p>

<h1 align="center">DuskRead</h1>

<p align="center">
  <strong>Save what you want to read, follow the blogs worth following, and hear it all read back.</strong><br>
  Summarised on your phone by Gemini Nano — no server, no API key, no round trip.<br>
  One Kotlin codebase; Android is the one that's finished.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Gemini_Nano_·_on--device-b45f3c?style=flat-square&logo=googlegemini&logoColor=white" alt="On-device AI via Gemini Nano">
  <img src="https://img.shields.io/badge/Kotlin_·_Compose_Multiplatform-1a1a1a?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin and Compose Multiplatform">
  <img src="https://img.shields.io/badge/Android_8.0+-1a1a1a?style=flat-square&logo=android&logoColor=white" alt="Android 8.0 and up">
  <img src="https://img.shields.io/badge/MIT-1a1a1a?style=flat-square" alt="MIT License">
</p>

<p align="center"><sub>Monochrome by default. One accent, spent on purpose.</sub></p>

---

Every blog you follow and every link you meant to get back to, in one app in your pocket — RSS and Atom on one side, whatever you shared from a browser on the other, and no bare URLs sitting there waiting for a title. Sync a [readback](https://github.com/MKS-01/readback) library and all of it is waiting as audio, with a transport bar that seeks and keeps playing after you leave. Ask for a summary and Gemini Nano writes it on the phone through Android AICore — no key, no account, fetching the page the only thing that touches the network. A focus timer sits alongside, ending in a real notification and a vibration. It's drawn as **Paper Black** — a page rather than a screen, warm-white ink on matte near-black, lit by one terracotta accent — or **Ink**, the same page with the hue drained to luminance alone, which is what it opens in.

<p align="center">
  <img src="docs/media/home.png" alt="Home — today's pick, Readback, Focus, and Following" width="22%">
  <img src="docs/media/following.png" alt="A followed blog's posts" width="22%">
  <img src="docs/media/readback.png" alt="Readback, with the transport bar playing a post" width="22%">
  <img src="docs/media/summary.png" alt="An on-device summary, generated from the reader" width="22%"><br>
  <sub>Home · Following · Readback mid-play · a summary written on the phone.</sub>
</p>

---

## Getting started

> **Phase one is Android.** It's the target this is built and tested against day to day. iOS, desktop and web share the same Compose Multiplatform codebase and compile today, but they're parked until the Android side is done — and summaries won't appear on them at all, because AICore has no other host.

```bash
git clone https://github.com/MKS-01/duskread.git && cd duskread
./gradlew :androidApp:installDebug      # ~5 s once warm
```

That's the whole setup. Saved links, feeds, the timer and the themes all work the moment it opens.

| | |
|---|---|
| **JDK** | 17 or newer — the toolchain targets JVM 17 |
| **Android** | 8.0 Oreo and up (`minSdk` 26, compile/target 36) |
| **Gradle** | 9.3.1, via the wrapper — don't install it yourself |
| **For summaries** | A phone with **AICore** — Pixel 9+, Galaxy S24+ and similar. No emulator has it |
| **For iOS** | Xcode, plus [XcodeGen](https://github.com/yonaskolb/XcodeGen) — the host project is generated, not committed |

<details>
<summary><strong>The other three targets</strong></summary>

```bash
./gradlew :composeApp:run                           # desktop
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # web

cd iosApp && xcodegen generate && cd ..             # iOS needs an Xcode host
open iosApp/iosApp.xcodeproj
```

A first Kotlin/Native build for iOS is ten minutes or more; incremental after that.
</details>

---

## Saved links and feeds

**Save it however it reaches you.** Paste a URL into the field, or share one straight from Chrome — DuskRead is in the share sheet. Either way the row appears immediately with the host as its stand-in title, then a fetch fills in the real one behind it. A link that can't be fetched keeps a retry glyph rather than pretending it worked.

**Follow blogs by feed.** RSS and Atom, synced on open, cached so the list is instant. Posts land in Following on Home and read back exactly like a saved link does.

---

## Readback

**Generation happens elsewhere; this app just listens.** [readback](https://github.com/MKS-01/readback) runs on a Mac and turns articles into neural-TTS audio, writing a `library.db` and an `audio/` folder. You sync that folder to the phone yourself — its own script, run whenever you like.

Point DuskRead at it once, through the system folder picker in the Readback tab:

- **Pick the `readback-audio-db` root**, not `audio/` inside it. Choose wrong and you get a message saying so, rather than a permanently-empty list.
- **The grant persists.** Android's scoped storage means a folder is opened once and remembered; there's nothing to re-do after a reboot.
- **Strictly read-only.** DuskRead copies `library.db` into its cache to query it and never writes back — the sync direction is one-way by design.

Playback runs in a foreground service with a media notification, so it survives leaving the app, and the floating bar at the foot of Home turns into the transport when something's playing.

---

## On-device summaries

Summaries go through [ML Kit GenAI](https://developer.android.com/ai), which routes to **Gemini Nano** via AICore on supported hardware. The app asks for article summarisation only — one bullet or three, folded into a paragraph — and lets the system pick the register. There's no prompt to write and nothing to configure beyond how long a summary you want.

- **First run downloads the model.** AICore handles it; it takes a moment once and never again.
- **Two ways in** — swipe a row in Saved, or the toolbar control inside the reader.
- **Everywhere else, it's simply absent.** iOS, desktop and web compile the same summary UI against a stub that reports itself unavailable, so the rest of the app never has to know the difference — the control just isn't drawn.

---

## Focus timer

A pomodoro that behaves like an alarm and not a widget: a real system notification and a vibration when the interval ends, so it works with the phone face-down and the app closed. It lives on Home beside the reading, because the point is to read for twenty-five minutes rather than to admire a timer.

---

## Tech stack

Deliberately small: everything but the leaf nodes is shared, and the platform seams are `expect`/`actual` pairs — the reader's storage, the audio player, the summariser, the timer, the key/value store, the HTTP client.

| Layer | Technology |
|---|---|
| **UI** | [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) 1.11 + Material 3 — Android, iOS, desktop, web from one source set |
| **Language** | Kotlin 2.3, JVM 17 target |
| **Navigation** | None — three tabs are a `HomeTab` enum behind the floating bar; focus mode, the summary panel and the reader are overlays in one `Box` gated on a `Boolean`, with system back through a single `expect fun PlatformBackHandler`. No Navigation Compose, no back stack |
| **State** | `remember { mutableStateOf(…) }` hoisted into `App.kt`; a flat key/value store for links, feeds and prefs. No ViewModel, no DI, no database |
| **On-device AI** | [ML Kit GenAI](https://developer.android.com/ai) `genai-summarization` → Gemini Nano through AICore |
| **Networking** | [Ktor](https://ktor.io/) 3.1 — OkHttp on Android, CIO on desktop, Darwin on iOS, JS on web |
| **Async** | kotlinx.coroutines |
| **Readback source** | Storage Access Framework + `DocumentFile` on Android; [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc) on desktop — read-only both ways |
| **Audio** | `MediaPlayer` in a foreground service with `androidx.media` session + notification |
| **Effects** | [Haze](https://github.com/chrisbanes/haze) — the blur behind the floating bar |
| **Build** | AGP 9 with the `androidLibrary` KMP DSL, Gradle 9.3.1 |
| **Lint** | [ktlint](https://pinterest.github.io/ktlint/) — several rules deliberately off, see `.editorconfig` |

```bash
./gradlew ktlintCheck    # lint
./gradlew ktlintFormat   # auto-fix
```

No tests yet.

---

## Design system

Two colour schemes, one layout, both dark. `DarkScheme` is **Paper Black**; `MonoScheme` is **Ink**, the same layout with hue drained to luminance alone. Paper Black's accent is an `AccentColor` — terracotta `#C6684A` or a dusty green `#4FA870`, built at the terracotta's own saturation and lightness so neither reads louder than the scheme was designed to carry. Ink ignores it, and the Settings swatches grey out while it's active. Ink is the default and persists.

The app swaps between them at runtime, which is why no screen hard-codes a colour — a literal survives the swap and immediately looks wrong.

Everything visual is specified in one browsable page:

```bash
open docs/design-system/design-system.html
```

Colour comes from `MaterialTheme.colorScheme` (`ui/theme/Theme.kt`), sizes that carry a decision from `ui/theme/Tokens.kt`, and icons from `ui/theme/DuskReadIcons.kt` — stroked to match the type weight, so a filled Material glyph mixed in is visible instantly.

---

## Licence

MIT — see [LICENSE](LICENSE).

<p align="center">
  <sub>Built agent-first with <a href="https://claude.ai/code">Claude Code</a></sub>
</p>
