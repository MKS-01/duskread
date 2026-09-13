package dev.mks.duskread.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Delegates to [KeyValueStore], which puts the value in `NSUserDefaults`, in the clear.
 */
@Composable
actual fun rememberSecretStore(): SecretStore {
    val store = rememberKeyValueStore()
    return remember(store) { PlaintextSecretStore(store) }
}
