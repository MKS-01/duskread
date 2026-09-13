package dev.mks.duskread.ui.theme

/**
 * The design tokens as plain numbers, for consumers that are not Compose.
 */
object DesignTokens {
    // ---- Colour ----------------------------------------------------------- Two
    // schemes, both dark: the toggle drops the hue, it does not raise the brightness.

    /** "Paper Black" — a page lit by a single terracotta accent. */
    val paperBlack = Palette(
        primary = 0xFFC6684A,
        onPrimary = 0xFF2B1006,
        primaryContainer = 0xFF352822,
        onPrimaryContainer = 0xFFFFD9C0,
        background = 0xFF101010,
        onBackground = 0xFFE8E6E2,
        surface = 0xFF1A1A1A,
        onSurface = 0xFFE8E6E2,
        surfaceVariant = 0xFF0D0D0D,
        onSurfaceVariant = 0xFFA3A19D,
        surfaceContainer = 0xFF212121,
        surfaceContainerHigh = 0xFF282828,
        outline = 0xFF3E3E3D,
        outlineVariant = 0xFF242423,
        error = 0xFFF0645F,
    )

    /** "Ink" — the same page with the hue drained out, error included. */
    val ink = Palette(
        primary = 0xFFDCDCDC,
        onPrimary = 0xFF161616,
        primaryContainer = 0xFF2E2E2E,
        onPrimaryContainer = 0xFFE4E4E4,
        background = 0xFF161616,
        onBackground = 0xFFDCDCDC,
        surface = 0xFF202020,
        onSurface = 0xFFDCDCDC,
        surfaceVariant = 0xFF121212,
        onSurfaceVariant = 0xFF9C9C9C,
        surfaceContainer = 0xFF272727,
        surfaceContainerHigh = 0xFF303030,
        outline = 0xFF464646,
        outlineVariant = 0xFF2B2B2B,
        error = 0xFFCBCBCB,
    )

    /** Ink is the default, so `mono` is the flag that is normally true. */
    fun palette(mono: Boolean): Palette = if (mono) ink else paperBlack

    // ---- Layout, in dp ----------------------------------------------------

    const val ReadingGutter = 18.0
    const val ListGutter = 14.0
    const val WideListGutter = 20.0
    const val BarClearance = 72.0
    const val BarHeight = 56.0
    const val BarInset = 24.0
    const val TwoPaneBreakpoint = 720.0
    const val RailWidth = 64.0
    const val ReadingMeasure = 640.0

    // ---- Radii, strokes and gaps, in dp -----------------------------------

    const val RadiusCard = 14.0
    const val RadiusInline = 10.0

    /** The one to reach for: at these sizes a softened corner, not a round one. */
    const val RadiusChip = 3.0

    const val StrokeHairline = 1.0
    const val ChipGap = 6.0
    const val CardGap = 9.0

    // ---- Motion, in milliseconds ------------------------------------------ Four
    // durations and no curves.

    const val MotionPushIn = 260
    const val MotionPopFade = 160
    const val MotionFade = 180
    const val MotionChip = 220
}

/**
 * One scheme's colours as packed ARGB.
 */
data class Palette(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val surfaceContainer: Long,
    val surfaceContainerHigh: Long,
    val outline: Long,
    val outlineVariant: Long,
    val error: Long,
)

/**
 * One text style, flattened.
 */
data class TypeSpec(
    val family: String,
    val weight: Int,
    val size: Double,
    val lineHeight: Double,
    val tracking: Double,
)
