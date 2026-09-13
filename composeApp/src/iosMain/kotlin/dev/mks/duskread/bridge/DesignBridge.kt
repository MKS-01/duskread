package dev.mks.duskread.bridge

import androidx.compose.ui.text.font.FontFamily
import dev.mks.duskread.ui.theme.AlgoTypography
import dev.mks.duskread.ui.theme.Palette
import dev.mks.duskread.ui.theme.TypeSpec
import dev.mks.duskread.ui.theme.duskReadTypeSpecs

/**
 * The design system, for the SwiftUI shell.
 */
class DesignBridge internal constructor() {
    /**
     * The font family is irrelevant to the metrics — only sizes, weights, line heights
     * and tracking cross the bridge, and `TypeSpec` names its own family.
     */
    private val specs: Map<String, TypeSpec> = duskReadTypeSpecs(AlgoTypography(FontFamily.Default))

    fun palette(mono: Boolean): Palette = dev.mks.duskread.ui.theme.DesignTokens.palette(mono)

    /** Null for a name that is not in the scale, so Swift fails loudly in a preview. */
    fun typeSpec(role: String): TypeSpec? = specs[role]

    fun typeRoles(): List<String> = specs.keys.toList()
}
