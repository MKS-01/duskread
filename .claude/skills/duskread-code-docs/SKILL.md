---
name: duskread-code-docs
description: Use when writing or fixing the comments on code — a new file or
  public type that needs its KDoc, an inline note justifying a non-obvious
  line, or a pass over a file whose comments have drifted from what it does.
  Covers the house style (why, not what), where a comment stops and
  docs/architecture.md starts, and how to keep that doc in step when a
  decision outgrows the file it lives in.
---

# Documenting DuskRead's code

Two places, one rule for choosing between them:

- **In the code** — why *this file, type or line* is the way it is. Local,
  point to point, read by whoever is already looking at it.
- **`docs/architecture.md`** — a decision that spans files, or that someone
  needs to know *before* opening any of them. Cross-cutting, and kept current.

If explaining a line needs a second paragraph about another module, it is not
a comment any more. Write one sentence in the code and put the argument in
the architecture doc.

## In the code

`CLAUDE.md` sets the style; this is how it looks in practice.

**Every file and public type carries a prose KDoc saying why it exists.**
Never a restatement of the signature — "Returns the canonical URL" is noise
beside `fun canonicalUrl`. `links/CanonicalUrl.kt` is the model: what the
thing is for, the problem that forced it, the rule it lives by in bold, then
the road not taken and why.

**Inline `//` comments justify a choice or flag a hazard**, and sit directly
above the line they are about:

```kotlin
// Resolution is a network call and can fail; the raw address is still
// worth following, since fetchFeed may well accept what discovery
// could not confirm.
val resolved = runCatching { discoverFeedUrl(http, source.feedUrl) }.getOrDefault(source.feedUrl)
```

Good triggers for one: a value that looks arbitrary (why 900 characters, why
350 ms), an ordering that matters, a refusal (why this does *not* do the
obvious thing), a workaround for something outside this repo.

**Mechanics:**

- British spelling in prose — colour, behaviour, normalised, amortised.
  Identifiers stay American.
- Hard-wrap comments at 80–90 columns. Content strings do not wrap.
- Match the surrounding density. It is the house style, not decoration — a
  file with a comment every few lines should not gain a bare patch, and one
  that reads cleanly should not gain three restatements of its own code.
- No `TODO`. Open work goes in an issue or a commit message, not in a comment
  nobody sweeps.

**Do not write:** a comment that repeats the line, a changelog ("was X, now
Y" — that is what git is for), a name in place of a reason, or a block that
will be wrong the first time the code changes.

## When it belongs in `docs/architecture.md`

Escalate when the answer is any of these:

| Signal | Section it lands in |
| --- | --- |
| Data crosses a plane — Notion, device, readback | *Where data lives*, or a Flow |
| A new Notion property is read or written | *Notion schema* |
| A new `KeyValueStore` key, or a record gains a field | *On-device storage* |
| A sequence spanning several files | a Flow diagram |
| Something that must stay true everywhere | *Invariants* |
| A new file or package | *Module map* |

The doc is written for someone who has just cloned the repo and wants to know
where things live before changing them — keep that reader. Its conventions:

- Prose plus **ASCII diagrams**, no images, no code listings beyond a line or
  two. Flows are drawn top to bottom with `│ ▼ ├─`.
- **Say what it refuses to do**, not just what it does. Half the doc's value
  is in the refusals — a delete is a delete, an unchanged row is never
  rewritten, a canonical URL is never persisted.
- Tables for schemas and keys; prose for reasoning.
- A new `##` section must be added to the **Contents** list at the top, with
  a matching anchor.

Cross-link rather than repeat: the code says "see [Same article, different
address]" once; the doc holds the argument.

## Keeping it in step

The doc drifts silently — nothing fails when it is wrong. So a change that
touches any row of the table above updates `docs/architecture.md` **in the
same commit**, not later.

Cheap checks after editing either side:

```bash
grep -n '^## ' docs/architecture.md                  # every one in Contents?
ls composeApp/src/commonMain/kotlin/dev/mks/duskread/*/   # every file in Module map?
grep -rn 'const val\|\.get(' composeApp/src/commonMain/kotlin/dev/mks/duskread/data/  # keys still as listed?
```

When code and doc disagree, the code wins — fix the doc, don't fix the code
to match the doc. And when the doc's own claim is a measurement (165 cached
posts, ~75 s sync, 134 of 165 offline), either re-measure it or leave it and
say nothing; do not adjust a number by guessing.

`README.md` is a third audience again — see `duskread-readme` for what stays
out of it.
