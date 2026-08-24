---
name: blogmark-design-system
description: Use when building a new UI flow or making a visual fix anywhere in
  this Blogmark app — a new screen, a restyled row, an icon button, a colour or
  spacing tweak. Points at docs/design-system/design-system.html as the visual
  source of truth and gives the reusable Compose patterns already shipped
  (ListRow, AppTextField, EyebrowHeader, the bordered icon-button shape, the
  one-accent rule) so new work reuses what exists instead of hand-rolling a
  fourth copy of something that already has three.
---

# Blogmark design system

Blogmark has one shipped visual language (the "Amplitude" direction) and a
small, fixed set of design tokens. Before writing new UI or touching existing
UI, load this skill so the change lands consistent with what is already on
screen rather than introducing a second system next to the first.

## Source of truth

- **`docs/design-system/design-system.html`** — open it in a browser. Per-screen
  mockups (Readback, Home, Saved, Focus, empty states), the Monochrome (Ink)
  and Brand sections, the Type scale, the shipped icon set's stroke language,
  an **"In use today"** list (cross-checked against the live code, not stale
  notes), and a **"Next"** list of genuinely open work.
- **`docs/design/amplitude-migration.md`** — the day-by-day log of how the app
  got here, screen by screen. Read it for *why* a pattern exists; read the
  design-system doc for *what* the pattern is.
- **`CLAUDE.md`**'s "Colour and design tokens" and "Icons" sections carry the
  house rules this skill assumes: colour only from `MaterialTheme.colorScheme`
  (never a hard-coded `Color(0x…)`), icons only from `ui/theme/BlogmarkIcons.kt`
  (never `Icons.Filled.*`).

**Keep the doc in sync.** A UI change that isn't cosmetic-only (a new pattern,
a rule bent for a reason, a "Next" item resolved or added) should update
`design-system.html` in the same pass — not as a separate follow-up that never
happens. Verify claims against the real code before writing them; do not carry
a stale note forward just because the old doc said so (see the doc's own
history for two examples of exactly that mistake, caught and removed).

## The tokens (`ui/theme/Tokens.kt`)

| Token | Value | Use |
|---|---|---|
| `Radius.Card` | 14dp | Dashboard/list cards |
| `Radius.Inline` | 10dp | Text fields, filled CTAs, rows |
| `Radius.Chip` | 3dp | Pills, sourcechips, bordered icon buttons — a *softened* corner, not a rounded one |
| `Stroke.Hairline` | 1dp | Every border in the app |
| `Space.ChipGap` / `Space.CardGap` | 6dp / 9dp | Recurring gaps |
| `Motion.Chip` / `Fade` / `PushIn` / `PopFade` | ms | State-change vs. navigation timing — navigation is instant, a tone change is slow enough to *see* |

`Radius.Chip` is the one to reach for by default: it is what makes a bordered
control read as "this app's instrument panel" rather than a generic Material
surface. `Radius.Card`/`Inline` exist for cards and text fields specifically —
don't reach for a rounder corner because it "looks nicer" without a reason.

## Colour: the one-accent rule

Two schemes only — `DarkScheme` (Paper Black, terracotta `primary`) and
`MonoScheme` (Ink, hue drained to luminance). Never a third. Within a screen,
the accent (`colorScheme.primary`) is mostly reserved for **one thing**: the
row currently doing something (the read that's playing, the note currently
saved). `ui/common/ListRow.kt`'s `RowTone` enum (`Normal` / `Accent` / `Faded`)
is how that's expressed — reach for it before inventing a new colour branch.

Two narrow, deliberate exceptions exist and are the right model for adding a
third: a **selected control** (the active sort chip, a selected tab) may take
the accent even with nothing "playing," because selection is its own state,
not a competitor to the one-accent rule; and a **persistent affordance glyph**
(the external-link icon on every row) may carry a permanently muted
(`primary.copy(alpha = 0.75f)`-ish) hint of the accent, because it marks "this
leaves the app" regardless of playback state, not "this is playing." If you
want a third exception, it needs the same kind of reasoning, not just "it
looked flat."

## Reusable components — check here before writing a new one

- **`ui/common/ListRow.kt`** — `ListRow` / `ListRowBody` + `ListRowDivider` for
  the one caller (Saved's swipe-to-remove) that can't use `ListRow` whole,
  `RowMeta` for one mono fact on a meta line, `HairlineDivider` for a bare 1dp
  separator with no baked-in spacing, `ChipSize` (22dp) for a host chip placed
  outside `ListRow` itself. This is the sourcechip/title/meta skeleton every
  list in the app is built from — Saved, Readback and a followed blog's topics
  had each grown their own copy before this existed. Don't grow a fourth.
- **`ui/common/AppTextField.kt`** — the one text-field shape: hairline border,
  `Radius.Inline`, never a pill. Every field in the app goes through it.
- **`ui/common/EyebrowHeader.kt`** — label + inline trailing hairline rule, one
  row. Defaults to the accent tint; pass `tint = onSurfaceVariant` explicitly
  for a lower-priority section.
- **`ui/common/MonogramBadge`**, **`ui/common/WaveformMeter`**,
  **`ui/common/PrimaryButton`** — the sourcechip, the per-row/per-timer meter,
  and the one filled call-to-action shape, respectively.

## The bordered square icon button

Established this session for Settings (`DashboardTab.kt`) and the folder
picker (`Reader.android.kt` / `Reader.desktop.kt`), replacing bare glyphs and
a plain `CircleShape` clip that drew no visible border. Use this shape for any
new icon-only affordance that needs the same weight as a sort chip nearby:

```kotlin
Icon(
    imageVector = BlogmarkIcons.SomeIcon,
    contentDescription = "…",
    modifier = Modifier
        .size(34.dp)
        .clip(RoundedCornerShape(Radius.Chip))
        .border(Stroke.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.Chip))
        .clickable(onClick = ...)
        .padding(9.dp),
    tint = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

A bare `Icon` with only a `.clickable().padding()` (no clip, no border) reads
as noticeably smaller than everything bordered around it — that's the bug this
pattern fixes, not a style preference.

## Before calling a UI change done

Per `CLAUDE.md`: compiling proves nothing about layout. For any visual change —
`./gradlew ktlintCheck`, then `:composeApp:compileAndroidMain` (or the desktop
target if it touches `commonMain`/`desktopMain`), then actually look at it:

```bash
adb shell am force-stop dev.mks.blogmark
adb shell am start -n dev.mks.blogmark/dev.mks.blogmark.android.MainActivity
adb exec-out screencap -p > /tmp/check.png
```

Check it in **both** colour schemes if the change touches anything that reads
`colorScheme.primary` — Ink is not optional coverage, it's the same screen
with the hue drained out, and a literal colour will survive the swap and look
wrong.
