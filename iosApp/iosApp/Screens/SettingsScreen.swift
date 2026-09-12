import ComposeApp
import SwiftUI

/// Ordered by what you came here to change.
///
/// Summaries, voice and swipe are absent rather than disabled: iOS has no
/// summariser and no speaker, so a control for either would be a promise the
/// app cannot keep. They appear when those actuals do.
struct SettingsScreen: View {
    let onClose: () -> Void

    @Environment(PrefsStore.self) private var prefs
    @Environment(NotionStore.self) private var notion
    @Environment(LinksStore.self) private var links
    @Environment(FeedsStore.self) private var feeds
    @Environment(SpeechStore.self) private var speech
    @Environment(\.dusk) private var dusk

    @State private var setupOpen = false
    @State private var name = ""
    @State private var confirmingReset = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                header
                voice
                notionSection
                appearance
                profile
                reset
                version
            }
            .padding(.horizontal, Layout.readingGutter)
            .padding(.bottom, Layout.barClearance)
        }
        .background(dusk.background.ignoresSafeArea())
        .sheet(isPresented: $setupOpen) {
            NotionSetupScreen(onClose: { setupOpen = false })
                .environment(notion)
                .environment(\.dusk, dusk)
        }
        .onAppear { name = prefs.name ?? "" }
    }

    private var header: some View {
        HStack(spacing: 14) {
            DuskIcon(path: IconPaths.shared.Close, size: 20, tint: dusk.onSurfaceVariant)
                .contentShape(Rectangle())
                .onTapGesture(perform: onClose)
            Text("Settings")
                .dusk(.headlineSmall)
                .foregroundStyle(dusk.onBackground)
            Spacer()
        }
        .padding(.top, 8)
    }

    /// Present only because iOS can now speak. The section is absent rather
    /// than disabled where it cannot — a control that explains why it does
    /// nothing is still a control that does nothing.
    @ViewBuilder
    private var voice: some View {
        VStack(alignment: .leading, spacing: 12) {
            EyebrowHeader(label: "Voice")
            Text("Articles are read aloud on this phone. Nothing is sent anywhere to be spoken.")
                .dusk(.bodyMedium)
                .foregroundStyle(dusk.onSurfaceVariant)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: Space.chipGap) {
                Pill(label: "System voice", active: true) {}
            }
            Text(speech.unavailableReason ?? "Ready")
                .dusk(.bodySmall)
                .foregroundStyle(dusk.onSurfaceVariant)
        }
    }

    private var notionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            EyebrowHeader(label: "Notion")
            Text("Optional. Connect Notion and the blogs you follow and the links you save are each kept in step with a database there — both built for you — so you can read and sort them on a laptop too.")
                .dusk(.bodyMedium)
                .foregroundStyle(dusk.onSurfaceVariant)
                .fixedSize(horizontal: false, vertical: true)

            if notion.connected {
                Text("Connected · \(feeds.feeds.count) feeds · \(links.links.count) saved")
                    .dusk(.titleSmall)
                    .foregroundStyle(dusk.onSurface)
                if let last = notion.lastSyncAt {
                    Text("Last synced \(relative(last))")
                        .dusk(.bodySmall)
                        .foregroundStyle(dusk.onSurfaceVariant)
                }
                HStack(spacing: 20) {
                    HeaderAction(label: "Set up again") { setupOpen = true }
                    HeaderAction(label: notion.busy ? "Syncing…" : "Sync now") {
                        Task { await notion.sync() }
                    }
                    HeaderAction(label: "Disconnect") { notion.disconnect() }
                }
            } else {
                PrimaryButton(label: "Connect Notion") { setupOpen = true }
            }

            if let note = notion.note {
                Text(note)
                    .dusk(.bodySmall)
                    .foregroundStyle(dusk.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var appearance: some View {
        VStack(alignment: .leading, spacing: 12) {
            EyebrowHeader(label: "Appearance")
            HStack(spacing: Space.chipGap) {
                Pill(label: "Ink", active: prefs.mono) { if !prefs.mono { prefs.toggleTheme() } }
                Pill(label: "Paper Black", active: !prefs.mono) { if prefs.mono { prefs.toggleTheme() } }
            }
            Text(prefs.mono
                 ? "No colour at all — lightness and spacing do the separating."
                 : "One terracotta accent, reserved for whatever is playing.")
                .dusk(.bodySmall)
                .foregroundStyle(dusk.onSurfaceVariant)
        }
    }

    private var profile: some View {
        VStack(alignment: .leading, spacing: 12) {
            EyebrowHeader(label: "Profile")
            AppTextField(placeholder: "Your name", text: $name) {
                prefs.updateName(name.isEmpty ? nil : name)
            }
            Text("Only used to say hello.")
                .dusk(.bodySmall)
                .foregroundStyle(dusk.onSurfaceVariant)
        }
    }

    private var reset: some View {
        VStack(alignment: .leading, spacing: 12) {
            EyebrowHeader(label: "Reset", tint: dusk.onSurfaceVariant)
            if confirmingReset {
                Text("This erases every saved link, followed blog and preference on this device. Notion keeps its own copy.")
                    .dusk(.bodyMedium)
                    .foregroundStyle(dusk.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
                HStack(spacing: 20) {
                    HeaderAction(label: "Erase everything") {
                        links.clear()
                        feeds.clear()
                        notion.disconnect()
                        prefs.reset()
                        confirmingReset = false
                    }
                    HeaderAction(label: "Cancel") { confirmingReset = false }
                }
            } else {
                HeaderAction(label: "Erase everything on this device") { confirmingReset = true }
            }
        }
    }

    private var version: some View {
        Text("DuskRead · SwiftUI")
            .dusk(.code)
            .foregroundStyle(dusk.onSurfaceVariant.opacity(0.7))
    }

    /// Relative only, same as the shared side: an absolute date needs a
    /// calendar library this project does not carry.
    private func relative(_ millis: Int64) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .abbreviated
        return formatter.localizedString(for: Date(timeIntervalSince1970: Double(millis) / 1000), relativeTo: Date())
    }
}
