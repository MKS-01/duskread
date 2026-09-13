import ComposeApp
import Foundation
import Security

/// `SecretStore` backed by the Keychain.
final class KeychainSecretStore: SecretStore {
    private let service: String

    init(service: String = Bundle.main.bundleIdentifier ?? "dev.mks.duskread") {
        self.service = service
    }

    func get(key: String) -> String? {
        var query = baseQuery(key)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var item: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
              let data = item as? Data,
              let value = String(data: data, encoding: .utf8),
              !value.isEmpty
        else { return nil }
        return value
    }

    func put(key: String, value: String?) {
        let query = baseQuery(key)

        // Delete-then-add rather than SecItemUpdate: an update against a missing item
        // fails, so the add path would need writing anyway.
        SecItemDelete(query as CFDictionary)

        guard let value, let data = value.data(using: .utf8) else { return }

        var insert = query
        insert[kSecValueData as String] = data
        insert[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        SecItemAdd(insert as CFDictionary, nil)
    }

    private func baseQuery(_ key: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
    }
}
