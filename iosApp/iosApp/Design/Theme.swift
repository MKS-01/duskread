import ComposeApp
import SwiftUI

/// The two schemes, and the axis they differ on.
///
/// Not `@Environment(\.colorScheme)`: both schemes are dark, and what the
/// toggle changes is whether there is any hue at all. Reading the system's
/// light/dark setting would answer a question this app does not ask — and Ink,
/// the colourless one, is the default.
struct DuskTheme {
    let mono: Bool

    var palette: Palette { DesignTokens.shared.palette(mono: mono) }

    var primary: Color { Color(argb: palette.primary) }
    var onPrimary: Color { Color(argb: palette.onPrimary) }
    var primaryContainer: Color { Color(argb: palette.primaryContainer) }
    var onPrimaryContainer: Color { Color(argb: palette.onPrimaryContainer) }
    var background: Color { Color(argb: palette.background) }
    var onBackground: Color { Color(argb: palette.onBackground) }
    var surface: Color { Color(argb: palette.surface) }
    var onSurface: Color { Color(argb: palette.onSurface) }
    var surfaceVariant: Color { Color(argb: palette.surfaceVariant) }
    var onSurfaceVariant: Color { Color(argb: palette.onSurfaceVariant) }
    var surfaceContainer: Color { Color(argb: palette.surfaceContainer) }
    var surfaceContainerHigh: Color { Color(argb: palette.surfaceContainerHigh) }
    var outline: Color { Color(argb: palette.outline) }
    var outlineVariant: Color { Color(argb: palette.outlineVariant) }
    var error: Color { Color(argb: palette.error) }
}

private struct DuskThemeKey: EnvironmentKey {
    static let defaultValue = DuskTheme(mono: true)
}

extension EnvironmentValues {
    var dusk: DuskTheme {
        get { self[DuskThemeKey.self] }
        set { self[DuskThemeKey.self] = newValue }
    }
}

extension Color {
    /// Packed ARGB from Kotlin, which is how `DesignTokens` carries a colour —
    /// `Color` has no Obj-C representation, and a hex string would only be a
    /// number pretending not to be one.
    init(argb: Int64) {
        let value = UInt64(bitPattern: Int64(argb))
        self.init(
            .sRGB,
            red: Double((value >> 16) & 0xFF) / 255,
            green: Double((value >> 8) & 0xFF) / 255,
            blue: Double(value & 0xFF) / 255,
            opacity: Double((value >> 24) & 0xFF) / 255
        )
    }
}
