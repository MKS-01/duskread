import ComposeApp
import SwiftUI

/// The whole app, in SwiftUI.
///
/// Navigation is a tab plus a presented destination, not a stack: the shared
/// app has no navigation library either, and its full-screen surfaces are
/// overlays over one tree rather than routes. Keeping that shape here means
/// the two agree about what "back" means.
struct DuskReadRootView: View {
    @EnvironmentObject private var host: DuskReadHost
    @State private var tab: AppTab = .home
    @State private var collapse = BarCollapse()
    @State private var destination: Destination?
    @State private var browser = BrowserRouter()

    var body: some View {
        let theme = host.prefs.theme

        Group {
            if host.prefs.introSeen {
                shell(theme)
            } else {
                OnboardingScreen { name in
                    host.prefs.updateName(name)
                    host.prefs.markIntroSeen()
                }
            }
        }
        .environment(host.links)
        .environment(host.feeds)
        .environment(host.prefs)
        .environment(host.pomodoro)
        .environment(host.suggestions)
        .environment(host.notion)
        .environment(host.speech)
        .environment(browser)
        .environment(collapse)
        .environment(\.dusk, theme)
        .background(theme.background.ignoresSafeArea())
        .onAppear { tintBrowser(theme) }
        .onChange(of: host.prefs.mono) { tintBrowser(theme) }
    }

    private func shell(_ theme: DuskTheme) -> some View {
        GeometryReader { proxy in
            // `BarInset` is the gap the bar keeps from the system's own
            // furniture, and it is sized for Android — where, under gesture
            // navigation, the system inset is only a few dp and the token has
            // to supply the clearance itself. iPhone's home-indicator safe
            // area is already about 34pt, so adding the token on top of it
            // floats the bar halfway up the screen.
            //
            // The intent ports, the arithmetic does not: keep at least
            // `BarInset` between the bar and the physical bottom edge, and let
            // the safe area count towards it.
            let systemInset = proxy.safeAreaInsets.bottom
            let gap = max(0, Layout.barInset - systemInset)

            ZStack(alignment: .bottom) {
                tabContent
                    .safeAreaInset(edge: .bottom) {
                        // Constant clearance: the pill shrinks *within* the
                        // space it reserved rather than handing any of it
                        // back, so the list underneath never reflows as the
                        // bar collapses.
                        Color.clear.frame(height: Layout.barHeight + gap)
                    }

                FloatingBar(
                    tab: $tab,
                    collapsed: collapse.collapsed,
                    mono: host.prefs.mono,
                    onToggleTheme: { host.prefs.toggleTheme() },
                    onOpenSettings: { destination = .settings }
                )
                .padding(.bottom, gap)
                // The player face fills the width it is given, so the bar
                // takes the list's gutter rather than running edge to edge.
                // The tabs face is intrinsically narrow and ignores it.
                .padding(.horizontal, Layout.listGutter)
                .contentShape(Capsule())
                .onTapGesture { if collapse.collapsed { collapse.expand() } }
            }
        }
        .fullScreenCover(item: $destination) { destination in
            cover(destination, theme)
        }
    }

    @ViewBuilder
    private var tabContent: some View {
        switch tab {
        case .home:
            DashboardScreen(
                onOpenFocus: { destination = .focus },
                onOpenSaved: { tab = .saved },
                onOpenFollowing: { tab = .following }
            )
        case .following:
            FollowingScreen(onOpenTopics: { destination = .topics($0) })
        case .saved:
            SavedScreen()
        }
    }

    @ViewBuilder
    private func cover(_ destination: Destination, _ theme: DuskTheme) -> some View {
        Group {
            switch destination {
            case .focus:
                FocusScreen(onClose: { self.destination = nil })
            case .settings:
                SettingsScreen(onClose: { self.destination = nil })
            case .topics(let feed):
                TopicsScreen(feed: feed, onClose: { self.destination = nil })
            }
        }
        .environment(host.links)
        .environment(host.feeds)
        .environment(host.prefs)
        .environment(host.pomodoro)
        .environment(host.suggestions)
        .environment(host.notion)
        .environment(host.speech)
        .environment(browser)
        .environment(collapse)
        .environment(\.dusk, theme)
    }

    /// The reader sheet is UIKit's, so its tint cannot come from the
    /// environment — it is pushed in whenever the scheme changes.
    private func tintBrowser(_ theme: DuskTheme) {
        browser.barTint = theme.surface
        browser.controlTint = theme.primary
    }

    enum Destination: Identifiable, Hashable {
        case focus
        case settings
        case topics(Feed)

        var id: String {
            switch self {
            case .focus: return "focus"
            case .settings: return "settings"
            case .topics(let feed): return "topics-\(feed.id)"
            }
        }
    }
}

/// Feeds scroll offset to [BarCollapse].