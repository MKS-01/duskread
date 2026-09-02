<p align="center">
  <img src="docs/media/duskread-mark.svg" alt="DuskRead" width="96" height="96">
</p>

<h1 align="center">DuskRead</h1>

<p align="center">
  <strong>A reading habit, automated around a Notion database.</strong><br>
  An AI agent files what lands in your inbox, feeds pull themselves, and<br>
  the phone only ever shows what you actually chose to keep.
</p>

<p align="center">
  <a href="https://www.notion.com/product/dev"><img src="https://img.shields.io/badge/Notion_·_curation-b45f3c?style=flat-square&logo=notion&logoColor=white" alt="Curated through Notion"></a>
  <a href="https://www.jetbrains.com/compose-multiplatform/"><img src="https://img.shields.io/badge/Kotlin_·_Compose_Multiplatform-1a1a1a?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin and Compose Multiplatform"></a>
  <img src="https://img.shields.io/badge/Android_12+-1a1a1a?style=flat-square&logo=android&logoColor=white" alt="Android 12 and up">
  <a href="LICENSE"><img src="https://img.shields.io/badge/MIT-1a1a1a?style=flat-square" alt="MIT License"></a>
</p>

<p align="center">
  <a href="https://mks-01.github.io/duskread/">See every screen</a> &nbsp;·&nbsp;
  <a href="docs/architecture.md">How it fits together</a> &nbsp;·&nbsp;
  <a href="#getting-started">Run it</a>
</p>

<p align="center"><sub>Monochrome by default. One accent, spent on purpose.</sub></p>

---

Newsletters and blogs file themselves into one Notion database — ranked by
what you actually read, not by when it arrived — then you set a twenty-five
minute timer and read, on **Paper Black** or **Ink**, the same page with the
colour drained out.

## What it does

<p align="center">
  <img src="docs/media/notion-flow.png" alt="Three sources — Gmail, RSS feeds, and links you paste or share. Claude files the mail into Notion's Sources and Reading List, which syncs both ways with DuskRead; feeds and shared links reach the app directly, never touching Notion. The app caches everything, reads offline, and reads articles aloud on the phone">
</p>

- **Curated through Notion.** Claude files inbox newsletters straight into
  the reading list; blogs with a working feed are fetched by the app and
  never touch Notion. Either way, only rows ticked `Saved` reach the phone —
  see [docs/architecture.md](docs/architecture.md) for the schema and sync
  rules. Following a blog writes it back to Notion too, so it's there to edit
  next time you open it.
- **Save it however it reaches you** — the Chrome share sheet, or the paste
  field on Saved behind **Add**.
- **Reads offline.** Every screen renders from local storage; a feed with
  full-content RSS is cached whole at sync time, so most articles were
  already on the phone before you tapped them.
- **Saved is a queue and a record.** Unread leads; what you've read stays
  under its own heading. Filter to either, or search a title, host or topic.
- **On-device summaries and reading aloud.** Long articles get a one-tap
  summary via Gemini Nano; the phone can also read one aloud with its own
  text-to-speech. Both are absent on hardware that can't run them, and a read
  that can't happen says why instead of staying silent.
- **A focus timer** that behaves like an alarm, not a widget — a real system
  notification and vibration when the interval ends, phone face-down.

## Getting started

> Android is the app, built and tested day to day. Desktop, web and iOS
> compile from the same source to prove it travels.

```bash
git clone https://github.com/MKS-01/duskread.git && cd duskread
./gradlew :androidApp:installDebug      # ~5 s once warm
```

That's the whole setup — Notion is optional, and connecting it is one pasted
token in Settings.

| | |
|---|---|
| **JDK** | 17 or newer |
| **Android** | 12 and up (`minSdk` 31, compile/target 36) |
| **Gradle** | 9.3.1, via the wrapper |
| **For summaries** | A phone with **AICore** — Pixel 9+, Galaxy S24+ and similar. No emulator has it |
| **For iOS** | Xcode, plus [XcodeGen](https://github.com/yonaskolb/XcodeGen) |

<details>
<summary><strong>Desktop, web and iOS</strong></summary>

```bash
./gradlew :composeApp:runDistributable              # desktop — use this, not `run`
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # web, on :8080

cd iosApp && xcodegen generate && cd ..             # iOS needs an Xcode host
open iosApp/iosApp.xcodeproj
```

`runDistributable`, not `run`, for desktop — the embedded Chromium needs the
packaged `.app` layout, or it segfaults on init. First iOS build is ten
minutes or more; incremental after that.
</details>

## How it's built

One `commonMain` source set feeds Android, desktop, web and iOS; storage,
audio, the summariser and the HTTP client are `expect`/`actual` pairs behind
it. UI is [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)
1.11 with [Haze](https://github.com/chrisbanes/haze) for the floating-bar
blur, on [Kotlin](https://kotlinlang.org/) 2.3 and [Ktor](https://ktor.io/)
3.1. Summaries run on-device via [ML Kit GenAI](https://developer.android.com/ai)
/ Gemini Nano; reading aloud is Android's own `TextToSpeech`; the optional
Readback tab queries [readback](https://github.com/MKS-01/readback)'s
`library.db` read-only. No navigation library, no ViewModel, no DI, no
database — a ceiling chosen on purpose.

```bash
./gradlew ktlintCheck    # several rules deliberately off, see .editorconfig
./gradlew ktlintFormat
```

No tests yet — a build that succeeds is the start of checking a change, not
the end of it.

**[The design system](https://mks-01.github.io/duskread/)** — Paper Black
and Ink, and every token behind them.
**[docs/architecture.md](docs/architecture.md)** — how the pieces connect,
the Notion schema, and the end-to-end flows.

## Contributing

A personal learning project, built in the open. Issues, forks and small pull
requests are welcome. Before opening one:

```bash
./gradlew ktlintCheck
./gradlew :composeApp:compileKotlinDesktop
./gradlew :androidApp:installDebug   # then look at the change on a device
```

Compiling proves nothing about layout and there are no tests, so exercise the
screen you touched. [`CLAUDE.md`](CLAUDE.md) has the house style in full.

## Licence

MIT — see [LICENSE](LICENSE).

<p align="center">
  <sub>Built agent-first with <a href="https://claude.ai/code">Claude Code</a></sub><br>
  <sub>The synced-audio Readback tab is a separate, optional open-source project: <a href="https://github.com/MKS-01/readback">readback</a></sub>
</p>
