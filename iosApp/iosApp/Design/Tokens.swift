import ComposeApp
import SwiftUI

/// Layout, radius, stroke and spacing, read from the shared module.
///
/// These are thin because they have to be: the moment a number is typed on
/// this side rather than read from `DesignTokens`, the two UIs have started
/// drifting. Kotlin's `dp` and SwiftUI's points are the same unit at the same
/// scale, so there is nothing to convert.
enum Layout {
    static let readingGutter = CGFloat(DesignTokens.shared.ReadingGutter)
    static let listGutter = CGFloat(DesignTokens.shared.ListGutter)
    static let wideListGutter = CGFloat(DesignTokens.shared.WideListGutter)
    static let barClearance = CGFloat(DesignTokens.shared.BarClearance)
    static let barHeight = CGFloat(DesignTokens.shared.BarHeight)
    static let barInset = CGFloat(DesignTokens.shared.BarInset)
    static let twoPaneBreakpoint = CGFloat(DesignTokens.shared.TwoPaneBreakpoint)
    static let railWidth = CGFloat(DesignTokens.shared.RailWidth)
    static let readingMeasure = CGFloat(DesignTokens.shared.ReadingMeasure)
}

enum Radius {
    static let card = CGFloat(DesignTokens.shared.RadiusCard)
    static let inline = CGFloat(DesignTokens.shared.RadiusInline)
    /// The one to reach for: a softened corner, not a round one.
    static let chip = CGFloat(DesignTokens.shared.RadiusChip)
}

enum Stroke {
    /// One **point**, not one physical pixel — the hairline is a weight, not a
    /// hardware detail, and `1 / UIScreen.scale` would make it vanish.
    static let hairline = CGFloat(DesignTokens.shared.StrokeHairline)
}

enum Space {
    static let chipGap = CGFloat(DesignTokens.shared.ChipGap)
    static let cardGap = CGFloat(DesignTokens.shared.CardGap)
}

/// Durations, mirrored as timing curves rather than springs.
///
/// The shared UI has four `tween`s on Compose's default easing and not one
/// spring anywhere. SwiftUI's idiom is springs, and reaching for one here
/// would be redesigning the motion rather than porting it — so this is the
/// same cubic Compose uses, at the same four durations.
enum Motion {
    static let pushIn = seconds(DesignTokens.shared.MotionPushIn)
    static let popFade = seconds(DesignTokens.shared.MotionPopFade)
    static let fade = seconds(DesignTokens.shared.MotionFade)
    static let chip = seconds(DesignTokens.shared.MotionChip)

    private static func seconds(_ ms: Int32) -> Double { Double(ms) / 1000 }

    /// Compose's `FastOutSlowInEasing`.
    static func ease(_ duration: Double) -> Animation {
        .timingCurve(0.4, 0.0, 0.2, 1.0, duration: duration)
    }
}
