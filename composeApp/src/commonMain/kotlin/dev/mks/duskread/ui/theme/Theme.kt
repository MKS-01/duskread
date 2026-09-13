package dev.mks.duskread.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Paper Black" — a page, not a screen; ink, not a glow.
private val DarkScheme = schemeOf(DesignTokens.paperBlack)

// "Ink" — the same page, with the ink drained out of it.
private val MonoScheme = schemeOf(DesignTokens.ink)

/**
 * Builds a Material scheme from a [Palette].
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
 * Both themes are dark; [mono] picks which.
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
