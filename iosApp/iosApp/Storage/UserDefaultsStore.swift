import ComposeApp
import Foundation

/// `KeyValueStore` backed by `UserDefaults`. The shared module declares the port and iOS
/// supplies the adapter, rather than Kotlin reaching for `NSUserDefaults` itself.
final class UserDefaultsStore: KeyValueStore {
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func getString(key: String) -> String? {
        defaults.string(forKey: key)
    }

    func putString(key: String, value: String?) {
        if let value {
            defaults.set(value, forKey: key)
        } else {
            defaults.removeObject(forKey: key)
        }
    }

    func getBoolean(key: String, fallback: Bool) -> Bool {
        guard let raw = defaults.string(forKey: key) else { return fallback }
        switch raw {
        case "true": return true
        case "false": return false
        default: return fallback
        }
    }

    func putBoolean(key: String, value: Bool) {
        defaults.set(value ? "true" : "false", forKey: key)
    }
}
