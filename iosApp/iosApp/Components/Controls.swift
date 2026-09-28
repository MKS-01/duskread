import ComposeApp
import SwiftUI

/// Section headers: a small uppercase label with the rule trailing off on the same line,
/// never above or below it.
struct EyebrowHeader<Trailing: View>: View {
    let label: String
    var tint: Color?
    /// Fills the rule itself, muted, while something behind the section is loading.
    var progress: Double?
    @ViewBuilder var trailing: () -> Trailing

    @Environment(\.dusk) private var dusk

    var body: some View {
        HStack(spacing: 10) {
            Text(label.uppercased())
                .dusk(.sectionLabel)
                .foregroundStyle(tint ?? dusk.primary)
            Rectangle()
                .fill(dusk.outlineVariant)
                .frame(height: Stroke.hairline)
                .frame(maxWidth: .infinity)
                .overlay(alignment: .leading) { RuleFill(progress: progress, color: dusk.onSurfaceVariant) }
                // The rule yields first; the label and the actions keep their width.
                .layoutPriority(-1)
            trailing()
        }
    }
}

/// The header rule's fill: finishes the line before fading, so an end reads as done.
private struct RuleFill: View {
    let progress: Double?
    let color: Color

    @State private var fill: Double = 0
    @State private var shown = false

    var body: some View {
        GeometryReader { proxy in
            Rectangle()
                .fill(color)
                .frame(width: proxy.size.width * fill)
                .opacity(shown ? 1 : 0)
        }
        .onAppear { if let progress { fill = progress; shown = true } }
        .onChange(of: progress) { old, new in
            if let new {
                if old == nil {
                    fill = 0
                    withAnimation(Motion.ease(Motion.fade)) { shown = true }
                }
                withAnimation(Motion.ease(Motion.chip)) { fill = new }
            } else {
                withAnimation(Motion.ease(Motion.chip)) { fill = 1 } completion: {
                    withAnimation(Motion.ease(Motion.fade)) { shown = false }
                }
            }
        }
    }
}

extension EyebrowHeader where Trailing == EmptyView {
    init(label: String, tint: Color? = nil, progress: Double? = nil) {
        self.init(label: label, tint: tint, progress: progress) { EmptyView() }
    }
}

/// A sort or filter control.
struct Pill: View {
    let label: String
    var active: Bool
    var onTap: () -> Void

    @Environment(\.dusk) private var dusk

    var body: some View {
        Button(action: onTap) {
            Text(label.uppercased())
                .dusk(.code)
                .foregroundStyle(active ? dusk.primary : dusk.onSurfaceVariant)
                .padding(.horizontal, 14)
                .padding(.vertical, 9)
                .overlay(
                    RoundedRectangle(cornerRadius: Radius.chip)
                        .stroke(active ? dusk.primary : dusk.outlineVariant, lineWidth: Stroke.hairline)
                )
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .animation(Motion.ease(Motion.chip), value: active)
    }
}

/// The one filled call-to-action. Corners at the inline radius, not a pill — a capsule
/// button reads as Material, which is the thing this set avoids.
struct PrimaryButton: View {
    let label: String
    var enabled: Bool = true
    var onTap: () -> Void

    @Environment(\.dusk) private var dusk

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .dusk(.labelLarge)
                .foregroundStyle(dusk.onPrimary)
                .padding(.horizontal, 22)
                .padding(.vertical, 13)
                .background(RoundedRectangle(cornerRadius: Radius.inline).fill(dusk.primary))
                .opacity(enabled ? 1 : 0.5)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

/// The one text-field shape: hairline border at the inline radius, never a pill,
/// placeholder drawn rather than borrowed from the system.
struct AppTextField: View {
    let placeholder: String
    @Binding var text: String
    var mono: Bool = false
    var onSubmit: () -> Void = {}

    @Environment(\.dusk) private var dusk

    var body: some View {
        TextField("", text: $text, prompt: Text(placeholder).foregroundStyle(dusk.onSurfaceVariant))
            .dusk(mono ? .code : .bodyMedium)
            .foregroundStyle(dusk.onSurface)
            .tint(dusk.primary)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(.done)
            .onSubmit(onSubmit)
            .padding(.horizontal, 13)
            .padding(.vertical, 12)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.inline)
                    .stroke(dusk.outlineVariant, lineWidth: Stroke.hairline)
            )
    }
}

/// A word-form action sitting on an eyebrow's rule. The label says what the next tap
/// does, not what the current state is.
struct HeaderAction: View {
    let label: String
    var onTap: () -> Void

    @Environment(\.dusk) private var dusk

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .dusk(.sectionLabel)
                .foregroundStyle(dusk.onSurfaceVariant)
                // Never wraps: these sit on an eyebrow's rule, and a two-line action
                // pushes the whole header out of shape.
                .lineLimit(1)
                .fixedSize()
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// The bordered square icon button.
struct IconButton: View {
    let path: IconPath
    var onTap: () -> Void

    @Environment(\.dusk) private var dusk

    var body: some View {
        Button(action: onTap) {
            DuskIcon(path: path, size: 16, tint: dusk.onSurfaceVariant)
                .padding(9)
                .frame(width: 34, height: 34)
                .overlay(
                    RoundedRectangle(cornerRadius: Radius.chip)
                        .stroke(dusk.outlineVariant, lineWidth: Stroke.hairline)
                )
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// A glyph that toggles something on a row.
struct RowToggle: View {
    let path: IconPath
    let tint: Color
    var size: CGFloat = 18
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            DuskIcon(path: path, size: size, tint: tint)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
