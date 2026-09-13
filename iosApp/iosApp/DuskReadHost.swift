import ComposeApp
import SwiftUI

/// Owns the Kotlin side for the lifetime of the app.
final class DuskReadHost: ObservableObject {
    let bridge: DuskReadBridge
    let links: LinksStore
    let prefs: PrefsStore
    let feeds: FeedsStore
    let pomodoro: PomodoroStore
    let suggestions: SuggestionsStore
    let notion: NotionStore
    let speech: SpeechStore

    init() {
        bridge = DuskReadBridge(
            store: UserDefaultsStore(),
            secrets: KeychainSecretStore()
        )
        links = LinksStore(bridge.links)
        prefs = PrefsStore(bridge.prefs)
        feeds = FeedsStore(bridge.feeds)
        pomodoro = PomodoroStore(bridge.pomodoro)
        suggestions = SuggestionsStore(bridge.signals)
        notion = NotionStore(bridge.notion)
        speech = SpeechStore(bridge.speaker)
        // The type scale is read off the shared module once, here, rather than looked up
        // per view.
        DuskType.design = bridge.design
    }

    deinit {
        bridge.close()
    }
}
