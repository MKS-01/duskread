import ComposeApp
import SwiftUI

/// The SwiftUI root.
///
/// Only Saved is native so far; the rest of the app is still the Compose view
/// controller, reached from the tab bar below. That is deliberate — the two
/// can coexist while the screens are ported one at a time, and iOS stays
/// runnable the whole way rather than being broken until the last screen
/// lands.
struct DuskReadRootView: View {
    @EnvironmentObject private var host: DuskReadHost
    @State private var tab: Tab = .saved

    var body: some View {
        let theme = host.prefs.theme
        VStack(spacing: 0) {
            Group {
                switch tab {
                case .saved:
                    SavedScreen()
                case .rest:
                    ComposeView().ignoresSafeArea(.all)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            switcher(theme)
        }
        .background(theme.background.ignoresSafeArea())
        .environment(host.links)
        .environment(host.prefs)
        .environment(\.dusk, theme)
    }

    /// A temporary switcher, not the floating bar.
    ///
    /// The real one is a single pill that is either tabs or a transport, with
    /// hysteretic scroll collapse and two seek gestures on it. Approximating
    /// that now would mean writing it twice; this is scaffolding that says so.
    private func switcher(_ theme: DuskTheme) -> some View {
        HStack(spacing: Space.chipGap) {
            ForEach(Tab.allCases, id: \.self) { option in
                Pill(label: option.label, active: tab == option) { tab = option }
            }
        }
        .padding(.horizontal, Layout.listGutter)
        .padding(.top, 10)
        .padding(.bottom, Layout.barInset)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.background)
        .overlay(alignment: .top) { HairlineDivider() }
        .environment(\.dusk, theme)
    }

    enum Tab: CaseIterable {
        case saved, rest

        var label: String {
            switch self {
            case .saved: return "Saved · SwiftUI"
            case .rest: return "Rest · Compose"
            }
        }
    }
}
