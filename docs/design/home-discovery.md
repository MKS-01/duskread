# Home discovery — plan

Unbuilt. This is the plan for making Home pick *something worth opening*
rather than something at random, and for the on-device layer that would make
the pick personal without anything leaving the phone.

Nothing here is in the code yet. `docs/design/amplitude-migration.md` holds
the state of what **is** built.

## Why

Home has four sections. Three of them are honest reports of local state — the
focus timer, the newest readback, the feed digest. The fourth, `FROM SAVED`,
is the only one that makes a *choice*, and today the choice is
`unread.randomOrNull()`, re-rolled only when the set of ids changes.

That has two costs:

- **It is static in practice.** Open the app four times in an afternoon and
  the same article sits there, because the id list has not changed. The
  section that is supposed to say "here, read this" is the least alive thing
  on the screen.
- **It knows nothing.** A blog you read every post of and a link you saved by
  accident in March are equally likely. A twenty-minute essay is as likely as
  a four-minute one when the focus timer is set to five.

And the followed blogs are walled off from it entirely. `FeedPostCache` holds
the last sync of every feed — dozens of real, dated, often full-text posts —
and none of them can ever be the thing Home suggests. They are reachable only
by tapping a digest line open. The app's best content is one deliberate
gesture away from a section that has nothing good to offer.

## The shape

Three layers. Each one ships and is useful on its own, and each one is
allowed to be absent.

| Layer | Where | Needs | What it buys |
| --- | --- | --- | --- |
| 0 — signals | `commonMain` | nothing | a record of what actually gets read |
| 1 — scorer | `commonMain` | layer 0 | a ranked pick on every platform |
| 2 — topics | `androidMain` | Gemini Nano | affinity by subject, not just by host |

Layer 1 is the feature. Layer 2 is the part that makes it feel like it knows
you, and it is the part that may simply not exist on a given device — so it
is arranged as a cache that refines a ranking already on screen, never as a
step the ranking waits for.

---

## Layer 0 — signals

A new `links/ReadingSignals.kt`, same flat separator-packed `KeyValueStore`
encoding as `FeedPostCache` and `SummaryCache`, for the same reason: this is
a few dozen short records and a database would be ceremony.

One record per **host**, not per link — a link is read once and then gone, and
a per-link record could never inform the next pick:

```
host, opens, reads, skips, lastReadAt
```

Written from the places the app already knows something happened:

- `LinkLibrary.toggleRead` → a read
- the shuffle icon stepping past a candidate → a skip
- a summary generated for an article → an open (asking for a summary is
  interest, even if the article is never opened)

**A skip is not a dislike.** It is one bit of "not right now", worth a
fractional penalty that decays, not an exclusion. The failure mode of any
recommender on a list of forty items is that it prunes itself down to five
and then repeats them; the skip term must be too weak to do that.

Add to the same file a `TopicSignals` map — tag → reads — populated only when
layer 2 exists. Absent, it is an empty map and every term that reads it is
zero.

---

## Layer 1 — the scorer

`links/Recommender.kt`. One pure function, no Compose, no I/O:

```kotlin
fun rank(
    candidates: List<Candidate>,
    signals: ReadingSignals,
    now: Long,
    seed: Int,
): List<Scored>
```

`Candidate` is the merged pool — the thing that does not exist today:

- every unread `SavedLink`
- every `FeedPost` in `FeedPostCache` **not** already in `LinkLibrary`

flattened to one type carrying url, title, host, a date, an optional body
(`FeedPost.content` or `SavedLink.description`) and an optional tag.

### The terms

Each bounded, each named, each explicable in one line when the ranking looks
wrong on the phone:

- **Freshness** — a feed post's `publishedAt`, or a link's `savedAt`, decayed
  over about a fortnight. New things surface.
- **Stale rescue** — a saved link untouched for more than a month gets a
  bump. Without this the section only ever shows the last thing you saved,
  which is the one thing you do not need reminding of.
- **Source affinity** — reads from this host over total reads, smoothed so
  one read of one blog does not swamp everything.
- **Topic affinity** — the same, over tags. Zero until layer 2 exists.
- **Fit** — estimated minutes (body words ÷ 200, falling back to a per-host
  median, falling back to a flat guess) against the focus timer's current or
  last-picked length. A five-minute timer should not be offered a
  twenty-minute essay.
- **Skip decay** — the fractional penalty above.
- **Jitter** — a small seeded term.

The jitter is not decoration and not a tie-breaker. **It is the shuffle.**
Tapping shuffle re-seeds and re-ranks; the ordering shifts without the
ranking being abandoned, so a shuffle is "something else good" rather than
"anything at all", which is what the current `random()` gives. Seed is
derived from the day plus a tap counter, so the pick is also stable within a
session rather than changing under a scroll.

Ship layer 1 with the weights as named constants in one block at the top of
the file, with a comment per constant. They will be wrong at first and the
only way to fix them is on the phone.

---

## Layer 2 — topics, on the device

The one thing the score above cannot do is know that three of your saved
links are about databases. Host affinity is a proxy for subject and a poor
one — a general-interest blog gets credited for its one post you liked.

### The API to use

ML Kit's GenAI **Prompt** API (`com.google.mlkit:genai-prompt`), not the
summarisation feature already in the app. Summarisation has one dial —
bullet count — and no prompt, which is exactly why it was the right choice
for summaries and is the wrong one here: tagging needs a closed vocabulary
imposed from outside.

**But first, a five-minute probe, before any of this is designed further.**
`Summariser.android.kt` records that AICore answered `FEATURE_NOT_FOUND` for
the Prompt API on the S25, which is why the whole summary feature was built
on the summarisation feature instead. That note is from an older AICore. The
Prompt API is now Beta, with structured output in Alpha and system
instructions in Beta, and the S25 is on Google's supported list. So:

- [ ] Add `genai-prompt`, call `checkFeatureStatus()` on the S25, log the
      result. **That is the whole first task.** Everything below is
      conditional on it.

If it still answers `FEATURE_NOT_FOUND`, the fallback is at the end of this
section and is not bad.

### The call

One inference per item, on a **shortlist only** — the top dozen out of
layer 1's ranking, never the whole pool — and cached forever:

- **System instruction**: it tags articles, it answers with tags from the
  given list and nothing else.
- **Input**: title plus the first ~200 words of body. Never the full article;
  this is a classification, not a read.
- **Vocabulary**: closed, about a dozen — systems, ml, web, design, product,
  career, security, hardware, science, culture, tooling, other. Closed
  because an open vocabulary produces forty tags across forty articles and
  every affinity count is one.
- **Output**: structured output if the Alpha holds, one or two tags. Failing
  that, a bare line, split on commas, intersected with the vocabulary,
  anything unrecognised dropped. The intersection is worth keeping either way
   — it is the thing that makes a bad answer harmless.

### `TopicCache`

Shaped exactly like `SummaryCache`, keyed by url, holding tag plus the model
name plus a timestamp. Same reasoning as the comment already in that file:
inference is seconds of the phone's own silicon, shows up as heat, and AICore
meters it per app.

Budget, and mean it:

- at most **eight** inferences per sync
- only for shortlist items with no cached tag
- never while the reader is generating a summary — one GenAI job at a time
- never on the draw path

That last one is the whole architecture. Home renders layer 1's ranking
immediately. `TopicCache` is Compose state; when a tag lands, the ranking
recomputes and the section settles. A reader who never waits never notices
the model is there, which is the point.

### The engine's four states

Reuse `SummariserState` verbatim — Checking / Unavailable / Downloadable /
Downloading / Ready. It already models exactly this and the reasoning in its
KDoc applies unchanged.

`Unavailable` is **silent**. No banner, no "enable smart picks", no empty
state. Layer 1 alone is a good section; a device without a model should look
like a device that was never promised one. The only place the state is ever
named is Settings, next to the summariser block that already names its own.

### If the Prompt API is still absent

Tag with the summarisation feature that already ships: `ONE_BULLET` over the
title and lead, then keyword-match that bullet against the closed vocabulary.
It is cruder, it will mis-tag, and the closed vocabulary plus a
two-signal-minimum before topic affinity counts for anything keeps the damage
to "one term is noisy" rather than "the picks are wrong". Worth doing if the
probe fails, because the alternative is layer 2 not existing at all.

---

## What changes on Home

`FROM SAVED` becomes **`NEXT UP`**, and it is the section that finally spans
both halves of the app:

- One hero row — sourcechip, title, and a mono meta line that is
  `host · 6 min` today and `host · systems · 6 min` once a tag exists. The
  same two-facts-and-done row the rest of the screen is built from; the tag
  takes the slot, it does not add a slot.
- Two compact runner-up rows beneath it. Enough to feel like a choice rather
  than a decree, few enough that the section stays three lines and done.
- The shuffle icon stays exactly where it is and does what it looks like it
  does, only better: advance the ranking, record a skip, re-seed at the end.
- A feed post opened from here is **saved into `LinkLibrary` on the way
  out**. Otherwise it is read and forgotten and the signal is lost — and
  `FeedPostCache` is replaced on the next sync, so the record would go with
  it.

`FOLLOWING` keeps its job — following, unfollowing, syncing, per-feed counts.
It stops being the *only* road to a feed post, which is what makes it a
digest rather than a hidden second inbox.

No new section, no new tab, no card boxes. This is one section doing more,
which is the whole reason it can afford to.

---

## Testing it on the phone

Design decisions like these cannot be judged from the code — the weights are
wrong until they are seen wrong. So the plan includes its own instrument.

A **Discovery** block in Settings, beside the existing summariser one:

- engine state, verbatim from `SummariserState`
- how many candidates in the pool, how many carry a tag
- the top five with their scores broken out by term
- **Clear signals** and **Re-rank** buttons

The score breakdown is the thing that matters. Every candidate ranking system
is opaque exactly when it misbehaves, and "why is *that* at the top" is a
question that has to be answerable in the room, on the device, without a
debugger — the same reason `describe()` in `Summariser.android.kt` refuses to
flatten error codes into "something went wrong".

The normal loop:

```bash
./gradlew :androidApp:installDebug
adb shell am force-stop dev.mks.duskread
adb shell am start -n dev.mks.duskread/dev.mks.duskread.android.MainActivity
```

The model half needs real hardware — AICore is absent on emulators, the same
constraint the summary feature hit.

---

## Order of work

- [ ] **Probe the Prompt API on the S25.** One dependency, one status call,
      one log line. It decides the shape of layer 2 and nothing else can be
      settled before it.
- [ ] `ReadingSignals` — the store, and the three write sites.
- [ ] `Candidate` — the merged pool over `LinkLibrary` + `FeedPostCache`.
- [ ] `Recommender.rank` — every term except topic affinity, weights as
      named constants.
- [ ] `NEXT UP` on Home — hero row, two runners-up, shuffle re-seeds, feed
      post saves on open.
- [ ] Discovery block in Settings.
- [ ] **Live on it for a few days.** Everything above works on every platform
      and is the feature. Tune the weights before adding a model to a thing
      that is not yet right without one.
- [ ] `TopicCache` + the tagging engine, budgeted, behind the four states.
- [ ] Topic affinity term, off until a tag count threshold is crossed.
- [ ] Tag in the meta line.

## Open questions

- [ ] Does `NEXT UP` keep the readback library out of the pool? It is
      currently `TODAY'S READBACK`'s own section, and folding audio into the
      same ranking would mean one section proposing two different activities.
      Leaning: keep it out, and revisit only if the two sections start
      looking like they are arguing.
- [ ] Should the fit term read the focus timer's *current* setting or the
      most recently chosen length? The current one is more responsive and
      also means the section changes under you the moment a timer starts,
      which may read as a glitch rather than as intelligence.
- [ ] Is a tag worth showing at all, or is it only ever an input? A tag on
      screen is a claim the app is making about an article it has not read
      properly, and a wrong one is visible in a way a wrong weight is not.
