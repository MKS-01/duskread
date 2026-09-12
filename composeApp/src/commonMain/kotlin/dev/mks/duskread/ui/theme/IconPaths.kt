package dev.mks.duskread.ui.theme

/**
 * The icon set as SVG path data.
 *
 * The glyphs used to be built with Compose's `PathBuilder` DSL, which meant
 * they could only ever be drawn by Compose — and a SwiftUI shell would have
 * needed all twenty-two redrawn by hand, in a second hand, diverging from the
 * first on the first correction. Path strings are the one representation both
 * renderers can read: [DuskReadIcons] parses them into an `ImageVector`, Swift
 * parses the same strings into a `Path`.
 *
 * Every glyph is drawn on a 24x24 viewport and stroked at 2.4 with round caps
 * and joins. That is not per-icon data — it is the construction rule the whole
 * set shares — so it lives in the renderers, not here.
 *
 * Where an icon was two separate stroked paths, it is one path string with two
 * subpaths. With identical stroke settings on both, that draws the same.
 */
object IconPaths {
    val Home = IconPath("M4 12L12 5.5L20 12M6.5 13.5L6.5 20M17.5 13.5L17.5 20M12 20L12 15.5")

    /**
     * Outer ring.
     * Inner ring.
     * Centre, stroked rather than filled, to match the rest of the set.
     */
    val Target = IconPath("M3.6 12a8.4 8.4 0 1 1 16.8 0a8.4 8.4 0 1 1 -16.8 0ZM8.6 12a3.4 3.4 0 1 1 6.8 0a3.4 3.4 0 1 1 -6.8 0ZM11.3 12a0.7 0.7 0 1 1 1.4 0a0.7 0.7 0 1 1 -1.4 0Z")

    val Clock = IconPath("M3.6 12a8.4 8.4 0 1 1 16.8 0a8.4 8.4 0 1 1 -16.8 0ZM12 7.2L12 12L15.2 14")

    val Chevron = IconPath("M9.5 5.5L16 12L9.5 18.5")

    val Back = IconPath("M19.5 12L4.5 12M11 5L4 12L11 19")

    val Close = IconPath("M6.5 6.5L17.5 17.5M17.5 6.5L6.5 17.5")

    val Play = IconPath("M8.5 5.5L18.5 12L8.5 18.5Z")

    val Pause = IconPath("M9 5L9 19M15 5L15 19")

    val Shuffle = IconPath("M5 7L19 17M19 17L15.5 17M19 17L19 13.5M5 17L19 7M19 7L15.5 7M19 7L19 10.5")

    val Waveform = IconPath("M4 10L4 14M8 6.5L8 17.5M12 3.5L12 20.5M16 6.5L16 17.5M20 10L20 14")

    /** Small "connect" badge, bottom right. */
    val FolderConnect = IconPath("M2.5 7.5L8.5 7.5L10.5 10L16.5 10L16.5 17L2.5 17ZM17 14.8a3.2 3.2 0 1 1 6.4 0a3.2 3.2 0 1 1 -6.4 0ZM20.2 13.2L20.2 16.4M18.6 14.8L21.8 14.8")

    val External = IconPath("M13.5 4.5L19.5 4.5L19.5 10.5M19.5 4.5L11 13M17 14.5L17 19a1.5 1.5 0 0 1 -1.5 1.5L6 20.5a1.5 1.5 0 0 1 -1.5 -1.5L4.5 9a1.5 1.5 0 0 1 1.5 -1.5L10.5 7.5")

    val Bookmark = IconPath("M7 19.5L7 5.5L17 5.5L17 19.5L12 15Z")

    val BookmarkFilled = IconPath("M7 19.5L7 5.5L17 5.5L17 19.5L12 15Z", filled = true)

    val Feed = IconPath("M4 7L6 7M9 7L20 7M4 12L6 12M9 12L20 12M4 17L6 17M9 17L20 17")

    val Reader = IconPath("M5 7L19 7M5 12L19 12M5 17L12.5 17")

    val Summary = IconPath("M4 7L20 7M4 12L15 12M4 17L10 17")

    val Check = IconPath("M5 12.5L9.5 17L19 7")

    val Settings = IconPath("M4.5 8L19.5 8M4.5 16L19.5 16M9 5L9 11M15 13L15 19")

    val Offline = IconPath("M5 19.5L5 18M10 19.5L10 14.5M15 19.5L15 10.5M20 19.5L20 6.5M4 20L20 4")

    val Contrast = IconPath("M3.6 12a8.4 8.4 0 1 1 16.8 0a8.4 8.4 0 1 1 -16.8 0ZM12 5.5L12 18.5M15 8.5L15 15.5M18 11L18 13")

    val Search = IconPath("M4.5 11a6.5 6.5 0 1 1 13 0a6.5 6.5 0 1 1 -13 0ZM15.8 15.8L20 20")
}

/**
 * One glyph.
 *
 * [filled] is the *on* half of a pair and nothing else. The set is otherwise
 * entirely unfilled on purpose: a control with two states has to be tellable
 * apart at icon size, and a tint change is a colour difference — the one thing
 * the Ink scheme deliberately does not have. Filled and hollow survive the
 * palette swap; terracotta and grey do not.
 */
data class IconPath(val d: String, val filled: Boolean = false)
