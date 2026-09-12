import ComposeApp
import SwiftUI

/// The reading queue: unread leads, read stays under its own heading.
///
/// The split is the point — a single list sorted by date buries the three
/// things you actually meant to read under thirty you already have.
struct SavedScreen: View {
    @Environment(LinksStore.self) private var links
    @Environment(BrowserRouter.self) private var browser
    @Environment(BarCollapse.self) private var collapse
    @Environment(\.dusk) private var dusk

    @State private var filter: LinkFilter = .all
    @State private var searching = false
    @State private var adding = false
    @State private var query = ""
    @State private var draft = ""
    @State private var rejected = false

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                controls
                    .padding(.bottom, 18)

                if visible.isEmpty {
                    EmptyState(
                        title: links.links.isEmpty ? "Nothing saved yet" : "No matches",
                        message: links.links.isEmpty
                            ? "Paste a link, or share one to DuskRead from anywhere."
                            : "Nothing here matches that."
                    )
                    .padding(.top, 60)
                } else {
                    if filter != .read, !unread.isEmpty {
                        section("Unread · \(unread.count)", rows: unread)
                    }
                    if filter != .unread, !read.isEmpty {
                        section("Read · \(read.count)", rows: read)
                    }
                }
            }
            .padding(.horizontal, Layout.listGutter)
            .padding(.bottom, Layout.barClearance)
        }
        .tracksBarCollapse(collapse)
        .background(dusk.background)
        .task { await links.backfillTitles() }
    }

    // MARK: - Pieces

    private var controls: some View {
        VStack(alignment: .leading, spacing: 14) {
            EyebrowHeader(label: "Saved · \(links.links.count)") {
                HStack(spacing: 16) {
                    HeaderAction(label: searching ? "Done" : "Search") {
                        withAnimation(Motion.ease(Motion.chip)) {
                            searching.toggle()
                            if !searching { query = "" }
                        }
                    }
                    HeaderAction(label: adding ? "Done" : "Add") {
                        withAnimation(Motion.ease(Motion.chip)) {
                            adding.toggle()
                            if !adding { draft = ""; rejected = false }
                        }
                    }
                }
            }

            HStack(spacing: Space.chipGap) {
                ForEach(LinkFilter.allCases, id: \.self) { option in
                    Pill(label: option.label, active: filter == option) {
                        withAnimation(Motion.ease(Motion.chip)) { filter = option }
                    }
                }
            }

            if searching {
                AppTextField(placeholder: "Search titles", text: $query)
            }

            if adding {
                VStack(alignment: .leading, spacing: 8) {
                    AppTextField(placeholder: "Paste a link", text: $draft, mono: true) { add() }
                    if rejected {
                        Text("That does not look like a link.")
                            .dusk(.bodySmall)
                            .foregroundStyle(dusk.error)
                    }
                }
            }
        }
    }

    private func section(_ label: String, rows: [SavedLink]) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            EyebrowHeader(label: label, tint: dusk.onSurfaceVariant)
                .padding(.bottom, 14)
            ForEach(Array(rows.enumerated()), id: \.element.id) { index, link in
                row(link, last: index == rows.count - 1)
            }
        }
        .padding(.bottom, 8)
    }

    private func row(_ link: SavedLink, last: Bool) -> some View {
        ListRow(
            host: SavedLinkKt.hostOf(url: link.url),
            title: link.title,
            meta: meta(for: link),
            tone: link.read ? .faded : .normal,
            last: last,
            onTap: { open(link) }
        ) {
            RowToggle(
                path: link.read ? IconPaths.shared.Check : IconPaths.shared.External,
                // The affordance glyph keeps a permanently muted hint of the
                // accent: it marks "this leaves the app" regardless of state.
                tint: link.read ? dusk.onSurfaceVariant : dusk.primary.opacity(0.75)
            ) { links.toggleRead(link) }
        }
        // A context menu, not a swipe, until the Compose gesture is ported.
        // That one changes its own label at 40% travel and never settles, and
        // half-porting it would leave a gesture that looks the same and is not.
        .contextMenu {
            Button(link.read ? "Mark unread" : "Mark read") { links.toggleRead(link) }
            if link.fetchFailed {
                Button("Try again") { links.retry(link) }
            }
            Button("Remove", role: .destructive) { links.remove(link) }
        }
    }

    private func meta(for link: SavedLink) -> [RowMetaItem] {
        var items: [RowMetaItem] = []
        if link.fetchFailed {
            items.append(RowMetaItem(text: "couldn't load this page"))
        } else if !link.fetched {
            items.append(RowMetaItem(text: "reading the page…"))
        }
        if let topic = link.topic, !topic.isEmpty {
            items.append(RowMetaItem(text: topic))
        }
        items.append(RowMetaItem(text: links.savedAgo(link)))
        return items
    }

    // MARK: - Behaviour

    private var visible: [SavedLink] {
        let pool: [SavedLink]
        switch filter {
        case .all: pool = links.links
        case .unread: pool = links.unread
        case .read: pool = links.read
        }
        guard !query.isEmpty else { return pool }
        return pool.filter { $0.title.localizedCaseInsensitiveContains(query) }
    }

    private var unread: [SavedLink] { visible.filter { !$0.read } }
    private var read: [SavedLink] { visible.filter { $0.read } }

    private func add() {
        guard links.save(draft) else {
            rejected = true
            return
        }
        draft = ""
        rejected = false
    }

    private func open(_ link: SavedLink) {
        browser.open(link.url)
    }
}

enum LinkFilter: CaseIterable {
    case all, unread, read

    var label: String {
        switch self {
        case .all: return "All"
        case .unread: return "Unread"
        case .read: return "Read"
        }
    }
}

/// The "no signal" ornament: a flat meter, left-aligned, with the message
/// under it rather than centred in the middle of the screen.
struct EmptyState: View {
    let title: String
    var message: String?

    @Environment(\.dusk) private var dusk

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            WaveformMeter(progress: 0, flat: true)
            Text(title)
                .dusk(.titleMedium)
                .foregroundStyle(dusk.onSurface)
            if let message {
                Text(message)
                    .dusk(.bodyMedium)
                    .foregroundStyle(dusk.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
