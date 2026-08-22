package dev.mks.blogmark.ui.theme

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/*
 * Hand-drawn icons, as vector paths rather than the Material set.
 *
 * Two reasons. They are stroked at a consistent 1.7 units where Material's are
 * filled, which sits better against the thin type and hairline borders used
 * everywhere else. And they can mean something specific to this app — the
 * Learn icon is the same stepped motif as the launcher mark, so the app's one
 * visual idea appears in three places rather than none.
 *
 * `Icon` tints the whole vector, so the stroke colour below is only a
 * placeholder.
 */
private object IconStroke {
    val Colour = SolidColor(androidx.compose.ui.graphics.Color.Black)
    const val Width = 1.7f
}

private fun icon(name: String, block: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit) = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply(block).build()

object BlogmarkIcons {

    /** Home: a roofline over a floor. */
    val Home: ImageVector by lazy {
        icon("Home") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 11.5f)
                lineTo(12f, 4.5f)
                lineTo(20f, 11.5f)
                moveTo(6.2f, 10f)
                lineTo(6.2f, 19.5f)
                lineTo(17.8f, 19.5f)
                lineTo(17.8f, 10f)
            }
        }
    }

    /**
     * Practice: a target. Questions are aimed at something specific — the one
     * insight that unlocks them — which a lightning bolt does not say.
     */
    val Target: ImageVector by lazy {
        icon("Target") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
            ) {
                // Outer ring.
                moveTo(3.6f, 12f)
                arcToRelative(8.4f, 8.4f, 0f, true, true, 16.8f, 0f)
                arcToRelative(8.4f, 8.4f, 0f, true, true, -16.8f, 0f)
                close()
                // Inner ring.
                moveTo(8.6f, 12f)
                arcToRelative(3.4f, 3.4f, 0f, true, true, 6.8f, 0f)
                arcToRelative(3.4f, 3.4f, 0f, true, true, -6.8f, 0f)
                close()
            }
            path(fill = IconStroke.Colour) {
                moveTo(10.7f, 12f)
                arcToRelative(1.3f, 1.3f, 0f, true, true, 2.6f, 0f)
                arcToRelative(1.3f, 1.3f, 0f, true, true, -2.6f, 0f)
                close()
            }
        }
    }

    /** Clock face with hands at ten-past-ten — for picking or showing a session duration. */
    val Clock: ImageVector by lazy {
        icon("Clock") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.6f, 12f)
                arcToRelative(8.4f, 8.4f, 0f, true, true, 16.8f, 0f)
                arcToRelative(8.4f, 8.4f, 0f, true, true, -16.8f, 0f)
                close()
                moveTo(12f, 7.2f)
                lineTo(12f, 12f)
                lineTo(15.2f, 14f)
            }
        }
    }

    /** A right chevron, for a row that opens something — same weight as [Back], not Material's filled arrow. */
    val Chevron: ImageVector by lazy {
        icon("Chevron") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 5f)
                lineTo(16f, 12f)
                lineTo(9f, 19f)
            }
        }
    }

    /** Back. Same weight as the rest, which Material's arrow is not. */
    val Back: ImageVector by lazy {
        icon("Back") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(19.5f, 12f)
                lineTo(4.5f, 12f)
                moveTo(11f, 5f)
                lineTo(4f, 12f)
                lineTo(11f, 19f)
            }
        }
    }

    /** A small cross for clearing the field. */
    val Close: ImageVector by lazy {
        icon("Close") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(6.5f, 6.5f)
                lineTo(17.5f, 17.5f)
                moveTo(17.5f, 6.5f)
                lineTo(6.5f, 17.5f)
            }
        }
    }

    /** Play, for starting or resuming a focus session — filled, not stroked, to read as a button. */
    val Play: ImageVector by lazy {
        icon("Play") {
            path(fill = IconStroke.Colour) {
                moveTo(8f, 5.5f)
                lineTo(19f, 12f)
                lineTo(8f, 18.5f)
                close()
            }
        }
    }

    /** Pause, for a focus session in progress. */
    val Pause: ImageVector by lazy {
        icon("Pause") {
            path(fill = IconStroke.Colour) {
                moveTo(6.5f, 5.5f)
                lineTo(10.5f, 5.5f)
                lineTo(10.5f, 18.5f)
                lineTo(6.5f, 18.5f)
                close()
                moveTo(13.5f, 5.5f)
                lineTo(17.5f, 5.5f)
                lineTo(17.5f, 18.5f)
                lineTo(13.5f, 18.5f)
                close()
            }
        }
    }

    /** Re-roll a random pick — two crossing paths, each ending in an arrow. */
    val Shuffle: ImageVector by lazy {
        icon("Shuffle") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 7f)
                lineTo(19f, 17f)
                moveTo(19f, 17f)
                lineTo(15.5f, 17f)
                moveTo(19f, 17f)
                lineTo(19f, 13.5f)

                moveTo(5f, 17f)
                lineTo(19f, 7f)
                moveTo(19f, 7f)
                lineTo(15.5f, 7f)
                moveTo(19f, 7f)
                lineTo(19f, 10.5f)
            }
        }
    }

    /** Reader: a small waveform — bars of varying height, like a played-back recording. */
    val Waveform: ImageVector by lazy {
        icon("Waveform") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(4.5f, 10f)
                lineTo(4.5f, 14f)
                moveTo(8.5f, 8f)
                lineTo(8.5f, 16f)
                moveTo(12.5f, 5f)
                lineTo(12.5f, 19f)
                moveTo(16.5f, 8f)
                lineTo(16.5f, 16f)
                moveTo(20.5f, 10f)
                lineTo(20.5f, 14f)
            }
        }
    }

    /** A plain folder, no connect badge — for switching an already-linked folder, not pairing a new one. */
    val Folder: ImageVector by lazy {
        icon("Folder") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 6f)
                lineTo(9.5f, 6f)
                lineTo(11.5f, 8.5f)
                lineTo(21f, 8.5f)
                lineTo(21f, 18f)
                lineTo(3f, 18f)
                close()
            }
        }
    }

    /**
     * A folder with a small link badge — for connecting the Reader to a
     * synced readback folder for the first time.
     */
    val FolderConnect: ImageVector by lazy {
        icon("FolderConnect") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(2.5f, 7.5f)
                lineTo(8.5f, 7.5f)
                lineTo(10.5f, 10f)
                lineTo(16.5f, 10f)
                lineTo(16.5f, 17f)
                lineTo(2.5f, 17f)
                close()
            }
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
            ) {
                // Small "connect" badge, bottom right.
                moveTo(17f, 14.8f)
                arcToRelative(3.2f, 3.2f, 0f, true, true, 6.4f, 0f)
                arcToRelative(3.2f, 3.2f, 0f, true, true, -6.4f, 0f)
                close()
                moveTo(20.2f, 13.2f)
                lineTo(20.2f, 16.4f)
                moveTo(18.6f, 14.8f)
                lineTo(21.8f, 14.8f)
            }
        }
    }

    /** Outbound link, for the web fallbacks in search. */
    val External: ImageVector by lazy {
        icon("External") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(13.5f, 4.5f)
                lineTo(19.5f, 4.5f)
                lineTo(19.5f, 10.5f)
                moveTo(19.5f, 4.5f)
                lineTo(11f, 13f)
                moveTo(17f, 14.5f)
                lineTo(17f, 19f)
                arcToRelative(1.5f, 1.5f, 0f, false, true, -1.5f, 1.5f)
                lineTo(6f, 20.5f)
                arcToRelative(1.5f, 1.5f, 0f, false, true, -1.5f, -1.5f)
                lineTo(4.5f, 9f)
                arcToRelative(1.5f, 1.5f, 0f, false, true, 1.5f, -1.5f)
                lineTo(10.5f, 7.5f)
            }
        }
    }

    /**
     * Saved links: a bookmark.
     *
     * Not a chain-link glyph, which is what "link" usually gets — a chain says
     * *this is a URL*, and the tab is not about URLs, it is about things put
     * aside to read. A bookmark is the only thing in this set that means
     * "later".
     */
    val Bookmark: ImageVector by lazy {
        icon("Bookmark") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.5f, 4.5f)
                lineTo(17.5f, 4.5f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
                lineTo(18.5f, 20f)
                lineTo(12f, 15.6f)
                lineTo(5.5f, 20f)
                lineTo(5.5f, 5.5f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
                close()
            }
        }
    }

    /** A tick, for marking a saved link read. */
    val Check: ImageVector by lazy {
        icon("Check") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 12.5f)
                lineTo(10f, 17.5f)
                lineTo(19f, 6.5f)
            }
        }
    }

    /**
     * Contrast: a circle with one half filled.
     *
     * The theme toggle swaps colour for greyscale, not light for dark, so the
     * usual sun/moon pair would say the wrong thing — this is the same circle
     * either way, differing only in how much of it is ink.
     */
    val Contrast: ImageVector by lazy {
        icon("Contrast") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
            ) {
                moveTo(3.6f, 12f)
                arcToRelative(8.4f, 8.4f, 0f, true, true, 16.8f, 0f)
                arcToRelative(8.4f, 8.4f, 0f, true, true, -16.8f, 0f)
                close()
            }
            // The filled half, drawn as its own closed semicircle so the ring
            // above stays an even stroke all the way round.
            path(fill = IconStroke.Colour) {
                moveTo(12f, 3.6f)
                arcToRelative(8.4f, 8.4f, 0f, false, true, 0f, 16.8f)
                close()
            }
        }
    }
}
