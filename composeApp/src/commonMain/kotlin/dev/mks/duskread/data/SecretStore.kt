package dev.mks.duskread.data

import androidx.compose.runtime.Composable

/**
 * Where a credential goes, as opposed to where a preference goes.
 *
 * [KeyValueStore] is deliberately plaintext — it holds a name, a theme flag
 * and a list of URLs, none of which is worth encrypting. A Notion token is a
 * different kind of value: it grants access to a whole workspace, it is
 * long-lived, and it is the first thing in this app that would genuinely
 * matter if it leaked. Giving it its own interface means it can never be
 * written to the shared preferences file by accident, and means the storage
 * can be hardened on one platform without the callers knowing.
 *
 * Only Android implements this properly, because Android is the only target
 * with a Settings screen to enter a token into and a hardware-backed keystore
 * to protect it with. The others delegate to [KeyValueStore] and say so in
 * their own KDoc rather than pretending otherwise.
 *
 * Kept to two methods on purpose: a credential is written once and read on
 * every request, and anything else here would be a feature nobody asked for.
 */
interface SecretStore {
    fun get(key: String): String?

    /** Passing null removes the secret — this is what a disconnect calls. */
    fun put(key: String, value: String?)
}

/**
 * Composable for the same reason as [rememberKeyValueStore]: Android needs
 * the local `Context` to reach both its preferences file and its keystore,
 * and that is only available from composition.
 */
@Composable
expect fun rememberSecretStore(): SecretStore

/** The Notion bearer token — a personal access token today, an OAuth grant later. */
const val NotionTokenKey = "notion.token"

/**
 * The fallback: a [SecretStore] that is not one.
 *
 * Used by desktop, iOS and Wasm, where the token would land in a home-directory
 * `.properties` file, `NSUserDefaults` and `localStorage` respectively — all
 * plaintext. That is stated here rather than hidden behind the interface,
 * because the failure mode of a security abstraction is someone trusting it.
 *
 * It is acceptable only because those targets have no way to enter a token
 * today: Settings' Notion section is reachable on Android, and this exists so
 * the shared code compiles everywhere rather than to be relied on. Any target
 * that grows a real entry point needs a real implementation first.
 */
internal class PlaintextSecretStore(private val store: KeyValueStore) : SecretStore {
    override fun get(key: String): String? = store.getString(key)

    override fun put(key: String, value: String?) = store.putString(key, value)
}
