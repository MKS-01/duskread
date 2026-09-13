package dev.mks.duskread.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import duskread.composeapp.generated.resources.Res
import duskread.composeapp.generated.resources.inconsolata_bold
import duskread.composeapp.generated.resources.inconsolata_medium
import duskread.composeapp.generated.resources.inconsolata_regular
import duskread.composeapp.generated.resources.inconsolata_semibold
import duskread.composeapp.generated.resources.jost_bold
import duskread.composeapp.generated.resources.jost_medium
import duskread.composeapp.generated.resources.jost_regular
import duskread.composeapp.generated.resources.jost_semibold
import org.jetbrains.compose.resources.Font

/**
 * Jost, everywhere — a geometric grotesk built from circles and straight lines, which is
 * why it pairs with [DuskReadIcons]' Bar hand rather than merely tolerating it.
 */
@Composable
fun DuskReadFontFamily(): FontFamily = FontFamily(
    Font(Res.font.jost_regular, FontWeight.Normal),
    Font(Res.font.jost_medium, FontWeight.Medium),
    Font(Res.font.jost_semibold, FontWeight.SemiBold),
    Font(Res.font.jost_bold, FontWeight.Bold),
)

/**
 * Tuned for reading long-form notes on a phone: slightly larger body text and looser line
 * height than the Material defaults.
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
 * Inconsolata, for anything that means data rather than prose: durations, complexity
 * notation, code.
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

/**
 * The type scale flattened to plain numbers, for the SwiftUI shell. Read off the live
 * [Typography] rather than restated as literals.
 */
fun duskReadTypeSpecs(typography: Typography): Map<String, TypeSpec> {
    fun spec(style: TextStyle, family: String = "jost") = TypeSpec(
        family = family,
        weight = style.fontWeight?.weight ?: FontWeight.Normal.weight,
        size = style.fontSize.value.toDouble(),
        lineHeight = style.lineHeight.value.toDouble(),
        tracking = style.letterSpacing.value.toDouble(),
    )
    return mapOf(
        "headlineMedium" to spec(typography.headlineMedium),
        "headlineSmall" to spec(typography.headlineSmall),
        "titleLarge" to spec(typography.titleLarge),
        "titleMedium" to spec(typography.titleMedium),
        "titleSmall" to spec(typography.titleSmall),
        "bodyLarge" to spec(typography.bodyLarge),
        "bodyMedium" to spec(typography.bodyMedium),
        "bodySmall" to spec(typography.bodySmall),
        "labelLarge" to spec(typography.labelLarge),
        "labelMedium" to spec(typography.labelMedium),
        "labelSmall" to spec(typography.labelSmall),
        // The two the app owns outright. SectionLabel is labelSmall reopened at 11sp/1sp
        // tracking; CodeStyle is the only mono style there is.
        "sectionLabel" to spec(typography.labelSmall).copy(size = 11.0, tracking = 1.0),
        "code" to TypeSpec(family = "inconsolata", weight = FontWeight.Normal.weight, size = 12.5, lineHeight = 20.0, tracking = 0.0),
    )
}
