package dev.mks.duskread.data

import androidx.compose.runtime.Composable

/**
 * Where a credential goes, as opposed to where a preference goes.
 */
interface SecretStore {
    fun get(key: String): String?

    /** Passing null removes the secret — this is what a disconnect calls. */
    fun put(key: String, value: String?)
}

/**
 * Composable for the same reason as [rememberKeyValueStore]: Android needs the local
 * `Context` to reach both its preferences file and its keystore.
 */
@Composable
expect fun rememberSecretStore(): SecretStore

/** The Notion bearer token — a personal access token today, an OAuth grant later. */
const val NotionTokenKey = "notion.token"

/**
 * The fallback: a [SecretStore] that is not one.
 */
internal class PlaintextSecretStore(private val store: KeyValueStore) : SecretStore {
    override fun get(key: String): String? = store.getString(key)

    override fun put(key: String, value: String?) = store.putString(key, value)
}
