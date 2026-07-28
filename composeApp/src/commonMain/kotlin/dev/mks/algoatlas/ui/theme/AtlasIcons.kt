package dev.mks.algoatlas.ui.theme

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

object AtlasIcons {

    /**
     * Learn: a staircase climbing to the right.
     *
     * The curriculum runs basic to advanced, and every scene in the app is
     * something you step through — so the one motif does both jobs.
     */
    val Steps: ImageVector by lazy {
        icon("Steps") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.5f, 19.5f)
                lineTo(8f, 19.5f)
                lineTo(8f, 14.5f)
                lineTo(13f, 14.5f)
                lineTo(13f, 9.5f)
                lineTo(18f, 9.5f)
                lineTo(18f, 4.5f)
                lineTo(21f, 4.5f)
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

    /** Search. Deliberately the conventional shape — recognition beats novelty here. */
    val Search: ImageVector by lazy {
        icon("Search") {
            path(
                stroke = IconStroke.Colour,
                strokeLineWidth = IconStroke.Width,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(3.8f, 10.6f)
                arcToRelative(6.8f, 6.8f, 0f, true, true, 13.6f, 0f)
                arcToRelative(6.8f, 6.8f, 0f, true, true, -13.6f, 0f)
                close()
                moveTo(15.6f, 15.6f)
                lineTo(20.4f, 20.4f)
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
}
