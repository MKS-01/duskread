# DuskRead — architecture

How the app is put together, what it stores, how the pieces reach each other,
and why each part is shaped the way it is. `README.md` says what DuskRead is
for; this says how it works.

Written for someone who has just cloned the repository and wants to know where
things live before changing them. Every number in it was read out of the code
or measured on a device, not estimated.

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
- [The UI layer](#the-ui-layer)
- [Platform notes](#platform-notes)
- [Invariants](#invariants)
- [Module map](#module-map)
- [Known limits](#known-limits)
- [Further reading](#further-reading)

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
  Email ──────►  an assistant  ──►  Notion                │ fetch
                 (MCP client)      ├─ Sources ───────────►│ FeedLibrary
                                   └─ Reading List ◄─────►  LinkLibrary
                                                              ▲
  readback (separate project) ──► library.db ──────────►  Reader (read-only)
```

There is **no backend of DuskRead's own** — no server, no account, no
database. Notion is the only thing resembling one, it is the reader's own
workspace, and the app talks to it directly over HTTPS with a token that never
leaves the device.

Two rules that most of the design follows from:

1. **The app never destroys anything upstream.** Not a Notion row, not a
   readback record. It is a working set over archives it does not own.
2. **The device works offline.** Notion is where subscriptions are curated, not
   a dependency for reading. Every screen renders from local storage, and most
   articles open with no network at all — see [Offline](#offline).

---

## Where data lives

| Plane | Holds | Written by | Read by |
| --- | --- | --- | --- |
| **Notion** | subscriptions, article archive, read state | you, an assistant, the app | the app |
| **Device** | followed feeds, cached posts, saved links, signals | the app | the app |
| **readback** | `library.db` + `audio/`, synced onto the device by a separate script | the readback project | the app, **read-only** |

Feed *posts* are never uploaded anywhere. Around 165 sit in the cache at any
time (15 per feed, for the feeds that answer) and almost none are things anyone
chose — only deliberately saved links reach Notion.

---

## Notion schema

Two databases. Their IDs are entered in **Settings ▸ Notion** and stored on the
device; they are deliberately not in this repository, because a database ID
identifies a private workspace and is configuration rather than documentation.
To find yours, open the database as a full page and take the 32-hex segment of
its URL: `notion.so/<workspace>/<DATABASE_ID>?v=…`.

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

`Feed URL` and `Site URL` are separate on purpose. Registries of blogs record a
site address where a feed was meant often enough that conflating the two is
exactly how a sync ends up fetching an HTML page and finding no entries.

### `Reading List` — articles

The only table the app writes to.

| Property | Type | Direction | Notes |
| --- | --- | --- | --- |
| `Title` | Title | ↕ | phone wins once it has fetched the real page |
| `URL` | URL | ↑ create only | the fallback match key; rewriting it would orphan the row |
| `Duskread ID` | Text | ↑ | the app's own id for the link |
| `Saved` | Checkbox | ↕ | **the gate — only ticked rows reach the phone** |
| `Dismissed` | Checkbox | ↑ | "not interested" — never pulled, and nothing should re-file it |
| `Status` | Status | ↕ | read / unread; option names are read from the schema |
| `Read At` | Date | ↕ | when, as opposed to whether |
| `Saved At` | Date | ↕ | when it was filed |
| `Topic` | Select | ↕ | set in Notion or from the phone |
| `Excerpt` | Text | ↕ | |
| `Source`, `Author`, `Newsletter`, `Published At`, `Tags`, `Notes` | — | — | curation's own columns, untouched by the app |
| `last_edited_time` | built-in | ↑ read | how conflicts are resolved |

**`Saved` is the load-bearing column.** The table holds everything filed from
feeds and mail, most of which nobody chose; ticking a row is what puts it on
the phone. Without that gate the sync would mirror an archive onto a device
whose whole point is the deliberate subset.

**Status option names are read, not assumed.** Notion ships
`Not started` / `In progress` / `Done` and will not rename them through the
API, so anyone who wants `Unread` / `Read` renames them by hand. The sync reads
the option names out of the schema's own `to_do` and `complete` groups, so
either spelling works and neither is hard-coded.

---

## On-device storage

No database. `KeyValueStore` is a four-method interface over
`SharedPreferences` / a `.properties` file / `NSUserDefaults` / `localStorage`.
Records pack into one string per key with ASCII separators — unit separator
(`0x1F`) between fields, record separator (`0x1E`) between records — which need
no escaping because no URL or title can contain them.

**Every decoder is positional and tolerant**: new fields are appended and read
with `getOrNull`, so a record written by an older build still loads. This is
the reason the format survives schema changes without migrations, and it is why
`Feed` could grow a `title` and then a `topic` without a single migration step.

| Key | Holds |
| --- | --- |
| `links.saved` | saved links — id, url, title, description, savedAt, readAt, fetched, fetchFailed, changedAt, topic |
| `links.removed` | deleted addresses, so a pull cannot resurrect them and a sync can refuse them upstream (bounded, oldest evicted) |
| `links.inbox` | URLs captured by the widget, drained on next app open |
| `feeds.followed` | id, url, addedAt, title, topic |
| `feeds.posts` | cached posts — feedId, url, title, imageUrl, content, publishedAt, words, offline |
| `signals.hosts` | reads / opens / skips per host |
| `signals.topics` | reads per topic |
| `signals.skipped` | per-URL skips, bounded to the most recent 60 |
| `notion.database.sources`, `notion.database.reading`, `notion.sync.last`, `notion.database.name` | connection state |
| `user.name`, `intro.seen`, `theme.mono`, `summary.length` | preferences |

The Notion **token is not here.** It lives in a separate `SecretStore`:
AES-GCM under a hardware-backed keystore key, ciphertext parked in its own
`duskread_secrets` file. See [Authentication](#authentication) for why it is
not in the app's main preferences file.

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

**Feeds are parsed, not deserialised.** RSS and Atom disagree about almost
everything — `<pubDate>` in RFC 822 against `<updated>` in RFC 3339,
`<content:encoded>` against `<content>`, two-digit years in the wild — so
`FeedSync` scans the bytes with targeted extraction, caps a response at 500 kB,
and rejects any date before 1995 as a parse artefact rather than a very old
post.

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
that realistically diverges. `SavedLink.changedAt` exists for exactly this:
`savedAt` says when a link arrived, and `readAt` is null on precisely the rows
that need comparing.

Three refusals worth knowing:

- **Claiming is not a conflict.** Stamping `Duskread ID` and ticking `Saved`
  happens regardless of timestamps — a row filed from mail is pulled with
  `changedAt` set to its own edit time, so it is never "newer" and would
  otherwise never be adopted.
- **An unchanged row is never rewritten.** The whole reconciliation rests on
  `last_edited_time` meaning something, and a sync that touched every row every
  time would make it meaningless.
- **A null never overwrites a value.** The app having nothing to say about a
  topic is not the same as knowing there is none. Title and description are
  taken only to fill a gap — the phone fetches the real page, so overwriting a
  fetched title with a filed one is a downgrade even when the row is newer.

**Refusals go up first.** A row deleted on the phone is marked `Dismissed`
before the pull could read it as still saved, and before the push could re-tick
it. `Dismissed` is what makes a delete outlive the device: the local tombstone
makes it instant, the column makes it survive a reinstall, a second phone, and
anything filing into this table without asking. The app still never deletes or
archives the row itself — the refusal is the thing worth remembering, and a
deleted row is indistinguishable from one that was never filed.

---

## Flow 3 — newsletters, from inbox to phone

The part with no code in this repository. An assistant with mail and Notion
connected does the curation — in practice Claude over the
[Notion MCP server](https://www.notion.com/product/dev), which is also what
created both databases and verified every feed address in `Sources` — and the
app only reads the result.

```
  newsletter arrives in the inbox
            │
            ▼
  it is read and filed as a row in Reading List
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
path is for newsletters with **no public feed**, which in a typical catalogue
is a fifth or so of them.

Nothing about this is DuskRead-specific: anything that can write a row into a
Notion database — an automation, a script, a form — reaches the phone the same
way. The app's contract is the schema above, not the tool that fills it.

---

## Flow 4 — capture

Four ways a link gets in, all landing in `LinkLibrary`:

| Route | Path |
| --- | --- |
| **Share sheet** | `ACTION_SEND` → `SharedLinkRequest` → saved and shown |
| **Home-screen widget** | clipboard read in an invisible activity → `links.inbox` → drained on next open |
| **Paste field** | Saved tab, top |
| **Bookmark** | Following or NEXT UP → carries the feed's topic with it |

The widget writes to `links.inbox` rather than `links.saved` because
`LinkLibrary` rewrites its whole blob on every mutation — a second writer from
another process would be clobbered. The inbox is a queue with one drain point.

### The widget itself

Two things, one tap each, without opening the app: capture whatever is on the
clipboard, or start a focus session. Only one of them is ever expanded — a
capture takes the width for a few seconds to confirm itself, a running session
takes it for as long as it runs.

- **`RemoteViews`, not Glance.** Glance is the modern answer, but it has no
  `Chronometer` — and a `Chronometer` is the whole battery story here, because
  it is a real view in the launcher's process, so the countdown is ticked by
  the launcher and this app is never woken to redraw it.
- **Nothing polls.** `updatePeriodMillis` is 0; the only redraws are the two
  the focus service pushes at the ends of a session, plus one non-repeating
  alarm that retires a capture confirmation. Idle, the widget costs nothing.
- **The clock is wall-clock, not `elapsedRealtime`,** which resets on reboot
  and would render as a session that is somehow still running.

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
        skip       -0.15   this host is not landing (7-day half-life)
        post-skip  -1.40   you just shuffled past this exact article (2 days)
        │
        ▼
 topPicks(ranked, 3)          at most one per host
        │
        ▼
 hero + two runners-up
```

`rank` is a pure function of named, bounded, individually explicable terms —
no model, no embedding, nothing that has to be trained or shipped. That is the
constraint the whole feature is built to, because a recommendation you cannot
account for is one you cannot fix.

### Why these numbers

Four of the terms behaved differently once the pool grew from a handful of
saved links to ~165, and a fifth had never done anything at all. The
arithmetic, since it is the sort of thing that gets silently re-guessed:

- **Jitter must not out-vote freshness.** At a 14-day half-life, three days of
  a fresh sync spanned `1.0 → 0.86` — a range of 0.14 — against a jitter of
  0.35, so the top of the list was mostly noise. At a **4-day** half-life the
  same three days span `1.0 → 0.59`, and jitter is **0.18**.
- **A skip has to punish the article, not the publisher.** Skipping used to
  resolve the host and penalise every post that blog had published, while doing
  nothing to the one on screen — which could return on the next tap. The
  per-URL `post-skip` term sinks that exact post for two days; the host term
  stays as the weak hint it always should have been.
- **Variety is enforced after ranking, not inside it.** `topPicks` takes the
  best candidate per host. A diversity penalty folded into the arithmetic
  would make the honest answer to "why this one?" *"because of what else was
  in the list"*.
- **Word counts are computed once, at sync time.** Estimating minutes used to
  re-split every cached body on every shuffle tap, on the main thread. Counting
  at sync also made it *more* accurate, because it runs on the whole body,
  before the cache truncates it.

### Topics come from curation, not inference

`Sources.Topic` is already chosen per feed, so it rides through `Feed.topic`
into every post that feed carries, and reading a post credits it. Zero
inference, no new dependency, works on every platform, editable by hand — and
it does the one thing host affinity structurally cannot: **pool across hosts.**
Three security posts read from three different blogs credit `Security` once, so
a fourth from a blog never opened still ranks.

It is per-*source*, not per-article, so a general-interest blog gets one topic
for everything. That is the known cost of not running a model; the `tagFor`
seam in `pool()` is where per-article tagging would later override it.

Weights live in one block at the top of `links/Recommender.kt`. They are wrong
until seen wrong on a phone, which is what **Settings ▸ Discovery** is for — it
shows the pool, how many candidates carry a topic, read and skip counts, and
the top five with each contributing term broken out. Only the terms that
actually contributed are listed: a row of eight values where five are `0.00`
hides the three that decided it.

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
reader would see on a train. The button in Settings reports for itself, with a
typed reason: `Unauthorized`, `NotFound`, `RateLimited`, `Network`, `Malformed`
— because "sync failed" and "that database ID does not exist" need different
things done about them.

There is **no scheduler**: no `WorkManager`, no background job, no push. Sync
happens when the app is open, which is when its results can be seen.

A full sync is ~75 seconds, almost all of it the feed fetches; the Notion half
is two or three requests.

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

A **personal access token**, pasted once into Settings.

Notion offers OAuth, but `/v1/oauth/token` authorises with HTTP Basic over
`CLIENT_ID:CLIENT_SECRET` and accepts no `code_verifier` — without PKCE, a
phone-only app would have to ship an extractable secret. The browser redirect
is fine; the exchange behind it needs a server this project does not have.

**For a public repository this is the right answer, not a compromise.** Whoever
clones DuskRead runs it against their own workspace, so they are their own
operator — which is exactly what a personal access token is for. OAuth would
mean routing strangers' authorisation codes through a server the author pays
for and is liable for, or asking every user to register an integration and
deploy their own, to read a database. It would also make the build
non-reproducible from source, since the secret could never be in the repo.

The trigger that would flip this is not publishing the code; it is *hosting*
it, so that other people sign in to someone else's instance.

A personal access token also acts as the user who created it, so there is no
per-database *Add connections* step to forget — the failure mode of an internal
integration token is a `404` on a database that plainly exists.

`NotionAuth` is an interface with `bearer()` and `disconnect()`. Every request
path takes it rather than a raw string, so no sync code knows whether the
credential behind it is a pasted token or an OAuth grant; an `OAuthAuth` would
write its `access_token` into the same `SecretStore` and nothing else would
change. `disconnect()` is the logout for both — and it keeps the followed
feeds, which are DuskRead's own data, not Notion's.

**Where the token is kept.** `SecretStore` is `expect`/`actual`. On Android it
is AES-GCM under a hardware-backed keystore key, ciphertext in its own
`duskread_secrets` file — not the app's main preferences file, which is read by
the widget from another process, rewritten wholesale by `LinkLibrary`, and is
where someone will one day add a debug dump. `androidx.security:security-crypto`
would have done exactly this, but it is deprecated with no replacement, and
forty lines of platform API at `minSdk` 31 is the better trade. Losing the key
(a restore to a new device) makes the value undecryptable, which reads as "no
token" and asks for a reconnect — the honest outcome anyway.

On desktop, iOS and Wasm `SecretStore` delegates to `KeyValueStore` in
plaintext and says so in its KDoc. Android is the only target with a Settings
entry point for a token.

**Rate limits.** Notion allows roughly three requests a second. Reads page at
100; writes are spaced 350 ms so the backoff rarely has to fire; a 429 is
retried up to three times honouring `Retry-After`.

**No JSON model.** Responses are navigated with `Json.parseToJsonElement(body)`
and `jsonObject[…]`, with no `@Serializable` types and no Ktor content
negotiation. Notion's payload is deeply variant-typed — a `select`, a
`multi_select` and a `url` share no shape — and installing a plugin would mean
touching all four `createHttpClient()` actuals to model a handful of fields.

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
that publishes full-content RSS is cached completely, 15 posts deep. The gap is
the feeds that publish only a summary — three sources in the catalogue
measured — whose posts can never be cached at sync time and always need a
network.

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

## The UI layer

The visual language is specified on the design system page
(`docs/design-system/design-system.html`) and every value is listed in
`docs/design-system/design-tokens.md`. What matters architecturally is small:

- **Two schemes, one layout.** `ui/theme/Theme.kt` holds `DarkScheme` (Paper
  Black — near-black ground, warm-white ink, one terracotta accent meaning
  *there is sound here*) and `MonoScheme` (Ink — the same page with the hue
  drained out). Ink is the default and persists. The app swaps at runtime, so
  **a hard-coded `Color(0x…)` survives the swap and is immediately wrong**;
  colour comes from `MaterialTheme.colorScheme` only.
- **One row, three screens.** `ui/common/ListRow.kt` holds the flat row —
  sourcechip, title, mono meta line, bottom hairline — used by Saved, Readback
  and the topic lists. `RowTone` (`Normal` / `Accent` / `Faded`) is the whole
  vocabulary of row states, an enum rather than two booleans because they are
  mutually exclusive. Nothing in a list is a boxed card.
- **Sizes that carry a decision live in `ui/theme/Tokens.kt`** — reading
  gutters, the three corner radii, bar clearance, and the `Motion` object every
  animation reads its duration and easing from. Optical one-off nudges stay
  inline; naming one implies a system that is not there.
- **Icons come from `ui/theme/DuskReadIcons.kt`**, all stroked to match the
  type weight. A filled Material glyph mixed in is visible instantly.
- **`AppTextField` is the one text input** in the app — hairline border,
  10 dp corners, never a pill.
- **Layout adapts at one breakpoint.** `ui/layout/WindowClass.kt` reads the
  window against 720 dp; past it a nav rail replaces the floating bar, the
  transport anchors to the bottom edge, and prose caps at 640 dp.

State is plain `remember { mutableStateOf(...) }` hoisted into `App.kt`. No
ViewModel, no dependency injection, no navigation library — at this size they
would be ceremony, and their absence is deliberate rather than pending.

---

## Platform notes

**Android** is the target the app is built and exercised against; the other
three prove the shared code travels. `summariesSupported()` and
`readbackSupported()` are `expect`/`actual` constants, and a screen asks before
it offers, so a control that could only disappoint is never drawn.

- **Summaries** go through ML Kit GenAI to Gemini Nano via AICore. No emulator
  can stand in — those system images ship no `com.google.android.aicore`, so
  the engine there only ever answers "unavailable". Verification needs real
  hardware.
- **The engine returns bullets, always**, so `parseSummary` flattens them into
  prose rather than the panel growing a layout per engine mood. Short and Full
  map to the engine's own `ONE_BULLET` / `THREE_BULLETS`; a word limit was
  tried first and dropped, because a limit can only cut, and three points often
  run under any ceiling worth naming.
- **Playback** runs in a foreground service with a media notification, so it
  survives leaving the app.
- **readback integration is read-only**: `library.db` is copied into the app's
  cache to be queried, never written. The folder is granted once through the
  Storage Access Framework and the grant persists across reboots.
- **The launcher icon's crescent is an opaque circle in the ground colour**,
  drawn after the bar — *not* an `evenOdd` subtraction, which is wrong for a
  cut shape extending outside the base shape: `evenOdd` fills that outside part
  back in as a second, disconnected blob. The themed-icon layer, which reads
  only alpha and so cannot use the ground-colour trick, keeps a true `evenOdd`
  hole sized to sit fully inside the bar.

**Desktop** runs the embedded browser through JCEF, which needs the packaged
`.app` layout — hence `runDistributable` rather than `run`.

**Web** has no readback library and no summaries; the Readback tab shows its
"not configured" state rather than disappearing, because a missing tab is a
worse answer than an empty one. The window is not the app's own — a tab can be
resized to anything mid-session — so the breakpoint is read continuously.

---

## Invariants

Things that are true everywhere, and worth keeping true:

1. **The app never deletes or archives a Notion row.** Refusing an article
   ticks `Dismissed` and leaves the row where it is.
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
   something is configured.
9. **No credential is in the repository, and none is in the main preferences
   file.**

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
              NotionSources      Sources rows → followed feeds
              NotionReadingList  the two-way saved-links reconciliation
              NotionSync         runFullSync — the one entry point
              NotionPrefs

  pomodoro/   the focus timer
  reader/     Reader, AudioPlayer — read-only over readback's library.db
  summary/    on-device summaries (Android only; ML Kit GenAI)
  ui/         screens, theme, shared components
```

Platform code lives in `androidMain` / `iosMain` / `desktopMain` /
`wasmJsMain` behind `expect`/`actual`. The Android app module holds the
launcher activity, the home-screen widget and the foreground service.

---

## Known limits

Stated rather than hidden, because each is a deliberate stopping point:

- **No tests.** `commonTest` declares `kotlin("test")` and has no sources.
  Verification is a build, a lint pass and the app on a device.
- **Wide windows have a rail but not two panes.** The list-and-detail split,
  and the hover / focus / keyboard states that go with it, are specified and
  not built — touch never needed them.
- **iOS and Wasm compile but are not exercised routinely.** A cold
  Kotlin/Native build is over ten minutes, so it is done deliberately rather
  than per change.
- **Desktop ships no app icon** — `nativeDistributions` sets no `iconFile`.
- **Writing more to Notion is deferred, not designed out.** `NotionClient`
  already carries the auth, pagination and backoff a further write path would
  need.

---

## Further reading

The external material this design leans on, for anyone reading the code
without the context that produced it.

**The APIs**

- [Notion API reference](https://developers.notion.com/reference/intro) — pagination, property shapes, `last_edited_time`
- [Notion developer platform](https://www.notion.com/product/dev) — the MCP server and CLI the curation side of this runs on, as opposed to the REST API the app uses
- [Notion authorization](https://developers.notion.com/docs/authorization) — why the OAuth exchange needs a server
- [Notion request limits](https://developers.notion.com/reference/request-limits) — the ~3 req/s the client is spaced against
- [RSS 2.0 specification](https://www.rssboard.org/rss-specification) and [RFC 4287 (Atom)](https://datatracker.ietf.org/doc/html/rfc4287) — the two shapes `FeedSync` has to read as one

**Android**

- [App widgets overview](https://developer.android.com/develop/ui/views/appwidgets) — `RemoteViews`, and what a widget may and may not do
- [Foreground services](https://developer.android.com/develop/background-work/services/fgs) — the playback and timer services
- [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider) — persistable folder grants, how the readback library is reached
- [Android keystore](https://developer.android.com/privacy-and-security/keystore) — the hardware-backed key behind `SecretStore`
- [ML Kit GenAI summarization](https://developers.google.com/ml-kit/genai/summarization/android) — the on-device summariser
- [The splash screen API](https://developer.android.com/develop/ui/views/launch/splash-screen) and [adaptive icons](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive) — safe zones, masks, and the themed layer

**Kotlin and Compose Multiplatform**

- [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)
- [`expect`/`actual` declarations](https://kotlinlang.org/docs/multiplatform-expect-actual.html) — every platform seam in this app
- [Ktor client documentation](https://ktor.io/docs/welcome.html) — one engine per target

**Ranking and extraction**

- [How Hacker News ranking works](https://medium.com/hacking-and-gonzo/how-hacker-news-ranking-algorithm-works-1d9b0cf2c08d) — the time-decay score `freshness` is a cousin of
- [How not to sort by average rating](https://www.evanmiller.org/how-not-to-sort-by-average-rating.html) — why small counts need smoothing, which is what `AffinitySmoothing` is
- [Consolidate duplicate URLs](https://developers.google.com/search/docs/crawling-indexing/consolidate-duplicate-urls) — canonicalisation as the rest of the web understands it
- [Mozilla Readability](https://github.com/mozilla/readability) — the reference implementation of article extraction

---

## Where to look next

- `README.md` — what the app is and how to build it
- `docs/design-system/design-system.html` — the visual language
- `docs/design-system/design-tokens.md` — every colour, type style and value
