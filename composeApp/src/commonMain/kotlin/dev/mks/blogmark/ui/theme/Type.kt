package dev.mks.blogmark.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import blogmark.composeapp.generated.resources.Res
import blogmark.composeapp.generated.resources.inconsolata_bold
import blogmark.composeapp.generated.resources.inconsolata_medium
import blogmark.composeapp.generated.resources.inconsolata_regular
import blogmark.composeapp.generated.resources.inconsolata_semibold
import blogmark.composeapp.generated.resources.jost_bold
import blogmark.composeapp.generated.resources.jost_medium
import blogmark.composeapp.generated.resources.jost_regular
import blogmark.composeapp.generated.resources.jost_semibold
import org.jetbrains.compose.resources.Font

/**
 * Jost, everywhere — a geometric grotesk built from circles and straight
 * lines, which is why it pairs with [BlogmarkIcons]' Bar hand rather than
 * merely tolerating it: both are drawn from the same handful of angles. Four
 * static weights (not the variable font) because static weights are what
 * render correctly on every target Compose Multiplatform reaches here,
 * including Wasm. SIL Open Font License; files under `composeResources/font/`.
 */
@Composable
fun BlogmarkFontFamily(): FontFamily = FontFamily(
    Font(Res.font.jost_regular, FontWeight.Normal),
    Font(Res.font.jost_medium, FontWeight.Medium),
    Font(Res.font.jost_semibold, FontWeight.SemiBold),
    Font(Res.font.jost_bold, FontWeight.Bold),
)

/**
 * Tuned for reading long-form notes on a phone: slightly larger body text and
 * looser line height than the Material defaults, set in [BlogmarkFontFamily]
 * rather than the platform default.
 */
@Suppress("ktlint:standard:function-naming")
fun AlgoTypography(fontFamily: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.styled() = copy(fontFamily = fontFamily)
    return Typography(
        displayLarge = base.displayLarge.styled(),
        displayMedium = base.displayMedium.styled(),
        displaySmall = base.displaySmall.styled(),
        headlineLarge = base.headlineLarge.styled(),
        headlineMedium = base.headlineMedium.styled().copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        ),
        headlineSmall = base.headlineSmall.styled().copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
        ),
        titleLarge = base.titleLarge.styled(),
        titleMedium = base.titleMedium.styled().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.styled().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.styled().copy(fontSize = 15.5.sp, lineHeight = 25.sp),
        bodyMedium = base.bodyMedium.styled().copy(fontSize = 14.5.sp, lineHeight = 22.sp),
        bodySmall = base.bodySmall.styled(),
        labelLarge = base.labelLarge.styled().copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.styled(),
        labelSmall = base.labelSmall.styled().copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        ),
    )
}

/** Section headers: small, uppercase, wide-tracked. */
val SectionLabel: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.sp)

/**
 * Inconsolata, for anything that means data rather than prose: durations,
 * complexity notation, code. Previously `FontFamily.Monospace`, which is a
 * request rather than a font — it resolves to a different face on every
 * platform (Droid Sans Mono on Android, Menlo on iOS, the browser default on
 * Wasm), so the same duration label looked like a different app depending on
 * where it ran. A bundled family removes that variance the same way
 * [BlogmarkFontFamily] already does for prose.
 *
 * Not `@ReadOnlyComposable` — loading a resource font is a `remember`-backed
 * read, which that annotation forbids.
 */
val Mono: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.inconsolata_regular, FontWeight.Normal),
        Font(Res.font.inconsolata_medium, FontWeight.Medium),
        Font(Res.font.inconsolata_semibold, FontWeight.SemiBold),
        Font(Res.font.inconsolata_bold, FontWeight.Bold),
    )

val CodeStyle: TextStyle
    @Composable
    get() = TextStyle(fontFamily = Mono, fontSize = 12.5.sp, lineHeight = 20.sp)
