package dev.mks.algoatlas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tuned for reading long-form notes on a phone: slightly larger body text and
 * looser line height than the Material defaults.
 */
@Suppress("ktlint:standard:function-naming")
fun AlgoTypography(): Typography {
    val base = Typography()
    return base.copy(
        headlineMedium = base.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
        ),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontSize = 15.5.sp, lineHeight = 25.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 22.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        ),
    )
}

/** Section headers: small, uppercase, wide-tracked. */
val SectionLabel: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.sp)

val Mono: FontFamily = FontFamily.Monospace

val CodeStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = TextStyle(fontFamily = Mono, fontSize = 12.5.sp, lineHeight = 20.sp)
