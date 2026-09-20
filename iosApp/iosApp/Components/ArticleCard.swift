import ComposeApp
import SwiftUI

/// The bordered card Home's Latest section is built from: the same sourcechip, title and
/// mono meta line as a row, with room between them for what the piece says.
///
/// Every card is the same height closed, whatever the length of its title or text — a
/// column of cards that each stop somewhere different reads as a mistake rather than as
/// variety. Room the text does not use is left empty, and text that does not fit is
/// behind "more".
struct ArticleCard: View {
    let host: String
    let title: String
    /// The article's own opening. Nothing on this platform summarises, so it stays the
    /// author's words; see `UnavailableSummariser`.
    let text: String
    var timeAgo: String?
    var meta: [RowMetaItem] = []
    /// Recession, not a strikethrough — the same thing `RowTone.faded` means on a row.
    var faded: Bool = false
    var onTap: () -> Void = {}

    @Environment(\.dusk) private var dusk
    @State private var expanded = false
    @State private var shownHeight: CGFloat = 0
    @State private var fullHeight: CGFloat = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 10) {
                MonogramBadge(host: host)
                Text(host)
                    .dusk(.code)
                    .foregroundStyle(dusk.onSurfaceVariant)
                    .lineLimit(1)
                Spacer(minLength: 8)
                if let timeAgo {
                    Text(timeAgo)
                        .dusk(.code)
                        .foregroundStyle(dusk.onSurfaceVariant)
                }
            }

            Color.clear.frame(height: 14)

            Text(title)
                .dusk(.titleMedium)
                .foregroundStyle(dusk.onSurface)
                // Both, and equal: the second line is held open for a one-line title.
                .lineLimit(Self.titleLines, reservesSpace: true)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)

            Color.clear.frame(height: 9)

            bodyText
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .measured { shownHeight = $0 }
                // SwiftUI has no `hasVisualOverflow`, so "does this fit" is answered by
                // laying the same text out a second time with nothing holding it back.
                .background(alignment: .topLeading) { probe }

            Color.clear.frame(height: 14)

            HStack(spacing: 10) {
                ForEach(meta) { item in
                    Text(item.text)
                        .dusk(.code)
                        .foregroundStyle(item.accent ? dusk.primary : dusk.onSurfaceVariant)
                        .lineLimit(1)
                }
                Spacer(minLength: 8)

                // Only offered when there is something behind it. A "more" that opens two
                // more words is a broken promise.
                if truncated {
                    Button {
                        withAnimation(Motion.ease(Motion.chip)) { expanded.toggle() }
                    } label: {
                        Text(expanded ? "LESS" : "MORE")
                            .dusk(.code)
                            .foregroundStyle(dusk.onSurfaceVariant)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(Self.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.card)
                .stroke(dusk.outlineVariant, lineWidth: Stroke.hairline)
        )
        .opacity(faded ? 0.5 : 1)
        .contentShape(Rectangle())
        // The card's own tap opens the article; "more" is a button inside it and takes
        // its own tap first.
        .onTapGesture(perform: onTap)
    }

    /// Two branches rather than one call: `reservesSpace` is what holds every closed
    /// card to the same height, and it has no meaning once the card is open.
    @ViewBuilder
    private var bodyText: some View {
        let styled = Text(text)
            .dusk(.bodyMedium)
            .foregroundStyle(dusk.onSurfaceVariant)
            .multilineTextAlignment(.leading)

        if expanded {
            styled.lineLimit(nil)
        } else {
            styled.lineLimit(Self.bodyLines, reservesSpace: true)
        }
    }

    /// The same text unbounded and invisible, only ever measured.
    private var probe: some View {
        Text(text)
            .dusk(.bodyMedium)
            .fixedSize(horizontal: false, vertical: true)
            .hidden()
            .measured { fullHeight = $0 }
    }

    /// Asked of the closed card only: open, nothing overflows, and reading it then would
    /// take "less" away the moment it was needed.
    private var truncated: Bool { expanded || fullHeight > shownHeight + 1 }

    /// Generous next to a list row's, because the card's whole point is the room.
    private static let cardPadding: CGFloat = 16

    /// Held open whether the title needs both or not; see the note on uniform height.
    private static let titleLines = 2

    private static let bodyLines = 4
}

private extension View {
    func measured(_ onChange: @escaping (CGFloat) -> Void) -> some View {
        overlay(
            GeometryReader { geometry in
                Color.clear
                    .onAppear { onChange(geometry.size.height) }
                    .onChange(of: geometry.size.height) { _, height in onChange(height) }
            }
        )
    }
}
