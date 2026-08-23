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

## Brand — app icon and splash (this pass)

The mockup's Brand section had never been built. The launcher icon was still
three fanned bookmark ribbons, whose fault the mockup names exactly: scale,
not concept — the artwork occupied about 15% of its canvas and dissolved into
a speck at 48px.

- [x] `ic_blogmark_mark.xml` rebuilt to the mockup's Amplitude mark: a level
      meter whose tallest bar is a bookmark, so the notch reads as both a
      ribbon tail and a peak. Fills roughly 70% of the canvas and folds *save*
      and *listen* into one silhouette
- [x] Terracotta, not the mockup's cyan — one accent at four alpha weights
      (0.45 / 0.7 / 1.0 / 0.55), keeping the two-palette rule
- [x] Geometry is the mockup's own, shifted 2 units left to centre it on the
      canvas and scaled 0.85 about the centre so nothing leaves the 66-unit
      safe zone a launcher mask and the splash icon both crop to
- [x] One drawable, three consumers: the adaptive foreground now references
      `ic_blogmark_mark` directly rather than duplicating its path data, and
      `ic_launcher_foreground.xml` is deleted. Only the monochrome layer is
      separate, because a themed icon needs one opaque colour
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
