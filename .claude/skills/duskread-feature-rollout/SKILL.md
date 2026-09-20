---
name: duskread-feature-rollout
description: Use after a user-visible feature ships in this repo — a screen
  redesigned, a section added or removed, a flow renamed — to find and update
  everywhere else that feature is described: docs/architecture.md, README.md,
  and the landing page's product mockup (a separate repo, mksbrew). Load this
  before considering the work done, not just before touching code.
---

# Rolling a shipped feature out past this repo

A change to `composeApp/` is not finished when it compiles and looks right on
device. Three other things describe the same feature and go stale silently —
nothing fails when they do, which is exactly why they drift. Work through all
four in order; each one's context is cheap while the feature is still in your
head and expensive once it isn't.

## The four places, in the order to update them

1. **The code itself** — inline KDoc, why not what. See `duskread-code-docs`.
2. **`docs/architecture.md`** — only if the change crosses files or is
   something a future reader needs to know before opening any of them (a new
   `KeyValueStore` key, a sequence spanning several modules, an invariant).
   See `duskread-code-docs` for the exact trigger table.
3. **`README.md`** — only if the change touches what's in *"What it does"*,
   or contradicts the opening scene's characterisation of the app. Most
   changes don't reach here. See `duskread-readme`.
4. **The landing page** — `sites/duskread/` in the **separate `mksbrew`
   repo**, not this one. See below; this is the step actually worth loading
   this skill for, because it's easy to forget it exists.

Steps 2 and 3 have their own skills with their own trigger rules — load them
rather than re-deriving when to write in each. This skill's job is making
sure step 4 happens at all, and telling you how to do it without breaking
something that has broken before.

## Step 4: the landing page lives in another repo

`https://duskread.mksbrew.dev` is **not this repo**. Its source is
`sites/duskread/index.html` in `~/Desktop/C0D3/mksbrew` (or wherever that
clone sits — `find ~/Desktop -maxdepth 3 -type d -name mksbrew`). Before
touching it:

- **Read `mksbrew`'s own `CLAUDE.md`** and, if the change is visual,
  `.claude/skills/design-system/SKILL.md` in that repo — both carry
  rejected-idea history and layout constraints specific to that page that
  this repo knows nothing about.
- **The page is a product pitch, not a design-system showcase** (rewritten
  Sep 2026) — problem → benefits → proof → demo → build → close. A feature
  earns a place in the hero mockup and, if it changes what the app promises
  to do, a line in the benefits grid or the hero standfirst. It does not
  earn a new section.
- **The hero phone mockup is hand-built HTML/CSS**, not a screenshot and not
  generated from this repo. Nothing updates it automatically. Find the
  slide under `.deck-slide` whose markup names the screen you changed and
  edit it by hand, matching the app's real copy and structure — chip radius,
  eyebrow labels, meta line — not by eyeballing "close enough."
- **The phone canvas has a fixed budget: 612px** (`320×660` phone,
  `overflow: hidden`, 76px reserved for the nav bar). Content that fits on
  the real, scrolling Home screen will not fit here. Cut items, don't shrink
  type or spacing to fake it fitting — check by injecting a script that
  reports `.deck-slide .canvas` `scrollHeight`/`clientHeight` and reading it
  back with `--dump-dom`.
- **The deck autoplays every 6500ms.** A headless screenshot with a
  `--virtual-time-budget` at or past that catches the next slide, and you'll
  spend a round convinced slide one is broken. Strip `data-autoplay` in the
  scratch copy you screenshot.
- **Verify per that repo's own recipe** (`portfolio-conventions` skill,
  "Verifying changes"): serve with `bun run dev`, never `file://`; force
  `[data-reveal]{opacity:1!important}` in a scratch copy; screenshot at
  1280w and, through the `_probe.html` iframe technique, at a true 390px
  viewport. Delete scratch files before finishing.
- **Check `bun run check`** before calling it done — it parses every script
  and loads every post; a syntax error here is a blank page a visitor finds,
  not a failed build.
- **Commit and deploy are separate, and both need the user's word.** That
  repo's own convention: commit to `main` only when asked, and its deploy
  workflows are `workflow_dispatch:` only — pushing to `main` does not put
  anything live. Say plainly what state you've left it in (uncommitted /
  committed-not-deployed / deployed) rather than assuming either.

## What this looked like the one time it was done end to end

Home was redesigned (Focus → Latest → Recommended) across
`composeApp/`+`iosApp/`. The rollout was: a "What Home offers" section in
`docs/architecture.md` (crossed files, a new refusal worth recording); the
README's opening scene and a new pillar bullet (it changed what the first
screen does); then, in `mksbrew`, the hero deck's Home slide rebuilt with a
new `.card` component mirroring `ArticleCard.kt`, the hero standfirst and one
benefit card's copy corrected to match, and a dead doc link
(`docs/design-system/design-tokens.md`, moved away in an earlier pass)
found and removed rather than repointed to a non-existent replacement. Each
of the four got exactly what it needed and no more — the architecture doc
didn't get sales copy, the landing page didn't get a class name.
