package dev.mks.blogmark.ui.theme

import androidx.compose.ui.unit.dp

/*
 * Design tokens: the values that carry a decision rather than a measurement.
 *
 * These are plain objects rather than CompositionLocals because none of them
 * vary by theme, platform or window size — a card is 16dp round in dark mode
 * too. Colour and typography, which *do* vary, live in [BlogmarkTheme] and
 * are reached through `MaterialTheme`.
 *
 * **What belongs here:** a value used in more than one place, or one whose
 * exact number matters to how the app reads. **What does not:** optical
 * one-offs. Padding of
 * `top = 14.dp, bottom = 13.dp` is a nudge to make a specific label sit right,
 * not a rule, and naming it would imply a system that is not there.
 *
 * Spacing follows a 2dp rhythm. New values should land on it.
 */

/** Sizes that shape the page rather than decorate it. */
object Layout {
    /** Horizontal padding for reading surfaces. */
    val ReadingGutter = 18.dp

    /** Horizontal padding for list surfaces, which carry their own card insets. */
    val ListGutter = 14.dp

    /** Bottom inset so the last item clears the floating bar. */
    val BarClearance = 72.dp
}

/** Corner radii, largest to smallest. */
object Radius {
    /**
     * Dashboard and list cards. 14dp rather than the 20dp this used to be —
     * the app draws its own data (waveforms, meters, a stroked icon set) and
     * a heavily rounded card reads as a generic Material surface sitting on
     * top of it rather than a panel drawn in the same instrument-panel hand.
     */
    val Card = 14.dp

    /** Panels inside a card — origin note, code block, complexity table. */
    val Panel = 12.dp

    /** Inline surfaces: reference rows, list-pane rows. */
    val Inline = 10.dp

    /**
     * The mockup's `.pill` and `.sourcechip`: 3dp, which at these sizes is a
     * softened corner rather than a rounded one. Anything rounder turns a
     * sort control into a Material chip and the source cell into an avatar,
     * both of which fight the squared-off waveform they sit beside.
     */
    val Chip = 3.dp
}

/** Line weights. */
object Stroke {
    val Hairline = 1.dp
}

/** Gaps that recur. Anything used once stays a literal at its call site. */
object Space {
    /** Between chips in a row. */
    val ChipGap = 6.dp

    /** Between cards in a list. */
    val CardGap = 9.dp
}

/**
 * Durations, in milliseconds.
 *
 * The visualiser and the navigation deliberately move at different speeds:
 * navigation should feel immediate, whereas a tone change is teaching — the
 * reader has to *see* an element switch from being examined to being
 * discarded, so it is slow enough to follow.
 */
object Motion {
    /** Pushing to a full-screen destination: the incoming screen slides and fades in. */
    const val PushIn = 260

    /** …fading slightly slower than it slides, so it does not vanish mid-travel. */
    const val PopFade = 160

    /** Cross-fades that should not draw attention: tab and pane swaps. */
    const val Fade = 180

    /** Chip and bar state changes. */
    const val Chip = 220
}
