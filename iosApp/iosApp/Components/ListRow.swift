import ComposeApp
import SwiftUI

/// What every list in the app is built from.
struct ListRow<Trailing: View>: View {
    let title: String
    var meta: [RowMetaItem] = []
    var tone: RowTone = .normal
    var last: Bool = false
    var titleLineLimit: Int = 2
    var onTap: () -> Void = {}
    @ViewBuilder var trailing: () -> Trailing

    @Environment(\.dusk) private var dusk

    /// A shade, not a fade: a done row recedes but stays as legible as its meta line.
    private var titleTint: Color {
        switch tone {
        case .accent: dusk.primary
        case .faded: dusk.onSurfaceVariant
        case .normal: dusk.onSurface
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 10) {
                // The row's own tap area stops short of the trailing slot.
                Button(action: onTap) {
                    HStack(alignment: .top, spacing: 10) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(title)
                                .dusk(.titleSmall)
                                .foregroundStyle(titleTint)
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

            ListRowDivider(last: last)
        }
    }
}

extension ListRow where Trailing == EmptyView {
    init(title: String, meta: [RowMetaItem] = [], tone: RowTone = .normal,
         last: Bool = false, titleLineLimit: Int = 2, onTap: @escaping () -> Void = {}) {
        self.init(title: title, meta: meta, tone: tone, last: last,
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
