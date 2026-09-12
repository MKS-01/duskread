import ComposeApp
import SwiftUI

/// Owns the Kotlin side for the lifetime of the app.
///
/// The bridge builds the whole object graph on construction — libraries, the
/// HTTP client, the Notion client — so there must be exactly one and it must
/// outlive any view. Holding it in a `StateObject` on the `App` is what
/// guarantees both; a view-scoped one would quietly open a second connection
/// pool on every navigation.
///
/// Storage is handed in from here rather than chosen by Kotlin, which is what
/// puts the token in the Keychain instead of preferences.
final class DuskReadHost: ObservableObject {
    let bridge: DuskReadBridge
    let links: LinksStore
    let prefs: PrefsStore

    init() {
        bridge = DuskReadBridge(
            store: UserDefaultsStore(),
            secrets: KeychainSecretStore()
        )
        links = LinksStore(bridge.links)
        prefs = PrefsStore(bridge.prefs)
        // The type scale is read off the shared module once, here, rather than
        // looked up per view.
        DuskType.design = bridge.design
    }

    deinit {
        bridge.close()
    }
}
