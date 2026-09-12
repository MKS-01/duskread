package dev.mks.duskread.ui.theme

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/*
 * Hand-drawn icons, as vector paths rather than the Material set.
 *
 * The construction rule is "Bar": anything that can be built from evenly
 * spaced vertical bars is — Readback, Feed, Settings' sliders, the shading on
 * Contrast — because the waveform is the one visual idea this app actually
 * has, and an icon set drawn from it agrees with the data on screen rather
 * than merely sitting next to it. What cannot be built that way (Target,
 * Shuffle, the folder shapes) borrows the same 2.4 weight and round terminal
 * so the set still looks cut from the same clip.
 *
 * The shapes themselves are in [IconPaths] as SVG path data, so the SwiftUI
 * shell draws the same twenty-two glyphs rather than a redrawn copy of them.
 * What stays here is the rule they are all built with — the viewport, the
 * weight, the terminal — which is shared, not per-glyph.
 *
 * `Icon` tints the whole vector, so the stroke colour below is only a
 * placeholder.
 */
private object IconStroke {
    val Colour = SolidColor(androidx.compose.ui.graphics.Color.Black)
    const val Width = 2.4f
}

private fun icon(name: String, path: IconPath) = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = PathParser().parsePathString(path.d).toNodes(),
    fill = if (path.filled) IconStroke.Colour else null,
    stroke = IconStroke.Colour,
    strokeLineWidth = IconStroke.Width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
).build()

object DuskReadIcons {

    /** Home: a roofline over a floor, the floor split at the door like a waveform's centre line. */
    val Home: ImageVector by lazy { icon("Home", IconPaths.Home) }

    /**
     * Practice: a target. Questions are aimed at something specific — the one
     * insight that unlocks them — which a lightning bolt does not say. Rings
     * rather than bars because a target has no bar-built equivalent; it keeps
     * the set's weight and round terminal instead.
     */
    val Target: ImageVector by lazy { icon("Target", IconPaths.Target) }

    /** Clock face with hands at ten-past-ten — for picking or showing a session duration. */
    val Clock: ImageVector by lazy { icon("Clock", IconPaths.Clock) }

    /** A right chevron, for a row that opens something — same weight as [Back], not Material's filled arrow. */
    val Chevron: ImageVector by lazy { icon("Chevron", IconPaths.Chevron) }

    /** Back. Same weight as the rest, which Material's arrow is not. */
    val Back: ImageVector by lazy { icon("Back", IconPaths.Back) }

    /** A small cross for clearing the field. */
    val Close: ImageVector by lazy { icon("Close", IconPaths.Close) }

    /** Play, for starting or resuming a focus session — stroked, like everything else in this set. */
    val Play: ImageVector by lazy { icon("Play", IconPaths.Play) }

    /** Pause, for a focus session in progress — two bars, the same construction as [Waveform]. */
    val Pause: ImageVector by lazy { icon("Pause", IconPaths.Pause) }

    /** Re-roll a random pick — two crossing paths, each ending in an arrow. */
    val Shuffle: ImageVector by lazy { icon("Shuffle", IconPaths.Shuffle) }

    /** Reader: the waveform itself — bars of varying height, like a played-back recording. */
    val Waveform: ImageVector by lazy { icon("Waveform", IconPaths.Waveform) }

    /**
     * A folder with a small link badge — for connecting the Reader to a
     * synced readback folder for the first time.
     */
    val FolderConnect: ImageVector by lazy { icon("FolderConnect", IconPaths.FolderConnect) }

    /** Outbound link, for the web fallbacks in search. */
    val External: ImageVector by lazy { icon("External", IconPaths.External) }

    /**
     * Saved links: a bookmark.
     *
     * Not a chain-link glyph, which is what "link" usually gets — a chain says
     * *this is a URL*, and the tab is not about URLs, it is about things put
     * aside to read. A bookmark is the only thing in this set that means
     * "later".
     */
    val Bookmark: ImageVector by lazy { icon("Bookmark", IconPaths.Bookmark) }

    /** [Bookmark] with the flag inked in: the saved half of the save control. */
    val BookmarkFilled: ImageVector by lazy { icon("BookmarkFilled", IconPaths.BookmarkFilled) }

    /**
     * Feed: three bulleted rules, a list of things followed rather than a
     * single article read.
     *
     * Used to be ascending bars — a signal getting stronger, standing in for
     * the RSS dot and its broadcast arcs — but on the tab bar that read as a
     * literal signal-strength glyph, not "following". A short bar next to a
     * long one is still the set's own vocabulary (the round line cap turns
     * the short one into a dot, so this is bars all the way down) but reads
     * unambiguously as a list.
     */
    val Feed: ImageVector by lazy { icon("Feed", IconPaths.Feed) }

    /**
     * Reader: a column of text with a ragged last line.
     *
     * The one icon in the set that is horizontal rather than barred, and
     * deliberately so — the thing it stands for *is* lines of text, and
     * turning those on their side to satisfy the Bar rule would draw
     * something that no longer says "article".
     */
    val Reader: ImageVector by lazy { icon("Reader", IconPaths.Reader) }

    /**
     * Summary: three rules, each shorter than the last — an article read
     * down to a paragraph.
     *
     * Left-aligned and stepping in from the right, as drawn in the design
     * system's concept sheet. A symmetrical wedge was tried and reads as a
     * filter or a funnel; ragged-right reads as text getting shorter, which
     * is what this actually does.
     */
    val Summary: ImageVector by lazy { icon("Summary", IconPaths.Summary) }

    /** A tick, for marking a saved link read. */
    val Check: ImageVector by lazy { icon("Check", IconPaths.Check) }

    /**
     * Settings: three tracks, each with a knob drawn as a crossing bar rather
     * than a filled dot — the sliders *are* bars, the same construction as
     * [Waveform] and [Feed].
     */
    val Settings: ImageVector by lazy { icon("Settings", IconPaths.Settings) }

    /**
     * Offline: [Feed]'s bars struck through — a fetch that could not reach the
     * network, told apart from a page that simply has nothing better to say
     * for itself. Reuses Feed's exact bars rather than a signal-bar or
     * cloud glyph: this set draws "can't reach the network" as the thing that
     * usually *would* be read failing to arrive, not as a generic warning icon.
     */
    val Offline: ImageVector by lazy { icon("Offline", IconPaths.Offline) }

    /**
     * Contrast: a ring with bars fanning inward, standing in for a half-fill.
     *
     * The theme toggle swaps colour for greyscale, not light for dark, so the
     * usual sun/moon pair would say the wrong thing — this reads as one dial
     * either way, differing only in how much of it is shaded, and the shading
     * is drawn the same way the waveform is: bars, not a flat fill.
     */
    val Contrast: ImageVector by lazy { icon("Contrast", IconPaths.Contrast) }

    /**
     * Search: a ring and a handle, same as everywhere else — one of the few
     * shapes in the set that cannot be built from bars, so it borrows the
     * plain stroke weight and round terminal instead, the way Target and
     * Shuffle already do.
     */
    val Search: ImageVector by lazy { icon("Search", IconPaths.Search) }
}
