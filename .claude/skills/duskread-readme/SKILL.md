---
name: duskread-readme
description: Use when README.md needs to catch up with the app — a new feature
  shipped, a module moved, a dependency or SDK level changed, or the README
  simply describes something the code no longer does. Covers what belongs in
  the README versus architecture.md, the landing page and CLAUDE.md, the
  section-by-section rules, the voice it is written in, and the facts to
  re-derive from source rather than trust. Load it before editing README.md.
---

# Updating the DuskRead README

The README is the **prose front door**: what the app is for, what it does,
how to run it. It is not a spec, not a module map, and not a changelog. Every
paragraph should still make sense to someone who will never open the code.

## The four documents, and the line between them

| File | Answers | Never carries |
| --- | --- | --- |
| `README.md` | What is this, why would I use it, how do I run it | Class names, flow diagrams, schemas, token tables |
| `docs/architecture.md` | How the pieces connect — Notion schema, on-device storage, the end-to-end flows, invariants | Sales prose |
| `docs/design-system/design-system.html` + `design-tokens.md` | What it looks like and every value behind it | Code paths, TODOs |
| `CLAUDE.md` | How to work in the repo — Gradle task names, lint rules, conventions | Feature description |

So: a new feature earns **a paragraph** in the README and **a flow** in
`docs/architecture.md`. If the README edit is starting to want a table of
fields or a sequence of steps, it belongs in the architecture doc, and the
README links to it. `CLAUDE.md` is explicit that the README describes how the
app is put together and must not be restated there — the reverse holds too.

## Sections, and what each is allowed to say

The order is fixed. Don't add top-level sections; fit new material into these.

```
masthead        mark, name, standfirst, badges, the monochrome sub-line
opening scene   one paragraph, second person, a day with the app
Getting started clone + installDebug, the requirements table, the collapsed
                "other three targets" block
What it does    one ### per pillar, bold-lead bullets underneath
How it's built  Platform support, then Tech stack, then the lint commands
Design system   two schemes in two sentences, then a link to the page
Architecture    three lines and a link to docs/architecture.md
Licence         MIT
```

**The opening paragraph is the piece to protect.** It is a single scene —
morning share, evening pile, the walk, the summary, the timer, the theme — in
second person, present tense, no feature names in bold, no bullets. A new
feature joins it only if it changes the shape of the day. Most don't; they go
in *What it does*. If you edit it, keep it one paragraph and read it back
whole.

**What it does** is the pillar list from `CLAUDE.md` — saved links and feeds
(`links/`), readback (`reader/`), summaries (`summary/`), the focus timer
(`pomodoro/`). A genuinely new pillar gets its own `###`; anything smaller is
a bold-lead bullet under the pillar it belongs to. Each bullet leads with the
decision, not the mechanism — "**The grant persists.**", not "Uses SAF".

**Tech stack** is one paragraph of prose with inline links, not a
dependency list. Adding a library means adding a clause, or replacing one.
Drop the clause when the dependency goes.

## Facts to re-derive, never to trust

The README states numbers that drift. Check each against source before
leaving them as they are:

```bash
grep -nE 'app|agp|android-(min|target|compile)Sdk|kotlin|composeMultiplatform|ktor' gradle/libs.versions.toml
grep -n distributionUrl gradle/wrapper/gradle-wrapper.properties
grep -rn jvmToolchain --include=build.gradle.kts .
```

- **Requirements table** — JDK, `minSdk`/compile/target, the Gradle version.
- **Tech stack paragraph** — Compose Multiplatform, Kotlin and Ktor versions.
- **Badges** — the `Android 12+` badge tracks `android-minSdk`.
- **"No tests yet."** — still true while `commonTest` has no sources
  (`CLAUDE.md`); if that changes, this line and `CLAUDE.md` change together.
- **Module names in prose** — the pillars map to real directories under
  `composeApp/src/commonMain/kotlin/dev/mks/duskread/`. `ls` it; a package
  present there and absent from the README is the usual reason to be editing.
- **Every link resolves** — relative paths (`docs/architecture.md`,
  `docs/media/duskread-mark.svg`, `LICENSE`) and the anchor
  `#platform-support`.

## Voice

Same house style as the code comments (`CLAUDE.md`):

- **British spelling in prose** — colour, summarised, behaviour, licence.
- Second person, present tense, plain declaratives. No "seamlessly",
  no "powerful", no "simply works", no exclamation marks.
- Bold leads a bullet; it does not sprinkle mid-sentence.
- Say what was decided and why it was decided that way. The README's whole
  register is a person explaining their own tool, not a product page.
- Hard-wrap only where the file already does — the "How it's built" prose is
  wrapped near 80 columns, the *What it does* paragraphs are single long
  lines. Match the block you are editing rather than reflowing it.

## Finishing

```bash
grep -oE '\]\([^)h#][^)]*\)' README.md | tr -d '])(' | while read -r p; do
  [ -e "${p%%#*}" ] || echo "broken: $p"; done
```

Then read the diff top to bottom as prose. A README change is finished when
the file still reads as one voice — not when the new sentence is factually
correct in isolation.

Commit with a sentence-case imperative subject, no `docs:` prefix
(`CLAUDE.md`) — e.g. "Bring the README in line with Notion sync".
