package dev.mks.stacks.ui.theme

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
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFC6684A),
    onPrimary = Color(0xFF2B1006),
    primaryContainer = Color(0xFF352822),
    onPrimaryContainer = Color(0xFFFFD9C0),
    background = Color(0xFF101010),
    onBackground = Color(0xFFE8E6E2),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE8E6E2),
    surfaceVariant = Color(0xFF0D0D0D),
    onSurfaceVariant = Color(0xFFA3A19D),
    surfaceContainer = Color(0xFF212121),
    surfaceContainerHigh = Color(0xFF282828),
    outline = Color(0xFF3E3E3D),
    outlineVariant = Color(0xFF242423),
    error = Color(0xFFF0645F),
)

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
private val MonoScheme = darkColorScheme(
    primary = Color(0xFFDCDCDC),
    onPrimary = Color(0xFF161616),
    primaryContainer = Color(0xFF2E2E2E),
    onPrimaryContainer = Color(0xFFE4E4E4),
    background = Color(0xFF161616),
    onBackground = Color(0xFFDCDCDC),
    surface = Color(0xFF202020),
    onSurface = Color(0xFFDCDCDC),
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFF9C9C9C),
    surfaceContainer = Color(0xFF272727),
    surfaceContainerHigh = Color(0xFF303030),
    outline = Color(0xFF464646),
    outlineVariant = Color(0xFF2B2B2B),
    error = Color(0xFFCBCBCB),
)

/**
 * Both themes are dark; [mono] picks which. The app is read on a phone in the
 * evening, so a light polarity never got used — what the toggle is actually
 * for is dropping colour entirely on the days the terracotta is a distraction.
 */
@Composable
fun StacksTheme(
    mono: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (mono) MonoScheme else DarkScheme,
        typography = AlgoTypography(),
        content = content,
    )
}
