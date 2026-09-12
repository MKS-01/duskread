import ComposeApp
import SwiftUI

/// Home: what to read next, the timer, and how far behind the blogs are.
///
/// Three sections and no more. The ranking already decided what matters, so a
/// dashboard that also showed counts, streaks and history would be undoing
/// that work in the name of completeness.
struct DashboardScreen: View {
    let onOpenFocus: () -> Void
    let onOpenSaved: () -> Void
    let onOpenFollowing: () -> Void

    @Environment(LinksStore.self) private var links
    @Environment(FeedsStore.self) private var feeds
    @Environment(PomodoroStore.self) private var pomodoro
    @Environment(SuggestionsStore.self) private var suggestions
    @Environment(PrefsStore.self) private var prefs
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
                } else {
                    nextUp
                }

                focus
                following
            }
            .padding(.horizontal, Layout.listGutter)
            .padding(.top, 10)
            .padding(.bottom, Layout.barClearance)
        }
        .background(dusk.background)
        .refreshable { await feeds.sync(); suggestions.refresh() }
        .onAppear { suggestions.refresh() }
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

    private var nextUp: some View {
        VStack(alignment: .leading, spacing: 14) {
            EyebrowHeader(label: "Next up") {
                DuskIcon(path: IconPaths.shared.Shuffle, size: 18, tint: dusk.onSurfaceVariant)
                    .contentShape(Rectangle())
                    .onTapGesture { suggestions.shuffle() }
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

    private var following: some View {
        VStack(alignment: .leading, spacing: 14) {
            EyebrowHeader(label: "Following") {
                Text("\(newCount) new")
                    .dusk(.code)
                    .foregroundStyle(dusk.primary)
            }

            if feeds.feeds.isEmpty {
                CompactEmptyState(title: "No blogs followed", message: "Add one in Following.", onTap: onOpenFollowing)
            } else {
                Text("\(feeds.feeds.count) feeds followed")
                    .dusk(.titleSmall)
                    .foregroundStyle(dusk.onSurface)
                ForEach(feeds.feeds.prefix(3), id: \.id) { feed in
                    HStack {
                        Text(feed.label)
                            .dusk(.bodyMedium)
                            .foregroundStyle(dusk.onSurfaceVariant)
                            .lineLimit(1)
                        Spacer()
                        Text("\(feeds.newCount(for: feed, saved: links.links)) new")
                            .dusk(.code)
                            .foregroundStyle(dusk.primary)
                    }
                }
                if feeds.feeds.count > 3 {
                    HStack(spacing: 6) {
                        Text("\(feeds.feeds.count - 3) more")
                            .dusk(.sectionLabel)
                            .foregroundStyle(dusk.primary)
                        DuskIcon(path: IconPaths.shared.Chevron, size: 14, tint: dusk.primary)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture(perform: onOpenFollowing)
                }
            }
        }
    }

    private var newCount: Int {
        feeds.feeds.reduce(0) { $0 + feeds.newCount(for: $1, saved: links.links) }
    }

    private func open(_ candidate: Candidate) {
        guard let url = URL(string: candidate.url) else { return }
        suggestions.recordOpen(candidate.url)
        _ = links.save(candidate.url)
        UIApplication.shared.open(url)
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
