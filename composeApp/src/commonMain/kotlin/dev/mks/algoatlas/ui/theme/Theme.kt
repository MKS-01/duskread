package dev.mks.algoatlas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.mks.algoatlas.model.Difficulty
import dev.mks.algoatlas.model.Level
import dev.mks.algoatlas.model.Tone

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
        Level.BASIC -> good
        Level.INTERMEDIATE -> active
        Level.ADVANCED -> bad
    }

    fun of(difficulty: Difficulty): Color = when (difficulty) {
        Difficulty.EASY -> good
        Difficulty.MEDIUM -> active
        Difficulty.HARD -> bad
    }
}

private val LightViz = VizPalette(
    idle = Color(0xFFE8ECF2), onIdle = Color(0xFF37404E),
    active = Color(0xFFF5A524), onActive = Color(0xFF3D2A00),
    good = Color(0xFF17945F), onGood = Color(0xFFFFFFFF),
    bad = Color(0xFFD94A4A), onBad = Color(0xFFFFFFFF),
    info = Color(0xFF3057E3), onInfo = Color(0xFFFFFFFF),
    warn = Color(0xFF9046D8), onWarn = Color(0xFFFFFFFF),
)

private val DarkViz = VizPalette(
    idle = Color(0xFF232B36), onIdle = Color(0xFFB6C0CD),
    active = Color(0xFFF0A92C), onActive = Color(0xFF241800),
    good = Color(0xFF2FBD7E), onGood = Color(0xFF04170E),
    bad = Color(0xFFF0645F), onBad = Color(0xFF240605),
    info = Color(0xFF6F92FF), onInfo = Color(0xFF060C1F),
    warn = Color(0xFFB478EE), onWarn = Color(0xFF14051F),
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
    background = Color(0xFFFBFBFD),
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

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6F92FF),
    onPrimary = Color(0xFF060C1F),
    primaryContainer = Color(0xFF1A2340),
    onPrimaryContainer = Color(0xFFB9C9FF),
    background = Color(0xFF0C0F14),
    onBackground = Color(0xFFE6EAF0),
    surface = Color(0xFF12161D),
    onSurface = Color(0xFFE6EAF0),
    surfaceVariant = Color(0xFF0F131A),
    onSurfaceVariant = Color(0xFF97A1B0),
    surfaceContainer = Color(0xFF161B23),
    surfaceContainerHigh = Color(0xFF1A2029),
    outline = Color(0xFF313A48),
    outlineVariant = Color(0xFF212832),
    error = Color(0xFFF0645F),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3057E3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6ECFD),
    onPrimaryContainer = Color(0xFF17307F),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF10151C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10151C),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF5A6472),
    surfaceContainer = Color(0xFFF0F2F5),
    surfaceContainerHigh = Color(0xFFECEFF3),
    outline = Color(0xFFCBD2DC),
    outlineVariant = Color(0xFFE0E4EA),
    error = Color(0xFFD94A4A),
)

@Composable
fun AlgoAtlasTheme(
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
