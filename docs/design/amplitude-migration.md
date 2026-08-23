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
