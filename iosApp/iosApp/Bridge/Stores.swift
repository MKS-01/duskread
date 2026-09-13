import ComposeApp
import Foundation
import Observation
import SwiftUI

/// SwiftUI's view of the saved links. One `Cancellable` per subscription, released on
/// `deinit`.
@Observable
final class LinksStore {
    private(set) var links: [SavedLink] = []

    @ObservationIgnored private let bridge: LinksBridge
    @ObservationIgnored private var subscription: Cancellable?

    init(_ bridge: LinksBridge) {
        self.bridge = bridge
        links = bridge.current()
        subscription = bridge.observe { [weak self] value in
            self?.links = value
        }
    }

    deinit { subscription?.cancel() }

    var unread: [SavedLink] { links.filter { !$0.read } }
    var read: [SavedLink] { links.filter { $0.read } }

    func save(_ url: String) -> Bool {
        bridge.save(url: url, title: nil, topic: nil) != nil
    }

    func toggleRead(_ link: SavedLink) { bridge.toggleRead(id: link.id) }

    func remove(_ link: SavedLink) { bridge.remove(id: link.id) }

    func retry(_ link: SavedLink) { bridge.retryFetch(id: link.id) }

    func savedAgo(_ link: SavedLink) -> String { bridge.savedAgoLabel(savedAt: link.savedAt) }

    func clear() { bridge.clear() }

    /// Fetches titles for anything saved without one. The work is Kotlin's; this is only
    /// the trigger, so the two UIs backfill identically.
    func backfillTitles() async {
        try? await bridge.backfillTitles()
    }
}

/// The reader's preferences, and the colour scheme with them.
@Observable
final class PrefsStore {
    private(set) var mono: Bool
    private(set) var name: String?
    private(set) var introSeen: Bool

    @ObservationIgnored private let bridge: PrefsBridge
    @ObservationIgnored private var subscriptions: [Cancellable] = []

    init(_ bridge: PrefsBridge) {
        self.bridge = bridge
        mono = bridge.mono()
        name = bridge.name()
        introSeen = bridge.introSeen()
        subscriptions = [
            bridge.observeMono { [weak self] in self?.mono = $0.boolValue },
            bridge.observeName { [weak self] in self?.name = $0 },
            bridge.observeIntroSeen { [weak self] in self?.introSeen = $0.boolValue },
        ]
    }

    deinit { subscriptions.forEach { $0.cancel() } }

    var theme: DuskTheme { DuskTheme(mono: mono) }

    func toggleTheme() { bridge.updateMono(value: !mono) }

    func updateName(_ value: String?) { bridge.updateName(value: value) }

    func markIntroSeen() { bridge.markIntroSeen() }

    func reset() { bridge.reset() }
}

/// Followed blogs and their cached posts.
@Observable
final class FeedsStore {
    private(set) var feeds: [Feed] = []
    private(set) var postsByFeed: [String: [FeedPost]] = [:]
    private(set) var syncing = false

    @ObservationIgnored private let bridge: FeedsBridge
    @ObservationIgnored private var subscriptions: [Cancellable] = []

    init(_ bridge: FeedsBridge) {
        self.bridge = bridge
        feeds = bridge.currentFeeds()
        postsByFeed = bridge.currentPosts() as? [String: [FeedPost]] ?? [:]
        subscriptions = [
            bridge.observeFeeds { [weak self] in self?.feeds = $0 },
            bridge.observePosts { [weak self] in self?.postsByFeed = $0 as? [String: [FeedPost]] ?? [:] },
        ]
    }

    deinit { subscriptions.forEach { $0.cancel() } }

    func posts(for feed: Feed) -> [FeedPost] { postsByFeed[feed.id] ?? [] }

    /// Posts not already saved — the count a row reports as "new".
    func newCount(for feed: Feed, saved: [SavedLink]) -> Int {
        let savedUrls = Set(saved.map(\.url))
        return posts(for: feed).filter { !savedUrls.contains($0.url) }.count
    }

    func follow(_ rawUrl: String) async {
        _ = try? await bridge.follow(rawUrl: rawUrl, title: nil, topic: nil)
    }

    func remove(_ feed: Feed) { bridge.remove(id: feed.id) }

    func clear() { bridge.clear() }

    func sync() async {
        syncing = true
        defer { syncing = false }
        _ = try? await bridge.sync()
    }
}

@Observable
final class PomodoroStore {
    private(set) var state: PomodoroState

    @ObservationIgnored private let bridge: PomodoroBridge
    @ObservationIgnored private var subscription: Cancellable?

    init(_ bridge: PomodoroBridge) {
        self.bridge = bridge
        state = PomodoroState(totalSeconds: 0, remainingSeconds: 0, running: false)
        subscription = bridge.observe { [weak self] in self?.state = $0 }
    }

    deinit { subscription?.cancel() }

    var pickableMinutes: [Int] { bridge.pickableMinutes.map { Int(truncating: $0) } }

    /// `mm:ss` from the shared side, so the two UIs cannot pad differently.
    var clockLabel: String { bridge.clockLabelFor(state: state) }

    var elapsedFraction: Double {
        guard state.totalSeconds > 0 else { return 0 }
        return 1 - Double(state.remainingSeconds) / Double(state.totalSeconds)
    }

    func start(_ minutes: Int) { bridge.start(minutes: Int32(minutes)) }
    func pause() { bridge.pause() }
    func resume() { bridge.resume() }
    func reset() { bridge.reset() }
}

/// Home's ranked shortlist.
@Observable
final class SuggestionsStore {
    private(set) var picks: [Scored] = []

    @ObservationIgnored private let bridge: SignalsBridge
    @ObservationIgnored private var seed: Int32 = 0

    init(_ bridge: SignalsBridge) {
        self.bridge = bridge
        refresh()
    }

    /// Re-seeding re-ranks rather than re-randomising, so shuffle means "something else
    /// good" and not "anything at all".
    func shuffle() {
        seed &+= 1
        refresh()
    }

    func refresh() {
        picks = bridge.nextUp(
            count: 3,
            now: Int64(Date().timeIntervalSince1970 * 1000),
            seed: seed,
            focusMinutes: nil
        )
    }

    func recordOpen(_ url: String) { bridge.recordOpen(url: url) }
}

/// Notion setup and sync.
@Observable
final class NotionStore {
    private(set) var connected: Bool
    private(set) var hasToken: Bool
    private(set) var lastSyncAt: Int64?
    private(set) var busy = false
    private(set) var note: String?

    @ObservationIgnored private let bridge: NotionBridge
    @ObservationIgnored private var subscription: Cancellable?

    init(_ bridge: NotionBridge) {
        self.bridge = bridge
        connected = bridge.isConnected()
        hasToken = bridge.hasToken()
        lastSyncAt = bridge.lastSyncAt()?.int64Value
        subscription = bridge.observeLastSync { [weak self] in self?.lastSyncAt = $0?.int64Value }
    }

    deinit { subscription?.cancel() }

    func saveToken(_ token: String) {
        bridge.saveToken(token: token)
        hasToken = bridge.hasToken()
    }

    func disconnect() {
        bridge.disconnect()
        connected = false
        hasToken = false
        note = nil
    }

    func provision(parentPageId: String? = nil) async -> NotionOutcome? {
        busy = true
        defer { busy = false }
        guard let outcome = try? await bridge.provisionDatabases(parentPageId: parentPageId) else { return nil }
        connected = bridge.isConnected()
        note = outcome.message
        return outcome
    }

    func sync() async {
        busy = true
        defer { busy = false }
        note = (try? await bridge.sync())?.message
    }
}

/// Reading an article aloud. Holds what the transport needs and nothing else: what is
/// playing, how far through, and whether it is paused.
@Observable
final class SpeechStore {
    private(set) var title: String?
    private(set) var fraction: Double = 0
    private(set) var playing = false
    private(set) var note: String?

    @ObservationIgnored private let bridge: SpeakerBridge

    init(_ bridge: SpeakerBridge) {
        self.bridge = bridge
    }

    var available: Bool { bridge.isReady() }

    /// Why it cannot speak, in the words the shared side chose — a missing voice and a
    /// missing engine need different answers.
    var unavailableReason: String? { bridge.status() }

    func speak(title: String, url: String) {
        self.title = title
        fraction = 0
        playing = true
        note = nil
        bridge.speak(
            url: url,
            title: title,
            onProgress: { [weak self] value in
                self?.fraction = Double(truncating: value)
            },
            onFinished: { [weak self] failure in
                self?.playing = false
                self?.title = nil
                self?.fraction = 0
                self?.note = failure
            }
        )
    }

    func togglePlayPause() {
        guard title != nil else { return }
        if playing {
            bridge.pause()
            playing = false
        } else {
            bridge.resume()
            playing = true
        }
    }

    func stop() {
        bridge.stop()
        playing = false
        title = nil
        fraction = 0
    }
}
