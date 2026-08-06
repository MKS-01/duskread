package dev.mks.stacks.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.mks.stacks.model.Difficulty
import dev.mks.stacks.model.Level
import dev.mks.stacks.model.Tone

/**
 * Semantic colours for the visualiser, kept out of the Material scheme because
 * they mean something specific ("this element is being compared", "this one is
 * settled") rather than being decorative roles.
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
    // Practice difficulty gets its own three colours rather than reusing
    // good/active/bad: those still mean IDLE/ACTIVE/etc. tones in the
    // visualiser, and a green dot meaning two different things next to each
    // other is worse than two green families. Topic level uses this same
    // trio (see `of(level)` below) — Level and Difficulty now share the
    // same three words (Basic/Intermediate/Advanced) everywhere in the UI,
    // so they share the same three colours too, rather than two blues that
    // would just look like a mismatch on the same label.
    val easy: Color,
    val medium: Color,
    val hard: Color,
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

    fun of(level: Level): Color = when (level) {
        Level.BASIC -> easy
        Level.INTERMEDIATE -> medium
        Level.ADVANCED -> hard
    }

    fun of(difficulty: Difficulty): Color = when (difficulty) {
        Difficulty.EASY -> easy
        Difficulty.MEDIUM -> medium
        Difficulty.HARD -> hard
    }
}

// Good/active/bad/warn were originally full-saturation traffic-light hues,
// which read as louder than everything else in the app — every other colour
// here is a muted tone (info *is* the primary — blue in light mode, orange in
// dark mode). These keep the same hue family per tone (still green/amber/red/
// purple, so the meaning carries over instantly) but pulled down in
// saturation to sit in the same register as the rest of the palette, the same
// move the background/surface colours already make relative to pure black.
private val LightViz = VizPalette(
    idle = Color(0xFFEFE7D6), onIdle = Color(0xFF4A4032),
    active = Color(0xFFC98A3C), onActive = Color(0xFF3D2A00),
    good = Color(0xFF3B8F68), onGood = Color(0xFFFFFFFF),
    bad = Color(0xFFC2685F), onBad = Color(0xFFFFFFFF),
    info = Color(0xFFB5562F), onInfo = Color(0xFFFFFFFF),
    warn = Color(0xFF7C63BE), onWarn = Color(0xFFFFFFFF),
    // A terracotta gradient rather than a second traffic-light set: palest
    // for Easy, the app's own primary orange for Medium, deepest/most
    // saturated for Hard — intensity carries the severity instead of hue.
    easy = Color(0xFFCB9A79),
    medium = Color(0xFFB5562F),
    hard = Color(0xFF7E3A1C),
)

private val DarkViz = VizPalette(
    idle = Color(0xFF232B36), onIdle = Color(0xFFB6C0CD),
    active = Color(0xFFD4A15C), onActive = Color(0xFF241800),
    good = Color(0xFF5CAB8A), onGood = Color(0xFF04170E),
    bad = Color(0xFFCC7A72), onBad = Color(0xFF240605),
    info = Color(0xFFC6684A), onInfo = Color(0xFF2B1006),
    warn = Color(0xFF9C8AD9), onWarn = Color(0xFF14051F),
    easy = Color(0xFFC4A98F),
    medium = Color(0xFFC6684A),
    hard = Color(0xFFB8582F),
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

private val LightCode = CodePalette(
    plain = Color(0xFF1F2328),
    keyword = Color(0xFFCF222E),
    string = Color(0xFF0A3069),
    number = Color(0xFF0550AE),
    comment = Color(0xFF6E7781),
    function = Color(0xFF8250DF),
    type = Color(0xFF953800),
    punctuation = Color(0xFF57606A),
    background = Color(0xFFF3EBDC),
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

val LocalCodePalette = staticCompositionLocalOf { DarkCode }

// "Paper Black" — the dark twin of the light theme's "Paper White" below:
// same idea (a page, not a screen; ink, not a glow), opposite polarity. A
// neutral, matte near-black rather than tinted brown, with soft warm-white
// "ink" text rather than stark white, and the same terracotta accent as
// Paper White so the two read as one theme, not two unrelated palettes.
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

// "Paper White" — an e-reader look, not a software-blue light theme: a warm
// cream page rather than cool white, dark ink-brown text rather than flat
// black, and the same terracotta accent as dark mode so the two themes read
// as one brand rather than two unrelated palettes.
private val LightScheme = lightColorScheme(
    primary = Color(0xFFB5562F),
    onPrimary = Color(0xFFFFFBF3),
    primaryContainer = Color(0xFFF0DCC8),
    onPrimaryContainer = Color(0xFF6B3113),
    background = Color(0xFFF7F1E6),
    onBackground = Color(0xFF2B2620),
    surface = Color(0xFFFAF5EA),
    onSurface = Color(0xFF2B2620),
    surfaceVariant = Color(0xFFEFE7D6),
    onSurfaceVariant = Color(0xFF7A7263),
    surfaceContainer = Color(0xFFEFE7D6),
    surfaceContainerHigh = Color(0xFFE8DEC9),
    outline = Color(0xFFD8CCB0),
    outlineVariant = Color(0xFFE3D9C2),
    error = Color(0xFFC94A3F),
)

@Composable
fun StacksTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalVizPalette provides if (dark) DarkViz else LightViz,
        LocalCodePalette provides if (dark) DarkCode else LightCode,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AlgoTypography(),
            content = content,
        )
    }
}
