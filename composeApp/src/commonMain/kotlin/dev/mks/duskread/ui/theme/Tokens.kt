package dev.mks.duskread.ui.theme

import androidx.compose.ui.unit.dp

/*
 * Design tokens: the values that carry a decision rather than a measurement.
 *
 * These are plain objects rather than CompositionLocals because none of them
 * vary by theme, platform or window size — a card is 16dp round in dark mode
 * too. Colour and typography, which *do* vary, live in [DuskReadTheme] and
 * are reached through `MaterialTheme`.
 *
 * **What belongs here:** a value used in more than one place, or one whose
 * exact number matters to how the app reads. **What does not:** optical
 * one-offs. Padding of
 * `top = 14.dp, bottom = 13.dp` is a nudge to make a specific label sit right,
 * not a rule, and naming it would imply a system that is not there.
 *
 * Spacing follows a 2dp rhythm. New values should land on it.
 *
 * The numbers themselves have moved to [DesignTokens], which carries no
 * Compose types and so can be read from Swift. These objects stay because
 * they are what the Compose UI reads — this file is now the `Dp` face of the
 * same values, not a second set of them.
 */

/** Sizes that shape the page rather than decorate it. */
object Layout {
    /** Horizontal padding for reading surfaces. */
    val ReadingGutter = DesignTokens.ReadingGutter.dp

    /** Horizontal padding for list surfaces, which carry their own card insets. */
    val ListGutter = DesignTokens.ListGutter.dp

    /** Bottom inset so the last item clears the floating bar. */
    val BarClearance = DesignTokens.BarClearance.dp

    /** Every face of the floating bar is this tall; only the width changes between them. */
    val BarHeight = DesignTokens.BarHeight.dp

    /**
     * What the floating bar keeps between itself and the safe area.
     */
    val BarInset = DesignTokens.BarInset.dp

    /**
     * The one width that changes the plan: below it the floating bar, above it the rail.
     */
    val TwoPaneBreakpoint = DesignTokens.TwoPaneBreakpoint.dp

    /**
     * The vertical navigation rail replacing the floating bar when wide. Narrow enough to
     * read as an edge rather than a column of its own.
     */
    val RailWidth = DesignTokens.RailWidth.dp

    /**
     * The widest a column of prose is allowed to get, regardless of how much room there
     * is around it.
     */
    val ReadingMeasure = DesignTokens.ReadingMeasure.dp

    /** [ListGutter], opened up once there is room. */
    val WideListGutter = DesignTokens.WideListGutter.dp
}

/** Corner radii, largest to smallest. */
object Radius {
    /**
     * Dashboard and list cards.
     */
    val Card = DesignTokens.RadiusCard.dp

    /**
     * Everything that sits inside or below a card: the filled call-to-action, text
     * fields, dashboard and list rows, the timer's state chips.
     */
    val Inline = DesignTokens.RadiusInline.dp

    /**
     * The mockup's `.pill` and `.sourcechip`: 3dp, which at these sizes is a softened
     * corner rather than a rounded one.
     */
    val Chip = DesignTokens.RadiusChip.dp
}

/** Line weights. */
object Stroke {
    val Hairline = DesignTokens.StrokeHairline.dp
}

/** Gaps that recur. Anything used once stays a literal at its call site. */
object Space {
    /** Between chips in a row. */
    val ChipGap = DesignTokens.ChipGap.dp

    /** Between cards in a list. */
    val CardGap = DesignTokens.CardGap.dp
}

/**
 * Durations, in milliseconds.
 */
object Motion {
    /** Pushing to a full-screen destination: the incoming screen slides and fades in. */
    const val PushIn = DesignTokens.MotionPushIn

    /** …fading slightly slower than it slides, so it does not vanish mid-travel. */
    const val PopFade = DesignTokens.MotionPopFade

    /** Cross-fades that should not draw attention: tab and pane swaps. */
    const val Fade = DesignTokens.MotionFade

    /** Chip and bar state changes. */
    const val Chip = DesignTokens.MotionChip
}
