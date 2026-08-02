# Roadmap

Where this goes after the DSA core. Written 2026-07-28, when the app had six
topics and was called Algo Atlas.

## What it becomes

A go-to reference for working software engineers and students: the thing you
open when you need to *actually understand* something quickly — not a tutorial,
not documentation, not a blog you have to search. One page per concept, each
with an explanation that starts from the problem, a visualisation you can step
through, code in the languages you actually use, and pointers to where to go
deeper.

The scope widens from algorithms to the rest of the field: systems, AI/ML,
LLMs, agentic coding, performance. The unit stays the same — one concept, one
page, read in ten minutes on a phone.

**What it is not:** a course, a book, a video platform, or a replacement for
primary sources. Every topic should end by pointing somewhere deeper. The value
is being the fastest possible *first* explanation, and being trustworthy enough
that the reader knows what to search for next.

---

## The risk worth naming first

"Everything around tech" is the failure mode, not the goal. A reference with
300 shallow pages is worse than one with 40 excellent ones, because shallow
pages train the reader not to trust it.

The current topics run 200–400 lines each: considered prose, verified origin
stories, three language implementations, a hand-built visualisation. That is
real work per topic — call it a few focused hours even with heavy AI
assistance. Two hundred topics at this standard is a year of evenings.

So the plan below is **ordered by usefulness, not by completeness**, and each
phase is designed to be a good product on its own. Stopping after any phase
should leave something worth using. The quality bar is the moat; breadth
without it is just another link farm.

**Rule to hold:** no topic ships without a visualisation or a concrete worked
example. If a concept cannot be shown, it probably needs to be split into ones
that can.

---

## Naming

`Algo Atlas` stopped fitting once the app grew a Pomodoro timer and a reading
mode alongside the DSA content — decided and executed: **`Stacks`**.

`Atlas` was the earlier front-runner (see history below) on the argument that
it scales cleanly as the app widens past algorithms. `Stacks` won instead for
the double meaning: a stack is already a topic in the curriculum, and "stacks
of books/reading" fits the direction toward general study and reading tools,
not just algorithms. Package is now `dev.mks.stacks`.

Alternatives considered and passed over:

| Name | Argument | Against |
|---|---|---|
| **Atlas** | Collection of maps you consult, dipped into rather than read cover to cover; carries over from the old name. | Reads as reference-only; doesn't hint at the focus/reading tools alongside the content. |
| **Bedrock** | Foundations, the layer everything sits on. Strong and concrete. | Slightly heavy; suggests only basics, not LLMs or agents. |
| **Lodestar** | What you navigate by. Memorable, uncommon. | Less obviously a reference work. |
| **Primer** | Exactly the genre — the first thing you read on a subject. | Generic, hard to search for, many products use it. |
| **Keystone** | The piece that holds an arch together. | Overused in enterprise software naming. |

Avoid: anything with "Codex" (collides with OpenAI), "Dev" or "Hub" (generic),
or a name containing "AI" (dates instantly, and this is not an AI product).

---

## Content architecture

Six topics fit in a flat chapter list. Two hundred do not. Two changes are
needed before the content grows, and doing them late is much more expensive
than doing them early.

### 1. Add a `Track` level above `Chapter`

```
Track          Chapter              Topic
─────          ───────              ─────
Foundations    Data Structures      Arrays, Linked Lists, Hash Tables…
               Algorithms           Binary Search, Merge Sort, BFS…
               Complexity           Big-O, Amortised Analysis…

Systems        Concurrency          Threads, Locks, Async…
               Networking           TCP, HTTP, TLS…
               Storage              Indexes, Transactions, Caching…

AI / ML        Fundamentals         Gradient Descent, Overfitting…
               Neural Networks      Backprop, Attention, Transformers…
               LLMs                 Tokenisation, Context Windows, RAG…
               Agentic Coding       Tool Use, Planning, Evals…

Craft          Performance          Profiling, Cache Locality…
               Practices            Testing, Code Review…
```

Tracks become the top-level browse in Learn. This is additive — `Chapter`
gains a `track` field, and the existing six topics land in Foundations.

### 2. Move content out of Kotlin, into bundled data

Today each topic is a compiled Kotlin file. That is genuinely nice for six —
type-safe, refactorable, no parsing. It stops being nice around fifty:

- Every content typo is an app rebuild and a release.
- Compile time grows with prose, which is absurd.
- APK size carries all content whether read or not.
- Non-code contributions (yours, from a laptop, in a text editor) are awkward.

Move topics to **bundled Markdown with YAML front matter**, parsed at startup
and cached. Keep the same `Topic` model — only the source changes.

```
content/
  foundations/
    arrays.md
    linked-lists.md
  ai-ml/
    attention.md
```

Frame generators stay in Kotlin — they are code, and they should be.

This unlocks the thing that matters later: **content updates without an app
release**, by fetching a newer content bundle and falling back to the shipped
one. Do the file move early; add remote fetch only when it is actually needed.

---

## Phases

Each phase should stand alone as a good product.

### Phase 1 — Finish the DSA core *(next)*

Complete what the app already claims to be. Roughly 30 more topics:

- **Structures**: stacks & queues, trees, BSTs, heaps, tries, graphs, union-find
- **Algorithms**: DFS, quicksort, Dijkstra, topological sort, sliding window,
  two pointers, backtracking
- **Dynamic programming**: memoisation, tabulation, knapsack, LCS, edit distance
- **Complexity**: Big-O properly, amortised analysis, space-time trade-offs

Three of these already have finished scenes waiting: stacks & queues, DFS, and
the coin-change DP table.

**Ships as:** a genuinely complete interview-prep and CS-refresher app.

### Phase 2 — Structural work

The `Track` level, the Markdown content move, splash screen, and the rename.
No new topics — this is the phase that makes the next 150 affordable.

**Ships as:** the same app, ready to grow.

### Phase 3 — AI / ML / LLM track

The differentiator. Almost nothing explains this material *visually* and
concisely for engineers who can already code:

- **Fundamentals**: gradient descent, loss functions, over/underfitting,
  train-test split, embeddings
- **Networks**: backprop, CNNs, RNNs, attention, transformer architecture
- **LLMs**: tokenisation, context windows, temperature and sampling,
  quantisation, fine-tuning vs RAG, prompt caching
- **Agentic coding**: tool use, planning loops, context management, evals,
  failure modes

The visualisation engine is a strong fit here — attention weights are a matrix
(the `Matrix` renderer already exists), embeddings are points in space, and
gradient descent is a path down a surface. Two new renderers would be needed:
**heatmap** and **2D scatter/surface**.

**Ships as:** the reason someone picks this over every other reference.

### Phase 4 — Systems and craft

Concurrency, networking, storage, performance, testing. Broadest and least
urgent — most of it is well covered elsewhere, so only add topics where a
visualisation genuinely beats the prose that already exists.

---

## Feature specs

### External links — use Custom Tabs, not an embedded WebView

**Your instinct is right, the mechanism needs changing.** An embedded WebView
will not work well here, for four reasons:

1. **Medium blocks embedding.** `X-Frame-Options` / CSP means the basecs links
   — the ones the app cites most — would render as a blank or refused frame.
   Same for many docs sites.
2. **There is no common WebView in Compose Multiplatform.** Android has
   `WebView`, iOS has `WKWebView`, desktop JVM needs JCEF/KCEF (a heavyweight
   dependency, ~100 MB), and Wasm cannot iframe most origins at all. That is
   four implementations, one of which is bad and one impossible.
3. **Logged-in content breaks.** Paywalled Medium articles, private docs, and
   anything behind auth will not carry the user's session.
4. **Reader experience is worse** — no password manager, no extensions, no
   reading list, no share sheet.

**Use the platform's in-app browser instead**, which is purpose-built for this:

| Platform | Mechanism |
|---|---|
| Android | **Chrome Custom Tabs** (`androidx.browser`) |
| iOS | **`SFSafariViewController`** |
| Desktop | System browser (current behaviour) |
| Wasm | New tab (current behaviour) |

These keep the user visually inside the app, share the browser's cookies and
logins, and cost roughly a day to wire up behind the existing
`rememberUrlOpener` expect/actual. That abstraction is already in place, so
this is a drop-in change.

**Where a real WebView does earn its place** is offline reading — capturing an
article for later. That is a different, bigger feature (fetch, sanitise, store,
render) and worth doing only if the reference-plus-link model proves
insufficient. Revisit after Phase 3.

### Search fallback to the web

When an in-app search returns nothing, offer the way out rather than a dead end:

```
Nothing on "kubernetes operators" yet.

  🔍  Search the web                    → Custom Tab, prefilled query
  📚  Look on Wikipedia                 → curated, usually decent for CS
  💬  Ask on Stack Overflow             → for concrete problems
  ✍️  Suggest this topic                → logs it locally
```

The last item is the valuable one: a local list of what you searched for and
did not find *is* the content backlog, ranked by real demand. Surface it in
settings as "Topics you looked for" — that tells you what to write next far
better than guessing.

No search API needed — this is just a URL with an encoded query.

### Splash screen

Use the Android 12+ `SplashScreen` API rather than a drawn screen, so it shows
during process start instead of adding time to it. Keep it under 800 ms and
never add an artificial delay.

Design: the app mark on the background colour, with the icon animating in.
Given the "Stacks" name, a mark built from stacked bars or plates would suit —
abstract, no literal book icon.

Also needed at the same time: **adaptive launcher icon** (foreground/background
layers), monochrome variant for themed icons, and a proper app label.

### Smaller items

- **Bookmarks** — filter chip in both Learn and Practice, not a third tab.
  Needs persistence: DataStore on Android, `NSUserDefaults` on iOS, a file on
  desktop, `localStorage` on Wasm. One expect/actual.
- **Progress** — mark topics read; a strip atop Learn. Same persistence layer.
- **Company filter in Practice** — `askedAt` is already in the model.
- **Deep links** — `stacks://topic/binary-search`, so notes and messages can
  point at a page. Needed before any sharing feature.
- **Text scaling** — respect the system font size; currently untested.
- **Offline is already true** and worth keeping: everything ships in the
  bundle, no network required except for outbound links.

---

## Suggested order

1. Finish DSA core (Phase 1)
2. Custom Tabs + search fallback + suggestion log — small, high value
3. Track level + Markdown move + rename + splash (Phase 2)
4. Bookmarks and progress
5. AI/ML/LLM track (Phase 3) — the differentiator
6. Systems and craft (Phase 4), selectively

Items 2 and 4 are each a day or two and make the app meaningfully better to
live with. The content phases are the long pole, and always will be.
