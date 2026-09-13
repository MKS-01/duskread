import ComposeApp
import SwiftUI

/// The per-row meter, and the "no signal" ornament when flat. Intrinsic width, not
/// stretched: `barWidth * count + gap * (count - 1)`.
struct WaveformMeter: View {
    var progress: Double
    var barCount: Int = 18
    var seed: Int32 = 0
    var flat: Bool = false
    var height: CGFloat = 15
    var filled: Color?

    private let barWidth: CGFloat = 2
    private let barGap: CGFloat = 1.5

    @Environment(\.dusk) private var dusk

    var body: some View {
        let heights = Self.heights(count: barCount, seed: seed)
        let filledUpTo = min(max(progress, 0), 1) * Double(barCount)

        HStack(alignment: .center, spacing: barGap) {
            ForEach(0..<barCount, id: \.self) { i in
                RoundedRectangle(cornerRadius: 1)
                    // Whole bars only: a half-tinted boundary bar reads as an
                    // anti-aliasing artefact at 2pt, not as sub-bar precision.
                    .fill(Double(i) < filledUpTo ? (filled ?? dusk.primary) : dim)
                    .frame(width: barWidth, height: flat ? barWidth : height * heights[i])
            }
        }
        .frame(height: height)
    }

    /// The mockup's off-state bar sits between the hairline and the meta grey.
    private var dim: Color { dusk.onSurfaceVariant.opacity(0.55) }

    /// The same integer hash the Compose meter uses, reproduced exactly.
    static func heights(count: Int, seed: Int32) -> [CGFloat] {
        var steps = [Int32](repeating: 0, count: count)
        var previous: Int32 = -9
        for i in 0..<count {
            var h = seed &* Int32(bitPattern: 0x9E37_79B9) &+ Int32(i) &* Int32(bitPattern: 0x85EB_CA6B)
            h ^= Int32(bitPattern: UInt32(bitPattern: h) >> 15)
            h = h &* 0x2545_F491
            h ^= Int32(bitPattern: UInt32(bitPattern: h) >> 13)
            var step = Int32(bitPattern: UInt32(bitPattern: h) >> 1) % 11
            if abs(step - previous) < 3 { step = (step + 5) % 11 }
            steps[i] = step
            previous = step
        }
        return steps.map { CGFloat(5 + $0) / 15 }
    }
}
