# Amplitude migration — progress

Tracking the app's migration to the Amplitude direction from
`docs/design/redesign-variants.html`, screen by screen. Terracotta and
monochrome are the only two palettes kept — no cyan "signature" accent.

This file is the state of what is **built**. One unbuilt feature has a plan
of its own: `docs/design/home-discovery.md`, on making Home's pick a ranking
over saved links and followed-blog posts together, with on-device topic
tagging behind it where the device has a model for it.

## Already done (previous pass)

- [x] Icon set redrawn in the Bar hand (18 icons, all stroked)
- [x] Jost (UI) + Inconsolata (data) fonts, replacing Space Grotesk + `FontFamily.Monospace`
- [x] `WaveformMeter` shared component (bars filling left-to-right)
- [x] `MonogramBadge` replacing the Feather-icon topic guesser; dependency removed
- [x] `Radius.Card`/`Panel`/`Inline` tightened (20dp → 14dp etc.)
- [x] In-app browser WebView background matches the theme ground

## Corrected understanding from the mockup (this pass)

The previous pass kept **boxed cards** (background + border + corner radius)
and added an eyebrow label with a rule *underneath* it, inside the box. The
mockup's actual Amplitude spec is different:

- **No card boxes at all.** Sections sit flush on the screen background.
  Rows are separated by their own bottom hairline, not a container.
- **Eyebrow = label + inline trailing rule on the same line.**
- Rows carry a **sourcechip** (monogram) + title + a **two-fact mono meta
  line** — nothing else per row.
- A **per-row waveform** is real, not decorative: dim/flat by default, and
  partially filled in the accent for the currently-playing row, which is the
  *only* coloured thing on that screen.
- **Empty states drop the icon badge entirely** — a flat zero-height
  waveform stands in for "no signal".
- **Following becomes a digest, not a carousel.**

## Task list

### Foundation
- [x] `EyebrowHeader` rewritten: label + inline trailing hairline, one row
- [x] Flat divider-row style (bottom hairline, no box) — Home, Readback, Saved
- [x] `EmptyState`/`CompactEmptyState` rewritten: flat zero waveform, no icon badge, bottom-anchored via caller alignment

### Home (`DashboardTab.kt`)
- [x] Boxed cards dropped entirely — four flat sections
- [x] "FROM SAVED": sourcechip + title + host meta
- [x] "TODAY'S READBACK": title + duration (mono) + a static preview waveform
- [x] "FOCUS": bare bordered pills when idle; mono clock + waveform-free status line when running
- [x] "FOLLOWING": digest (see below)
- [x] Settings icon is a bare glyph, no circle chrome

### Readback (`ReaderTab.kt`)
- [x] Flat rows, bottom hairline, no card box
- [x] Sourcechip (monogram) per row
- [x] Meta line: duration + word count only (mono), date/mode/voice dropped
- [x] Per-row waveform: dim by default; playing row fills to position, title + duration switch to accent
- [x] Pill row (Newest / Oldest / Folder) restyled to bordered `.pill` across all four platforms (`Reader.android.kt`, `Reader.desktop.kt`)

**Cut, not carried forward** (flagged for visibility, not silently dropped):
the read-along highlighted-excerpt view and the "Read original" external-link
row. Neither appears in the mockup's row, and the row is now two facts and a
waveform, full stop. Both were real, working features — say the word if you
want either reinstated as a deliberate addition beyond the mockup.

### Saved (`LinksTab.kt`)
- [x] "Paste a link" is a flat, full-width bordered pill (no filled background)
- [x] Flat rows, no card box, bottom hairline
- [x] Sourcechip + title + meta (host, time-ago) — description line dropped
- [x] Read rows: 0.5 alpha + trailing tick; unread rows carry no trailing icon at all (matches the mockup exactly)
- [x] Section eyebrows: "UNREAD · N" / "READ · N" (muted tint for Read)

### Following digest (`FollowingSection.kt` → exposes `FollowingDigest`)
- [x] Carousel replaced with a digest line per feed: `host — N new` (mono, accent when > 0)
- [x] Feed management (add/remove/sync) kept behind the existing "Manage" toggle
- [x] Per-post save-to-Saved kept via progressive disclosure: tapping a digest line expands the original post carousel in place, rather than losing the feature outright. This is a deliberate addition beyond the static mockup, not a literal reproduction of it.
- [x] "N new" is a proxy (posts not yet saved) — there's no real seen/unseen tracking in the data model, and building one would be a new feature, not a restyle

### Focus (`FocusScreen.kt`)
- [x] Eyebrow is inline-rule style ("FOCUS" / "FOCUS · N MIN")
- [x] Pill-row controls (Pause/Reset/length picker) restyled to bordered `.pill`, uppercase mono
- [x] Bottom-anchored layout, mono digits, waveform meter (already done previous pass)

### Sweep
- [x] Re-checked Home/Readback/Saved for leftover boxed-card styling — only the Following carousel's post cards and the feed-management list remain boxed, both deliberate secondary surfaces, not part of the primary flat-row language
- [x] Confirmed only terracotta (`DarkScheme`) and monochrome (`MonoScheme`) exist — no third palette
- [x] ktlint clean, desktop compile clean, Android compile clean
- [x] On-device screenshots: Home, Readback (with real reads, one playing), Saved (empty + populated), Focus running, Settings

### Settings screen + one reusable input (follow-up)
- [x] `AppTextField` — the one text-field shape in the app: hairline border, `Radius.Inline` (10dp) corners, never a pill. Supports single/multi-line, mono, optional trailing action, and `textAlign` for the centred onboarding case.
- [x] Every `BasicTextField`/`OutlinedTextField` in the app now goes through it: Saved's paste field, the feed-follow field, Settings' name field and import box, the desktop folder-path field, and the onboarding name field
- [x] Settings screen rewritten flat (`EyebrowHeader` sections, no boxed cards) to match the rest of the app — it was the one screen still built the old way
- [x] ktlint clean, desktop + Android compile clean, verified on-device (Settings, Import panel)

## Waveform + sourcechip correction (this pass)

The meter and the source cell were both built as the wrong *kind* of object.

### `WaveformMeter`
- [x] **Fixed size, not a full-width track.** The mockup's `.wave` is 18 bars
      of 2px separated by 1.5px — a mark about a fifth of the row wide.
      Stretched to fill the row it became ~200 bars at a 2px pitch: a hatch,
      in which the height variation averages out and nothing reads as a
      waveform. The component now has an intrinsic width and callers pass
      only a height.
- [x] Bars are **squared-off round rects (3dp radius)**, not round-capped
      strokes — the same terminal shape as the icon set's butt caps
- [x] **Whole bars only.** A bar is accent or it is dim; the lerped boundary
      bar read as an anti-aliasing artefact at 2dp
- [x] **Per-item silhouettes.** Heights come from a hash of (seed, index)
      quantised onto the mockup's own eleven steps (5–15px in a 15px box),
      with a nudge that stops neighbouring bars clumping. Every row in the
      mockup has its own sequence; ours all drew the identical shape
- [x] Off-state bar is the meta grey held back to 55%, which lands on the
      mockup's `--a-wave-off` over this ground
- [x] Empty states draw the meter `flat = true` — a line of dots at zero,
      the "no signal" the mockup specifies. It had been drawing a full-height
      wave, which said the opposite of what the empty state meant

### `MonogramBadge` → the mockup's `.sourcechip`
- [x] A **square** with a `Radius.Chip` (3dp) corner and a hairline border,
      holding one mono capital — not a filled circle. A filled circle is an
      avatar, and an avatar promises a person or a brand mark; this is a data
      cell in the same hairline-and-mono vocabulary as the meta line beside it
- [x] The playing row tints its chip border with the row, as the mockup does

### Sweep
- [x] Readback's Newest/Oldest chips were still filled Material surfaces
      despite the previous pass's note — now bordered `.pill`s, selection
      carried by border and text colour alone. `Radius.Chip` added and used
      by the pills (all platforms) and the sourcechip
- [x] ktlint clean, desktop + Android compile clean, verified on-device
      (Readback idle, Readback playing, Home, Saved, empty state)

**Cut, not carried forward:** the floating bar's ambient background waveform.
The mockup's bar carries icons and nothing else, and at the row pitch it
moirés into a hatch across the pill while any coarser pitch collides with the
play control. The remaining-time readout already answers "how much is left".

## One row, three screens (this pass)

Saved, Readback and a followed blog's topics had each grown their own copy of
the flat row — same 22dp sourcechip, same 14/19 title, same 10.5sp mono meta,
same 15dp-hairline-15dp divider — written out three times. The differences
between them are real and stay real; the skeleton was never one of them, and
three copies of a number is three chances for one to drift.

- [x] `ui/common/ListRow.kt` holds the skeleton and every metric in it:
      `ListRow` for the normal case, `ListRowBody` + `ListRowDivider` for
      Saved, whose swipe-to-remove box needs the hairline to stay put while
      the row slides out from over it, and `RowMeta` for one fact on the meta
      line
- [x] `RowTone` names the three states a row can be in — `Normal`, `Accent`
      for the one playing read, `Faded` for a read link. Mutually exclusive,
      so an enum rather than two booleans
- [x] What stays per-screen: how many facts the meta carries (Readback's two,
      everyone else's one), what sits at the right end (bookmark / tick /
      play glyph), and Readback's waveform, which goes in through the row's
      `content` slot
- [x] Verified on device against all three: Saved unread and read, Readback
      idle and playing, the topics list and the digest preview

Not folded in: Home's "FROM SAVED" pick. It looks like a row but is one
per screen at a larger title and carries no hairline — a feature line, not a
list row, and forcing it through the same component would have meant a knob
that exists for one caller.

## Brand — app icon and splash (this pass)

The mockup's Brand section had never been built. The launcher icon was still
three fanned bookmark ribbons, whose fault the mockup names exactly: scale,
not concept — the artwork occupied about 15% of its canvas and dissolved into
a speck at 48px.

- [x] `ic_blogmark_mark.xml` rebuilt to the mockup's Amplitude mark: a level
      meter whose tallest bar is a bookmark, so the notch reads as both a
      ribbon tail and a peak. Fills roughly 70% of the canvas and folds *save*
      and *listen* into one silhouette
- [x] Terracotta, not the mockup's cyan, keeping the two-palette rule — and
      solid, where the mockup steps the bars down in alpha (0.45 / 0.7 / 1.0 /
      0.55). That gradient works on a 132px brand plate and fails on a
      launcher tile: over this near-black ground a bar at 45% barely separates
      from the ground, so the mark lost its outer half at the size it is
      actually seen. The four heights carry the meter without it
- [x] Geometry is the mockup's own, shifted 2 units left to centre it on the
      canvas and scaled 0.85 about the centre so nothing leaves the 66-unit
      safe zone a launcher mask and the splash icon both crop to
- [x] The launcher takes it in further still — `ic_launcher_foreground` is a
      13% `<inset>` around the same drawable. The safe zone is the most a mark
      may be, not the most it should be, and an icon running to the edge of
      its mask reads as cramped beside home-screen icons that all sit in more
      air than the mask requires. The splash has a whole screen and keeps the
      full size, so the two genuinely differ — one geometry, wrapped, not a
      second copy free to drift
- [x] One drawable, three consumers: the adaptive foreground now references
      `ic_blogmark_mark` directly rather than duplicating its path data, and
      no path data is duplicated anywhere. Only the monochrome layer is
      separate, because a themed icon needs one opaque colour; it carries the
      inset as a matching 0.74 scale so themed and colour icons sit in the
      same air
- [x] **Splash animates**, as the mockup's rationale specifies: the outer bars
      settle to rest while the app boots, staggered left-to-right the way the
      app's own waveforms fill. `ic_blogmark_splash.xml` is an
      `animated-vector` over the same mark; the bookmark does not move
- [x] No delay added. `installSplashScreen` still dismisses the window on the
      first frame, so a warm start cuts the settle off part-way through —
      the splash covers a wait, it does not impose one
- [x] iOS `AppIcon-1024.png` and the Wasm `favicon.png` regenerated from the
      same geometry, full-bleed (neither platform crops the way an adaptive
      icon does, so they skip the safe-zone scale)
- [x] The Wasm page's body background was `#0c0f14`, a bluish near-black left
      over from an earlier direction — now `#101010`, the app's own ground
- [x] Verified on device: launcher icon under the circular mask, and the
      splash caught mid-settle

Open:

- [ ] The splash was only ever seen on the emulator's modern API level. The
      platform splash screen is API 31+; below that the androidx library
      draws the icon itself, and whether it runs the AVD or shows the mark at
      rest is unverified. Either is acceptable — the resting mark is the base
      vector — but nobody has looked
- [ ] Desktop has no icon at all (`nativeDistributions` sets no `iconFile`)

## Where this stands (end of the waveform pass)

Everything ticked above is committed and pushed. What follows is the open
list, so the next session starts from a state rather than from a re-reading
of the diff.

### Verified on device
Readback idle, Readback playing, Home, Saved, Readback's empty state.
ktlint, desktop compile and Android compile all clean. There are no tests, so
screenshots are the only verification that exists here.

### Changed but not yet seen running
- [ ] Onboarding — `TimerArt` and `WaveformArt` both took the new geometry;
      needs a fresh install to actually look at
- [ ] Focus running — the meter is 20 bars ≈ 78dp now, proportion unchecked
      against the mockup's
- [ ] Monochrome palette — untouched in principle (the off-state bar is an
      alpha over the meta tone, so it follows the scheme), but not eyeballed

### Known divergences still in the code
- [ ] Saved's "From clipboard" suggestion is a filled `surfaceContainer` box
      (`LinksTab.kt:269`), and there is a `primaryContainer` fill at `:395`.
      The mockup's paste row is a bordered pill
- [ ] Following's post carousel cards and feed-management list are still
      boxed (`FollowingSection.kt:309, 379, 436`). The last pass called these
      deliberate secondary surfaces — worth confirming rather than leaving
      implicit
- [ ] Settings still has five `CircleShape` chrome uses
      (`SettingsScreen.kt:75, 143, 326, 376`), never re-checked against the
      flat language
- [ ] The nav bar's selected item is a filled circle; the mockup's `.nb.sel`
      is a rounded-rect raise

### Decisions parked (not bugs)
- [ ] Waveform scale. 18 bars is 61.5dp — 17% of a 393dp screen, where the
      mockup's is 23% of its 320px frame. 24 bars would match the proportion;
      the literal count was kept because the current look was approved
- [ ] Three cut features awaiting a call: the read-along highlighted excerpt
      and the "Read original" row (cut in the flat-row pass), and the
      floating bar's ambient waveform (cut in this one)

### Build coverage
- [ ] iOS and Wasm have not been compiled across this entire migration — a
      cold Kotlin/Native build is ten minutes plus, so it was deliberately
      skipped, but one run should happen before the migration is called done

## Moved out of the design-system page (landing-page pass)

`docs/design-system/design-system.html` is now the product intro as well as
the visual reference, so the engineering inventory it had accumulated moved
here. None of it changed — it just stopped being something a first-time
reader has to scroll past.

### State of the system (verified against the code at the time of the move)

- No boxed surface is left in a list anywhere in the app. Home, Readback,
  Saved, Following and Settings all sit flush on the ground, separated by
  their own bottom hairline.
- `ui/common/ListRow.kt` is the one row: `ListRow`, `ListRowBody`,
  `ListRowDivider`, with `RowTone` (`Normal`, `Accent`, `Faded`) the whole
  vocabulary of row states.
- `ui/theme/Theme.kt` holds `DarkScheme` (Paper Black) and `MonoScheme`
  (Ink). Ink is the default and persists (`UserPrefs.mono`, default `true`).
  The app icon is the Ink mark at all times and does not follow the setting.
- Paper Black's accent is terracotta `#C6684A` — one hue, meaning "there is
  sound here" and nothing else. Ink ignores it entirely.
- [x] The second `AccentColor` (dusty green `#4FA870`) is gone from the code
      too — the enum, `UserPrefs.accent` and its stored key, the Settings
      swatch picker and its `greyed()` helper, and the `accent` parameter on
      `DuskReadTheme`. A second accent was never a feature: the whole argument
      of this palette is that exactly one hue means exactly one thing, and
      letting the reader choose which hue that is buys nothing while giving
      the palette a dial it then has to justify.
- `Radius.Chip` 3dp (hairline-bordered, squared-off: sort chips, sourcechip,
  the Settings and folder icon buttons), `Radius.Inline` 10dp (fields),
  `Radius.Card` 14dp (the summary panel, the one thing that floats).
- `AppTextField` is every text input: Saved's paste field, the feed address,
  Settings, the desktop folder path, onboarding, Focus's custom length.
- `ui/theme/DuskReadIcons.kt` — 22 icons, all stroked at 2.4, round cap and
  join. No Material glyph is mixed in.
- `ui/layout/WindowClass.kt` reads the window against a 720dp breakpoint;
  past it `ui/home/NavRail.kt` replaces the floating bar, the transport
  anchors to the bottom edge, and prose caps at `Layout.ReadingMeasure`
  (640dp).
- Summaries: ML Kit GenAI against AICore, confirmed end to end on a Galaxy
  S25. No emulator can stand in — those system images ship no
  `com.google.android.aicore`, so the engine there only ever answers
  "unavailable".

### Still open

- [ ] Two filled boxes survive in Saved: the "From clipboard" suggestion and
      one paste-row fill, still `surfaceContainer`/`primaryContainer`
      rectangles in `LinksTab.kt` — the last two painted surfaces in the app
- [ ] The wide layout has a rail but not two panes. The list-and-detail
      split, and the overlay rules that go with it, are drawn and not built
- [ ] No hover, no focus ring, no keys — touch never needed them, so none
      were built. A pointer and a keyboard need all three
- [ ] Desktop has no app icon: `nativeDistributions` sets no `iconFile`

### Wide-layout plan, in full

Cut from the design-system page because it documents unbuilt work at more
length than the built app gets.

**Three states a phone never needed.** Touch has one state worth drawing —
pressed, and it is gone before you look at it. A pointer hovers, a keyboard
focuses, and a two-pane layout has to say which row the right-hand side
belongs to. None may use colour: the accent still means *there is sound
here*. Hover is a 3.5% white wash; selected is a `surface` raise; focus is a
1px accent outline, inset — the one place the accent is spent on something
other than sound, because a focus ring that is not obvious is not a focus
ring.

**Keys** — deliberately few, and deliberately the same ones readback's own
terminal player uses, since the two projects are read by the same person on
the same evening and should not disagree about what `space` does:

| | |
|---|---|
| Play | `space` play/pause the transport, from any pane |
| Seek | `←` `→` ±5 s |
| Move | `↑` `↓` through the list pane, `enter` to open in the detail pane |
| Paste | `⌘V` anywhere on Saved fills the paste field, no click first |
| Dismiss | `esc` closes the topmost overlay — the same single `PlatformBackHandler` Android's gesture already goes through |

No shortcut opens a pane that a tab does not. Every key either drives the
transport or moves a selection; none is a hidden route to a screen, which
keeps the rail the only answer to "where am I?" and means the phone build
loses nothing by having no keyboard.

**Overlays, at width.** Focus, the summary panel and the reader are overlays
in one `Box` rather than destinations, and that stays true when the window is
wide — but a bottom-anchored sheet spanning 1180dp is a banner, not a panel.

- The summary panel anchors to the detail pane, not the window: same
  `Radius.Card`, same hairline, bottom-right of the pane that asked for it,
  capped at 420dp. Its job is to sit beside the article you are deciding
  about, and at this width it can do that without covering it at all.
- Focus becomes a centred panel, ~440dp, on a dimmed ground — not a
  full-screen takeover. The phone goes full-screen because a phone has no
  room to be beside anything; blanking 1180×820 to show a clock is theatre.
  The thumb-zone rule that put the meter low on the phone has no counterpart
  here: centred is correct once nothing has to be reachable.
- The reader stops being an overlay at all. It *is* the detail pane — which
  is the entire argument for two panes.
- Nothing gains a modal. No dialog, no confirm step.

**What web has to answer that desktop does not.**

- No readback library. The folder picker is Android SAF and desktop
  file-path; a browser tab has neither, so Readback in Wasm shows the
  "not configured" state and the rail's tab stays visible but empty. Not
  hidden — a missing tab is a worse answer than an empty one.
- No summaries. Same as iOS: `summariesSupported()` is false, the control is
  not drawn, nothing upstream knows.
- The window is not yours. A tab can be 320dp or 3000dp and can change
  mid-session, so the breakpoint has to be read continuously — the one place
  `BoxWithConstraints` earns its keep over a platform window-size class.
- Fonts arrive late. Jost and Inconsolata are bundled on the other three
  targets and a network fetch on web; the first paint has to be legible in
  the fallback stack rather than invisible.

### Summary-screen decisions, in full

- **Prose, not bullets.** The engine returns bullets *always*, so
  `parseSummary` flattens them into prose rather than the panel growing a
  layout per engine mood.
- **Two lengths, on the engine's own dial.** Short and Full map to AICore's
  `ONE_BULLET` and `THREE_BULLETS`. A word limit was tried first and
  dropped: a limit can only cut, never lengthen, and three points from this
  model often run under any ceiling worth naming, so every setting produced
  the same text.
- **One engine, no model picker.** AICore's free-form Prompt API answers
  `FEATURE_NOT_FOUND` on real hardware, so it and the three-model Settings
  picker that depended on it are both gone.
- **Swipe-right-to-summarise** draws `SummariseBackground` in
  `surfaceContainerHigh`, not Remove's accent container — one is destructive
  and should look like it.
- **One shared pill.** `ui/summary/SummaryChip.kt` is the download button in
  the panel and in Settings alike.

### App-icon geometry note

The crescent is an opaque circle in the exact ground colour, drawn after the
bar — *not* a `fillType="evenOdd"` subtraction, which was tried first and is
wrong for a cut shape extending outside the base shape: evenOdd fills that
outside part back in as a second, disconnected blob. The one place the
ground-colour trick cannot apply — the Android 13+ themed-icon layer, which
reads only alpha — keeps a true evenOdd hole, sized down to sit fully inside
the bar so the same bug cannot recur.

---

## The landing page — open work

`docs/design-system/design-system.html` is now the product landing page as
well as the visual-language reference, in four acts (brand, walkthrough,
design system, get it) with the reference values split out into
`docs/design-system/design-tokens.md`. Conventions for editing it are in the
`duskread-landing-page` skill.

### GitHub Pages

- [x] `.github/workflows/pages.yml` — copies `design-system.html` to
  `index.html`, adds `.nojekyll`, rewrites the relative `design-tokens.md`
  link to the GitHub blob URL, and deploys. Triggers on pushes to `main`
  touching `docs/design-system/**`, plus manual dispatch.
- [ ] **Enable Pages in the repo, once.** Settings → Pages → Build and
  deployment → Source → **GitHub Actions**. The deploy job fails with "Get
  Pages site failed" until this is done, and it cannot be done from a
  workflow.
- [ ] Merge `design-page-landing` into `main` — the workflow only fires there.
- [ ] Link the live page from `README.md` once the URL resolves
  (`https://mks-01.github.io/duskread/`). Left out for now rather than
  committing a dead link.
- [ ] Decide whether `design-tokens.md` should render as a page of its own
  rather than as a GitHub blob. It would need either Jekyll front matter or a
  Markdown step in the workflow; the blob link is deliberately the cheap
  option until the file is being read often enough to justify either.

### Page work still open

- [ ] Feel-check the deck drag on a real phone. The projection constant is
  `0.99` (snappier); `0.998` is the scroll-like default and is a one-character
  change. Cannot be judged from code.
- [ ] Feel-check the walkthrough autoplay interval (6.5s) against actually
  reading a slide's caption.
- [ ] The three corner radii (14 / 10 / 3dp) are drawn at true size on the
  "shape and motion" slide and are genuinely hard to tell apart there. Either
  accept it — they are one step apart on purpose — or draw a zoomed corner
  detail instead of whole squares.
- [x] Summaries is named and shown before the walkthrough after all — a
  subsection in act one, the Article summary frame shown twice, in Paper
  Black and in Ink side by side, framed as "the feature no other read-later
  app has" rather than as a fourth capability card. Getting the two frames
  to actually hold their schemes regardless of the page-wide switch needed a
  new `.paper` pin class (`.mono`'s mirror, `body.ink .v-amp.paper` to win
  the specificity fight) — a bare `.v-amp` "Paper Black" frame was silently
  flipping to Ink because the reader's toggle defaults to Ink. The focus
  timer is still only introduced in the walkthrough; leave it there unless it
  turns out to need the same treatment.
- [x] The mark's construction (Bar / Crescent / Mark, number and title only),
  its size ramp and the splash moved out of act one entirely, into a new
  "App icon" slide in the Design system deck (now six slides). Act one argues
  the identity; the deck is where a reader steps through how the mark is
  built — the two were competing for the same attention in one act.
- [x] The system deck's Monochrome slide went through two frames before
  landing: first reverted to Readback (to avoid repeating act one's Summary
  screen), then swapped again to a plain article-open reading view — no
  summary panel, no spinner, just the extracted text with the reader button
  active. Reading is the app's central concept, so the monochrome test
  demonstrates it on the screen that concept lives on, not on Readback
  (a secondary feature) or a second copy of the Summary comparison. The
  colour demonstration is quieter as a result — only the reader button's
  ring goes from terracotta to white, versus Readback's pill/title/chip/wave
  all switching at once — a deliberate trade of a stronger demo for the
  better conceptual fit.
- [x] The Monochrome slide's copy was still describing Paper Black as the
  baseline and Ink as what you get when you strip it ("becomes white here"),
  backwards from how the app actually defaults. Reworded to describe Paper
  Black as the one-tap addition on top of Ink, not Ink as the derived state.

- [x] The walkthrough deck cut from thirteen slides to seven: onboarding's
  name panel (the other three intro panels were the same panel with different
  art), Home, Following, reading + summary as one slide, Saved, Focus
  running, Readback. Thirteen screens is a tour; seven is an argument.
- [x] The Readback mockup had drifted from `ui/reader/ReaderTab.kt` and
  `ui/home/FloatingBar.kt` and is now redrawn against them: the floating bar
  shows its **player face** (play/pause, title, remaining, stop, divider, the
  tab you are already on) with the 2.5dp scrub line along its foot, because
  playback wins the bar while it runs — the mockup was still drawing the tab
  face. Added the `NowPlayingTip` line above the list ("use the bar below to
  control it") and the per-row external-link row, neither of which the mockup
  had.
- [x] The Focus mockup was missing its close button (top-left, the only way
  out of a full-screen destination), had the eyebrow accented where
  `FocusScreen` passes `tint = onSurfaceVariant`, and was left-aligned where
  the real column is `CenterHorizontally`. All three fixed; clock size
  corrected 56 → 52 to match the `52.sp` in code.

- [x] Every walkthrough phone was rendering narrower than 320px. The deck's
  figure column is `max-content`, and `.phone` is `width: 100%` — so a screen
  with sparse content (Focus is a clock and two pills) collapsed to the width
  of that content. `.deck-figure .phone { width: 320px; max-width: 100% }`
  gives max-content something definite to resolve to. The floor this replaces
  was lost when act one's deck was dismantled.
- [x] The bookmark was missing from Following entirely — and per
  `ui/home/TopicRow.kt` it is "the only way a feed post ever reaches the Saved
  tab", so its absence made the two screens look unconnected. Expanded rows
  now carry the sourcechip `ListRow` draws and the bookmark pair (filled =
  saved, hollow = not), with copy naming the route into Saved and the
  swipe-right-to-summarise gesture beside it.
- [x] Wording pass: "dashboard" is not a word this app uses (it is Home),
  "nested carousel" read oddly on a page built out of decks, and the floating
  bar was being called "tab bar" on one slide and "nav bar" on another. Act
  one's note still claimed everything in it was "drawn once, in Ink" — false
  since the Summary pair arrived. Two stale HTML structure comments fixed
  too: act one was described as Ink-pinned and act three as "grids, not a
  deck".

- [x] The app reports a version now. `app = "1.0.0"` in the version catalog
      feeds both `androidApp`'s `versionName` and a generated `AppVersion`
      constant in commonMain, shown at the foot of Settings. Generated rather
      than checked in because the value has two consumers in two languages —
      Gradle cannot read a Kotlin `const`, so the build has to own it or the
      About line eventually lies about which build you are holding.
- [x] `TODAY'S READBACK` on Home was neither dynamic nor playable: it showed
      `listReads().first()` with `progress = 0f` hardcoded and a tap that only
      opened the tab. Start a read from the Readback tab, come back to Home,
      and the one section about audio was the one section that could not tell
      you audio was running. It now prefers whatever is playing over whatever
      is newest, follows the real position in both the meter and the meta
      line, takes the accent while playing, and the row plays rather than
      navigates — the chevron on the eyebrow is the way to the tab. Tapping a
      read to play it is what the identical row does in the Readback tab.

- [x] `minSdk` 26 to 31 (Android 12). 26 was never a choice — it was the
      floor ML Kit GenAI's manifest imposed. 31 is chosen: AICore needs far
      newer anyway, and nothing this app is for happens on a phone that old.
      No dead guards fell out of it; the one `SDK_INT` check left
      (`MainActivity`'s notification permission) is API 33 and still needed.
      README badge and table, and the landing page's Get it table, updated to
      match.

- [x] Home's wireframe in the design system caught up with the code: `FROM
      SAVED` is `NEXT UP` with a hero and two runners-up, each carrying
      `host . N min`, and the shuffle sits at the end of the eyebrow. The
      readback row shows the playing state it now has — accented title, a
      play/pause glyph, `0:38 / 1:22` — with the chevron to the tab moved onto
      the eyebrow, since the row itself plays.
- [x] The README's eight device screenshots are gone, replaced by one image
      rendered straight out of `design-system.html`: the Article summary in
      Paper Black and Ink, side by side. Rendered rather than photographed so
      it cannot drift from the design system, and transparent-backed so it
      sits on either GitHub theme. The four Ink shots plus the four Paper
      Black ones behind a `<details>` were eight files to re-take every time a
      screen moved, which is why they were always slightly out of date.

### Not a page issue, but found while drawing it

- [ ] `ic_launcher_monochrome.xml`'s notch may not be contained after all. The
  bar spans x 40–68; the notch circle is centred at (67, 36) with r=9, so
  x 58–76 — roughly 8px hangs off the right edge. `evenOdd` only cuts cleanly
  where the shapes overlap, so that overhang should fill back in as a lens,
  which is the exact failure the file's own comment warns about. Check it on a
  device with themed icons turned on.

## Following becomes a tab (this pass)

The digest outgrew the section it lived in. `FollowingDigest` was written to
be "three lines and done" inside Home's own `LazyColumn` — fine for a
handful of feeds, not for fourteen, where the digest was already the longest
thing on the screen and still only showing a fraction of what following
those blogs actually produced.

- [x] `HomeTab` gains a fourth entry, `FOLLOWING`, and the order changes to
  `HOME, FOLLOWING, SAVED, READBACK` — Readback moves last, in line with
  `CLAUDE.md` calling it an add-on rather than the main feature. Both
  `FloatingBar.kt`'s `TabsFace` and `NavRail.kt` already looped
  `HomeTab.entries` rather than hardcoding three, so the fourth destination
  needed no layout rework in either — the floating pill wraps its icon row
  and the rail is a fixed-width column that just grew taller.
- [x] New `ui/home/FollowingTab.kt`: `FollowingDigest` promoted into its own
  `LazyColumn` with pull-to-refresh, the same shape `LinksTab.kt` already
  established, so a long feed list finally gets a scroll of its own instead
  of borrowing Home's.
- [x] Home's own following section shrinks to `FollowingShortcut`
  (`DashboardTab.kt`) — the total new-post count in the eyebrow, the feed
  count, and the top three feeds by unread count as a named preview, tapping
  through to the real tab. Not a bare number: naming the blogs is what makes
  it read as a summary rather than an afterthought under Focus.
- [x] `DuskReadIcons.Feed` redrawn. It used to be ascending bars — a signal
  getting stronger, standing in for the RSS dot and its broadcast arcs — which
  read as a literal signal-strength glyph once it sat in the tab bar next to
  three other destinations. Redrawn as three bulleted rules (a short bar next
  to a long one, the round line cap doing the bullet), unambiguously "a list."
- [x] `DuskReadIcons.Search` added — a ring and a handle, the one shape in the
  set that cannot be built from bars. Lives only inside the Following tab's
  own header, not the bottom bar: a fifth icon there would cost more width
  than a search field is worth, and search only has one screen that needs it.
- [x] `FollowingDigest` gains: a search field (filters by feed name, host or
  topic, and by post title when the feed itself doesn't match — narrows both,
  doesn't just narrow the feed list), a Newest/A–Z sort pair styled as the
  exact bordered pill Readback's own sort chips use rather than a second sort
  language, and a one-line hint under each collapsed feed showing its newest
  post's own title — already-cached data, not a generated summary, so it
  costs nothing to show and nothing to keep current. The hint disappears once
  a row is open, since the real post is sitting right underneath it by then.
- [x] The feed name's type was wrong twice before it was right. First pass
  gave it `titleSmall` (SemiBold) to stand apart from the plain-Mono line it
  used to be — correct call on family (Jost, not Inconsolata: the tokens doc
  reserves mono for a value, never a name) but SemiBold next to Regular read
  as shouting, not a header, since the row is smaller than what it
  introduces. Landed on `bodyLarge` + `FontWeight.Medium` — the one step this
  type scale actually has between Regular and SemiBold. A `MonogramBadge` was
  also tried on the collapsed row itself, matching the one every post row
  carries once expanded; pulled back out because a parent row and a child row
  wearing the same badge stopped reading as parent and child at all.
- [x] The floating bar's scroll-collapse now shrinks as well as slides. It
  used to only translate down, on the stated reasoning that the 42dp buttons
  would go under thumb size if shrunk — true for a *tappable* bar, but the
  collapsed bar isn't one: the whole pill becomes a single re-expand target
  the moment it collapses, so nothing was actually protected by staying full
  size. Scales to 0.82 from the bottom-centre, in the same tween as the
  existing drop, so it reads as one motion sinking into the edge rather than
  a slide and a separate shrink.

### Still open from this pass

- [ ] The landing page's Walkthrough deck still shows the old shape: a
  "Following goes last" Home mockup with the digest as its final section, and
  a standalone "03/07 · Following" slide built around the same digest
  in-place-on-Home design. Both predate the tab. Deliberately left for its own
  pass rather than rushed alongside the app change it documents.

## A second home-screen widget: one suggestion, not a control (this pass)

`DuskReadWidget` is a control — start, pause, capture. `NEXT UP` on Home is
the app's one already-ranked "here, read this" moment, but it only exists
where the app is open. `DuskReadSuggestionWidget` puts that same pick on the
home screen instead of a second control.

- [x] New `androidApp` provider, `DuskReadSuggestionWidget`, registered
  alongside `DuskReadWidget` for the same `APPWIDGET_UPDATE` and
  `WidgetState.ActionRefresh` actions — a capture or a focus-session
  transition already broadcasts a repaint, so the new widget rides that
  broadcast rather than adding a trigger of its own. `MainActivity.onStop`
  refreshes both for the same reason.
- [x] A separate provider rather than a third state in the bar widget's 64dp
  row — that bar is built around exactly two states on purpose, and an
  article title has nowhere to go in a 9sp caption slot without displacing
  the "start a session" affordance the bar exists to keep in reach.
- [x] Ranks its own throwaway copy of the candidate pool
  (`pool`/`rank`/`topPicks` over a fresh `LinkLibrary`/`FeedLibrary`/
  `FeedPostCache`/`ReadingSignals`) with a random seed on every redraw, rather
  than the day-stable seed Home's `NEXT UP` uses — Home is deliberately
  stable across a morning of openings; this widget only redraws for a real
  reason in the first place, so there is nothing to protect by not
  re-rolling.
- [x] Read-only ranking, but not a read-only tap: opening a pick has to go
  through the app's own live `LinkLibrary`/`ReadingSignals`, not the widget's
  throwaway copy, or it's two writers over the same storage key — exactly the
  hazard `docs/architecture.md` warns about. New `ui/home/SuggestionOpenRequest`
  hands the tapped URL to `HomeScreen`, which does the save, the read toggle
  and the signal through the one live copy of each — the same four lines
  `NextUpSection`'s own tap already runs.
- [x] `widget_suggestion.xml` has one TextView pair, not two behind
  visibility flags: "nothing to suggest yet" is the same title-and-meta shape
  with different text, not a different layout. Borrows the bar widget's brand
  mark at a smaller size in its eyebrow row rather than inventing a second
  glyph.
- [x] `dusk_suggestion_widget_info.xml` asks for `targetCellHeight="1"` —
  two rows left the card's content centred in a lot of empty space on
  launchers whose own row height runs 150dp or more. `updatePeriodMillis` is
  `0`, same as the bar widget: nothing here polls, every redraw is a real
  event.
