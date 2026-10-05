import ComposeApp
import SwiftUI

/// Home: the timer, the week the followed blogs published, and what to read out of
/// everything else. Three sections and no more.
struct DashboardScreen: View {
    let onOpenFocus: () -> Void
    let onOpenSaved: () -> Void
    let onOpenFollowing: () -> Void
    let onOpenSettings: () -> Void

    @Environment(LinksStore.self) private var links
    @Environment(BrowserRouter.self) private var browser
    @Environment(FeedsStore.self) private var feeds
    @Environment(LatestStore.self) private var latest
    @Environment(PomodoroStore.self) private var pomodoro
    @Environment(SuggestionsStore.self) private var suggestions
    @Environment(PrefsStore.self) private var prefs
    @Environment(SpeechStore.self) private var speech
    @Environment(BarCollapse.self) private var collapse
    @Environment(\.dusk) private var dusk
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 26) {
                // Settings lives here, top-right, not on the bar: it is opened rarely, so it
                // gives up the thumb's reach to the tabs. The row stays with no name to show.
                HStack(alignment: .center) {
                    Text(greeting ?? "")
                        .dusk(.headlineSmall)
                        .foregroundStyle(dusk.onBackground)
                    Spacer(minLength: 8)
                    IconButton(path: IconPaths.shared.Settings, onTap: onOpenSettings)
                        .accessibilityLabel("Settings")
                }

                if links.links.isEmpty && feeds.feeds.isEmpty {
                    welcome
                }

                // First, and small: what the reader came to do, before what there is to
                // read.
                focus

                // The body of the screen — the week itself rather than a count of it.
                latestSection
            }
            .padding(.horizontal, Layout.listGutter)
            .padding(.top, 10)
            .padding(.bottom, Layout.barClearance)
        }
        .tracksBarCollapse(collapse)
        .background(dusk.background)
        // Not awaited: the pull hands off to Latest's header, which fills as feeds answer,
        // rather than holding the spinner over the list for the whole sync.
        .refreshable { Task { await feeds.startSync() } }
        .onAppear { latest.refresh() }
        // Unlike pull-to-refresh, silent and only when stale: Home keeps itself current
        // without Notion's sync being the only thing that ever fetches.
        .task { await feeds.syncIfStale() }
        .onChange(of: scenePhase) { _, phase in
            guard phase == .active else { return }
            latest.refresh()
            Task { await feeds.syncIfStale() }
        }
        .animation(Motion.ease(Motion.chip), value: latest.items.map(\.url))
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

    /// The card a listen-through is on, as opposed to a single read started elsewhere.
    private var listeningUrl: String? {
        guard speech.inQueue, let key = speech.key else { return nil }
        return latest.items.contains(where: { $0.url == key }) ? key : nil
    }

    private var latestSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            EyebrowHeader(label: "Latest", progress: feeds.syncProgress) {
                if feeds.syncing {
                    Text(feeds.sync.label)
                        .dusk(.code)
                        .foregroundStyle(dusk.onSurfaceVariant)
                } else if !latest.items.isEmpty {
                    Text(latest.countLabel)
                        .dusk(.code)
                        .foregroundStyle(dusk.onSurfaceVariant)
                }
                if listeningUrl != nil || latest.items.contains(where: { !$0.read }) {
                    Button {
                        if listeningUrl != nil { speech.stop() } else { speech.listenToTheWeek() }
                    } label: {
                        DuskIcon(
                            path: listeningUrl != nil ? IconPaths.shared.Close : IconPaths.shared.Play,
                            size: 18,
                            tint: dusk.onSurfaceVariant
                        )
                        .frame(width: 30, height: 30)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(listeningUrl != nil ? "Stop listening" : "Listen to the week")
                }
            }

            if latest.items.isEmpty {
                if feeds.feeds.isEmpty {
                    CompactEmptyState(
                        title: "Follow a blog",
                        message: "Whatever it publishes this week lands here, with a line about what it says.",
                        onTap: onOpenFollowing
                    )
                } else if feeds.syncing {
                    // Not "nothing new" yet: that would be a verdict before the feeds answer.
                    CompactEmptyState(
                        title: "Checking your blogs",
                        message: "What they published this week lands here as each one answers."
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
                        read: item.read,
                        playing: item.url == listeningUrl,
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
            EyebrowHeader(label: "Focus")

            // One strip, clock left and controls right, so the section costs a single
            // line of height and the week below it starts higher.
            HStack(spacing: 14) {
                Text(focusClock)
                    .font(.custom("Inconsolata-Medium", size: 26))
                    .monospacedDigit()
                    .foregroundStyle(focusClockTint)
                Spacer(minLength: 0)
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
                    WaveformMeter(progress: pomodoro.elapsedFraction, barCount: 22, height: 18)
                    Pill(label: "Open", active: true, onTap: onOpenFocus)
                }
            }
            .contentShape(Rectangle())
            .onTapGesture { if !pomodoro.state.idle { onOpenFocus() } }
        }
    }

    /// Idle, the middle length stands in as what a start would count down from.
    private var focusClock: String {
        guard pomodoro.state.idle else { return pomodoro.clockLabel }
        let lengths = pomodoro.pickableMinutes
        return lengths.isEmpty ? "0:00" : "\(lengths[lengths.count / 2]):00"
    }

    private var focusClockTint: Color {
        if pomodoro.state.idle { return dusk.onSurfaceVariant }
        return pomodoro.state.running ? dusk.primary : dusk.onSurface
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
