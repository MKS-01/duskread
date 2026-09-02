# Design tokens

Every value the app treats as a decision rather than a measurement, with the
file it lives in. This is the reference half of the design system;
[`design-system.html`](design-system.html) is the argument half — it shows the
language, this lists it.

The rule that governs the whole file: **a value is here because more than one
place needs it, or because its exact number changes how the app reads.**
Optical one-offs stay inline at their call site. Padding of
`top = 14.dp, bottom = 13.dp` is a nudge to make one label sit right, not a
rule, and naming it would imply a system that is not there.

Spacing follows a 2dp rhythm. New values should land on it.

---

## Colour

`ui/theme/Theme.kt`. Two schemes, both dark — a light polarity was never
built, because the app is read on a phone in the evening. What the toggle is
actually for is dropping colour entirely on the days the accent is a
distraction.

**Never hard-code a `Color(0x…)` in a screen.** A literal survives the swap to
Ink and immediately looks wrong. Everything comes from
`MaterialTheme.colorScheme`.

| Role | Paper Black | Ink | What it is for |
| --- | --- | --- | --- |
| `background` | `#101010` | `#161616` | The ground. Just off pure black: cheap on an OLED panel, not so flat it loses depth against a surface. |
| `surface` | `#1A1A1A` | `#202020` | A raised panel — one step up, and the whole of the lift. |
| `surfaceVariant` | `#0D0D0D` | `#121212` | Recessed: below the ground rather than above it. |
| `surfaceContainer` | `#212121` | `#272727` | Rows and chips inside a panel. |
| `surfaceContainerHigh` | `#282828` | `#303030` | One step above, for something pressed or selected. |
| `onBackground` / `onSurface` | `#E8E6E2` | `#DCDCDC` | The ink. Never `#FFF` — with no hue anywhere, a full black-to-white span reads as glare at night. |
| `onSurfaceVariant` | `#A3A19D` | `#9C9C9C` | Meta lines, secondary prose, an unselected icon. |
| `outline` | `#3E3E3D` | `#464646` | A border that has to be seen. |
| `outlineVariant` | `#242423` | `#2B2B2B` | The hairline between rows, and the divider the whole layout is built on. |
| `primary` | `#C6684A` | `#DCDCDC` | The accent. Terracotta means *there is sound here*; on Ink, the brightest thing on the page. |
| `onPrimary` | `#2B1006` | `#161616` | The ink laid *on* the accent — the label inside the one filled button. |
| `primaryContainer` | `#352822` | `#2E2E2E` | A filled surface tinted toward the accent. |
| `onPrimaryContainer` | `#FFD9C0` | `#E4E4E4` | Its ink. |
| `error` | `#F0645F` | `#CBCBCB` | Grey on Ink rather than sneaking a red back in: loud through brightness and wording instead. |

### The one rule about the accent

Terracotta means **there is sound here**, and nothing else in the interface is
ever allowed to be coloured. The deliberate exception is the section eyebrow
(`From saved`, `Today's readback`, `Focus`, `Following`), which takes the
accent because four grey labels down one screen give the eye nothing to
structure itself against. `Read`'s eyebrow on the Saved screen is the single
case that opts back out — it sits under an unread list and should not compete
with it.

On Ink there is no hue to spend, so everything that carries the accent in
Paper Black becomes white. Nothing changes shape, weight or position. That is
the test of whether the layout was ever relying on colour.

---

## Type

`ui/theme/Type.kt`. **Jost** for everything the eye reads as language,
**Inconsolata** for every number and meta line, so data never borrows the
platform's default mono. Four static Jost weights (400/500/600/700) and no
light cut — at these sizes, on a near-black ground, a light weight disappears.
Static weights rather than the variable font because static is what renders
correctly on every target Compose Multiplatform reaches here, including Wasm.
Both are SIL Open Font License; files under `composeResources/font/`.

| Style | Family | Weight | Size / line height | Tracking |
| --- | --- | --- | --- | --- |
| `headlineMedium` | Jost | SemiBold | Material default | `-0.5sp` |
| `headlineSmall` | Jost | SemiBold | Material default | `-0.3sp` |
| `titleMedium` / `titleSmall` | Jost | SemiBold | Material default | — |
| `bodyLarge` | Jost | Regular | `15.5sp / 25sp` | — |
| `bodyMedium` | Jost | Regular | `14.5sp / 22sp` | — |
| `labelLarge` | Jost | SemiBold | Material default | — |
| `labelSmall` | Jost | Bold | Material default | `0.8sp` |
| `SectionLabel` | Jost | Bold | `11sp` | `1sp` |
| `CodeStyle` | Inconsolata | Regular | `12.5sp / 20sp` | — |

Body sizes and line heights run slightly larger and looser than the Material
defaults, because the app is for reading long-form prose on a phone.

**`SectionLabel` is the one that gets confused.** It is uppercase and tightly
tracked, so it reads as monospace at a glance — but it is Jost. Inconsolata
marks *data*: a duration, a word count, a host, a number of new posts. If a
label names a section rather than reporting a value, it is not mono.

---

## Layout

`ui/theme/Tokens.kt`, `object Layout`.

| Token | Value | What it is for |
| --- | --- | --- |
| `ReadingGutter` | `18.dp` | Horizontal padding for reading surfaces. |
| `ListGutter` | `14.dp` | Horizontal padding for list surfaces, which carry their own card insets. |
| `BarClearance` | `72.dp` | Bottom inset so the last item clears the floating bar. |
| `TwoPaneBreakpoint` | `720.dp` | The one width that changes the plan: the floating bar below it, the rail above. Named for a two-pane layout that was drawn and never built. |
| `RailWidth` | `64.dp` | The vertical rail that replaces the floating bar when wide. |
| `ReadingMeasure` | `640.dp` | The widest a column of prose may get, ~68 characters. |
| `WideListGutter` | `20.dp` | `ListGutter`, opened up once there is room. |

### Wide — desktop and web

Every decision in the app assumes a thumb and a phone held in one hand. A
mouse and a 1180dp window break all three assumptions at once: nothing is
reached by reaching, a line of text runs to a hundred characters if you let
it, and a bar floating at the bottom of a window nobody is holding is just a
bar in the wrong place.

**Android is the app; the rest is the workshop.** The breakpoint, the rail,
the bottom transport and the capped measure ship today. The two-pane split is
specified and not built. There is no mockup of it, on purpose — a drawn
desktop screen would claim more than the code does.

- **One threshold, not a five-class ladder.** Below `TwoPaneBreakpoint` the
  phone layout is used unchanged, including in a narrow desktop window; above
  it, two panes. 720 rather than 600 because at 600 both panes are too narrow
  to be worth the split, and a landscape phone — still held, still
  thumb-driven — stays on the layout built for it.
- **The bar stops floating.** On a phone it sits at the bottom because that is
  where the thumb is. On a desktop there is none, so navigation goes to a left
  rail and only the transport stays bottom, full-bleed — it is the one thing
  that outlives the pane above it. The rail's theme toggle and Settings sit at
  the far end, separated by a column of empty rail rather than a divider.
- **The list pane is a fixed width, not a fraction.** Two-line titles have a
  correct measure and a wider monitor does not change it; a percentage would
  only make the same four rows emptier.
- **Nothing gets bigger.** Titles keep their phone size, the waveform its 2dp
  bars, rows their hairline. Only the gutters open up. Scaling type with the
  window is the fastest way to turn a considered phone screen into a generic
  desktop one.

---

## Radius, stroke, spacing

`ui/theme/Tokens.kt`.

| Token | Value | What it is for |
| --- | --- | --- |
| `Radius.Card` | `14.dp` | Dashboard and list cards. 14 rather than the 20 it used to be — the app draws its own data, and a heavily rounded card reads as a generic Material surface sitting on top of it. |
| `Radius.Inline` | `10.dp` | Everything inside or below a card: the filled call-to-action, text fields, rows, the timer's state chips. One step tighter than `Card`, so a row never competes with the panel holding it. |
| `Radius.Chip` | `3.dp` | Sort and filter pills (`ui/common/Pill.kt`) and source chips. At these sizes a softened corner, not a rounded one — anything rounder turns a sort control into a Material chip. |
| `Stroke.Hairline` | `1.dp` | Every divider in the app. |
| `Space.ChipGap` | `6.dp` | Between chips in a row. |
| `Space.CardGap` | `9.dp` | Between cards in a list. |

Nothing in the app is fully round except an actual circular icon button.
`PrimaryButton` — the one filled call-to-action — is `Radius.Inline`, not the
pill Material's default `Button` draws.

---

## Motion

`ui/theme/Tokens.kt`, `object Motion`. Milliseconds.

| Token | Value | What it is for |
| --- | --- | --- |
| `PushIn` | `260` | Pushing to a full-screen destination: the incoming screen slides and fades in. |
| `PopFade` | `160` | The fade, slightly slower than the slide, so nothing vanishes mid-travel. |
| `Fade` | `180` | Cross-fades that should not draw attention: tab and pane swaps. |
| `Chip` | `220` | Chip and bar state changes. |

Navigation and the visualiser deliberately move at different speeds.
Navigation should feel immediate; a tone change is teaching — the reader has
to *see* an element switch from being examined to being discarded, so it is
slow enough to follow.

---

## Icons

`ui/theme/DuskReadIcons.kt`. Twenty-two glyphs, one spec throughout: **2.4
stroke weight, round cap and join, nothing filled.** Use these, never
`Icons.Filled.*` — the set is stroked to match the type weight, and a filled
Material glyph mixed in is visible instantly.

```
Home      Target    Clock      Chevron   Back      Close
Play      Pause     Shuffle    Waveform  FolderConnect
External  Bookmark  BookmarkFilled       Feed      Reader
Summary   Check     Settings   Offline   Contrast  Search
```

Every glyph that can be built from evenly spaced vertical bars is —
`Waveform`, `Feed`, `Offline`, `Settings`, the shading on `Contrast` — because
the waveform is the one visual idea this app has, and a set drawn from it
agrees with the data on screen rather than merely sitting beside it. What
cannot be (`Target`, `Shuffle`, `FolderConnect`) borrows the same weight and
round terminal.

**`BookmarkFilled` is the single filled glyph**, and only ever the *on* half
of a pair with `Bookmark`. Two states have to be told apart at icon size, and
a tint change is a colour difference — the one thing Ink does not have. Filled
and hollow survive the scheme swap; terracotta and grey do not.

New icons go in this file as vector paths, at the same spec.

---

## Brand assets

`androidApp/src/main/res/`.

| Asset | File | Notes |
| --- | --- | --- |
| Mark | `drawable/ic_duskread_mark_ink.xml` | Bar `#DCDCDC`, bite painted in `#161616`. Group scaled `0.85` about the centre. |
| Launcher | `mipmap-anydpi-v26/ic_launcher_ink.xml` | Adaptive: flat `#161616` background, mark inset 13% as foreground, plus a monochrome layer for Android 13 themed icons. |
| Launcher (API 24–25) | `mipmap/ic_launcher_ink.xml` | The same two colour layers as a plain `layer-list`. |
| Themed layer | `drawable/ic_launcher_monochrome.xml` | Flattened to one opaque colour; the notch is a real `evenOdd` hole, because the system reads only this layer's alpha. |
| Splash | `values/themes.xml` → `Theme.DuskRead.Splash.Ink` | Ground `@color/splash_background_ink` = `#161616`, matching `MonoScheme.background`. |
| Splash animation | `drawable/ic_duskread_splash_ink.xml`, `animator/splash_bar_main.xml` | `scaleY` 0.08 → 1, `decelerate_quint`, 380ms after a 40ms hold. The 700ms declared duration only tells the system how long the animation runs. |

The launcher icon and splash are Ink's, always. The terracotta pair the app
used to swap between at runtime is gone — an icon that changes colour behind
the user's back is a bug report waiting to happen.

---

## Home-screen widget

`androidApp/src/main/res/`, rendered by
`androidApp/src/main/kotlin/dev/mks/duskread/android/widget/DuskReadWidget.kt`.

A 3×1 bar — about 60% of the phone's width — with two things on it: capture the
URL on the clipboard, and start a 15-minute focus session.

**One layout, two states.** The idle bar was once its own arrangement and read
as a different widget beside the running one. It is now the same shape with
different content: the number holds its place whether it is a session counting
down or the length of one not yet started, the caption says which, and the two
slots at the right are always controls.

```
idle     ◗   15:00              ⧉    ▶
             READY

running  ◗   14:55              ⏸    ✕
             FOCUSING
```

Nothing moves when a session starts.

Values are repeated here rather than read from `Theme.kt` because a
`RemoteViews` tree is built outside composition and has no scheme to read —
**these must be changed with `Theme.kt`, not after it.**

| Token | Value | Notes |
| --- | --- | --- |
| Card | `drawable/widget_card_ink.xml`, `widget_card_paper.xml` | `Radius.Card` 14dp over `surface` at **90% opacity**, hairline in **`outline`** — not `outlineVariant`. In the app a card sits on `background` and the fill does half the separating; on a wallpaper the edge is the only thing defining the shape. Enough wallpaper comes through to place the bar on the home screen rather than on top of it. |
| Card size | `64dp` tall, 9/10 of the footprint wide | Fixed rather than filling the cell: launchers hand out ~100dp for "one row" and a control bar stretched to that stops reading as a bar. The width is the same idea, and gets past a five-column grid only offering 60% or 40% — 40% cannot hold the number and two controls. The leftover is transparent margin, not a gap. |
| Mark | `drawable/ic_widget_mark.xml` | 11×24dp badge at the head of the bar, tinted `primary`. One filled silhouette — the bar with the crescent already taken out — not two paths with the crescent painted in the ground, which stops working the instant the card is translucent. An `evenOdd` hole is no good either: the crescent overhangs the bar's edge. The two crossing points are computed, not eyeballed. |
| Number | Jost SemiBold 22sp, `onSurface` | Deliberately *not* the accent. The mark is the one accented thing on the bar and it is always there. Idle draws a plain `TextView`; only a live session gets the `Chronometer`. |
| Caption | Jost SemiBold 9sp, `letterSpacing 0.09`, `onSurfaceVariant` | `READY` / `FOCUSING` / `PAUSED`. |
| Glyphs | `ic_widget_paste`, `_clock`, `_pause`, `_play`, `_close` | 19dp in 48dp slots, drawn white and tinted at render. Copies of `DuskReadIcons` at the same 24×24 / 2.4 / round-cap spec, because RemoteViews cannot render an `ImageVector`. |
| Fonts | `res/font/jost_*.ttf` | Copies — Compose Resources fonts are not addressable as `R.font`. |
| Separation | Space only | Hairline rules between the cells were tried and cut: three of them across 64dp read as scaffolding. Lightness, weight and space are what Ink separates with. |
| Rhythm | One 24dp optical band | Mark, glyphs and caption cap-height all sit on it. |

Both schemes are mirrored in `DuskReadWidget.Palette`, and the widget follows
the app's Ink / Paper Black toggle by reading `theme.mono` from the same
preferences the app writes. `MainActivity.onStop` repaints it, which is what
makes a toggle take effect.

**Nothing here polls.** `updatePeriodMillis` is `0`; the countdown is a
`Chronometer` the launcher ticks in its own process, so DuskRead is never woken
to redraw it; and the only repaints are the ones `PomodoroService` pushes at a
session's transitions, plus one non-repeating alarm to retire a capture
confirmation.
