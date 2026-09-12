import ComposeApp
import SwiftUI

/// Section headers: a small uppercase label with the rule trailing off on the
/// same line, never above or below it.
struct EyebrowHeader<Trailing: View>: View {
    let label: String
    var tint: Color?
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
                // The rule yields first; the label and the actions keep their
                // width.
                .layoutPriority(-1)
            trailing()
        }
    }
}

extension EyebrowHeader where Trailing == EmptyView {
    init(label: String, tint: Color? = nil) {
        self.init(label: label, tint: tint) { EmptyView() }
    }
}

/// A sort or filter control. Uppercase mono, a hairline border at the chip
/// radius, and **never** filled — selection is carried by border and text
/// colour alone, which is the only thing that survives the Ink swap.
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

/// The one filled call-to-action. Corners at the inline radius, not a pill —
/// a capsule button reads as Material, which is the thing this set avoids.
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

/// The one text-field shape: hairline border at the inline radius, never a
/// pill, placeholder drawn rather than borrowed from the system.
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

/// A word-form action sitting on an eyebrow's rule. The label says what the
/// next tap does, not what the current state is.
struct HeaderAction: View {
    let label: String
    var onTap: () -> Void

    @Environment(\.dusk) private var dusk

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .dusk(.sectionLabel)
                .foregroundStyle(dusk.onSurfaceVariant)
                // Never wraps: these sit on an eyebrow's rule, and a two-line
                // action pushes the whole header out of shape.
                .lineLimit(1)
                .fixedSize()
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// The bordered square icon button.
///
/// A bare glyph with only padding reads noticeably smaller than everything
/// bordered around it — that is the bug this shape fixes, not a preference.
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
///
/// The drawn icon stays at the size the design asks for while the tap area is
/// padded out to something a thumb can actually hit — a bare 18pt glyph is
/// well under what Apple asks for, and on a row it sits next to another tap
/// target, so a near miss does the wrong thing rather than nothing.
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
