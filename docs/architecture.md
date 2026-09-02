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
- [When a sync happens](#when-a-sync-happens)
- [Same article, different address](#same-article-different-address)
- [Authentication](#authentication)
- [Offline](#offline)
- [Invariants](#invariants)
- [Module map](#module-map)

---

## The shape of it

A Compose Multiplatform app — Android, iOS, desktop, Wasm from one
`commonMain` — that does four things: keeps saved links, follows blogs by RSS,
plays articles back as audio, and runs a focus timer.

Three planes. Both Notion tables now run in both directions; readback is the
one that is still strictly one-way.

```
  PUBLISHERS                CURATION                   DEVICE
  ─────────────             ────────────               ──────────────

  RSS / Atom  ──────────────────────────────────────►  FeedPostCache
                                                          ▲
  Gmail ──────►  Claude  ──►  Notion                      │ fetch
                 (MCP)        ├─ Sources ◄───────────────►│ FeedLibrary
                              └─ Reading List ◄──────────►  LinkLibrary
                                                              ▲
  readback (separate project) ──► library.db ──────────►  Reader (read-only)

  On-device TTS ───────────────────────────────────────►  Speaker
```

The Readback tab — the browser over that synced `library.db` — is **hidden by
default**, because the sync script that fills it is one person's. What replaces
it for everyone else is `speech/`: the phone reads the article itself.

Two rules that most of the design follows from:

1. **The app never destroys anything upstream.** Not a Notion row, not a
   readback record. It is a working set over archives it does not own. It does
   *create* rows in both tables — following a blog files it in `Sources`, the
   same way saving a link files it in `Reading List` — but nothing it does
   deletes or archives one.
2. **The device works offline.** Notion is where subscriptions are curated, not
   a dependency for reading. Every screen renders from local storage, and most
   articles open with no network at all — see [Offline](#offline).

---

## Where data lives

| Plane | Holds | Written by | Read by |
| --- | --- | --- | --- |
| **Notion** | subscriptions, article archive, read state | you, Claude, the app | the app |
| **Notion (setup)** | the two databases themselves | the app, on first connect | — |
| **Device** | followed feeds, cached posts, saved links, signals | the app | the app |
| **readback** | `library.db` + `audio/`, synced onto the device by a separate script | the readback project | the app, **read-only** |

Feed *posts* are never uploaded anywhere. Around 165 sit in the cache at any
time (15 per feed, for the feeds that answer) and almost none are things anyone
chose — only deliberately saved links reach Notion.

---

## Notion schema

Two databases. **The app finds or creates them itself** — see
`notion/NotionProvision.kt`. It looks for `DuskRead Sources` and `DuskRead
Reading List` by exact title, falls back to the bare names `Sources` and
`Reading List` so a hand-made pair is adopted rather than duplicated, and
builds whatever is still missing inside a `DuskRead` page. Their IDs are then
stored on the device; they are deliberately not in this repository, and there
is no longer anywhere to type one in.

The one manual step that survives is sharing a page with the token: Notion's
API refuses to create a database, or a page, at the workspace root, so a
credential that reaches nothing can build nothing.

### `Sources` — what to follow

**Read and written.** This reversed: `Sources` used to be pulled only, on the
rule that Notion was upstream and the arrow never turned round. It turns round
because the table is now created empty rather than curated into existence
first — a reader who follows four blogs and finds nothing in Notion has been
handed a feature that looks broken. Following a blog on the phone creates its
row; nothing here ever deletes one, so unfollowing is still local only.

| Property | Type | Used by the app | Notes |
| --- | --- | --- | --- |
| `Name` | Title | ↕ | shown in Following instead of a hostname |
| `Feed URL` | URL | ↕ | a site URL also works — it is resolved via `<link rel=alternate>` |
| `Duskread ID` | Text | ↑ | the app's own `Feed.id`; the stable half of the match |
| `Site URL` | URL | — | human destination, often not the feed |
| `Source` | Select | — | RSS / Substack / Medium / Email / Manual |
| `Topic` | Select | ↕ | inherited by every post from this feed |
| `Tags` | Multi-select | — | |
| `Active` | Checkbox | ↕ | unticked rows are skipped; **a missing column means active** |
| `Feed status` | Select | — | ok / no feed / unverified |
| `Notes` | Text | — | |

### `Reading List` — articles

| Property | Type | Direction | Notes |
| --- | --- | --- | --- |
| `Title` | Title | ↕ | phone wins once it has fetched the real page |
| `URL` | URL | ↑ create only | the fallback match key; rewriting it would orphan the row |
| `Duskread ID` | Text | ↑ | the app's own id for the link |
| `Saved` | Checkbox | ↕ | **the gate — only ticked rows reach the phone** |
| `Dismissed` | Checkbox | ↑ | "not interested" — never pulled, and nothing should re-file it |
| `Status` | Status *or* Select | ↕ | read / unread; both the column type and the option names are read from the schema — creating a `status` column via the API is unreliable, so `NotionProvision` falls back to a `select` and everything downstream reads either |
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
| `links.removed` | deleted addresses, so a pull cannot resurrect them and a sync can refuse them upstream (bounded, oldest evicted) |
| `links.inbox` | URLs captured by the widget, drained on next app open |
| `feeds.followed` | id, url, addedAt, title, topic |
| `feeds.posts` | cached posts — feedId, url, title, imageUrl, content, publishedAt, words, offline |
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
 Settings ▸ Sync now, or automatically on open
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
        ├── DISMISS  every tombstoned address → Dismissed ☑, Saved ☐, once
        │
        ├── PULL  rows where Saved ☑ and not Dismissed
        │     ├─ address in links.removed?  skip — a delete is a delete
        │     ├─ not present locally?       create it
        │     └─ present?                   reconcile
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

**Refusals go up first.** A row deleted on the phone is marked `Dismissed`
before the pull could read it as still saved, and before the push could re-tick
it. `Dismissed` is what makes a delete outlive the device: the local tombstone
makes it instant, the column makes it survive a reinstall, a second phone, and
anything filing into this table without asking.

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

`NEXT UP` ranks saved links and cached feed posts together as one pool of
roughly 165.

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

## When a sync happens

Two triggers, one code path — `runFullSync` in `notion/NotionSync.kt`, called
by the button in Settings and by `HomeScreen` on launch.

```
 app opens
     │
     ├─ nothing configured?           do nothing, ever
     ├─ no token?                     do nothing
     ├─ last sync < 4h and nothing
     │  pending?                      do nothing
     └─ otherwise                     sync, in the background, silently
```

**The clock is not the whole rule.** Four hours stops four openings in an
evening costing four syncs, but anything saved, read, retitled or deleted since
the last sync overrides it — a link just pasted in should not wait four hours to
exist anywhere else. Local changes are found by comparing `SavedLink.changedAt`
against the last sync; deletions have to be asked about separately, because a
removed link is no longer in the list to carry a timestamp.

**Automatic failures are silent.** There is nothing useful to say about a sync
nobody asked for, and a network-error banner on launch is the first thing a
reader would see on a train. The button in Settings reports for itself.

A full sync is ~75 seconds, almost all of it the fourteen feed fetches; the
Notion half is two or three requests.

---

## Same article, different address

The same post reaches this app three ways — from its blog's feed, from a
newsletter carrying `?utm_source=`, and shared from a browser with a trailing
slash. Compared as written, those are three articles and become three rows in
Notion.

`links/CanonicalUrl.kt` reduces an address to the form used for comparison:
scheme and `www.` dropped, fragment dropped, default port dropped, trailing
slash dropped, known tracking parameters dropped, the rest sorted.

```
go.dev/blog/generic-methods
  ← https://go.dev/blog/generic-methods/
  ← http://www.go.dev/blog/generic-methods#intro
  ← https://go.dev/blog/generic-methods?utm_source=substack
```

Two rules it lives by:

- **It is a comparison key, never a destination.** The address a link was saved
  with is what opens and what goes to Notion. Stripping a parameter to compare
  is safe; stripping it to navigate is a guess about someone else's server.
- **Tracking parameters are a blocklist, not an allowlist.** The meaningful
  ones are unknowable — `?p=` is a WordPress post, `?v=` a YouTube video — and a
  wrong guess silently merges two different articles. An extra duplicate is the
  cheaper mistake.

**A canonical form is never stored.** It was, briefly, as the key of
`links.removed`; adding one parameter to the tracking list orphaned every
tombstone from the row it was written for. A key whose algorithm is expected to
change cannot be persisted, so the raw address is stored and the key derived on
demand.

---

## Authentication

A **personal access token**, pasted once — and now the *only* thing anyone
types. Setup is: create a token, switch it on for one Notion page, paste it.
Everything else the app works out for itself.

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

1. **The app never deletes or archives a Notion row.** Refusing an article
   ticks `Dismissed` and leaves the row where it is — the refusal is the thing
   worth remembering, and a deleted row is indistinguishable from one that was
   never filed.
2. **A sync never rewrites an unchanged row.**
3. **Feed posts are never uploaded.** Only deliberate saves.
4. **Every decoder tolerates a short record.** New fields append.
5. **All colour comes from `MaterialTheme.colorScheme`** — the app swaps
   between two schemes at runtime and a literal survives the swap.
6. **One writer per storage key.** A second instance of `LinkLibrary`,
   `FeedLibrary` or `NotionPrefs` will clobber the first; they are hoisted into
   `HomeScreen` and passed down. `LinkInbox` exists solely because the widget
   runs in another process and could not share one.
7. **A canonical URL is never persisted.** It is derived wherever two addresses
   are compared — see [Same article, different address](#same-article-different-address).
8. **Syncing is never blocking and never noisy.** It runs in the background,
   reports only when asked for by the button, and does nothing at all until
   there is a token.
9. **Notion is optional.** Every screen works without it. It is where
   subscriptions are curated for the people who curate them, never a
   dependency for reading.

---

## Offline

Every screen renders from local storage, so browsing never needs a network.
Reading an article usually does not either.

`loadArticle` tries the cache before the wire:

```kotlin
loadArticle(...) = articleFromFeed(url, feedTitle, feedContent) ?: fetchArticle(client, url)
```

`articleFromFeed` returns an article only when the feed carried a real body —
at least 900 characters, because *a teaser is not an article*. When it does,
the reader renders from `feeds.posts` and makes no request.

**What that covers, measured on a device:** 134 of 165 cached posts. Every feed
that publishes full-content RSS is cached completely, 15 posts deep. Three of
the eighteen sources — Google Bug Hunters, Spotify Engineering, SwiftLee —
publish only a summary, so none of their posts can be cached at sync time and
all of them need a network.

A post that will open offline is marked `offline` on its meta line. The flag is
computed at sync time by **the same `articleFromFeed` the reader calls**, given
the same truncated body — anything cheaper would eventually disagree, and a
badge that lies about what opens offline is worse than no badge.

**Cost.** Roughly 3 MB of `feeds.posts` for a fourteen-feed catalogue. The cache
re-encodes itself whole on every write, so `syncFeeds` gathers every feed's
posts and commits once via `replaceAll` — committing per feed made a sync
serialise the whole catalogue fourteen times over, a cost that grew with the
square of the number of feeds.

**When there is nothing cached and no signal**, the reader shows the app's own
empty state rather than letting the WebView render a `net::` error page.

---

## Module map

```
composeApp/src/commonMain/kotlin/dev/mks/duskread/
  data/       KeyValueStore  the plaintext store, expect/actual
              SecretStore    the encrypted one, for the token alone
              UserPrefs

  links/      SavedLink · LinkLibrary · LinkInbox      the reading list
              Feed · FeedLibrary · FeedSync · FeedPostCache   followed blogs
              Article · ArticleDocument                extraction, and the
                                                       reader's own HTML
              CanonicalUrl                             what "the same article"
                                                       means
              Recommender · ReadingSignals             what NEXT UP picks
              LinkMetadata · LinkTransfer · SharedLinkRequest

  notion/     NotionAuth         the seam OAuth would replace
              NotionClient       transport, paging, backoff, typed failures
              NotionProvision    finds or builds the two databases
              NotionSources      Sources rows ↔ followed feeds
              NotionReadingList  the two-way saved-links reconciliation
              NotionSync         runFullSync — the one entry point
              NotionPrefs

  pomodoro/   the focus timer
  reader/     Reader, AudioPlayer — read-only over readback's library.db
  speech/     Speaker — the phone reads an article aloud (Android only)
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
