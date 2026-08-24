---
name: blogmark-verify
description: Use before calling any change to this app done — a UI fix, a
  logic fix, a new flow. Compiling proves nothing about behaviour or layout,
  and this repo has no tests, so a build that succeeds is the start of
  verification, not the end. Gives the exact build/lint/install/exercise/
  check-logcat loop used throughout this session, plus a coordinate-scaling
  mistake worth not repeating.
---

# Blogmark verify

There are no tests in this repo (`CLAUDE.md` is explicit about this). A
change is not done when it compiles — it's done when it's been run and
looked at. This is the loop, in order.

## 1. Lint and compile

```bash
./gradlew ktlintCheck -q
./gradlew :composeApp:compileAndroidMain -q          # any commonMain/androidMain change
./gradlew :composeApp:compileKotlinDesktop -q         # if the change touches commonMain
```

Don't skip the desktop compile for a `commonMain` change even if the feature
is Android-only in practice — `expect`/`actual` mismatches and unused-import
warnings on one platform are easy to miss if only the other is checked.

Don't build iOS or Wasm unless asked — a cold Kotlin/Native build is ten
minutes-plus (`CLAUDE.md`).

## 2. Install and launch fresh

```bash
./gradlew :androidApp:installDebug -q
adb shell am force-stop dev.mks.blogmark
adb shell am start -n dev.mks.blogmark/dev.mks.blogmark.android.MainActivity
```

`force-stop` first — a change to a `Service` (foreground service timing, a
channel definition) can silently keep running the *previous* install's code
in a still-alive process otherwise.

## 3. Actually exercise the change

Screenshot, don't assume:

```bash
adb exec-out screencap -p > /tmp/check.png
```

Then read the file with the `Read` tool to actually look at it — a
screenshot taken and never viewed verifies nothing.

**For a colour/theme-reading change**, check it in both `DarkScheme` (Paper
Black) and `MonoScheme` (Ink) — see `blogmark-design-system`. A literal
colour survives the swap and looks wrong; a token-based one doesn't, and the
only way to tell them apart is to actually toggle and look.

**For a race or a rapid-interaction bug**, reproduce the actual sequence —
several `adb shell input tap` calls fired back-to-back right after the
triggering action, not one tap and a wait. See `blogmark-crash-audit` for
when this matters.

## 4. Check logcat for what a screenshot can't show

```bash
adb logcat -c                                          # before exercising the change
# ... exercise it ...
adb logcat -d 2>/dev/null | grep -iE "FATAL EXCEPTION|blogmark.*(Exception|Error)"
```

A screenshot after a crash can look identical to a screenshot of normal
operation if the process auto-relaunched or the state just happened to look
fine — logcat is the only reliable "did anything actually throw" check.

## 5. Coordinate scaling — the mistake to not repeat

`screencap` output is in **real device pixels** (e.g. 1080×2400 on the
emulator used this session). If a screenshot is shown to you scaled down
(e.g. "displayed at 900×2000, multiply by 1.2"), that scale factor is for
*your* reading of the image, not for `adb shell input tap` — `input tap`
always takes real device-pixel coordinates.

The reliable way to get a tap target's real coordinates, every time, instead
of eyeballing a scaled screenshot and doing mental arithmetic:

```bash
adb exec-out uiautomator dump /dev/tty 2>/dev/null | grep -o 'text="LABEL"[^>]*bounds="\[[0-9,\[\]]*\]"'
# or, for an icon with a contentDescription:
adb exec-out uiautomator dump /dev/tty 2>/dev/null | grep -o 'content-desc="LABEL"[^>]*bounds="\[[0-9,\[\]]*\]"'
```

`bounds="[x1,y1][x2,y2]"` is already in real device pixels — tap the
midpoint. This session mis-tapped the same button three separate times by
doing the scale-factor math by hand before switching to this; go straight to
`uiautomator dump` instead of estimating from a screenshot.

## 6. For a background/async result (a completing timer, a finished download)

Use `Monitor` or `Bash` with `run_in_background` and an `until`-loop rather
than a flat `sleep N` — long unconditional sleeps are blocked, and a poll
loop reports back the moment the condition is actually true instead of
guessing a duration.
