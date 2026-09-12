import ComposeApp
import SwiftUI

/// The one piece of navigation furniture.
///
/// A pill that floats above a full-screen surface rather than a bar that
/// occupies the bottom of one, so the reading surface keeps its height and the
/// controls stay where a thumb already is. It collapses out of the way while a
/// list is scrolled down and comes back on the first scroll up.
///
/// The transport face — the bar doubling as a player — is not here yet: iOS
/// has no speaker and no audio player, so there is nothing to transport. The
/// face swap is the reason the real bar is a single pill rather than tabs plus
/// a slab, and it goes in when the audio actuals do.
struct FloatingBar: View {
    @Binding var tab: AppTab
    let collapsed: Bool
    let mono: Bool
    let onToggleTheme: () -> Void
    let onOpenSettings: () -> Void

    @Environment(\.dusk) private var dusk

    var body: some View {
        HStack(spacing: 0) {
            ForEach(AppTab.allCases, id: \.self) { option in
                barButton(option.icon, active: tab == option) {
                    withAnimation(Motion.ease(Motion.fade)) { tab = option }
                }
            }

            Rectangle()
                .fill(dusk.outlineVariant)
                .frame(width: Stroke.hairline, height: 22)
                .padding(.horizontal, 6)

            barButton(IconPaths.shared.Contrast, active: false, onTap: onToggleTheme)
            barButton(IconPaths.shared.Settings, active: false, onTap: onOpenSettings)
        }
        .padding(.horizontal, 10)
        .frame(height: Layout.barHeight)
        .background(
            Capsule()
                .fill(.ultraThinMaterial)
                .overlay(Capsule().fill(dusk.surface.opacity(0.62)))
                .overlay(
                    // A three-stop gradient rather than a flat border, faking
                    // the light catching the top edge of a raised surface.
                    Capsule().strokeBorder(
                        LinearGradient(
                            colors: [dusk.outline.opacity(0.9), dusk.outlineVariant, dusk.outline.opacity(0.4)],
                            startPoint: .top, endPoint: .bottom
                        ),
                        lineWidth: Stroke.hairline
                    )
                )
        )
        .scaleEffect(collapsed ? 0.82 : 1, anchor: .bottom)
        .animation(Motion.ease(collapsed ? Motion.chip : Motion.fade), value: collapsed)
    }

    private func barButton(_ path: IconPath, active: Bool, onTap: @escaping () -> Void) -> some View {
        DuskIcon(path: path, size: 20, tint: active ? dusk.primary : dusk.onSurfaceVariant)
            .frame(width: 42, height: 42)
            .background(
                Circle()
                    .fill(active ? dusk.primary.opacity(0.14) : .clear)
            )
            .contentShape(Circle())
            .onTapGesture(perform: onTap)
    }
}

enum AppTab: CaseIterable {
    case home, following, saved

    var icon: IconPath {
        switch self {
        case .home: return IconPaths.shared.Home
        case .following: return IconPaths.shared.Feed
        case .saved: return IconPaths.shared.Bookmark
        }
    }
}

/// Tracks scroll direction to decide whether the bar is out of the way.
///
/// The thresholds are asymmetric and the run restarts on a direction change,
/// both deliberately: a bar that collapsed and expanded on the same distance
/// would flicker on any scroll that wobbles, and one that subtracted rather
/// than restarted would need a long pull back up to reappear. Reading up is
/// meant to bring it back almost immediately.
@Observable
final class BarCollapse {
    private(set) var collapsed = false

    private var run: CGFloat = 0
    private var lastOffset: CGFloat = 0

    private let collapseRun: CGFloat = 52
    private let expandRun: CGFloat = 18

    func track(offset: CGFloat) {
        let delta = offset - lastOffset
        lastOffset = offset
        // Zero deltas arrive while a fling settles and mean nothing.
        guard delta != 0 else { return }

        if (run > 0) != (delta > 0) { run = 0 }
        run += delta

        if !collapsed, run > collapseRun {
            collapsed = true
            run = 0
        } else if collapsed, run < -expandRun {
            collapsed = false
            run = 0
        }
    }

    func expand() {
        collapsed = false
        run = 0
    }
}

extension View {
    /// Feeds this scroll view's offset to the bar.
    ///
    /// Applied **to** a `ScrollView`, never above one: the modifier resolves
    /// against the nearest scroll view, so on an ancestor it silently matches
    /// nothing and the bar simply never moves.
    func tracksBarCollapse(_ collapse: BarCollapse) -> some View {
        onScrollGeometryChange(for: CGFloat.self) { geometry in
            geometry.contentOffset.y
        } action: { _, offset in
            collapse.track(offset: offset)
        }
    }
}
