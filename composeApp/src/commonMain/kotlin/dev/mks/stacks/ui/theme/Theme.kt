package dev.mks.stacks.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
/** Semantic colour roles. The theme maps these to concrete colours. */
enum class Tone { IDLE, ACTIVE, GOOD, BAD, INFO, WARN }

/**
 * Semantic colours for state, kept out of the Material scheme because they
 * mean something specific ("this is settled", "this needs attention") rather
 * than being decorative roles.
 */
@Immutable
data class VizPalette(
    val idle: Color,
    val onIdle: Color,
    val active: Color,
    val onActive: Color,
    val good: Color,
    val onGood: Color,
    val bad: Color,
    val onBad: Color,
    val info: Color,
    val onInfo: Color,
    val warn: Color,
    val onWarn: Color,
) {
    fun bg(tone: Tone): Color = when (tone) {
        Tone.IDLE -> idle
        Tone.ACTIVE -> active
        Tone.GOOD -> good
        Tone.BAD -> bad
        Tone.INFO -> info
        Tone.WARN -> warn
    }

    fun fg(tone: Tone): Color = when (tone) {
        Tone.IDLE -> onIdle
        Tone.ACTIVE -> onActive
        Tone.GOOD -> onGood
        Tone.BAD -> onBad
        Tone.INFO -> onInfo
        Tone.WARN -> onWarn
    }
}

// Good/active/bad/warn were originally full-saturation traffic-light hues,
// which read as louder than everything else in the app — every other colour
// here is a muted tone (info *is* the primary orange). These keep the same
// hue family per tone (still green/amber/red/purple, so the meaning carries
// over instantly) but pulled down in saturation to sit in the same register
// as the rest of the palette, the same move the background/surface colours
// already make relative to pure black.
private val DarkViz = VizPalette(
    idle = Color(0xFF232B36), onIdle = Color(0xFFB6C0CD),
    active = Color(0xFFD4A15C), onActive = Color(0xFF241800),
    good = Color(0xFF5CAB8A), onGood = Color(0xFF04170E),
    bad = Color(0xFFCC7A72), onBad = Color(0xFF240605),
    info = Color(0xFFC6684A), onInfo = Color(0xFF2B1006),
    warn = Color(0xFF9C8AD9), onWarn = Color(0xFF14051F),
)

// The monochrome twin. Hue is the whole encoding in [DarkViz] — "green means
// settled" — so stripping it means the tones have to be told apart by
// lightness alone, and the six then have to be spaced far enough apart to
// survive that. They are ordered by how much attention each deserves rather
// than by any traffic-light convention: ACTIVE is the brightest because it is
// the one element the reader should be looking at, IDLE the dimmest because
// it is the background of the structure, and the rest fall between.
private val MonoViz = VizPalette(
    idle = Color(0xFF262626), onIdle = Color(0xFF9C9C9C),
    active = Color(0xFFDCDCDC), onActive = Color(0xFF161616),
    good = Color(0xFF9C9C9C), onGood = Color(0xFF161616),
    bad = Color(0xFF565656), onBad = Color(0xFFDCDCDC),
    info = Color(0xFFBBBBBB), onInfo = Color(0xFF161616),
    warn = Color(0xFF787878), onWarn = Color(0xFFDCDCDC),
)

val LocalVizPalette = staticCompositionLocalOf { DarkViz }

/** Syntax-highlighting colours, shared by all three languages. */
@Immutable
data class CodePalette(
    val plain: Color,
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val function: Color,
    val type: Color,
    val punctuation: Color,
    val background: Color,
)

private val DarkCode = CodePalette(
    plain = Color(0xFFD1D7E0),
    keyword = Color(0xFFFF7B72),
    string = Color(0xFFA5D6FF),
    number = Color(0xFF79C0FF),
    comment = Color(0xFF8B949E),
    function = Color(0xFFD2A8FF),
    type = Color(0xFFFFA657),
    punctuation = Color(0xFF8D96A0),
    background = Color(0xFF0F131A),
)

// Highlighting without hue. Rather than nine near-identical greys, the roles
// collapse into four legibility bands — comments and punctuation recede,
// plain code sits at reading weight, and the two things you actually scan a
// snippet for (keywords and the names being declared or called) come forward.
// Type and string sit a step under those so a line still has texture.
private val MonoCode = CodePalette(
    plain = Color(0xFFC2C2C2),
    keyword = Color(0xFFDCDCDC),
    string = Color(0xFF9E9E9E),
    number = Color(0xFF9E9E9E),
    comment = Color(0xFF6E6E6E),
    function = Color(0xFFD2D2D2),
    type = Color(0xFFD2D2D2),
    punctuation = Color(0xFF808080),
    background = Color(0xFF171717),
)

val LocalCodePalette = staticCompositionLocalOf { DarkCode }

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
    CompositionLocalProvider(
        LocalVizPalette provides if (mono) MonoViz else DarkViz,
        LocalCodePalette provides if (mono) MonoCode else DarkCode,
    ) {
        MaterialTheme(
            colorScheme = if (mono) MonoScheme else DarkScheme,
            typography = AlgoTypography(),
            content = content,
        )
    }
}
