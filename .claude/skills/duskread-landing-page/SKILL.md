---
name: duskread-landing-page
description: Use when editing docs/design-system/design-system.html — the DuskRead
  landing page and visual-language reference — or docs/design-system/design-tokens.md
  beside it. Covers the four-act structure, the deck (slider) component and how to
  add a slide, what belongs on the page versus in the tokens file, the motion rules
  the page is built to, and how to actually check a change by rendering it. Load it
  before touching either file; it is not about the Compose app (that is
  duskread-design-system).
---

# The DuskRead landing page

`docs/design-system/design-system.html` is one file: the product landing page
and the visual-language reference in the same document. No build step, no
dependencies, no framework. Open it in a browser.

It is **not** the app's design-system skill. For Compose work — components,
tokens in `ui/theme/`, the one-accent rule as it applies to screens — load
`duskread-design-system` instead.

## The two-file split

| | Holds | Rule |
| --- | --- | --- |
| `design-system.html` | The argument. Mockups, the mark's construction, the lightness ramps, the glyph wall, the type specimen. | Things you look at. |
| `design-tokens.md` | The reference. Every hex, size, radius, duration and layout value, with the file it lives in. | Things you look **up**. |

**A table of values does not belong on the page.** When a section starts
growing columns of numbers, that is the signal to move it. The colour token
table and the whole wide/desktop breakpoint spec both left for exactly this
reason, and the page links out instead.

The inverse also holds: do not put a diagram in the markdown. It cannot draw.

Neither file may carry code paths as *structure* — no class names as headings,
no TODO lists. Open work lives in `docs/design/amplitude-migration.md`.

## Structure — four acts

```
masthead          lockup, headline, standfirst, act nav
01 · brand        laid out, not a deck: lockup + name, the mark built in
                  three steps, the size ramp, the splash
02 · walkthrough  one deck, 13 slides — every screen in the order you meet them
03 · design system one deck, 5 slides — monochrome, colour, type, icons,
                  shape and motion
04 · get it       clone, Gradle task, platform table
footer            "Built with AI", source links, MIT
```

Acts are `<section class="act">` with an `<h2>` eyebrow and a `.act-title`.
Blocks inside them are `<div class="subsection">` with a `.sublabel`.

**Do not stack three levels of heading in one act.** An eyebrow *and* a title
*and* a note for one row of icons is chrome restating the act title above it.
Act one carries exactly one sub-heading (`The name`) because it labels the
second column of a two-column row; everything else there runs as continuous
prose. That was a deliberate cut, not an omission.

## The deck

The slider. One component, two instances, driven by `[data-deck]`.

```html
<div class="deck" data-deck data-autoplay="6500">
  <div class="deck-window">
    <div class="deck-track">
      <article class="deck-slide">            <!-- first slide: no inert -->
        <div class="v-amp deck-figure"> … </div>
        <div class="deck-copy">
          <p class="num">01 / 13 &nbsp;&middot;&nbsp; Onboarding</p>
          <h3>…</h3>
          <p>…</p>
          <p class="rationale">…</p>
        </div>
      </article>
      <article class="deck-slide" aria-hidden="true" inert> … </article>
    </div>
  </div>
  <div class="deck-bar">
    <button class="deck-nav deck-prev" …><svg class="i"><use href="#w-back"/></svg></button>
    <div class="deck-dots" role="tablist" …>
      <button class="cdot is-on" aria-label="…"></button>
      …one per slide…
    </div>
    <span class="deck-count" aria-live="polite">01 / 13</span>
    <button class="deck-nav deck-next" …><svg class="i"><use href="#w-chev"/></svg></button>
  </div>
</div>
```

**To add a slide:** append an `<article class="deck-slide" aria-hidden="true"
inert>`, add one `.cdot`, and renumber every `.num` line. The script fixes
`inert`, `aria-hidden`, the dots and the counter on load, so getting the
initial attributes slightly wrong is survivable — but the `.num` lines are
static text and are not.

Numbers are two digits on both sides (`03 / 13`, never `3 / 13`) so a slide's
own number matches the counter beneath it.

It is a **filmstrip, not a stack of cross-fading cards** — that is what lets a
finger drag it. Do not "simplify" it into stacked slides with an opacity
swap; that silently removes the gesture.

What the script already does, so you do not rebuild it: 1:1 pointer drag with
`setPointerCapture`, 10px axis hysteresis so a vertical scroll that starts on
the deck stays a scroll, release-velocity projection
(`(v/1000)·d/(1−d)`, `d = 0.99`) to pick the landing slide, rubber-banding at
both ends, arrows, dots, keyboard left/right, `inert` on off-screen slides,
and autoplay that pauses on hover, sleeps off-screen and **stops permanently
on first interaction**.

### Column widths

The two decks size their figure column differently, on purpose:

- **Walkthrough** — `max-content minmax(280px, 540px)`. Every figure is a
  320px phone.
- **System** — `fit-content(760px) minmax(250px, 330px)`. Figures range from a
  320px phone to a 760px glyph wall; `fit-content` shrinks to the small ones
  and caps at the large ones, so the copy sits beside its figure on every
  slide rather than beside the widest one.

## The mockups are real token values

Everything inside `.v-amp` uses the app's own tokens (`--a-bg`, `--a-ink`,
`--a-sound`, …) mirrored from `ui/theme/Theme.kt`, and every glyph is a
`<use>` of the shipped path data from `DuskReadIcons.kt`. The frames are the
app, not a wireframe of it. **If a token changes in Kotlin, change it here in
the same pass** — a mockup that has drifted is worse than no mockup.

`.v-amp.mono` pins one frame to Ink permanently; `body.ink` (the switch in the
top corner) repaints every other frame at once.

## Motion

The page follows `apple-design` and `animate` (both in `.claude/skills/`).
Load them before adding motion; the short version:

- **Easing tokens exist — use them.** `--ease-out` for entering, leaving or a
  press; `--ease-in-out` for something on screen moving to a new place; plain
  `ease` for a colour change. Never invent a `cubic-bezier`.
- **`transform` and `opacity` only.** The one deliberate exception is the
  carousel dot's `width`, commented in place: the dot lengthens rather than
  lighting up, because Ink has no hue to light it with, and a `scaleX` on a
  7px pill flattens the ends it is drawn for.
- **UI motion stays under 300ms.** Marketing motion (the scroll reveal, a deck
  transition) may run longer.
- **Reduced motion is gentler, not none.** The block near the end of the
  stylesheet drops movement and keeps colour and opacity — the scheme swap
  without its cross-fade is a hard cut across fourteen frames at once, which
  is the jarring change the transition exists to prevent. Anything new goes in
  that block, not into a blanket `transition: none`.
- **The page accent follows the scheme switch.** Terracotta under Paper Black,
  drained to grey under Ink, so the page demonstrates the app's one-accent
  rule instead of only stating it.

## Checking a change

Compiling proves nothing, and neither does reading the diff. Render it:

```bash
CH="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
P="$PWD/docs/design-system/design-system.html"

# full page, then crop the part you changed
"$CH" --headless --disable-gpu --hide-scrollbars --window-size=1280,10000 \
      --screenshot=/tmp/page.png --virtual-time-budget=3000 "file://$P"

# did the scripts actually run?
"$CH" --headless --disable-gpu --dump-dom --virtual-time-budget=2500 "file://$P" \
  | grep -c 'inert=""'
```

Also worth running on any structural edit — this has caught real breakage
more than once:

```bash
python3 - <<'EOF'
from html.parser import HTMLParser
import io, re
VOID = {'meta','link','br','hr','img','input','use','path','rect','circle','source'}
class P(HTMLParser):
    def __init__(s): super().__init__(convert_charrefs=True); s.stack=[]; s.err=[]
    def handle_starttag(s,t,a):
        if t not in VOID: s.stack.append((t, s.getpos()))
    def handle_endtag(s,t):
        if t in VOID: return
        if not s.stack or s.stack[-1][0] != t: s.err.append((t, s.getpos())); return
        s.stack.pop()
src = io.open('docs/design-system/design-system.html', encoding='utf-8').read()
p = P(); p.feed(src)
print('errors', len(p.err), 'unclosed', len(p.stack))
ids = set(re.findall(r'id="([^"]+)"', src))
print('dangling:', sorted(l for l in re.findall(r'href="#([^"]+)"', src)
                          if l not in ids and not l.startswith('w-')))
EOF
```

Two things to check by eye, because code cannot tell you: the page in **both
schemes** (flip the switch — an accent that only reads in one is a bug), and
the page at **narrow width** (the decks and the two-column rows collapse at
1120px and 900px respectively).

## Publishing

The page deploys to GitHub Pages from `.github/workflows/pages.yml` on any
push to `main` that touches `docs/design-system/**`. The workflow copies
`design-system.html` to `index.html` and rewrites the relative
`design-tokens.md` link to the GitHub blob URL, so the same file works both
locally and deployed. Nothing about the page needs a build step; keep it that
way.

## House style for the prose

Match the file. It is opinionated and short: a bolded claim, then the reason,
then stop. British spelling. Every `.rationale` leads with `<b>the claim</b>`
so the page scans on the bold alone. If a paragraph is explaining an
implementation detail rather than a design decision, it belongs in a code
comment or in `design-tokens.md`, not here.
