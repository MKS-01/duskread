---
name: duskread-crash-audit
description: Use when auditing a module (or the whole app) for crash risks —
  either proactively before/after touching it, or in response to "the app
  crashed" with no repro steps or stack trace. Captures the method that found
  three real, previously-unnoticed crashes in reader/ this session (a
  foreground-service timing violation, a MediaPlayer state-machine race, and
  an uncaught exception at a SAF/SQLite boundary reachable on every app
  launch) and turned up nothing in pomodoro/, which has no equivalent risk
  surface — both outcomes are useful, this isn't a "find something" exercise.
---

# DuskRead crash audit

A logcat check alone is not an audit — it only proves nothing crashed
*during the specific interactions you happened to try*. This app has no
tests, so a static, systematic read of the module is what stands in for
them. Load this skill when asked to find crash risks, when a module needs a
pre-flight check before shipping a change to it, or when someone reports a
crash with nothing to reproduce it from.

## The method, in order

1. **Read every file in the module top to bottom first.** Not a grep pass —
   grepping for `!!` or `TODO` finds nothing that isn't already flagged;
   the real risks in this app so far have been in code that looked
   ordinary (a `MediaPlayer.apply { }` block, a `catch` clause one type too
   narrow). List the files before starting so none get skipped silently.
2. **For each external resource with a state machine, check every call site
   against the platform's actual contract** — not against what "seems like
   it should work." `MediaPlayer` is the sharpest example in this codebase:
   `start()`/`pause()`/`seekTo()`/`getCurrentPosition()`/`getDuration()` all
   throw `IllegalStateException` if called before `onPreparedListener` has
   fired, and *any* method but `release()`/`reset()` throws after `release()`
   has been called. A field holding such an object being non-null does not
   mean the object is in a state that's safe to call into — verify against
   the actual state diagram (search the official docs; don't guess from
   memory) before ruling a call site safe.
3. **Check every foreground-service `start()` path for the Android 12+ timing
   rule**: `startForeground()` must land within a few seconds of
   `startForegroundService()`, synchronously in the call chain that handles
   the intent — not from inside an async callback (`onPreparedListener`,
   a network callback) that might fire late or never. If the real work is
   async, claim the foreground state immediately with a placeholder, then
   swap it once the real state is known.
4. **Check exception handling at every system/IO boundary** — SAF file
   access, SQLite queries against a schema this app doesn't own (readback's
   `library.db` can be mid-sync or on a different schema version), network
   calls, `AudioSystem`/platform media APIs. A `catch` scoped to one
   exception type when the boundary can throw several unrelated ones is the
   single most common gap found here — `SQLiteException` was caught while
   `IllegalArgumentException` (from `getColumnIndexOrThrow` on a missing
   column) and `IOException` (from the file copy) were not, in the same
   function. Catch broadly at these true boundaries (the app doesn't
   control the failure modes), but always rethrow `CancellationException`
   first if the function is a suspend function — swallowing it breaks
   structured concurrency.
5. **Check for stale-callback races** wherever a resource can be superseded
   mid-flight — a second `play()` call while the first is still preparing, a
   second network request while the first is in-flight. A callback closure
   captures the specific instance it was registered on, not "whatever the
   current field holds" — if the field can move on before the callback
   fires, guard the callback with an identity check (`if (currentField !==
   theInstanceThisCallbackWasFor) return`).
6. **Check coroutine scopes for correctness**, not just presence: a
   `SupervisorJob`-scoped launch that never surfaces its failure anywhere is
   a silent-failure risk even if it can't crash the process; a `catch
   (e: Exception)` inside a suspend function that doesn't rethrow
   `CancellationException` is a structured-concurrency bug even when nothing
   currently cancels that particular coroutine.
7. **Reproduce the specific race live, don't just patch and hope.** A rapid
   double-tap race is exactly reproducible with a few `adb shell input tap`
   calls fired back-to-back right after starting the action being raced —
   see `duskread-verify` for the device-testing mechanics. Confirm the crash
   *before* the fix (if reasonably cheap to do so) and confirm its absence
   *after*, not just "the code looks right now."

## What "nothing found" looks like, and why it's a valid outcome

`pomodoro/`'s audit (same session) found nothing, correctly — no
`MediaPlayer`-style external resource with a state machine to violate, no
SAF/SQLite boundary, and `startForeground()` is always called synchronously
inside `onStartCommand()`'s own call stack, so the async-timing risk that
hit `reader/` structurally doesn't apply. Report that as clearly as a real
finding — a module that's actually fine doesn't need a fix invented for it,
and saying so explicitly (with the specific reasons it's structurally
different) is more useful than silence.

## Fixing what's found

Fix in the same pass, verify with `duskread-verify`'s device loop, and if the
fix is non-obvious (a guard flag, an identity check, a widened catch) leave a
comment explaining *why* per this repo's KDoc convention — the next reader
needs to know it's guarding against a specific race, not decorative
defensiveness.
