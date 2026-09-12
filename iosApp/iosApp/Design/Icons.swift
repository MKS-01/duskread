import ComposeApp
import SwiftUI

/// The shared icon set, drawn from the same path data Compose parses.
///
/// Not SF Symbols. The set is built from one construction rule at one stroke
/// weight and one terminal, and mixing in eight system glyphs would break that
/// visibly — SF's optical weight does not match a 2.4-on-24 round-capped
/// stroke. SF Symbols are for chrome that is *meant* to look like the OS: the
/// share sheet, system context menus.
struct DuskIcon: View {
    let path: IconPath
    var size: CGFloat = 24
    var tint: Color = .primary

    var body: some View {
        SVGPath(d: path.d)
            .scaled(to: size)
            .stroked(filled: path.filled, width: 2.4 * size / 24, tint: tint)
            .frame(width: size, height: size)
    }
}

/// A parsed `d` string.
///
/// Deliberately handles only the four commands the set actually uses — `M`,
/// `L`, relative arc, `Z`. A general SVG parser would be a lot of surface
/// area for shapes we generate ourselves and can keep simple; anything new
/// should extend this consciously rather than arrive unnoticed.
struct SVGPath: Shape {
    let d: String

    func path(in rect: CGRect) -> Path {
        var path = Path()
        var cursor = CGPoint.zero
        var start = CGPoint.zero

        var tokens = Tokeniser(d)
        while let command = tokens.command() {
            switch command {
            case "M":
                cursor = CGPoint(x: tokens.number(), y: tokens.number())
                start = cursor
                path.move(to: cursor)
            case "L":
                cursor = CGPoint(x: tokens.number(), y: tokens.number())
                path.addLine(to: cursor)
            case "a":
                let rx = tokens.number(), ry = tokens.number()
                let rotation = tokens.number()
                let largeArc = tokens.number() != 0
                let sweep = tokens.number() != 0
                let end = CGPoint(x: cursor.x + tokens.number(), y: cursor.y + tokens.number())
                path.addRelativeArc(from: cursor, to: end, rx: rx, ry: ry,
                                    rotation: rotation, largeArc: largeArc, sweep: sweep)
                cursor = end
            case "Z":
                path.closeSubpath()
                cursor = start
            default:
                assertionFailure("Unsupported path command \(command)")
                return path
            }
        }
        return path
    }
}

private struct Tokeniser {
    private let scalars: [Character]
    private var index = 0

    init(_ text: String) { scalars = Array(text) }

    mutating func command() -> Character? {
        skipSeparators()
        guard index < scalars.count, scalars[index].isLetter else { return nil }
        defer { index += 1 }
        return scalars[index]
    }

    mutating func number() -> CGFloat {
        skipSeparators()
        var text = ""
        while index < scalars.count {
            let c = scalars[index]
            if c.isNumber || c == "." || (c == "-" && text.isEmpty) {
                text.append(c); index += 1
            } else { break }
        }
        return CGFloat(Double(text) ?? 0)
    }

    private mutating func skipSeparators() {
        while index < scalars.count, scalars[index] == " " || scalars[index] == "," {
            index += 1
        }
    }
}

private extension Path {
    /// SVG's endpoint-parameterised arc, converted to a centre-parameterised
    /// one. Every arc in this set is a half-circle pair making a ring, so the
    /// general conversion is more than is strictly needed — but a special case
    /// that only works for circles would break the first time an icon is
    /// redrawn.
    mutating func addRelativeArc(from: CGPoint, to: CGPoint, rx: CGFloat, ry: CGFloat,
                                 rotation: CGFloat, largeArc: Bool, sweep: Bool) {
        guard rx != 0, ry != 0 else { addLine(to: to); return }

        let phi = rotation * .pi / 180
        let dx2 = (from.x - to.x) / 2, dy2 = (from.y - to.y) / 2
        let x1 = cos(phi) * dx2 + sin(phi) * dy2
        let y1 = -sin(phi) * dx2 + cos(phi) * dy2

        var rx = abs(rx), ry = abs(ry)
        let lambda = (x1 * x1) / (rx * rx) + (y1 * y1) / (ry * ry)
        if lambda > 1 { rx *= sqrt(lambda); ry *= sqrt(lambda) }

        let sign: CGFloat = largeArc == sweep ? -1 : 1
        let numerator = max(0, rx * rx * ry * ry - rx * rx * y1 * y1 - ry * ry * x1 * x1)
        let denominator = rx * rx * y1 * y1 + ry * ry * x1 * x1
        let coefficient = denominator == 0 ? 0 : sign * sqrt(numerator / denominator)
        let cx1 = coefficient * rx * y1 / ry
        let cy1 = -coefficient * ry * x1 / rx

        let cx = cos(phi) * cx1 - sin(phi) * cy1 + (from.x + to.x) / 2
        let cy = sin(phi) * cx1 + cos(phi) * cy1 + (from.y + to.y) / 2

        let startAngle = atan2((y1 - cy1) / ry, (x1 - cx1) / rx)
        var endAngle = atan2((-y1 - cy1) / ry, (-x1 - cx1) / rx)
        if sweep, endAngle < startAngle { endAngle += 2 * .pi }
        if !sweep, endAngle > startAngle { endAngle -= 2 * .pi }

        // Ellipses are drawn as a transformed circle: SwiftUI has no
        // ellipse-arc primitive, and scaling the unit arc is exact.
        var transform = CGAffineTransform(translationX: cx, y: cy)
            .rotated(by: phi)
            .scaledBy(x: rx, y: ry)
        var arc = Path()
        arc.addArc(center: .zero, radius: 1, startAngle: .radians(startAngle),
                   endAngle: .radians(endAngle), clockwise: !sweep)
        addPath(arc.applying(transform))
    }
}

private extension Shape {
    func scaled(to size: CGFloat) -> ScaledShape<Self> {
        scale(x: size / 24, y: size / 24, anchor: .topLeading)
    }
}

private extension ScaledShape where Content == SVGPath {
    /// Stroke over fill, so the filled and hollow variants keep the same
    /// silhouette and optical weight.
    @ViewBuilder
    func stroked(filled: Bool, width: CGFloat, tint: Color) -> some View {
        if filled {
            ZStack {
                fill(tint)
                stroke(tint, style: StrokeStyle(lineWidth: width, lineCap: .round, lineJoin: .round))
            }
        } else {
            stroke(tint, style: StrokeStyle(lineWidth: width, lineCap: .round, lineJoin: .round))
        }
    }
}
