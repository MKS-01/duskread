---
name: duskread-setup
description: Use when getting DuskRead running from scratch — a fresh clone, a
  new machine, a new phone, or helping someone else stand it up — and
  specifically when connecting it to Notion (the two databases, their exact
  property names and types, the personal access token) or wiring up readback
  audio and summaries. Covers the order the steps have to happen in, what each
  one is verified by, and the failures that look like bugs but are setup.
---

# Setting up DuskRead from scratch

DuskRead is a personal app: whoever clones it runs it against **their own**
Notion workspace and their own readback folder. There is no server, no
account and nothing shared — so setup is entirely local, and every step below
is done once per person, not once per project.

Do the steps in order. Each has a check; don't move on without it.

## 0. What is optional

Only the first two steps are required. The app is fully usable with none of
the rest — saved links, feeds added by hand, the reader and the focus timer
all work on a bare install.

| Step | Needed for | Skip if |
| --- | --- | --- |
| 1–2 Toolchain, install | anything | never |
| 3 Notion | followed blogs curated in Notion, saved links syncing both ways | you'll add feeds by hand in the app |
| 4 Readback | listening to articles as audio | you only read |
| 5 Summaries | on-device article summaries | not on AICore hardware — the control simply isn't drawn |

Say which of these the person actually wants before walking them through all
five.

## 1. Toolchain

- **JDK 17+** — the toolchain targets JVM 17.
- **Android SDK**, compile/target 36, and a device or emulator on **Android
  12+** (`minSdk` 31).
- **Gradle** comes from the wrapper. Do not install it.

Re-derive those numbers rather than trusting this file:

```bash
grep -nE 'android-(min|target|compile)Sdk|kotlin|composeMultiplatform' gradle/libs.versions.toml
```

Desktop, web and iOS are the workshop, not the app (`README.md`). iOS
additionally needs Xcode and [XcodeGen](https://github.com/yonaskolb/XcodeGen)
— the host project is generated, not committed — and a first Kotlin/Native
build is ten minutes plus. Don't build it as part of setup unless asked.

## 2. Build, install, first run

```bash
git clone https://github.com/MKS-01/duskread.git && cd duskread
./gradlew :androidApp:installDebug          # ~5 s once warm
adb shell am start -n dev.mks.duskread/dev.mks.duskread.android.MainActivity
```

Onboarding asks for a name once and nothing else. **Check:** the app opens on
Home, and pasting any URL into the field adds a row that fills in its real
title within a second or two.

## 3. Notion

### 3a. Create the two databases

The app reads **`Sources`** and reads/writes **`Reading List`**. Property
names are matched literally by `notion/NotionSources.kt` and
`notion/NotionReadingList.kt` — spelling and case must be exact, and the type
must match or the value is ignored.

`Sources` — what to follow. Only four properties are read:

| Property | Type | Notes |
| --- | --- | --- |
| `Name` | Title | shown in Following instead of a hostname |
| `Feed URL` | URL | a plain site URL works too — resolved via `<link rel=alternate>` |
| `Topic` | Select | inherited by every post from that feed; drives ranking |
| `Active` | Checkbox | unticked rows are skipped — **a missing column means active** |

`Reading List` — articles. The only table the app writes:

| Property | Type | Notes |
| --- | --- | --- |
| `Title` | Title | phone wins once it has fetched the real page |
| `URL` | URL | the fallback match key — never rewritten |
| `Duskread ID` | Text | the app's own id |
| `Saved` | Checkbox | **the gate — only ticked rows reach the phone** |
| `Dismissed` | Checkbox | "not interested"; never pulled |
| `Status` | Status | option names are read from the schema, not assumed |
| `Read At`, `Saved At` | Date | |
| `Topic` | Select | |
| `Excerpt` | Text | |

Anything else in either table is left alone. Adding columns is safe; renaming
these is not. `docs/architecture.md` ▸ *Notion schema* is the full table
including the properties the app deliberately ignores.

### 3b. Get a token

A **personal access token** from Notion's developer portal — an internal
integration in the person's own workspace. Not OAuth: `/v1/oauth/token`
authorises over HTTP Basic with a client secret and accepts no
`code_verifier`, so a phone-only app would have to ship an extractable
secret (`docs/architecture.md` ▸ *Authentication*). A PAT acts as the person
who created it, which also means **there is no per-database *Add connections*
step** — the usual "Notion returns 404 for a database that plainly exists"
trap does not apply here.

Notion shows the token once, at creation. It is stored on the device in
`SecretStore` (AES-GCM under a hardware-backed keystore key on Android),
never in `KeyValueStore` and never in this repository.

### 3c. Enter it on the phone

**Settings ▸ Notion**, three fields, paste-once each:

1. the token — masked after saving;
2. **Sources database ID** — masked to its last four characters;
3. **Reading List database ID** — optional; without it, feeds still sync and
   saved links simply stay local.

The ID is the 32-hex-character segment of the database's URL, not the whole
URL, and not the view's `?v=` id.

**Check:** press **Test connection**. Success shows *Connected to <database
title>*; a failure prints the API's own message. Then **Sync now** — it
reports counts for itself. A full sync is ~75 seconds, almost all of it feed
fetching.

### 3d. What happens after that, without asking

`runFullSync` (`notion/NotionSync.kt`) is called by that button *and* by
`HomeScreen` on launch. On launch it does nothing at all if nothing is
configured, nothing if there's no token, and nothing if the last sync was
under four hours ago **and** nothing local has changed. Anything saved, read,
retitled or deleted since overrides the clock.

**Automatic failures are silent by design** — a network-error banner on
launch is the first thing a reader would see on a train. So "it isn't
syncing" is diagnosed with the Settings button, which reports, never by
waiting for a toast that will not come.

Invariants worth stating to whoever is setting up: the app **never deletes or
archives a Notion row** (refusing an article ticks `Dismissed`), never
uploads feed posts — only deliberate saves — and never rewrites an unchanged
row.

## 4. Readback audio

Generation happens on a Mac in the separate
[readback](https://github.com/MKS-01/readback) project, which produces a
`library.db` and an `audio/` folder. Sync that folder to the phone yourself;
DuskRead never writes to it.

Then, in the **Readback** tab, pick the folder once through the system
picker:

- **Pick the `readback-audio-db` root**, not `audio/` inside it. Choosing
  wrong gives a message saying so rather than a silently empty list.
- **The grant persists** across reboots — scoped storage remembers it.
- **Read-only.** `library.db` is copied into the app's cache to be queried.

**Check:** the tab lists episodes, and playing one shows the transport in the
floating bar at the foot of Home; it keeps playing with the screen off.

## 5. On-device summaries

ML Kit GenAI routes to **Gemini Nano** via AICore — Pixel 9+, Galaxy S24+ and
similar. **No emulator has AICore**, so this cannot be verified on one; the
control is simply not drawn where `summariesSupported()` is false. First run
downloads the model once.

## Troubleshooting — setup, not bugs

| Looks like | Actually |
| --- | --- |
| Following is empty after connecting | rows unticked in `Active`, or `Feed URL` is missing/misnamed |
| A source never appears | its `Feed URL` doesn't resolve to a feed — check `Feed status` in Notion yourself; the app doesn't write it |
| Saved links don't reach the phone | `Saved` isn't ticked — it is the gate, not a label |
| A dismissed article keeps coming back | something re-filed it in Notion; the app only ever ticks `Dismissed` |
| Sync "does nothing" on launch | under four hours since the last one with no local changes — press **Sync now** |
| Topic ranking looks random | `Topic` empty on the `Sources` rows; every post inherits its feed's topic |
| 429s / a slow sync | Notion allows ~3 req/s; reads page at 100, writes are spaced 350 ms, and a 429 is retried three times honouring `Retry-After` |
| The summary control is missing | not AICore hardware — expected, see step 5 |

## Disconnecting

**Disconnect** in Settings clears the token and the database IDs and keeps the
followed feeds — they are DuskRead's own data by then, and signing out of a
source should not empty the app.

## When this file is wrong

The exact property names, the four-hour rule, the rate limits and the design
rationale behind all three live in `docs/architecture.md`, which is the single
design document. If the code and this skill disagree, the code
wins — fix the skill in the same change, the same way `README.md` is kept in
step (`duskread-readme`).
