package dev.mks.stacks.ui.theme

import androidx.compose.ui.unit.dp

/*
 * Design tokens: the values that carry a decision rather than a measurement.
 *
 * These are plain objects rather than CompositionLocals because none of them
 * vary by theme, platform or window size — a card is 16dp round in dark mode
 * too. Colour and typography, which *do* vary, live in [StacksTheme] and
 * are reached through `MaterialTheme` and `LocalVizPalette`.
 *
 * **What belongs here:** a value used in more than one place, or one whose
 * exact number matters to how the app reads (the two-pane breakpoint, the
 * tone-change duration). **What does not:** optical one-offs. Padding of
 * `top = 14.dp, bottom = 13.dp` is a nudge to make a specific label sit right,
 * not a rule, and naming it would imply a system that is not there.
 *
 * Spacing follows a 2dp rhythm. New values should land on it.
 */

/** Sizes that shape the page rather than decorate it. */
object Layout {
    /** Above this width the list and the topic sit side by side. */
    val TwoPaneBreakpoint = 720.dp

    /** The list column in two-pane mode. */
    val ListPaneWidth = 320.dp

    /** Long-form prose stops widening here — beyond it, lines get hard to track. */
    val ReadingMaxWidth = 860.dp

    /** Horizontal padding for reading surfaces. */
    val ReadingGutter = 18.dp

    /** Horizontal padding for list surfaces, which carry their own card insets. */
    val ListGutter = 14.dp

    /** Bottom inset so the last item clears the floating bar. */
    val BarClearance = 72.dp
}

/** Corner radii, largest to smallest. */
object Radius {
    /** Topic and question cards. */
    val Card = 16.dp

    /** Panels inside a card — origin note, code block, complexity table. */
    val Panel = 12.dp

    /** Inline surfaces: reference rows, list-pane rows. */
    val Inline = 10.dp

    /** Difficulty and level chips, and the floating bar itself. */
    val Pill = 999.dp

    /** The level marker down the side of a topic card. */
    val Marker = 2.dp
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
    /** A cell, node or bar changing [dev.mks.stacks.model.Tone]. */
    const val Tone = 320

    /** Pushing to a topic: the incoming screen slides and fades in. */
    const val PushIn = 260

    /** The outgoing screen just fades. */
    const val PushOut = 140

    /** Coming back: the topic screen fades in. */
    const val PopIn = 200

    /** The leaving screen slides back out. */
    const val PopOut = 240

    /** …fading slightly slower than it slides, so it does not vanish mid-travel. */
    const val PopFade = 160

    /** Cross-fades that should not draw attention: tab and pane swaps. */
    const val Fade = 180

    /** Chip and bar state changes. */
    const val Chip = 220

    /** Autoplay dwell per frame at 1x, before the speed multiplier. */
    const val FrameDwell = 950f
}
