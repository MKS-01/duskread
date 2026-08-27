# Amplitude migration — progress

Tracking the app's migration to the Amplitude direction from
`docs/design/redesign-variants.html`, screen by screen. Terracotta and
monochrome are the only two palettes kept — no cyan "signature" accent.

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
- [ ] The second `AccentColor` (dusty green `#4FA870`) is being dropped. It
      is out of the docs already; the enum, the Settings swatches and the
      picker still need removing from the code.
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
- [ ] Summaries and the focus timer are no longer named anywhere before the
  walkthrough, since the four capability cards were cut from act one as a
  table of contents for act two. If they should be named up front, a clause in
  the masthead standfirst is the cheap fix, not bringing the cards back.

### Not a page issue, but found while drawing it

- [ ] `ic_launcher_monochrome.xml`'s notch may not be contained after all. The
  bar spans x 40–68; the notch circle is centred at (67, 36) with r=9, so
  x 58–76 — roughly 8px hangs off the right edge. `evenOdd` only cuts cleanly
  where the shapes overlap, so that overhang should fill back in as a lens,
  which is the exact failure the file's own comment warns about. Check it on a
  device with themed icons turned on.
