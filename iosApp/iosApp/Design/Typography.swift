import ComposeApp
import SwiftUI

/// The type scale, read back from the live Compose `Typography`.
///
/// Roles are named the way Material names them because that is what the shared
/// side calls them; a screen asks for `.dusk(.bodyMedium)` and neither side has
/// to know what that resolves to.
///
/// Both families are bundled. Inconsolata in particular must not fall back to
/// `.monospaced`, which resolves to Menlo here — the exact variance the bundled
/// face was added to remove.
enum TypeRole: String {
    case headlineMedium, headlineSmall
    case titleLarge, titleMedium, titleSmall
    case bodyLarge, bodyMedium, bodySmall
    case labelLarge, labelMedium, labelSmall
    case sectionLabel
    case code
}

struct DuskTextStyle {
    let font: Font
    let tracking: CGFloat
    /// Extra leading, which is what SwiftUI's `lineSpacing` actually means —
    /// not the total line height Compose specifies.
    let lineSpacing: CGFloat
}

enum DuskType {
    private static var cache: [TypeRole: DuskTextStyle] = [:]

    static func style(_ role: TypeRole) -> DuskTextStyle {
        if let hit = cache[role] { return hit }
        guard let spec = design.typeSpec(role: role.rawValue) else {
            assertionFailure("No type spec for \(role.rawValue)")
            return DuskTextStyle(font: .body, tracking: 0, lineSpacing: 0)
        }
        let name = spec.family == "inconsolata"
            ? inconsolata(weight: Int(spec.weight))
            : jost(weight: Int(spec.weight))
        let size = CGFloat(spec.size)
        let made = DuskTextStyle(
            font: .custom(name, size: size),
            tracking: CGFloat(spec.tracking),
            lineSpacing: max(0, CGFloat(spec.lineHeight) - size * 1.2)
        )
        cache[role] = made
        return made
    }

    /// Only the four static weights are bundled, so anything between them
    /// rounds to the nearest one rather than asking for a face that is absent
    /// and getting a synthesised one.
    private static func jost(weight: Int) -> String {
        switch weight {
        case ..<500: return "Jost-Regular"
        case ..<600: return "Jost-Medium"
        case ..<700: return "Jost-SemiBold"
        default: return "Jost-Bold"
        }
    }

    private static func inconsolata(weight: Int) -> String {
        switch weight {
        case ..<500: return "Inconsolata-Regular"
        case ..<600: return "Inconsolata-Medium"
        case ..<700: return "Inconsolata-SemiBold"
        default: return "Inconsolata-Bold"
        }
    }

    /// Set once by `DuskReadHost`; the scale is the same for the app's life.
    static var design: DesignBridge!
}

extension View {
    func dusk(_ role: TypeRole) -> some View {
        let style = DuskType.style(role)
        return self
            .font(style.font)
            .tracking(style.tracking)
            .lineSpacing(style.lineSpacing)
    }
}
