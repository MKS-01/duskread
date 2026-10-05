import ComposeApp
import SwiftUI

/// The reading queue: unread as a slider of cards, read as a list under it.
struct SavedScreen: View {
    @Environment(LinksStore.self) private var links
    @Environment(BrowserRouter.self) private var browser
    @Environment(SpeechStore.self) private var speech
    @Environment(BarCollapse.self) private var collapse
    @Environment(\.dusk) private var dusk

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
                    // No filter: the slider and the list already split unread from read.
                    if !unread.isEmpty {
                        unreadSlider
                    }
                    if !read.isEmpty {
                        section("Read · \(read.count)", rows: read)
                            .padding(.top, unread.isEmpty ? 0 : 22)
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

    /// The same card Home and Following use. Opening it is the one action: that marks it
    /// read, and the read list below keeps its menu.
    private var unreadSlider: some View {
        VStack(alignment: .leading, spacing: 0) {
            EyebrowHeader(label: "Unread · \(unread.count)")
                .padding(.bottom, 14)
            CardSlider(items: unread, id: \.id) { link in
                ArticleCard(
                    // The topic rides on the host line, so the card needs no bottom line.
                    host: [link.host, link.topic].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · "),
                    title: link.title,
                    text: cardText(for: link),
                    timeAgo: links.savedAgo(link),
                    // A page description runs a line or two; four held open was mostly air.
                    bodyLines: 2,
                    showsMeta: false,
                    expandable: false,
                    onTap: { open(link) }
                )
            }
        }
        .padding(.bottom, 8)
    }

    private func cardText(for link: SavedLink) -> String {
        if !link.fetched { return "Reading the page…" }
        if link.fetchFailed { return "Couldn't load this page." }
        // Said, not left blank: an empty card reads as one that failed to draw.
        if let text = link.description_, !text.isEmpty { return text }
        return "No preview for this page — open it to read."
    }

    @ViewBuilder
    private func menu(for link: SavedLink) -> some View {
        if speech.available {
            Button("Read aloud") { speech.speak(title: link.title, url: link.url) }
        }
        Button(link.read ? "Mark unread" : "Mark read") { links.toggleRead(link) }
        if link.fetchFailed {
            Button("Try again") { links.retry(link) }
        }
        Button("Remove", role: .destructive) { links.remove(link) }
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
            title: link.title,
            meta: meta(for: link),
            tone: link.read ? .faded : .normal,
            last: last,
            onTap: { open(link) }
        ) {
            RowToggle(
                path: link.read ? IconPaths.shared.Check : IconPaths.shared.External,
                // The affordance glyph keeps a permanently muted hint of the accent: it
                // marks "this leaves the app" regardless of state.
                tint: link.read ? dusk.onSurfaceVariant : dusk.primary.opacity(0.75)
            ) { links.toggleRead(link) }
        }
        // A context menu, not a swipe, until the Compose gesture is ported.
        .contextMenu { menu(for: link) }
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
        guard !query.isEmpty else { return links.links }
        return links.links.filter { $0.title.localizedCaseInsensitiveContains(query) }
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

/// The "no signal" ornament: a flat meter, left-aligned, with the message under it rather
/// than centred in the middle of the screen.
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
