import SwiftUI

/// The one-line, self-dismissing confirmation Compose's `ToastRequest` shows — one
/// centre for the whole app, so two screens never stack their own.
@Observable
final class ToastCenter {
    private(set) var message: String?

    @ObservationIgnored private var dismissal: Task<Void, Never>?

    func show(_ text: String) {
        message = text
        dismissal?.cancel()
        dismissal = Task { @MainActor [weak self] in
            try? await Task.sleep(for: .milliseconds(1800))
            guard !Task.isCancelled else { return }
            self?.message = nil
        }
    }
}

/// Mounted once, at the top: the floating bar already owns the bottom of the screen.
struct ToastOverlay: View {
    let center: ToastCenter

    @Environment(\.dusk) private var dusk

    var body: some View {
        VStack {
            if let message = center.message {
                Text(message)
                    .dusk(.labelLarge)
                    .foregroundStyle(dusk.onSurface)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 11)
                    .background(Capsule().fill(dusk.surfaceContainerHigh))
                    .overlay(Capsule().stroke(dusk.outlineVariant, lineWidth: Stroke.hairline))
                    .padding(.top, 12)
                    .transition(.move(edge: .top).combined(with: .opacity))
                    .id(message)
            }
            Spacer(minLength: 0)
        }
        .animation(Motion.ease(Motion.chip), value: center.message)
        .allowsHitTesting(false)
    }
}
