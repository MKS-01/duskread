import ComposeApp
import SwiftUI

/// Four steps, one pasted token, one shared page.
///
/// The steps check themselves rather than reporting a generic error: a setup
/// that says "something went wrong" leaves the reader guessing which of four
/// things it was.
struct NotionSetupScreen: View {
    let onClose: () -> Void

    @Environment(NotionStore.self) private var notion
    @Environment(\.dusk) private var dusk

    @State private var token = ""
    @State private var pages: [NotionPage] = []
    @State private var message: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                HStack(spacing: 14) {
                    DuskIcon(path: IconPaths.shared.Close, size: 20, tint: dusk.onSurfaceVariant)
                        .contentShape(Rectangle())
                        .onTapGesture(perform: onClose)
                    Text("Connect Notion")
                        .dusk(.headlineSmall)
                        .foregroundStyle(dusk.onBackground)
                    Spacer()
                }
                .padding(.top, 18)

                step(1, "Create an integration", done: notion.hasToken) {
                    Text("In Notion, open Settings → Connections → Develop or manage integrations, and make a new internal integration.")
                        .dusk(.bodyMedium)
                        .foregroundStyle(dusk.onSurfaceVariant)
                        .fixedSize(horizontal: false, vertical: true)
                    // Safari proper, deliberately, while every article opens
                    // in-app. Signing in inside a sheet this app presented
                    // means copying a secret out of it and back in; handing
                    // the task to the browser leaves it where a password
                    // manager already works.
                    HeaderAction(label: "Open Notion integrations") {
                        if let url = URL(string: "https://www.notion.so/my-integrations") {
                            UIApplication.shared.open(url)
                        }
                    }
                }

                step(2, "Paste its token", done: notion.hasToken) {
                    AppTextField(placeholder: "ntn_…", text: $token, mono: true) { saveToken() }
                    if notion.hasToken {
                        Text("Token saved to the Keychain.")
                            .dusk(.bodySmall)
                            .foregroundStyle(dusk.onSurfaceVariant)
                    } else {
                        PrimaryButton(label: "Save token", enabled: !token.isEmpty) { saveToken() }
                    }
                }

                step(3, "Share one page with it", done: notion.connected) {
                    Text("Open any Notion page, then Share → add your integration. DuskRead builds its two databases inside it.")
                        .dusk(.bodyMedium)
                        .foregroundStyle(dusk.onSurfaceVariant)
                        .fixedSize(horizontal: false, vertical: true)
                    if !pages.isEmpty {
                        Text("Pick where they go:")
                            .dusk(.titleSmall)
                            .foregroundStyle(dusk.onSurface)
                        ForEach(pages, id: \.id) { page in
                            HeaderAction(label: page.title) {
                                Task { await run(parent: page.id) }
                            }
                        }
                    }
                }

                step(4, "Build the databases", done: notion.connected) {
                    PrimaryButton(label: notion.busy ? "Working…" : "Find or create them",
                                  enabled: notion.hasToken && !notion.busy) {
                        Task { await run(parent: nil) }
                    }
                    if let message {
                        Text(message)
                            .dusk(.bodySmall)
                            .foregroundStyle(dusk.onSurfaceVariant)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
            .padding(.horizontal, Layout.readingGutter)
            .padding(.bottom, 40)
        }
        .background(dusk.background.ignoresSafeArea())
    }

    private func step<Content: View>(_ number: Int, _ title: String, done: Bool,
                                     @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                Text("\(number)")
                    .dusk(.code)
                    .foregroundStyle(done ? dusk.primary : dusk.onSurfaceVariant)
                    .frame(width: 22, height: 22)
                    .overlay(
                        RoundedRectangle(cornerRadius: Radius.chip)
                            .stroke(done ? dusk.primary : dusk.outlineVariant, lineWidth: Stroke.hairline)
                    )
                Text(title)
                    .dusk(.titleSmall)
                    .foregroundStyle(dusk.onSurface)
                Spacer()
                if done {
                    DuskIcon(path: IconPaths.shared.Check, size: 16, tint: dusk.primary)
                }
            }
            content()
        }
    }

    private func saveToken() {
        let trimmed = token.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        notion.saveToken(trimmed)
        token = ""
    }

    private func run(parent: String?) async {
        guard let outcome = await notion.provision(parentPageId: parent) else {
            message = "Could not reach Notion."
            return
        }
        switch outcome.status {
        case .ready:
            message = "Both databases are ready."
            pages = []
        case .needsParent:
            pages = outcome.pages
            message = "Pick which page they should live in."
        case .noPagesShared:
            message = "No page is shared with the integration yet — share one and try again."
        default:
            message = outcome.message
            pages = []
        }
    }
}
