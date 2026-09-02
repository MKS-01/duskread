# DuskRead — architecture

How the app is put together, what it stores, and how the pieces reach each
other. `README.md` says what DuskRead is for; this says how it works.

Every section opens with the one-line version. Expand a `Details` block only
if you need the schema, the diagram, or the reasoning behind a number.

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
- [Deliberately not built](#deliberately-not-built)
- [Module map](#module-map)

---

## The shape of it

A Compose Multiplatform app — Android, iOS, desktop, Wasm from one
`commonMain` — that does four things: keeps saved links, follows blogs by RSS,
plays articles back as audio, and runs a focus timer. Notion curates what to
follow and what's worth reading; the device never depends on it being reachable.

<details>
<summary>The three planes, and the two rules the design follows from</summary>

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

Both Notion tables run in both directions now; readback is the one still
strictly one-way. The Readback tab — the browser over that synced
`library.db` — is **hidden by default**, because the sync script that fills
it is one person's. What replaces it for everyone else is `speech/`: the
phone reads the article itself.

1. **The app never destroys anything upstream.** Not a Notion row, not a
   readback record. It is a working set over archives it does not own. It
   does *create* rows in both tables — following a blog files it in
   `Sources`, the same way saving a link files it in `Reading List` — but
   nothing it does deletes or archives one.
2. **The device works offline.** Notion is where subscriptions are curated,
   not a dependency for reading. See [Offline](#offline).
</details>

---

## Where data lives

| Plane | Holds | Written by | Read by |
| --- | --- | --- | --- |
| **Notion** | subscriptions, article archive, read state | you, Claude, the app | the app |
| **Notion (setup)** | the two databases themselves | the app, on first connect | — |
| **Device** | followed feeds, cached posts, saved links, signals | the app | the app |
| **readback** | `library.db` + `audio/`, synced onto the device by a separate script | the readback project | the app, **read-only** |

Feed *posts* are never uploaded anywhere. Only deliberately saved links reach
Notion.

---

## Notion schema

Two databases, `Sources` and `Reading List`. **The app finds or creates them
itself** — see `notion/NotionProvision.kt` — so there is nowhere to type a
database ID in. The one manual step that survives is sharing a page with the
token: Notion's API refuses to create a database at the workspace root, so a
credential that reaches nothing can build nothing.

<details>
<summary>Both tables, property by property</summary>

### `Sources` — what to follow

**Read and written.** This reversed: the table used to be pulled only, on the
rule that Notion was upstream and the arrow never turned round. It turns
round because the table is now created empty rather than curated first — a
reader who follows four blogs and finds nothing in Notion has been handed a
feature that looks broken. Nothing here ever deletes a row, so unfollowing is
still local only.

| Property | Type | Used by the app | Notes |
| --- | --- | --- | --- |
| `Name` | Title | ↕ | shown in Following instead of a hostname |
| `Feed URL` | URL | ↕ | a site URL also works — resolved via `<link rel=alternate>` |
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
| `Status` | Status *or* Select | ↕ | read / unread; both the column type and option names are read from the schema — a `status` column via the API is unreliable, so `NotionProvision` falls back to `select` and everything downstream reads either |
| `Read At` | Date | ↕ | when, as opposed to whether |
| `Saved At` | Date | ↕ | when it was filed |
| `Topic` | Select | ↕ | set in Notion or from the phone |
| `Excerpt` | Text | ↕ | |
| `Source`, `Author`, `Newsletter`, `Published At`, `Tags`, `Notes` | — | — | Claude's, untouched by the app |
| `last_edited_time` | built-in | ↑ read | how conflicts are resolved |

`Saved` is the load-bearing column — the table holds everything filed from
feeds and mail, and ticking a row is what puts it on the phone.
</details>

---

## On-device storage

No database. `KeyValueStore` is a four-method interface over
`SharedPreferences` / a `.properties` file / `NSUserDefaults` / `localStorage`.
Every decoder is positional and tolerant — new fields are appended and read
with `getOrNull`, so a record written by an older build still loads, with no
migrations.

<details>
<summary>Every key, and where the token actually lives</summary>

| Key | Holds |
| --- | --- |
| `links.saved` | saved links — id, url, title, description, savedAt, readAt, fetched, fetchFailed, changedAt, topic |
| `links.removed` | deleted addresses, so a pull cannot resurrect them (bounded, oldest evicted) |
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
It must never reach the plain `KeyValueStore`, which is what the widget reads
from another process.

On desktop, iOS and Wasm `SecretStore` delegates to `KeyValueStore` and says
so in its own KDoc: it is plaintext there. Android is the only target with a
Settings entry point for a token, and a fallback that pretended otherwise
would be worse than one that admits it.
</details>

---

## Flow 1 — following blogs

Notion's `Sources` pulls into `FeedLibrary`, additively — a row deleted in
Notion leaves the feed followed on the phone, because unfollowing stays a
deliberate act in the app, never a side effect of a short response.

<details>
<summary>The path, step by step</summary>

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
</details>

---

## Flow 2 — saved links, both directions

The only two-way path, and the only place the app writes to Notion.
Conflicts resolve per row, newest wins — local `changedAt` against Notion's
`last_edited_time` — because a rule that fits in one sentence can be reasoned
about on a phone.

<details>
<summary>The reconciliation, and three refusals worth knowing</summary>

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

- **Claiming is not a conflict.** Stamping `Duskread ID` and ticking `Saved`
  happens regardless of timestamps — a row filed from Gmail is pulled with
  `changedAt` set to its own edit time, so it is never "newer" and would
  otherwise never be adopted.
- **An unchanged row is never rewritten.** The whole reconciliation rests on
  `last_edited_time` meaning something.
- **A null never overwrites a value.** The app having nothing to say about a
  topic is not the same as knowing there is none.

**Refusals go up first.** A row deleted on the phone is marked `Dismissed`
before the pull could read it as still saved, and before the push could
re-tick it — the local tombstone makes the delete instant, the column makes
it survive a reinstall or a second phone.
</details>

---

## Flow 3 — newsletters, from inbox to phone

The part with no code in this repository. Claude, with Gmail and Notion
connected, does the curation; the app only reads the result, for newsletters
with **no public feed** — a publication that has one is better registered in
`Sources` directly.

<details>
<summary>The path</summary>

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
</details>

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
`LinkLibrary` rewrites its whole blob on every mutation — a second writer
from another process would be clobbered. The inbox is a queue with one drain
point.

---

## Flow 5 — what Home suggests

`NEXT UP` ranks saved links and cached feed posts together as one pool, by a
weighted sum of bounded terms — freshness, source affinity, topic affinity,
staleness, fit against the timer, jitter, and two skip penalties.

<details>
<summary>The weights, and the arithmetic behind each one</summary>

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

Weights live in one block at the top of `links/Recommender.kt`. They are
wrong until seen wrong on a phone, which is what **Settings ▸ Discovery** is
for — it shows the pool, how many carry a topic, and the top five with each
term broken out.

- **Jitter used to out-vote freshness.** At a 14-day half-life, three days of
  a fresh sync spanned `1.00 → 0.86` — a range of 0.14 — against a jitter of
  0.35, so the top of the list was mostly noise. A 4-day half-life spans the
  same three days `1.00 → 0.59`, and jitter came down to 0.18.
- **A skip used to punish the wrong thing.** It wrote only a host signal, so
  stepping past one post penalised everything that blog had published and
  did nothing to the article on screen. `signals.skipped` now sinks that
  exact post, decaying over two days; the host term stays at less than half
  its old weight, as the weak hint it always should have been.
- **Variety is enforced after ranking, not inside it.** `topPicks` takes the
  best candidate per host, because a diversity term folded into the
  arithmetic makes the honest answer to "why this one?" *"because of what
  else was in the list"*.
- **Topics come from Notion, not a model.** `Sources.Topic` rides through
  `Feed.topic` into every post that feed carries — the one thing host
  affinity structurally cannot do: pool across hosts, so three security
  posts from three different blogs make a fourth from a blog never opened
  rank. It's per *source*, not per article — the known cost of not running
  a model. `pool()`'s `tagFor` is the seam where per-article tagging would
  override it.
</details>

---

## When a sync happens

`runFullSync` runs on the Settings button and on launch if the last sync was
over four hours ago — but anything saved, read, retitled or deleted since
overrides the clock. Failures on an automatic sync are silent; the button
reports for itself. A full sync is ~75 seconds, almost all of it feed
fetches.

<details>
<summary>The trigger</summary>

```
 app opens
     │
     ├─ nothing configured?           do nothing, ever
     ├─ no token?                     do nothing
     ├─ last sync < 4h and nothing
     │  pending?                      do nothing
     └─ otherwise                     sync, in the background, silently
```
</details>

---

## Same article, different address

The same post can reach this app three ways — its blog's feed, a newsletter
carrying `?utm_source=`, a browser share with a trailing slash — and those
are the same article. `links/CanonicalUrl.kt` reduces an address to a
comparison key: scheme and `www.` dropped, fragment dropped, tracking
parameters dropped, the rest sorted.

<details>
<summary>The rules, and why a canonical form is never stored</summary>

```
go.dev/blog/generic-methods
  ← https://go.dev/blog/generic-methods/
  ← http://www.go.dev/blog/generic-methods#intro
  ← https://go.dev/blog/generic-methods?utm_source=substack
```

- **It is a comparison key, never a destination.** The address a link was
  saved with is what opens and what goes to Notion. Stripping a parameter to
  compare is safe; stripping it to navigate is a guess about someone else's
  server.
- **Tracking parameters are a blocklist, not an allowlist.** The meaningful
  ones are unknowable — `?p=` is a WordPress post, `?v=` a YouTube video —
  and a wrong guess silently merges two different articles. An extra
  duplicate is the cheaper mistake.
- **Never persisted.** It was, briefly, as the key of `links.removed`; adding
  one tracking parameter orphaned every tombstone from the row it was
  written for. A key whose algorithm is expected to change cannot be stored,
  so the raw address is kept and the key derived on demand.
</details>

---

## Authentication

A **personal access token**, pasted once — the only thing anyone types.
Notion offers OAuth, but its token exchange needs HTTP Basic auth over
`CLIENT_ID:CLIENT_SECRET` with no PKCE, which a phone-only app can't do
without shipping an extractable secret.

<details>
<summary>Why a PAT is the right answer here, not a compromise</summary>

Whoever clones DuskRead runs it against their own workspace, so they are
their own operator — exactly what a PAT is for. OAuth would mean routing
strangers' authorisation codes through a server the author pays for, or
asking every user to deploy their own. A PAT also acts as the user who
created it, so there's no per-database *Add connections* step to forget.

`NotionAuth` is an interface with `bearer()` and `disconnect()`. If DuskRead
is ever *hosted* for other people, `OAuthAuth` slots in behind it and no
sync code changes.

**Rate limits.** Notion allows roughly three requests a second. Reads page
at 100; writes are spaced 350 ms; a 429 is retried up to three times
honouring `Retry-After`.
</details>

---

## Offline

Every screen renders from local storage. `loadArticle` tries the feed cache
before the wire, and only falls back to a request when the feed didn't
carry a real body (under 900 characters — a teaser is not an article).

<details>
<summary>What that covers, and the offline badge</summary>

```kotlin
loadArticle(...) = articleFromFeed(url, feedTitle, feedContent) ?: fetchArticle(client, url)
```

Every feed that publishes full-content RSS is cached completely, 15 posts
deep — a minority of feeds publish only a summary, so none of their posts
can be cached and all need a network.

A post that will open offline is marked `offline` on its meta line, computed
at sync time by **the same `articleFromFeed` the reader calls**, given the
same truncated body — anything cheaper would eventually disagree, and a
badge that lies is worse than no badge.

The cache re-encodes itself whole on every write, so `syncFeeds` gathers
every feed's posts and commits once via `replaceAll` rather than per feed,
which made a sync serialise the whole catalogue once per feed — a cost that
grew with the square of the feed count.

**When there is nothing cached and no signal**, the reader shows the app's
own empty state rather than letting the WebView render a `net::` error page.
</details>

---

## Invariants

Things that are true everywhere, and worth keeping true:

1. **The app never deletes or archives a Notion row.** Refusing an article
   ticks `Dismissed` and leaves the row where it is.
2. **A sync never rewrites an unchanged row.**
3. **Feed posts are never uploaded.** Only deliberate saves.
4. **Every decoder tolerates a short record.** New fields append.
5. **All colour comes from `MaterialTheme.colorScheme`** — a literal survives
   the runtime scheme swap.
6. **One writer per storage key.** A second instance of `LinkLibrary`,
   `FeedLibrary` or `NotionPrefs` will clobber the first; they are hoisted
   into `HomeScreen` and passed down. `LinkInbox` exists solely because the
   widget runs in another process and could not share one.
7. **A canonical URL is never persisted.** See
   [Same article, different address](#same-article-different-address).
8. **Syncing is never blocking and never noisy.** Background, silent unless
   asked, and does nothing at all until there is a token.
9. **Notion is optional.** Every screen works without it.
10. **There is one transport, and it is a face of the floating bar.**
    Readback and a live read never play at once — starting either stops the
    other, in `HomeScreen`, the one place that can see both. The bar is
    drawn after the full-screen surfaces it shares a `Box` with, so a read
    started from one of them still has a player; over those it carries the
    transport alone, since the tabs behind it aren't reachable anyway.
    Anything bottom-anchored asks `BottomFurniture` how much room is taken
    rather than assuming a bar is there.
11. **A read that cannot happen says why.** `Speaker.speak` closes its flow
    with the reason — no engine, no voice installed, an engine that never
    started — and `SpeechPlaybackService` shows it, instead of the silent
    failure this used to be (including a race against `TextToSpeech`'s
    asynchronous `onInit` that fired on most reads).
12. **Every sync write is conditional on the data epoch.** `DataEpoch`
    (`data/DataEpoch.kt`) is what tells a real sync apart from the erase in
    Settings: the erase bumps it before clearing anything, and every sync
    routine takes a mark at the start and declines to write once it's stale
    — otherwise a sync in flight when someone taps Erase restores the whole
    Following list from rows it had already read.

---

## Deliberately not built

Said plainly rather than half-built:

- **No scheduler.** Sync happens on the button and on launch. No
  `WorkManager`, no daily job.
- **No Gmail ingestion in the app.** Newsletters reach Notion through
  Claude's own Gmail connection; the app only reads the result.
- **No JSON plugin.** Notion's response is deeply variant-typed — a
  `select`, a `multi_select` and a `url` share no shape — so
  `Json.parseToJsonElement` navigated with `jsonObject[...]` is the smaller,
  more honest tool than a `@Serializable` model.
- **No OAuth**, for the reasons under [Authentication](#authentication).
- **No PDFs**, and no upload of anything not deliberately saved.
- **No Settings entry for Notion outside Android** — `SecretStore` is only
  really encrypted there.

---

## Module map

<details>
<summary>Package by package</summary>

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
  speech/     Speaker        the phone reads an article aloud (Android only)
              SpeechSession  the one live read, and what the bar shows
  summary/    on-device summaries (Android only; ML Kit GenAI)
  ui/         screens, theme, shared components
              common/          ListRow · AppTextField · EyebrowHeader ·
                               Pill · HeaderAction · EmptyState · Toast
              home/            the tabs, the floating bar, BottomFurniture
              summary/         the panel a swiped row opens
```

State is plain `remember { mutableStateOf(...) }` hoisted into `App.kt`. No
ViewModel, no dependency injection, no navigation library — at this size
they would be ceremony, and their absence is deliberate rather than pending.

Platform code lives in `androidMain` / `iosMain` / `desktopMain` /
`wasmJsMain` behind `expect`/`actual`. The Android app module holds the
launcher activity, the home-screen widget and the foreground service.
</details>

## Where to look next

- `README.md` — what the app is and how to build it
- `docs/design-system/design-system.html` — the visual language
- `docs/design-system/design-tokens.md` — every colour, type style and value
