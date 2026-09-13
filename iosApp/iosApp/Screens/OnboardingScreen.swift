import ComposeApp
import SwiftUI

/// One screen, one field. The name is only ever used to say hello.
struct OnboardingScreen: View {
    let onDone: (String?) -> Void

    @State private var name = ""
    @Environment(\.dusk) private var dusk

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Spacer()

            Text("DuskRead")
                .dusk(.headlineMedium)
                .foregroundStyle(dusk.onBackground)
            Text("Save a link, follow a blog, hear it read back.")
                .dusk(.bodyLarge)
                .foregroundStyle(dusk.onSurfaceVariant)
                .padding(.top, 6)

            EyebrowHeader(label: "One last thing")
                .padding(.top, 44)

            Text("What should I call you?")
                .dusk(.titleMedium)
                .foregroundStyle(dusk.onSurface)
                .padding(.top, 18)
            Text("Only used to say hello. It stays on this device — there is no account and nothing leaves the app.")
                .dusk(.bodyMedium)
                .foregroundStyle(dusk.onSurfaceVariant)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 6)

            AppTextField(placeholder: "Your name", text: $name) { finish() }
                .padding(.top, 18)

            PrimaryButton(label: "Get started") { finish() }
                .padding(.top, 20)

            Spacer()
        }
        .padding(.horizontal, Layout.readingGutter)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(dusk.background)
    }

    /// A blank name is not an error — it gets a generated one, the same way the Compose
    /// screen does, so the greeting always has something to say.
    private func finish() {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        onDone(trimmed.isEmpty ? nil : trimmed)
    }
}
