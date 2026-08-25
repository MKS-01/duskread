package dev.mks.duskread.ui.theme

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
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
 * so the set still looks cut from the same clip. Nothing is filled — Play and
 * Pause used to be the one exception, which is the kind of inconsistency this
 * hand exists to remove.
 *
 * `Icon` tints the whole vector, so the stroke colour below is only a
 * placeholder.
 */
private object IconStroke {
    val Colour = SolidColor(androidx.compose.ui.graphics.Color.Black)
    const val Width = 2.4f
}

private fun icon(name: String, block: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit) = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply(block).build()

/** The set's default: outline only, no fill. */
private fun ImageVector.Builder.stroked(pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
    path(
        stroke = IconStroke.Colour,
        strokeLineWidth = IconStroke.Width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )
}

/**
 * The same outline with its interior filled — the *on* half of a pair, never
 * an icon on its own.
 *
 * The set is otherwise entirely unfilled on purpose, so this exists only
 * where a control has two states that must be told apart at a glance and at
 * icon size: a tint change alone is a colour difference, and a colour
 * difference is the one thing the monochrome scheme deliberately does not
 * have. Filled and hollow survive the palette swap; terracotta and grey do
 * not. Draws the stroke over the fill so both variants keep the same
 * silhouette and optical weight.
 */
private fun ImageVector.Builder.filled(pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
    path(
        fill = IconStroke.Colour,
        stroke = IconStroke.Colour,
        strokeLineWidth = IconStroke.Width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )
}

object DuskReadIcons {

    /** Home: a roofline over a floor, the floor split at the door like a waveform's centre line. */
    val Home: ImageVector by lazy {
        icon("Home") {
            stroked {
                moveTo(4f, 12f)
                lineTo(12f, 5.5f)
                lineTo(20f, 12f)
                moveTo(6.5f, 13.5f)
                lineTo(6.5f, 20f)
                moveTo(17.5f, 13.5f)
                lineTo(17.5f, 20f)
                moveTo(12f, 20f)
                lineTo(12f, 15.5f)
            }
        }
    }

    /**
     * Practice: a target. Questions are aimed at something specific — the one
     * insight that unlocks them — which a lightning bolt does not say. Rings
     * rather than bars because a target has no bar-built equivalent; it keeps
     * the set's weight and round terminal instead.
     */
    val Target: ImageVector by lazy {
        icon("Target") {
            stroked {
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
                // Centre, stroked rather than filled, to match the rest of the set.
                moveTo(11.3f, 12f)
                arcToRelative(0.7f, 0.7f, 0f, true, true, 1.4f, 0f)
                arcToRelative(0.7f, 0.7f, 0f, true, true, -1.4f, 0f)
                close()
            }
        }
    }

    /** Clock face with hands at ten-past-ten — for picking or showing a session duration. */
    val Clock: ImageVector by lazy {
        icon("Clock") {
            stroked {
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
            stroked {
                moveTo(9.5f, 5.5f)
                lineTo(16f, 12f)
                lineTo(9.5f, 18.5f)
            }
        }
    }

    /** Back. Same weight as the rest, which Material's arrow is not. */
    val Back: ImageVector by lazy {
        icon("Back") {
            stroked {
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
            stroked {
                moveTo(6.5f, 6.5f)
                lineTo(17.5f, 17.5f)
                moveTo(17.5f, 6.5f)
                lineTo(6.5f, 17.5f)
            }
        }
    }

    /** Play, for starting or resuming a focus session — stroked, like everything else in this set. */
    val Play: ImageVector by lazy {
        icon("Play") {
            stroked {
                moveTo(8.5f, 5.5f)
                lineTo(18.5f, 12f)
                lineTo(8.5f, 18.5f)
                close()
            }
        }
    }

    /** Pause, for a focus session in progress — two bars, the same construction as [Waveform]. */
    val Pause: ImageVector by lazy {
        icon("Pause") {
            stroked {
                moveTo(9f, 5f)
                lineTo(9f, 19f)
                moveTo(15f, 5f)
                lineTo(15f, 19f)
            }
        }
    }

    /** Re-roll a random pick — two crossing paths, each ending in an arrow. */
    val Shuffle: ImageVector by lazy {
        icon("Shuffle") {
            stroked {
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

    /** Reader: the waveform itself — bars of varying height, like a played-back recording. */
    val Waveform: ImageVector by lazy {
        icon("Waveform") {
            stroked {
                moveTo(4f, 10f)
                lineTo(4f, 14f)
                moveTo(8f, 6.5f)
                lineTo(8f, 17.5f)
                moveTo(12f, 3.5f)
                lineTo(12f, 20.5f)
                moveTo(16f, 6.5f)
                lineTo(16f, 17.5f)
                moveTo(20f, 10f)
                lineTo(20f, 14f)
            }
        }
    }

    /** A plain folder, no connect badge — for switching an already-linked folder, not pairing a new one. */
    val Folder: ImageVector by lazy {
        icon("Folder") {
            stroked {
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
            stroked {
                moveTo(2.5f, 7.5f)
                lineTo(8.5f, 7.5f)
                lineTo(10.5f, 10f)
                lineTo(16.5f, 10f)
                lineTo(16.5f, 17f)
                lineTo(2.5f, 17f)
                close()
            }
            stroked {
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
            stroked {
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
            stroked {
                moveTo(7f, 19.5f)
                lineTo(7f, 5.5f)
                lineTo(17f, 5.5f)
                lineTo(17f, 19.5f)
                lineTo(12f, 15f)
                close()
            }
        }
    }

    /** [Bookmark] with the flag inked in: the saved half of the save control. */
    val BookmarkFilled: ImageVector by lazy {
        icon("BookmarkFilled") {
            filled {
                moveTo(7f, 19.5f)
                lineTo(7f, 5.5f)
                lineTo(17f, 5.5f)
                lineTo(17f, 19.5f)
                lineTo(12f, 15f)
                close()
            }
        }
    }

    /**
     * Feed: ascending bars, like a signal getting stronger — the RSS dot and
     * broadcast arcs redrawn in the set's own vocabulary rather than
     * borrowed from a glyph everyone else already uses.
     */
    val Feed: ImageVector by lazy {
        icon("Feed") {
            stroked {
                moveTo(5f, 19.5f)
                lineTo(5f, 18f)
                moveTo(10f, 19.5f)
                lineTo(10f, 14.5f)
                moveTo(15f, 19.5f)
                lineTo(15f, 10.5f)
                moveTo(20f, 19.5f)
                lineTo(20f, 6.5f)
            }
        }
    }

    /**
     * Reader: a column of text with a ragged last line.
     *
     * The one icon in the set that is horizontal rather than barred, and
     * deliberately so — the thing it stands for *is* lines of text, and
     * turning those on their side to satisfy the Bar rule would draw
     * something that no longer says "article".
     */
    val Reader: ImageVector by lazy {
        icon("Reader") {
            stroked {
                moveTo(5f, 7f)
                lineTo(19f, 7f)
                moveTo(5f, 12f)
                lineTo(19f, 12f)
                moveTo(5f, 17f)
                lineTo(12.5f, 17f)
            }
        }
    }

    /**
     * Summary: three rules, each shorter than the last — an article read
     * down to a paragraph.
     *
     * Left-aligned and stepping in from the right, as drawn in the design
     * system's concept sheet. A symmetrical wedge was tried and reads as a
     * filter or a funnel; ragged-right reads as text getting shorter, which
     * is what this actually does.
     */
    val Summary: ImageVector by lazy {
        icon("Summary") {
            stroked {
                moveTo(4f, 7f)
                lineTo(20f, 7f)
                moveTo(4f, 12f)
                lineTo(15f, 12f)
                moveTo(4f, 17f)
                lineTo(10f, 17f)
            }
        }
    }

    /** A tick, for marking a saved link read. */
    val Check: ImageVector by lazy {
        icon("Check") {
            stroked {
                moveTo(5f, 12.5f)
                lineTo(9.5f, 17f)
                lineTo(19f, 7f)
            }
        }
    }

    /**
     * Settings: three tracks, each with a knob drawn as a crossing bar rather
     * than a filled dot — the sliders *are* bars, the same construction as
     * [Waveform] and [Feed].
     */
    val Settings: ImageVector by lazy {
        icon("Settings") {
            stroked {
                moveTo(4.5f, 8f)
                lineTo(19.5f, 8f)
                moveTo(4.5f, 16f)
                lineTo(19.5f, 16f)
                moveTo(9f, 5f)
                lineTo(9f, 11f)
                moveTo(15f, 13f)
                lineTo(15f, 19f)
            }
        }
    }

    /**
     * Offline: [Feed]'s bars struck through — a fetch that could not reach the
     * network, told apart from a page that simply has nothing better to say
     * for itself. Reuses Feed's exact bars rather than a signal-bar or
     * cloud glyph: this set draws "can't reach the network" as the thing that
     * usually *would* be read failing to arrive, not as a generic warning icon.
     */
    val Offline: ImageVector by lazy {
        icon("Offline") {
            stroked {
                moveTo(5f, 19.5f)
                lineTo(5f, 18f)
                moveTo(10f, 19.5f)
                lineTo(10f, 14.5f)
                moveTo(15f, 19.5f)
                lineTo(15f, 10.5f)
                moveTo(20f, 19.5f)
                lineTo(20f, 6.5f)
                moveTo(4f, 20f)
                lineTo(20f, 4f)
            }
        }
    }

    /**
     * Contrast: a ring with bars fanning inward, standing in for a half-fill.
     *
     * The theme toggle swaps colour for greyscale, not light for dark, so the
     * usual sun/moon pair would say the wrong thing — this reads as one dial
     * either way, differing only in how much of it is shaded, and the shading
     * is drawn the same way the waveform is: bars, not a flat fill.
     */
    val Contrast: ImageVector by lazy {
        icon("Contrast") {
            stroked {
                moveTo(3.6f, 12f)
                arcToRelative(8.4f, 8.4f, 0f, true, true, 16.8f, 0f)
                arcToRelative(8.4f, 8.4f, 0f, true, true, -16.8f, 0f)
                close()
                moveTo(12f, 5.5f)
                lineTo(12f, 18.5f)
                moveTo(15f, 8.5f)
                lineTo(15f, 15.5f)
                moveTo(18f, 11f)
                lineTo(18f, 13f)
            }
        }
    }
}
