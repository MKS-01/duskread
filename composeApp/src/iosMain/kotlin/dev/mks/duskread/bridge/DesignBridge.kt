package dev.mks.duskread.bridge

import androidx.compose.ui.text.font.FontFamily
import dev.mks.duskread.ui.theme.AlgoTypography
import dev.mks.duskread.ui.theme.Palette
import dev.mks.duskread.ui.theme.TypeSpec
import dev.mks.duskread.ui.theme.duskReadTypeSpecs

/**
 * The design system, for the SwiftUI shell.
 *
 * Swift builds its `Color`, `Font` and `CGFloat` values from these rather than
 * from a transcription, so there is one place a radius or an accent changes
 * and both UIs follow. `DesignTokens` is already Compose-free and exports on
 * its own; this exists for the one thing that is not — the type scale, which
 * is read back off the live `Typography` rather than restated.
 */
class DesignBridge internal constructor() {
    /**
     * The font family is irrelevant to the metrics — only sizes, weights,
     * line heights and tracking cross the bridge, and `TypeSpec` names its own
     * family — so the default one is passed rather than loading Jost through
     * a composition this cannot enter.
     */
    private val specs: Map<String, TypeSpec> = duskReadTypeSpecs(AlgoTypography(FontFamily.Default))

    fun palette(mono: Boolean): Palette = dev.mks.duskread.ui.theme.DesignTokens.palette(mono)

    /** Null for a name that is not in the scale, so Swift fails loudly in a preview. */
    fun typeSpec(role: String): TypeSpec? = specs[role]

    fun typeRoles(): List<String> = specs.keys.toList()
}
