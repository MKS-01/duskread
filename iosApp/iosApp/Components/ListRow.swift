import ComposeApp
import SwiftUI

/// What every list in the app is built from.
struct ListRow<Trailing: View>: View {
    let host: String
    let title: String
    var meta: [RowMetaItem] = []
    var tone: RowTone = .normal
    var last: Bool = false
    var titleLineLimit: Int = 2
    var onTap: () -> Void = {}
    @ViewBuilder var trailing: () -> Trailing

    @Environment(\.dusk) private var dusk

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 10) {
                // The row's own tap area stops short of the trailing slot.
                Button(action: onTap) {
                    HStack(alignment: .top, spacing: 10) {
                        MonogramBadge(host: host, accent: tone == .accent)
                        VStack(alignment: .leading, spacing: 6) {
                            Text(title)
                                .dusk(.titleSmall)
                                .foregroundStyle(tone == .accent ? dusk.primary : dusk.onSurface)
                                .lineLimit(titleLineLimit)
                                .multilineTextAlignment(.leading)
                                .fixedSize(horizontal: false, vertical: true)
                            if !meta.isEmpty {
                                HStack(spacing: 10) {
                                    ForEach(meta) { item in
                                        Text(item.text)
                                            .dusk(.code)
                                            .foregroundStyle(item.accent ? dusk.primary : dusk.onSurfaceVariant)
                                            .lineLimit(1)
                                    }
                                }
                            }
                        }
                        Spacer(minLength: 8)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                trailing()
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .opacity(tone == .faded ? 0.5 : 1)

            ListRowDivider(last: last)
        }
    }
}

extension ListRow where Trailing == EmptyView {
    init(host: String, title: String, meta: [RowMetaItem] = [], tone: RowTone = .normal,
         last: Bool = false, titleLineLimit: Int = 2, onTap: @escaping () -> Void = {}) {
        self.init(host: host, title: title, meta: meta, tone: tone, last: last,
                  titleLineLimit: titleLineLimit, onTap: onTap) { EmptyView() }
    }
}

/// The accent is mostly reserved for the one row actually doing something. `faded` is the
/// read half of a list, not a disabled state.
enum RowTone { case normal, accent, faded }

struct RowMetaItem: Identifiable {
    let id = UUID()
    let text: String
    var accent: Bool = false
}

/// Split out because a swipe host has to keep the hairline still while the row slides
/// over it.
struct ListRowDivider: View {
    var last: Bool
    var topSpacing: CGFloat = 15

    @Environment(\.dusk) private var dusk

    var body: some View {
        if last {
            Color.clear.frame(height: topSpacing)
        } else {
            Color.clear.frame(height: topSpacing)
            HairlineDivider()
            Color.clear.frame(height: topSpacing)
        }
    }
}

struct HairlineDivider: View {
    @Environment(\.dusk) private var dusk

    var body: some View {
        Rectangle()
            .fill(dusk.outlineVariant)
            .frame(height: Stroke.hairline)
    }
}

/// The source chip: one mono capital in a softened square.
struct MonogramBadge: View {
    let host: String
    var size: CGFloat = 22
    var accent: Bool = false

    @Environment(\.dusk) private var dusk

    var body: some View {
        Text(monogram)
            .font(.custom("Inconsolata-Medium", size: size * 0.45))
            .foregroundStyle(accent ? dusk.primary : dusk.onSurfaceVariant)
            .frame(width: size, height: size)
            .overlay(
                RoundedRectangle(cornerRadius: Radius.chip)
                    .stroke(accent ? dusk.primary : dusk.outlineVariant, lineWidth: Stroke.hairline)
            )
    }

    private var monogram: String {
        let stripped = host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
        return String(stripped.prefix(1)).uppercased()
    }
}
