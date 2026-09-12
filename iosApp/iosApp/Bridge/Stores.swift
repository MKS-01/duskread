import ComposeApp
import Observation
import SwiftUI

/// SwiftUI's view of the saved links.
///
/// One `Cancellable` per subscription, released on `deinit`. That is not
/// tidiness: a collector that outlives its observer keeps the Kotlin graph
/// alive and goes on delivering into a view that is gone.
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

    /// Fetches titles for anything saved without one. The work is Kotlin's;
    /// this is only the trigger, so the two UIs backfill identically.
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
}
