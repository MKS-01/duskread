# Notion as DuskRead's subscription source

## Context

DuskRead's followed blogs live only in `feeds.followed`, a delimited string in
`SharedPreferences` on one phone. Adding a blog means typing a URL into the
Following tab; nothing else knows the list exists.

Separately, a Claude cowork session has already built the other half: a Notion
workspace holding 18 subscriptions gathered from RSS, Substack and a connected
Gmail account, plus a *Duskread Digest* artifact rendering 47 recent posts
across 17 of them.

The gap is that these two halves have never met. This plan connects them in the
direction that carries the most value for the least risk: **Notion becomes the
place subscriptions are curated, and a Sync button in Settings pulls them down
into `FeedLibrary`, where the existing `syncFeeds` takes over unchanged.**
Nothing in the app writes to Notion.

## What already exists (verified, not assumed)

- **`Reading List` database** already existed, with a schema matching the
  handoff plan exactly (Title, URL, Source, Author, Newsletter, Published At, Saved At,
  Status, Tags, Excerpt, Notes, Duskread ID).
- **Its 18 rows are *sources*, not articles** — one per feed, e.g.
  `TechLeader Voices → …/feed`. The article-shaped schema is entirely unused:
  every row has a null `Published At`, `Author` and `Duskread ID`. This is the
  finding that shapes the plan; the table is doing two jobs and does neither.
- **The Digest artifact is a static snapshot.** Its 47 posts are a hardcoded
  `DATA` array in the HTML. It cannot be "synced from" — it is a rendering of a
  fetch that already happened. It is, however, a fine one-off backfill corpus.
- **The repo has no JSON, no credential, and no background sync.** Every Ktor
  actual is a bare `HttpClient(Engine)` with zero plugins; everything is
  `bodyAsText()` + regex. `kotlinx-serialization` is not declared anywhere.
  `ExportSink.kt`'s KDoc explicitly rejected Google Drive because it would mean
  "an OAuth flow, a Cloud project and API keys shipped in the binary" — that
  reasoning applies here and is honoured below.
- **Notion OAuth cannot work client-only.** Notion does offer OAuth — it is one
  of the two authentication methods when creating a connection — but
  `/v1/oauth/token` authorises with HTTP Basic auth over
  `CLIENT_ID:CLIENT_SECRET` and accepts no `code_verifier`. Without PKCE a
  phone-only app would have to ship an extractable secret. The browser redirect
  is fine; it is the exchange on the far side that needs a server this project
  does not have.

  **Open-sourcing this strengthens the case rather than weakening it.** A
  public repo is not a hosted service: whoever clones DuskRead runs it on their
  own phone against their own workspace, so they are their own operator, which
  is exactly what a personal access token is for. OAuth would mean either
  routing strangers' authorisation codes through a server the author pays for
  and is liable for, or asking every user to register an integration and deploy
  their own — to read a database. It would also make the build
  non-reproducible from source, since the secret could never be in the repo.

  The one trigger that would flip this is not publishing the code; it is
  *hosting* it, so other people sign in to someone else's instance. `NotionAuth`
  is an interface for that day, and nothing in `NotionClient`,
  `syncReadingList` or Settings would change.
- **A personal access token beats an internal integration secret here.** Both
  are `ntn_…` bearer tokens and the client code is identical, but a PAT "acts
  as the user who created it and uses that user's permissions in the selected
  workspace" — whereas an integration only reaches pages manually shared with
  it via *Add connections*, which is the setup step everyone forgets. Create it
  at **Developer portal → New token**, copy once. Use a PAT.

---

## Workflows

### W1. Where each thing lives, and who is allowed to write it

The one rule that keeps this simple: **Notion is upstream, DuskRead is
downstream, and the arrow never reverses.** Anything the app writes, it writes
to its own store.

```
  Gmail ─┐
  RSS  ──┼──► Claude (cowork / MCP) ──► ┌─────────── Notion ───────────┐
  Substack ┘        curates             │  Sources DB     18 feeds     │
                                        │  Reading List   47 articles  │
                                        └───────────┬──────────────────┘
                                                    │  read-only, HTTPS
                                                    ▼
                                        ┌─── DuskRead (phone) ─────────┐
                                        │  FeedLibrary   feeds.followed│
                                        │  FeedPostCache feeds.posts   │
                                        │  LinkLibrary   links.saved   │
                                        │  SecretStore   the token     │
                                        └──────────────────────────────┘

  Nothing in the app writes to Notion. A row deleted upstream does not
  delete a feed downstream — see B5.
```

### W2. Connecting, and disconnecting

```
  Not connected
      │
      │  paste PAT  ──►  SecretStore.put()        ← encrypted, Android
      ▼
  Token saved
      │
      │  [Test connection]  ──►  GET /v1/databases/{id}
      │                              ├ 401 ──► "Token rejected"
      │                              ├ 404 ──► "Database not found"
      │                              └ 200 ──► title: "Sources"
      ▼
  Connected · Sources · 18 feeds · synced 2m ago
      │
      │  [Disconnect]
      ▼
  SecretStore.put(null) + NotionPrefs.clear()
      │
      └──►  Not connected        feeds and posts are KEPT
                                 (they're DuskRead's data, not Notion's)
```

When OAuth replaces the paste step later, only the top two rows change — the
browser round-trip lands an `access_token` in the same `SecretStore`, and
`[Disconnect]` is already the logout for it.

### W3. A tap on **Sync now** — the whole path

```
  Settings ▸ NOTION ▸ [Sync now]
        │
        ▼
  NotionAuth.bearer()  ──── null ────►  "Not connected", stop
        │ ntn_…
        ▼
  NotionClient — POST /v1/databases/{sources}/query
        │   ├ 429 ──► honour Retry-After, retry ≤3×      (Notion allows ~3 req/s)
        │   ├ 401/404/network ──► typed error, feeds untouched, stop
        │   └ has_more ──► loop on next_cursor           (page_size 100)
        ▼
  List<NotionSource>            name · feedUrl · topic · active
        │
        │  filter { it.active }                          Google Bug Hunters drops out
        ▼
  FeedLibrary.add(feedUrl)      ── already followed? ──► reuse it, count "skipped"
        │ new                                             (add() already dedupes)
        ▼
  feeds.followed
        │
        ▼
  syncFeeds(client, feeds.feeds, cache)     ← EXISTING code, called unchanged
        │
        ▼
  FeedPostCache  ──►  Home dashboard + Following tab
        │
        ▼
  NotionPrefs.lastSync = now
  Toast — "17 feeds · 4 new"
```

The second half of that chain is the part already shipped: `FollowingSection`'s
own *Sync now* and Home's pull-to-refresh both end at `syncFeeds`. This feature
only bolts a new front end onto it, which is why Part B adds no fetching logic.

### W4. The ongoing loop, once it's built

```
  weekly-ish   You (or Claude) add a newsletter to Sources in Notion
                     │
  on the phone   Settings ▸ Sync now
                     │
                     ▼
               feed appears in Following, posts appear on Home
                     │
                     ▼
               read it, or hear it read back — unchanged
```

Manual on purpose. There is no scheduler in this repo and this plan does not
add one; the button is the whole mechanism.

---

## Part A — Notion workspace  ✅ done

Both databases live at workspace level.

**Their IDs are deliberately not in this repo.** They identify a private
workspace, and this project is public — like the token, a database ID is
configuration, not documentation. Both are typed into Settings once and stored
on the device. To find yours: open the database as a full page and take the
32-hex segment of its URL, `notion.so/<workspace>/<DATABASE_ID>?v=…`.

### A1. `Sources`

`Name` (title), `Feed URL`, `Site URL`, `Source` (RSS / Substack / Medium /
Email / Manual), `Topic` (the digest's six groupings), `Tags`, `Active`
(checkbox), `Feed status` (ok / no feed / unverified), `Notes`.

`Feed URL` and `Site URL` are separate on purpose: half the original rows
recorded a site address where a feed was meant, and conflating them is exactly
how a sync ends up fetching an HTML page and finding no entries.

### A2. The 18 rows, with feed addresses actually verified

Rather than copy the registry's URLs across, every candidate was fetched and
checked for an RSS/Atom root element. **14 of 18 resolve to a real feed**; the
other four are `Active = false` so the sync skips them.

Corrections worth knowing:

- **Android Developers Blog** was registered as `developer.android.com`, which
  serves no feed. The real one is
  `android-developers.googleblog.com/feeds/posts/default`.
- **Google Bug Hunters** — the digest marked it *unavailable*, but
  `bughunters.google.com/feed/en` serves 76 entries. It is the blog URL that
  has no feed, not the publication. Active.
- **SwiftLee** arrived as an email course, but `avanderlee.com/feed/` works, so
  the app can follow it directly instead of waiting on Gmail.
- **Bugcrowd** advertises `/feed/` via `link rel=alternate`, but that path
  serves the Hacker Portal SPA shell under any user agent. Paused.
- **HackerOne**, **NativeWeekly**, **androidengineers.in** — no feed at any
  common path. Paused; NativeWeekly still reaches the Reading List by email.

### A3. 47 articles backfilled into `Reading List`

From the artifact's `DATA` array — 47 posts across 16 sources, 41 with a date.
A `Topic` select was added to `Reading List` mirroring the one on `Sources`, so
the two tables group the same way.

### A4. One manual step left for you

The Notion connector exposes no delete-page capability, so the **18 original
source rows are still in `Reading List`**. A view named
**"Old source rows — safe to delete"** filters to exactly those (they are the
rows with no `Topic`). Select all and delete, and `Reading List` is purely
articles.

> `Status` stays Notion's stock `Not started / In progress / Done` rather than
> the `Inbox / Reading / Read / Archived` in the handoff doc — the mapping is
> obvious and renaming status groups is fiddly. Say if you'd rather it match.

---

## Part B — App code

New package `composeApp/src/commonMain/kotlin/dev/mks/duskread/notion/`.

### B1. Dependencies — one plugin, one library

`gradle/libs.versions.toml`: add the `org.jetbrains.kotlin.plugin.serialization`
plugin (pinned to the existing `kotlin` version) and
`kotlinx-serialization-json`; apply the plugin in `composeApp/build.gradle.kts`
and add the library to `commonMain`.

**Deliberately not added:** `ktor-client-content-negotiation` and
`ktor-serialization-kotlinx-json`. Installing a plugin would mean touching all
four `createHttpClient()` actuals, and Notion's response is deeply
variant-typed — a `select` and a `multi_select` and a `url` share no shape. The
smaller, more honest tool is `Json.parseToJsonElement(body)` navigated with
`jsonObject[...]`, no `@Serializable` model at all. Same reasoning that keeps
`KeyValueStore` a four-method interface.

### B2. `SecretStore` — the repo's first credential

`data/SecretStore.kt`, an `expect`/`actual` mirroring `KeyValueStore`'s shape:

```kotlin
expect class SecretStore(...) { fun get(key: String): String?; fun put(key: String, value: String?) }
```

- **androidMain** — `EncryptedSharedPreferences` (`androidx.security:security-crypto`).
- **desktop / ios / wasmJs** — delegate to `KeyValueStore` with a KDoc saying
  plainly that it is plaintext there and why that is acceptable (Android is the
  only target that will hold a token; the others have no Settings entry point
  for one yet).

A token must never reach `algo_atlas` SharedPreferences, which is world-readable
to anything with root and is what the widget reads.

### B3. `NotionAuth.kt` — the seam

```kotlin
interface NotionAuth {
    suspend fun bearer(): String?     // null = not connected
    fun disconnect()                  // clears the token, whatever kind it is
}
class PastedTokenAuth(private val secrets: SecretStore) : NotionAuth
```

Every request path takes `NotionAuth`, never a raw string, so the app never
learns whether the credential behind it is a PAT or an OAuth grant.

**The OAuth path this leaves open**, since it's the reason for the seam: an
`OAuthAuth` implementation writes its `access_token` (and refresh token) into
the *same* `SecretStore`, encrypted the same way — the only new pieces are the
authorize-URL launch, a `duskread://` callback activity, and the exchange
endpoint. `disconnect()` is the logout for both: one call clears the store,
`NotionPrefs`, and the cached connection state, dropping the section back to
`Not connected`. Followed feeds already pulled are *not* removed — they're
DuskRead's own data, and signing out of a source shouldn't empty the app.

### B4. `NotionClient.kt` — the transport

Thin wrapper over `createHttpClient()`. Handles exactly the four things the
Notion API forces on a caller:

- `Authorization: Bearer …`, `Notion-Version: 2022-06-28`, JSON content type.
- **Pagination** — `POST /v1/databases/{id}/query` returns `has_more` /
  `next_cursor`; loop until exhausted, `page_size` 100.
- **429 backoff** — honour the `Retry-After` header, exponential otherwise,
  capped at 3 attempts. Notion allows ~3 req/s.
- **Typed failure** — a `NotionResult` sealed class distinguishing
  `Unauthorized` (bad or revoked token), `NotFound` (wrong database ID, or —
  if an integration token is used instead of a PAT — one never shared with it),
  `RateLimited`, `Network` and `Malformed`, because Settings has to say *which*
  went wrong.

Plus `suspend fun databaseTitle(id: String)` for **Test connection** — one
cheap `GET /v1/databases/{id}` that proves token and ID together, and returns
the name to show back ("Sources") so a correct connection is legible rather
than just green.

### B5. `NotionSources.kt` + `NotionSync.kt` — the pull

```kotlin
data class NotionSource(val name: String, val feedUrl: String, val topic: String?, val active: Boolean)

suspend fun pullSources(client, auth, databaseId): NotionResult<List<NotionSource>>
suspend fun applySources(sources: List<NotionSource>, feeds: FeedLibrary): SourceSyncSummary
```

`applySources` is **additive only**, the same contract as
`LinkLibrary.import`: it calls the existing `FeedLibrary.add(url)` for each
active source, which already dedupes case-insensitively and returns the
existing `Feed`. A row removed in Notion does *not* unfollow the blog locally —
silently deleting a user's feeds on a network response is exactly the kind of
thing this app shouldn't do. Summary returns `found / added / skipped`.

### B6. `Feed` gains a name (small, optional, worth it)

Notion supplies "JetBrains Kotlin Blog"; `Feed` has nowhere to put it and the
Following list shows `blog.jetbrains.com`. Add `val title: String? = null` as a
fourth encoded field. `FeedLibrary.decode` guards on `fields.size < 3` and
`encode` writes positionally, so old records decode unchanged — the same
tolerant-decode trick `SavedLink` uses for its `getOrNull(7)` field.

### B7. `NotionPrefs.kt`

Alongside `UserPrefs`, same snapshot-state-then-store idiom. Keys
`notion.database.sources`, `notion.sync.last` (epoch ms), `notion.sync.error`.
The token lives in `SecretStore`, not here.

---

## Part C — Settings UI

One new `NOTION` section in
`composeApp/src/commonMain/kotlin/dev/mks/duskread/ui/settings/SettingsScreen.kt`,
placed after `APPEARANCE`. Built entirely from what the file already uses — no
new primitives:

- `EyebrowHeader(text = "NOTION")` + `Spacer(14.dp)`, matching every other section.
- **Token field** — `AppTextField`, masked, with the `AnimatedVisibility(dirty)`
  trailing "Save" that `NameField` already does. Once saved it shows `ntn_••••`
  and never the value again; the field is the way in, not a display.
- **Database field** — `AppTextField` for the `Sources` database ID.
- **Connection state** — a `DestinationRow`-shaped two-line block: title is the
  state (`Not connected` / `Sources · 18 feeds` / `Token rejected`), detail is
  the relative time via the existing `savedAgo()`.
- **Actions** — `TransferAction`s, disabled while running, exactly as
  `DataTransfer`'s buttons behave:
  - `Test connection` and `Sync now`, always.
  - `Disconnect`, shown only once connected. Calls `NotionAuth.disconnect()`,
    clears `NotionPrefs`, returns the section to `Not connected`. It is the
    logout, and it stays the logout after OAuth lands. Keeps the followed feeds.
    Rendered in `onSurfaceVariant`, not `primary` — the one accent on the screen
    shouldn't be spent on the destructive action.
- Result via `ToastRequest.show(...)`, and a local `note` that self-clears after
  `delay(5_000)` — the pattern `DataTransfer` and `FollowingSection` both use.

**Sync now** = `pullSources` → `applySources` → then reuse the existing
`syncFeeds(client, feeds.feeds, cache)` so one tap ends with posts on Home.

Colour comes from `MaterialTheme.colorScheme` only; no literal survives the Ink
swap.

---

## Implementation order — built

| Step | What | State |
| --- | --- | --- |
| 1 | Notion workspace (A1–A3) | done, one manual delete left (A4) |
| 2 | `SecretStore`, serialization dependency | done |
| 3 | `NotionAuth`, `NotionClient` | done |
| 4 | `NotionSources`, `NotionSync`, `NotionPrefs`, `Feed.title` | done |
| 5 | Settings' `NOTION` section | done |
| 6 | Device verification | **outstanding — needs a token and a phone** |

Files added:

```
composeApp/src/commonMain/kotlin/dev/mks/duskread/
  data/SecretStore.kt              interface + the plaintext fallback
  notion/NotionAuth.kt             the seam OAuth will replace
  notion/NotionClient.kt           transport, paging, 429 backoff, NotionResult
  notion/NotionSources.kt          Notion's row shape -> NotionSource
  notion/NotionSync.kt             pullSources, applySources, the summary
  notion/NotionPrefs.kt            database id, last sync, cached name
  androidMain/.../data/SecretStore.android.kt    AES-GCM under the keystore
  {desktop,ios,wasmJs}Main/.../SecretStore.*.kt  delegating, and say so
```

Changed: `links/Feed.kt` and `links/FeedLibrary.kt` (optional `title`, a
fourth positional field old records decode past), `ui/settings/SettingsScreen.kt`
(the section), `ui/home/HomeScreen.kt` (passes its own `FeedLibrary` down),
`ui/home/FollowingSection.kt` and `ui/home/TopicsScreen.kt` (show `Feed.label`).

## Verification

Compiling proves nothing here, so:

1. `./gradlew :composeApp:compileKotlinDesktop` — common code compiles, and
   catches the serialization plugin wiring early.
2. `./gradlew ktlintCheck` — note `.editorconfig` disables argument-list
   wrapping et al.; do not "fix" a violation by re-enabling a rule.
3. `./gradlew :androidApp:installDebug`, then exercise on the phone:
   - `adb shell am force-stop dev.mks.duskread && adb shell am start -n dev.mks.duskread/dev.mks.duskread.android.MainActivity`
   - Settings → NOTION → **Test connection** with no token → expect
     `Not connected`, not a crash.
   - Paste a deliberately wrong token → expect `Token rejected`.
   - Paste a valid token but a wrong database ID → expect the distinct
     `not found` message, not a generic error.
   - Paste the real PAT and `Sources` ID → **Sync now** → Following shows 17
     feeds with real names; Home fills with posts.
   - **Sync now** a second time → `found 17, added 0`, no duplicates.
   - Airplane mode → **Sync now** → a network message, feeds untouched.
   - **Disconnect** → back to `Not connected`; the 17 feeds are still followed
     and Home still has its posts. Then force-stop, relaunch, open Settings →
     still disconnected, and no `ntn_` string survives anywhere:
     `adb shell run-as dev.mks.duskread grep -rl ntn_ shared_prefs/` finds
     nothing, and `algo_atlas.xml` never held it in the first place.
4. `adb logcat -d | grep -i duskread` for anything uncaught.
5. Confirm in Notion that nothing was written — row counts unchanged.

## Out of scope (say so rather than half-build it)

- **Writing to Notion.** No upsert of saved links, no read-status push. The
  `Duskread ID` column stays empty until that phase.

  Deferred, not designed out — this plan deliberately leaves the door open:
  `NotionClient` already carries the auth, pagination and 429 backoff a write
  path needs, so a later push adds a `POST /v1/pages` call and a mapper, not a
  new transport. `Reading List` already has the two columns that make an upsert
  idempotent — `Duskread ID` for `SavedLink.id`, `URL` for canonical-URL
  dedupe — and `SavedLink` already carries `readAt`, which is the only field a
  `Status` write would need. Whether it happens at all depends on whether the
  round trip earns its keep; the read half may well be the whole feature.
- **Scheduled/daily sync.** The button is manual, matching `FollowingSection`'s
  existing "Sync now". No WorkManager.
- **Gmail ingestion in the app.** Newsletters keep arriving in Notion via your
  Claude-side Gmail connection; the app only reads the result.
- **PDFs**, per the handoff doc — Duskread-local, never Notion.
- **iOS/desktop/Wasm Settings entry.** The section is built in `commonMain` and
  will compile everywhere, but `SecretStore` is only properly encrypted on
  Android and that is the only target being exercised.

---

# Part 2 — Home discovery at scale

The sync changed the problem it fed into. `NEXT UP` was calibrated when the
pool was a handful of saved links; it now chooses three rows out of roughly
**210 feed posts** (`EntriesPerFeed = 15` × 14 active feeds) plus every unread
saved link. Four of its terms behaved differently at that size, and a fifth had
never done anything at all.

The architecture did not change. `Recommender.rank` stays a pure function of
named, bounded, individually explicable terms.

## What was wrong, with the arithmetic

**Jitter out-voted freshness.** At a 14-day half-life, three days of a fresh
sync spanned `1.0 → 0.86` — a range of 0.14 — against a jitter of 0.35. The top
of the list was mostly noise. Freshness is now a **4-day** half-life (same three
days span `1.0 → 0.59`) and jitter is **0.18**.

**A skip punished the wrong thing.** `recordSkip` resolved `hostOf(url)` and
wrote a `HostSignal`, so stepping past one JetBrains post penalised every post
that blog had published and did nothing to the one on screen — which could
return on the next tap. With the shuffle as the main way through a 210-item
pool, its primary signal was backwards. `ReadingSignals` now also keeps
`signals.skipped` (url → time, most recent 60, oldest evicted) and a
`PostSkipWeight` term sinks that exact post, decaying over two days. The host
term stays at half its old weight as the weak hint it always should have been.

**Nothing enforced variety.** `topPicks` takes the best candidate per host,
after ranking rather than as a scoring penalty — every score has to stay
explicable on its own, and a diversity term folded into the arithmetic makes
the honest answer "because of what else was in the list".

**Every re-rank re-split every body.** `estimatedMinutes` split cached
`<content:encoded>` for all candidates on each shuffle tap, on the main thread.
`FeedPost.words` is now counted once at sync time — from the *whole* body,
before the cache truncates it to 24k chars and drops it entirely past the sixth
entry per feed. So the estimate also got more accurate: most posts used to fall
back to a flat 7-minute guess.

**Topic affinity was a socket with nothing in it.** `recordTopicRead`,
`signals.topicReads` and `TopicAffinityWeight = 0.7` were all built and read by
`rank()`, but `pool()`'s `tagFor` defaulted to `{ null }` and no caller passed
one. A seventh of the scoring function was permanently zero.

## Topics come from Notion, not a model

`Sources.Topic` is already curated per feed, and `NotionSource.topic` was
already parsed and then discarded. It now rides through `Feed.topic` (fifth
positional field, tolerant decode) into every post that feed carries, and
`openCandidate` credits it on read.

Zero inference, no new dependency, every platform, editable by hand. It does
the one thing host affinity structurally cannot: **pool across hosts.** Three
security posts read from three different blogs credit `Security` once, so a
fourth from a blog never opened still ranks.

It is per-**source**, not per-article — a general-interest blog gets one topic
for everything. That is the known cost of not running a model, and the
`tagFor`-shaped seam is where per-article tagging would later override it.

## The Discovery block

`Settings ▸ DISCOVERY`. Pool size, how many carry a topic, read and skip
counts, and the top five with their score broken out per term — only the terms
that actually contributed, since a row of seven values where four are `0.00`
hides the three that decided it. Plus **Re-rank** and **Clear signals**.

The weights above are a starting point chosen by arithmetic, not a final
answer. They get tuned here, on the phone, against real candidates.

---

# Part 3 — Saved links, both directions

Parts 1 and 2 ran one way: `Sources` → `FeedLibrary` → posts on Home. Saved
links stayed on one phone, in one key/value blob, with no copy anywhere.

## Saved is the deliberate subset

The constraint that shapes this: ~210 feed posts pass through each sync and 47
articles sit in `Reading List` from the digest backfill. Almost none of those
are things anyone chose. **Saved links are the favourites, not the firehose**,
so the sync cannot mirror the table.

That is what the one new column is for.

| Need | Column | State before |
| --- | --- | --- |
| Match a row without duplicating it | `Duskread ID` | existed, empty on all 65 rows |
| Read / unread | `Status` | existed |
| Subject, for ranking | `Topic` | added in Part 1 |
| Who changed last | `last_edited_time` | free on every page |
| **Which rows belong on the phone** | **`Saved`** | **new, a checkbox** |

Only ticked rows come down. The 47 backfilled articles stay in Notion as a
browsable archive. Saving on the phone ticks the box; ticking one by hand puts
that article on the phone at the next sync — which is also how a newsletter
filed from Gmail reaches the reader.

## Two new local fields

- **`SavedLink.changedAt`** — nothing else can resolve a conflict. `savedAt`
  says when a link arrived, and `readAt` is null on exactly the rows that need
  comparing. Compared against `last_edited_time`, newest takes the row.
- **`SavedLink.topic`** — the ranking otherwise infers a topic by matching a
  link's host against a followed feed, which fails for precisely the mailed
  newsletters this brings in.

Both are positional and decoded with `getOrNull`, so existing records load
unchanged. A record with no change stamp falls back to `savedAt`.

## Conflicts, and the two things it refuses to do

**Whole-row last-write-wins** on read state. Per row rather than per field: a
rule that fits in one sentence is one that can be reasoned about on a phone at
midnight, and read state is the only field that realistically diverges. Title
and description are taken only to fill a gap — the phone fetches the real page,
so overwriting a fetched title with a filed one is a downgrade even when the
row is newer.

**It never deletes or archives a Notion row.** The app is a working set over an
archive it does not own. That leaves a deleted link still ticked in Notion and
ready to return, so `LinkLibrary.removedUrls` remembers refusals — bounded to
200, oldest evicted, the same shape as the skip list.

**It never rewrites a row that has not changed.** A sync that touched every row
every time would make `last_edited_time` meaningless, and the whole
reconciliation rests on it.

## Status names are read, not assumed

Notion ships `Not started` / `In progress` / `Done`, and its DDL refuses to
rename them — so a table that says `Unread` / `Read` gets renamed by hand. The
sync reads the option names out of the schema's own `to_do` and `complete`
groups, so either spelling works and neither is hard-coded. Renaming them in
Notion is safe.

## Rate limiting

Notion allows ~3 requests/second and a first push is one request per saved
link. The 429 backoff from Part 1 is a recovery; writes are now spaced 350 ms
so it rarely has to fire.
