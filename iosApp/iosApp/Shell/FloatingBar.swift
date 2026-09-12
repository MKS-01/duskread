import ComposeApp
import SwiftUI

/// The one piece of navigation furniture.
///
/// A pill that floats above a full-screen surface rather than a bar that
/// occupies the bottom of one, so the reading surface keeps its height and the
/// controls stay where a thumb already is. It collapses out of the way while a
/// list is scrolled down and comes back on the first scroll up.
///
/// It has two faces — tabs, or the transport for whatever is being read aloud.
/// One pill that changes what it is, rather than a player slab appearing above
/// a tab bar: the reading surface keeps its height either way, and the read
/// stays reachable from every screen without taking a second row of it.
struct FloatingBar: View {
    @Binding var tab: AppTab
    let collapsed: Bool
    let mono: Bool
    let onToggleTheme: () -> Void
    let onOpenSettings: () -> Void

    @Environment(SpeechStore.self) private var speech
    @Environment(\.dusk) private var dusk

    /// Showing the transport does not mean hiding the tabs forever — tapping
    /// the title peeks back at them, the way the Compose bar does.
    @State private var peekingTabs = false

    private var showsPlayer: Bool { speech.title != nil && !peekingTabs }

    var body: some View {
        HStack(spacing: 0) {
            if showsPlayer {
                playerFace
            } else {
                tabsFace
            }
        }
        .padding(.horizontal, 10)
        .frame(height: Layout.barHeight)
        .animation(Motion.ease(Motion.chip), value: showsPlayer)
        // Any new read puts the transport back in front.
        .onChange(of: speech.title) { peekingTabs = false }
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

    private var tabsFace: some View {
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

            // Only while something is playing, so the pill can be put back
            // without waiting for the read to end.
            if speech.title != nil {
                barButton(IconPaths.shared.Waveform, active: true) { peekingTabs = false }
            }
        }
    }

    private var playerFace: some View {
        HStack(spacing: 10) {
            barButton(speech.playing ? IconPaths.shared.Pause : IconPaths.shared.Play, active: true) {
                speech.togglePlayPause()
            }

            VStack(alignment: .leading, spacing: 5) {
                Text(speech.title ?? "")
                    .dusk(.labelMedium)
                    .foregroundStyle(dusk.onSurface)
                    .lineLimit(1)
                // A progress line, not a scrubber. Speech has no seek — the
                // synthesiser speaks from where it is — so a track that looked
                // draggable would be a control that does nothing.
                GeometryReader { proxy in
                    ZStack(alignment: .leading) {
                        Capsule().fill(dusk.outlineVariant)
                        Capsule().fill(dusk.primary)
                            .frame(width: proxy.size.width * speech.fraction)
                    }
                }
                .frame(height: 2.5)
            }
            .contentShape(Rectangle())
            .onTapGesture { peekingTabs = true }

            barButton(IconPaths.shared.Close, active: false) { speech.stop() }
        }
        .padding(.leading, 2)
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
