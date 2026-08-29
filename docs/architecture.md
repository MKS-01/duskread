# DuskRead — architecture

How the app is put together, what it stores, and how the pieces reach each
other. `README.md` says what DuskRead is for; this says how it works.

Written for someone who has just cloned the repository and wants to know where
things live before changing them.

---

## Contents

- [The shape of it](#the-shape-of-it)
- [Where data lives](#where-data-lives)
- [Notion schema](#notion-schema)
- [On-device storage](#on-device-storage)
- [Flow 1 — following blogs](#flow-1--following-blogs)
- [Flow 2 — saved links, both directions](#flow-2--saved-links-both-directions)
- [Flow 3 — newsletters, from inbox to phone](#flow-3--newsletters-from-inbox-to-phone)
- [Flow 4 — capture](#flow-4--capture)
- [Flow 5 — what Home suggests](#flow-5--what-home-suggests)
- [Authentication](#authentication)
- [Invariants](#invariants)
- [Module map](#module-map)

---

## The shape of it

A Compose Multiplatform app — Android, iOS, desktop, Wasm from one
`commonMain` — that does four things: keeps saved links, follows blogs by RSS,
plays articles back as audio, and runs a focus timer.

Three planes, and the arrows between them only run one way each:

```
  PUBLISHERS                CURATION                   DEVICE
  ─────────────             ────────────               ──────────────

  RSS / Atom  ──────────────────────────────────────►  FeedPostCache
                                                          ▲
  Gmail ──────►  Claude  ──►  Notion                      │ fetch
                 (MCP)        ├─ Sources ────────────────►│ FeedLibrary
                              └─ Reading List ◄──────────►  LinkLibrary
                                                              ▲
  readback (separate project) ──► library.db ──────────►  Reader (read-only)
```

Two rules that most of the design follows from:

1. **The app never destroys anything upstream.** Not a Notion row, not a
   readback record. It is a working set over archives it does not own.
2. **The device works offline.** Notion is where subscriptions are curated, not
   a dependency for reading. Every screen renders from local storage.

---

## Where data lives

| Plane | Holds | Written by | Read by |
| --- | --- | --- | --- |
| **Notion** | subscriptions, article archive, read state | you, Claude, the app | the app |
| **Device** | followed feeds, cached posts, saved links, signals | the app | the app |
| **readback** | `library.db` + `audio/`, synced onto the device by a separate script | the readback project | the app, **read-only** |

Feed *posts* are never uploaded anywhere. Roughly 210 pass through each sync
(15 per feed × 14 feeds) and almost none are things anyone chose — only
deliberately saved links reach Notion.

---

## Notion schema

Two databases. Their IDs are entered in **Settings ▸ Notion** and stored on the
device; they are deliberately not in this repository.

### `Sources` — what to follow

Read by the app, never written.

| Property | Type | Used by the app | Notes |
| --- | --- | --- | --- |
| `Name` | Title | ✅ | shown in Following instead of a hostname |
| `Feed URL` | URL | ✅ | a site URL also works — it is resolved via `<link rel=alternate>` |
| `Site URL` | URL | — | human destination, often not the feed |
| `Source` | Select | — | RSS / Substack / Medium / Email / Manual |
| `Topic` | Select | ✅ | inherited by every post from this feed |
| `Tags` | Multi-select | — | |
| `Active` | Checkbox | ✅ | unticked rows are skipped; **a missing column means active** |
| `Feed status` | Select | — | ok / no feed / unverified |
| `Notes` | Text | — | |

### `Reading List` — articles

The only table the app writes to.

| Property | Type | Direction | Notes |
| --- | --- | --- | --- |
| `Title` | Title | ↕ | phone wins once it has fetched the real page |
| `URL` | URL | ↑ create only | the fallback match key; rewriting it would orphan the row |
| `Duskread ID` | Text | ↑ | the app's own id for the link |
| `Saved` | Checkbox | ↕ | **the gate — only ticked rows reach the phone** |
| `Status` | Status | ↕ | read / unread; option names are read from the schema |
| `Read At` | Date | ↕ | when, as opposed to whether |
| `Saved At` | Date | ↕ | when it was filed |
| `Topic` | Select | ↕ | set in Notion or from the phone |
| `Excerpt` | Text | ↕ | |
| `Source`, `Author`, `Newsletter`, `Published At`, `Tags`, `Notes` | — | — | Claude's, untouched by the app |
| `last_edited_time` | built-in | ↑ read | how conflicts are resolved |

**`Saved` is the load-bearing column.** The table holds everything filed from
feeds and mail; ticking a row is what puts it on the phone.

---

## On-device storage

No database. `KeyValueStore` is a four-method interface over
`SharedPreferences` / a `.properties` file / `NSUserDefaults` / `localStorage`.
Records pack into one string per key with ASCII separators — `` between
fields, `` between records — which need no escaping because no URL or
title can contain them.

**Every decoder is positional and tolerant**: new fields are appended and read
with `getOrNull`, so a record written by an older build still loads. This is
the reason the format survives schema changes without migrations.

| Key | Holds |
| --- | --- |
| `links.saved` | saved links — id, url, title, description, savedAt, readAt, fetched, fetchFailed, changedAt, topic |
| `links.removed` | deleted URLs, so a pull cannot resurrect them (bounded, oldest evicted) |
| `links.inbox` | URLs captured by the widget, drained on next app open |
| `feeds.followed` | id, url, addedAt, title, topic |
| `feeds.posts` | cached posts — feedId, url, title, imageUrl, content, publishedAt, words |
| `signals.hosts` | reads / opens / skips per host |
| `signals.topics` | reads per topic |
| `signals.skipped` | per-URL skips, bounded |
| `notion.database.sources`, `notion.database.reading`, `notion.sync.last`, `notion.database.name` | connection state |
| `user.name`, `intro.seen`, `theme.mono`, `summary.length` | preferences |

The Notion **token is not here.** It lives in a separate `SecretStore` —
`duskread_secrets` on Android, AES-GCM under a hardware-backed keystore key.

---

## Flow 1 — following blogs

```
 Settings ▸ Sync now
        │
        ▼
 GET  /v1/databases/{sources}                     read Status option names
 POST /v1/databases/{sources}/query               paged, 100 at a time
        │
        ▼
 rows ──► NotionSource(name, feedUrl, topic, active)
        │  filter { active }
        ▼
 discoverFeedUrl()          a site URL resolves to its feed
        │
        ▼
 FeedLibrary.add(url, name, topic)                additive — never unfollows
        │
        ▼
 syncFeeds()                15 entries per feed, whole body word-counted once
        │
        ▼
 FeedPostCache  ──►  Home, Following, NEXT UP
```

A row deleted in Notion leaves the feed followed on the phone. Silently
emptying a Following list because a response came back short is damage nobody
notices in time; unfollowing stays a deliberate act in the app.

---

## Flow 2 — saved links, both directions

The only two-way path, and the only place the app writes to Notion.

```
 query Reading List once, index by Duskread ID then by canonical URL
        │
        ├── PULL  rows where Saved ☑
        │     ├─ URL in links.removed?  skip — a delete is a delete
        │     ├─ not present locally?   create it
        │     └─ present?               reconcile
        │
        └── PUSH  every local saved link
              ├─ no matching row?          POST /v1/pages
              ├─ local newer AND differs?  PATCH everything
              ├─ row unclaimed?            PATCH id + Saved only
              └─ otherwise                 write nothing
```

**Conflicts resolve per row, newest wins** — local `changedAt` against Notion's
`last_edited_time`. Per row rather than per field, because a rule that fits in
one sentence can be reasoned about on a phone, and read state is the only field
that realistically diverges.

Three refusals worth knowing:

- **Claiming is not a conflict.** Stamping `Duskread ID` and ticking `Saved`
  happens regardless of timestamps — a row filed from Gmail is pulled with
  `changedAt` set to its own edit time, so it is never "newer" and would
  otherwise never be adopted.
- **An unchanged row is never rewritten.** The whole reconciliation rests on
  `last_edited_time` meaning something.
- **A null never overwrites a value.** The app having nothing to say about a
  topic is not the same as knowing there is none.

---

## Flow 3 — newsletters, from inbox to phone

The part with no code in this repository. Claude, with Gmail and Notion
connected, does the curation; the app only reads the result.

```
  newsletter arrives in Gmail
            │
            ▼
  Claude reads it (Gmail MCP), files a row in Reading List
     Title · URL · Source=Email · Newsletter · Excerpt · Topic
            │
            │   Saved ☐  →  stays in Notion as archive
            │   Saved ☑  →  ↓
            ▼
  Settings ▸ Sync now
            │
            ▼
  appears in Saved, with its topic, rankable by NEXT UP
```

A publication with a working feed is better registered in `Sources` — the app
fetches it directly and nothing has to run for new posts to appear. The mail
path is for newsletters with **no public feed**; four of the eighteen
registered sources are in that position.

---

## Flow 4 — capture

Three ways a link gets in, all landing in `LinkLibrary`:

| Route | Path |
| --- | --- |
| **Share sheet** | `ACTION_SEND` → `SharedLinkRequest` → saved and shown |
| **Home-screen widget** | clipboard read in an invisible activity → `links.inbox` → drained on next open |
| **Paste field** | Saved tab, top |
| **Bookmark** | Following or NEXT UP → carries the feed's topic with it |

The widget writes to `links.inbox` rather than `links.saved` because
`LinkLibrary` rewrites its whole blob on every mutation — a second writer from
another process would be clobbered. The inbox is a queue with one drain point.

---

## Flow 5 — what Home suggests

`NEXT UP` ranks saved links and cached feed posts together as one pool of ~210.

```
 pool(links, cache, feeds)     unread saved links + posts not already saved
        │
        ▼
 rank(...)  every term bounded 0..1, then weighted:
        freshness   1.00   4-day half-life
        source      0.70   reads from this host, Laplace-smoothed
        topic       0.70   reads of this subject — pools across hosts
        stale       0.55   a saved link untouched for a month
        fit         0.45   estimated minutes vs the focus timer
        jitter      0.18   the shuffle, seeded by day + tap count
        skip       -0.15   this host is not landing
        post-skip  -1.40   you just shuffled past this exact article
        │
        ▼
 topPicks(ranked, 3)          at most one per host
        │
        ▼
 hero + two runners-up
```

Weights live in one block at the top of `links/Recommender.kt`. They are wrong
until seen wrong on a phone, which is what **Settings ▸ Discovery** is for — it
shows the pool, how many carry a topic, and the top five with each term broken
out.

---

## Authentication

A **personal access token**, pasted once into Settings.

Notion offers OAuth, but `/v1/oauth/token` authorises with HTTP Basic over
`CLIENT_ID:CLIENT_SECRET` and accepts no `code_verifier` — without PKCE, a
phone-only app would have to ship an extractable secret. The browser redirect
is fine; the exchange behind it needs a server this project does not have.

**For a public repository this is the right answer, not a compromise.** Whoever
clones DuskRead runs it against their own workspace, so they are their own
operator — which is exactly what a PAT is for. OAuth would mean routing
strangers' authorisation codes through a server the author pays for, or asking
every user to deploy their own.

A PAT also acts as the user who created it, so there is no per-database
*Add connections* step to forget.

`NotionAuth` is an interface with `bearer()` and `disconnect()`. If DuskRead is
ever *hosted* for other people, `OAuthAuth` slots in behind it and no sync code
changes.

**Rate limits.** Notion allows roughly three requests a second. Reads page at
100; writes are spaced 350 ms; a 429 is retried up to three times honouring
`Retry-After`.

---

## Invariants

Things that are true everywhere, and worth keeping true:

1. **The app never deletes or archives a Notion row.**
2. **A sync never rewrites an unchanged row.**
3. **Feed posts are never uploaded.** Only deliberate saves.
4. **Every decoder tolerates a short record.** New fields append.
5. **All colour comes from `MaterialTheme.colorScheme`** — the app swaps
   between two schemes at runtime and a literal survives the swap.
6. **One writer per storage key.** A second instance of `LinkLibrary` or
   `FeedLibrary` will clobber the first; they are hoisted and passed down.
7. **No background sync.** The button is the whole mechanism.

---

## Module map

```
composeApp/src/commonMain/kotlin/dev/mks/duskread/
  data/       KeyValueStore, SecretStore, UserPrefs
  links/      SavedLink, LinkLibrary, LinkInbox, Feed, FeedLibrary,
              FeedSync, FeedPostCache, Recommender, ReadingSignals, Topics
  notion/     NotionAuth, NotionClient, NotionSources, NotionSync,
              NotionReadingList, NotionPrefs
  pomodoro/   the focus timer
  reader/     Reader, AudioPlayer — read-only over readback's library.db
  summary/    on-device summaries (Android only; ML Kit GenAI)
  ui/         screens, theme, shared components
```

State is plain `remember { mutableStateOf(...) }` hoisted into `App.kt`. No
ViewModel, no dependency injection, no navigation library — at this size they
would be ceremony, and their absence is deliberate rather than pending.

Platform code lives in `androidMain` / `iosMain` / `desktopMain` /
`wasmJsMain` behind `expect`/`actual`. The Android app module holds the
launcher activity, the home-screen widget and the foreground service.

## Where to look next

- `README.md` — what the app is and how to build it
- `docs/design-system/design-system.html` — the visual language
- `docs/design-system/design-tokens.md` — every colour, type style and value
- `docs/design/notion-sync.md` — how the Notion integration was designed
- `docs/design/amplitude-migration.md` — the state of the design system
