package dev.mks.duskread.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Paper Black" — a page, not a screen; ink, not a glow. A neutral, matte
// near-black rather than tinted brown, with soft warm-white "ink" text rather
// than stark white, lit by a single terracotta accent.
//
// `background` sits just above pure black — close enough to still save real
// power on an OLED/AMOLED panel (background is by far the largest area on
// screen), but not so flat that it loses depth against the cards. Cards get
// a slightly lifted `surface` so they read as raised above that background.
private val DarkScheme = schemeOf(DesignTokens.paperBlack)

// "Ink" — the same page, with the ink drained out of it. Not a second dark
// theme in a different hue but the *absence* of hue: black through white and
// nothing else, so the only things that can distinguish one element from
// another are lightness, weight and spacing.
//
// Neither end of the range is taken all the way. The ground is a soft
// charcoal rather than #000 and the ink stops short of #FFF: with no hue
// anywhere, a true-black-to-true-white span is the harshest possible contrast
// and reads as glare on a phone at night. Pulling both ends in costs a little
// range but leaves the greys sitting in a band the eye can rest on, and the
// steps between surfaces stay visible because they are spaced, not extreme.
//
// `primary` is the lightest ink: in a scheme with no colour, "the accent" can
// only mean the brightest thing on the page. `error` stays grey rather than
// sneaking a red back in — anything that has to read as wrong here is loud
// through brightness and wording, not hue.
private val MonoScheme = schemeOf(DesignTokens.ink)

/**
 * Builds a Material scheme from a [Palette].
 *
 * The values themselves live in [DesignTokens] rather than here, so the
 * SwiftUI shell reads the same fifteen numbers this does instead of carrying
 * a transcription of them. Everything not named falls through to the Material
 * 3 dark baseline, which is what both schemes did when the literals sat in
 * this file.
 */
private fun schemeOf(palette: Palette) = darkColorScheme(
    primary = Color(palette.primary),
    onPrimary = Color(palette.onPrimary),
    primaryContainer = Color(palette.primaryContainer),
    onPrimaryContainer = Color(palette.onPrimaryContainer),
    background = Color(palette.background),
    onBackground = Color(palette.onBackground),
    surface = Color(palette.surface),
    onSurface = Color(palette.onSurface),
    surfaceVariant = Color(palette.surfaceVariant),
    onSurfaceVariant = Color(palette.onSurfaceVariant),
    surfaceContainer = Color(palette.surfaceContainer),
    surfaceContainerHigh = Color(palette.surfaceContainerHigh),
    outline = Color(palette.outline),
    outlineVariant = Color(palette.outlineVariant),
    error = Color(palette.error),
)

/**
 * Both themes are dark; [mono] picks which. The app is read on a phone in the
 * evening, so a light polarity never got used — what the toggle is actually
 * for is dropping colour entirely on the days the accent is a distraction.
 *
 * One accent, not a choice of them. A picker briefly offered a second
 * (a green at the terracotta's own saturation), and the answer was that a
 * second accent is not a feature: the whole argument of this palette is that
 * exactly one hue means exactly one thing, and letting the reader swap which
 * hue that is buys nothing while giving the palette a dial it has to justify.
 */
@Composable
fun DuskReadTheme(
    mono: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (mono) MonoScheme else DarkScheme,
        typography = AlgoTypography(DuskReadFontFamily()),
        content = content,
    )
}
