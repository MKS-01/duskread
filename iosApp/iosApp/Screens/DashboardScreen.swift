import ComposeApp
import SwiftUI

/// Home: the timer, the week the followed blogs published, and what to read out of
/// everything else. Three sections and no more.
struct DashboardScreen: View {
    let onOpenFocus: () -> Void
    let onOpenSaved: () -> Void
    let onOpenFollowing: () -> Void

    @Environment(LinksStore.self) private var links
    @Environment(BrowserRouter.self) private var browser
    @Environment(FeedsStore.self) private var feeds
    @Environment(LatestStore.self) private var latest
    @Environment(PomodoroStore.self) private var pomodoro
    @Environment(SuggestionsStore.self) private var suggestions
    @Environment(PrefsStore.self) private var prefs
    @Environment(BarCollapse.self) private var collapse
    @Environment(\.dusk) private var dusk

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 26) {
                if let greeting {
                    Text(greeting)
                        .dusk(.headlineSmall)
                        .foregroundStyle(dusk.onBackground)
                }

                if links.links.isEmpty && feeds.feeds.isEmpty {
                    welcome
                }

                // First, and small: what the reader came to do, before what there is to
                // read.
                focus

                // The body of the screen — the week itself rather than a count of it.
                latestSection

                // Last, and still a choice rather than a list: what to read when the
                // week has already been looked at.
                if !(links.links.isEmpty && feeds.feeds.isEmpty) {
                    recommended
                }
            }
            .padding(.horizontal, Layout.listGutter)
            .padding(.top, 10)
            .padding(.bottom, Layout.barClearance)
        }
        .tracksBarCollapse(collapse)
        .background(dusk.background)
        .refreshable { await feeds.sync(); latest.refresh(); suggestions.refresh(excluding: shownAsCards) }
        .onAppear { latest.refresh(); suggestions.refresh(excluding: shownAsCards) }
        // The week can change under the screen — a sync lands, or a card is opened and
        // marked read — and the picks below it have to drop whatever it now shows.
        .onChange(of: latest.items.map(\.url)) { _, _ in suggestions.refresh(excluding: shownAsCards) }
    }

    private var greeting: String? {
        guard let name = prefs.name, !name.isEmpty else { return nil }
        return "Hello, \(name)"
    }

    private var welcome: some View {
        VStack(alignment: .leading, spacing: 12) {
            EyebrowHeader(label: "Start here")
            Text("Nothing to read yet")
                .dusk(.titleMedium)
                .foregroundStyle(dusk.onSurface)
            Text("Save a link, or follow a blog, and this becomes a shortlist of what to read next.")
                .dusk(.bodyMedium)
                .foregroundStyle(dusk.onSurfaceVariant)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: Space.chipGap) {
                Pill(label: "Saved", active: false, onTap: onOpenSaved)
                Pill(label: "Following", active: false, onTap: onOpenFollowing)
            }
        }
    }

    private var recommended: some View {
        VStack(alignment: .leading, spacing: 14) {
            EyebrowHeader(label: "Recommended") {
                RowToggle(path: IconPaths.shared.Shuffle, tint: dusk.onSurfaceVariant) {
                    suggestions.shuffle()
                }
            }

            if suggestions.picks.isEmpty {
                CompactEmptyState(title: "Nothing ranked yet", message: "Sync a feed or save a link.")
            } else {
                ForEach(Array(suggestions.picks.enumerated()), id: \.offset) { index, pick in
                    let candidate = pick.candidate
                    ListRow(
                        host: candidate.host,
                        title: candidate.title,
                        meta: meta(for: candidate),
                        last: index == suggestions.picks.count - 1,
                        onTap: { open(candidate) }
                    )
                }
            }
        }
    }

    private func meta(for candidate: Candidate) -> [RowMetaItem] {
        var items = [RowMetaItem(text: candidate.host)]
        if let tag = candidate.tag, !tag.isEmpty { items.append(RowMetaItem(text: tag)) }
        if let words = candidate.words?.intValue, words > 0 {
            items.append(RowMetaItem(text: "\(max(1, words / 220)) min"))
        }
        return items
    }

    /// Nothing on this screen twice: the cards above already offered these.
    private var shownAsCards: Set<String> { Set(latest.items.map(\.url)) }

    private var latestSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            EyebrowHeader(label: "Latest") {
                if !latest.items.isEmpty {
                    Text("\(latest.items.count) this week")
                        .dusk(.code)
                        .foregroundStyle(dusk.onSurfaceVariant)
                }
            }

            if latest.items.isEmpty {
                if feeds.feeds.isEmpty {
                    CompactEmptyState(
                        title: "Follow a blog",
                        message: "Whatever it publishes this week lands here, with a line about what it says.",
                        onTap: onOpenFollowing
                    )
                } else {
                    CompactEmptyState(
                        title: "Nothing new this week",
                        message: "The blogs you follow haven't published since last week. Pull down to check again."
                    )
                }
            } else {
                ForEach(latest.items, id: \.url) { item in
                    ArticleCard(
                        host: item.host,
                        title: item.title,
                        text: item.excerpt,
                        timeAgo: links.savedAgo(item.publishedAt),
                        meta: cardMeta(for: item),
                        faded: item.read,
                        onTap: { open(item) }
                    )
                }
            }
        }
    }

    private func cardMeta(for item: LatestItem) -> [RowMetaItem] {
        var items = [RowMetaItem(text: "\(item.minutes) min")]
        if let topic = item.topic, !topic.isEmpty { items.append(RowMetaItem(text: topic.lowercased())) }
        return items
    }

    private func open(_ item: LatestItem) {
        // The same record a card's tap leaves in Compose: reading something offered is
        // how it becomes the reader's own.
        suggestions.recordOpen(item.url)
        _ = links.save(item.url)
        browser.open(item.url)
    }

    private var focus: some View {
        VStack(alignment: .leading, spacing: 14) {
            EyebrowHeader(label: "Focus") {
                if !pomodoro.state.idle {
                    Text(pomodoro.clockLabel)
                        .dusk(.code)
                        .foregroundStyle(dusk.primary)
                }
            }

            if pomodoro.state.idle {
                HStack(spacing: Space.chipGap) {
                    ForEach(pomodoro.pickableMinutes, id: \.self) { minutes in
                        Pill(label: "\(minutes) min", active: false) {
                            pomodoro.start(minutes)
                            onOpenFocus()
                        }
                    }
                }
            } else {
                HStack(spacing: 14) {
                    WaveformMeter(progress: pomodoro.elapsedFraction, barCount: 18)
                    Pill(label: "Open", active: true, onTap: onOpenFocus)
                }
            }
        }
    }

    private func open(_ candidate: Candidate) {
        suggestions.recordOpen(candidate.url)
        _ = links.save(candidate.url)
        browser.open(candidate.url)
    }
}

/// The two-line inline empty state, for a section rather than a screen.
struct CompactEmptyState: View {
    let title: String
    var message: String?
    var onTap: (() -> Void)?

    @Environment(\.dusk) private var dusk

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .dusk(.titleSmall)
                .foregroundStyle(dusk.onSurface)
            if let message {
                Text(message)
                    .dusk(.bodyMedium)
                    .foregroundStyle(dusk.onSurfaceVariant)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture { onTap?() }
    }
}
