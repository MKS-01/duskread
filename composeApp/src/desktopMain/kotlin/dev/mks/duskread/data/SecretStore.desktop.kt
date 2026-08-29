package dev.mks.duskread.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Delegates to [KeyValueStore], which puts the value in the `~/.algoatlas/prefs.properties` file, in the clear.
 *
 * See [PlaintextSecretStore]: this target has no way to enter a token, so
 * nothing reaches here today. It exists so the shared Notion code compiles,
 * and must be replaced before this target grows a Settings entry for one.
 */
@Composable
actual fun rememberSecretStore(): SecretStore {
    val store = rememberKeyValueStore()
    return remember(store) { PlaintextSecretStore(store) }
}
